/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.Preview;
import com.xebialabs.deployit.plugin.api.flow.PreviewStep;
import com.xebialabs.deployit.plugin.api.flow.StepExitCode;
import com.xebialabs.deployit.plugin.api.rules.RulePostConstruct;
import com.xebialabs.deployit.plugin.api.rules.StepMetadata;
import com.xebialabs.deployit.plugin.api.rules.StepParameter;
import com.xebialabs.deployit.plugin.api.rules.StepPostConstructContext;
import com.xebialabs.deployit.plugin.api.udm.Container;
import com.xebialabs.deployit.plugin.api.udm.Deployable;
import com.xebialabs.deployit.plugin.api.udm.Deployed;
import com.xebialabs.deployit.plugin.api.udm.artifact.Artifact;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overtherepy.DirectorySync;
import com.xebialabs.overtherepy.action.ActionBuilder;

import static java.lang.String.format;

@StepMetadata(name = "upload-artifact")
public class UploadArtifactStep extends BaseArtifactStep implements PreviewStep {

    @StepParameter(name = "artifact", description = "Artifact that has been uploaded to the target host.")
    private Artifact artifact;

    @StepParameter(name = "previousArtifact", description = "Previous deployed artifact.", calculated = true, required = false)
    private Artifact previousArtifact;

    @StepParameter(name = "uploadOnly", description = "Copy only the new and updated files, leave the missing files as is, default false", calculated = true)
    private boolean uploadOnly = false;


    @RulePostConstruct
    public void postContruct(StepPostConstructContext ctx) {
        doConfigure(ctx);
        if (order == 0) {
            order = 60;
        }

        if (artifact == null) {
            artifact = defaultArtifact(ctx);
        }

        if (previousArtifact == null) {
            previousArtifact = defaultPreviousArtifact(ctx);
        }

        if (description.isEmpty()) {
            final Deployed<?, ?> deployedOrPrevious = getDeployedOrPrevious(ctx.getDelta());
            description = format("Upload '%s' to '%s'", deployedOrPrevious.getName(), deployedOrPrevious.getContainer().getName());
        }
    }


    public StepExitCode execute(ExecutionContext ctx) throws Exception {
        try (OverthereConnection connection = getTargetHost().getConnection()) {
            ctx.logOutput("Analyze..." + getTargetPath());
            ActionBuilder analyze = analyze(connection);
            ctx.logOutput("Execute...");
            analyze.execute(ctx);
            return StepExitCode.SUCCESS;
        }
    }


    @Override
    public Preview getPreview() {
        try (OverthereConnection connection = getTargetHost().getConnection()) {
            return analyze(connection).preview();
        } catch (Exception e) {
            e.printStackTrace();
            return Preview.withContents(e.getMessage());
        }
    }


    public ActionBuilder analyze(OverthereConnection connection) throws Exception {
        ActionBuilder actions = new ActionBuilder();
        final OverthereFile remoteTargetPath = connection.getFile(getTargetPath());
        if (!remoteTargetPath.exists()) {
            actions.systemOut("Remote path " + getTargetPath() + " does not exists, create it");
            actions.mkdirs(remoteTargetPath);
        }

        final OverthereFile artifactFile = artifact.getFile();
        final String artifactFilePath = artifactFile.getPath();
        if (artifactFile.isFile()) {
            actions.systemOut("Artifact: file");
            final OverthereFile remoteFile = connection.getFile(remoteTargetPath, artifactFile.getName());
            if (remoteFile.exists()) {
                actions.systemOut(format("Remote file '%s' exists, it will be removed", remoteFile.getPath()));
                actions.delete(remoteFile);
            }
            actions.systemOut(format("Copy %s -> %s", artifactFilePath, remoteFile.getPath()));
            actions.copyTo(artifactFile, remoteFile);
        }


        if (artifactFile.isDirectory() && !remoteTargetPath.exists()) {
            actions.systemOut("Artifact: new Folder ");
            actions.systemOut(format("Copy %s -> %s", artifactFilePath, remoteTargetPath.getPath()));
            actions.copyTo(artifactFile, remoteTargetPath);
        }

        if (artifactFile.isDirectory() && remoteTargetPath.exists()) {
            actions.systemOut("Artifact: existing Folder");
            DirectorySync sync = new DirectorySync(remoteTargetPath, artifactFile, previousArtifact.getFile(), isSharedTarget());
            if (uploadOnly) {
                actions.addAll(sync.update().getActions());
            } else {
                actions.addAll(sync.sync().getActions());
            }
        }

        return actions;
    }

    private Artifact defaultArtifact(StepPostConstructContext ctx) {
        final Deployed<? extends Deployable, ? extends Container> deployed = ctx.getDelta().getDeployed();
        return deployed instanceof Artifact ? (Artifact) deployed : null;
    }

    private Artifact defaultPreviousArtifact(StepPostConstructContext ctx) {
        final Deployed<? extends Deployable, ? extends Container> deployed = ctx.getDelta().getPrevious();
        return deployed instanceof Artifact ? (Artifact) deployed : null;
    }


}
