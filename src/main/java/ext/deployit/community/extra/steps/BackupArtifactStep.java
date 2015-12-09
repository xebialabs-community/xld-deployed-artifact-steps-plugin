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
import com.xebialabs.overtherepy.DirectoryChangeSet;
import com.xebialabs.overtherepy.DirectoryDiff;

import ext.deployit.community.extra.steps.action.ActionBuilder;

import static java.lang.String.format;

@StepMetadata(name = "backup-artifact")
public class BackupArtifactStep extends BaseArtifactStep implements PreviewStep {

    @StepParameter(name = "artifact", description = "the future uploaded  artifact.", calculated = true)
    private Artifact artifact;

    @StepParameter(name = "backup-folder", description = "the backup directory on the remote-host")
    private String backupFolder;

    @RulePostConstruct
    public void postContruct(StepPostConstructContext ctx) {
        doConfigure(ctx);

        if (artifact == null) {
            artifact = defaultArtifact(ctx);
        }

        if (order == 0) {
            order = 40;
        }

        if (description.isEmpty()) {
            final Deployed<?, ?> deployedOrPrevious = getDeployedOrPrevious(ctx.getDelta());
            description = String.format("Backup '%s' from '%s'", deployedOrPrevious.getName(), deployedOrPrevious.getContainer().getName());
        }
    }


    @Override
    public StepExitCode execute(final ExecutionContext ctx) throws Exception {
        try (OverthereConnection connection = getTargetHost().getConnection()) {
            analyze(connection).execute(ctx);
            return StepExitCode.SUCCESS;
        }
    }


    private Artifact defaultArtifact(StepPostConstructContext ctx) {
        final Deployed<? extends Deployable, ? extends Container> deployed = ctx.getDelta().getDeployed();
        return deployed instanceof Artifact ? (Artifact) deployed : null;
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

    private ActionBuilder analyze(final OverthereConnection connection) throws Exception {
        ActionBuilder actions = new ActionBuilder();

        final OverthereFile remoteTargetPath = connection.getFile(getTargetPath());
        if (!remoteTargetPath.exists()) {
            actions.systemOut("Remote path " + getTargetPath() + " does not exists, skip");
            return actions;
        }

        final OverthereFile artifactFile = artifact.getFile();
        if (artifactFile.isFile()) {
            actions.systemOut("Backup Step is not available for files.");
            return actions;
        }

        //Directory
        final OverthereFile backupRemoteFolder = connection.getFile(backupFolder);
        if (!backupRemoteFolder.exists()) {
            actions.systemOut("Create remote backup folder " + backupFolder);
            actions.mkdirs(backupRemoteFolder);
        }

        DirectoryDiff diff = new DirectoryDiff(remoteTargetPath, artifactFile);
        final DirectoryChangeSet changeSet = diff.diff();
        actions.systemOut(format("%d modified files to be back up.", changeSet.getChanged().size()));
        final String artifactFilePath = artifactFile.getPath();

        if (changeSet.getChanged().size() > 0) {
            actions.systemOut("Backup modified files...");
            for (OverthereFile f : changeSet.getChanged()) {
                final String pathPrefix = stringPathPrefix(f, artifactFilePath);
                OverthereFile changedFile = remoteTargetPath.getFile(pathPrefix);
                OverthereFile backupFile = backupRemoteFolder.getFile(pathPrefix);
                actions.systemOut(format("Backup  file %s -> %s", changedFile.getPath(), backupFile.getPath()));
                actions.mkdirs(backupFile.getParentFile());
                actions.copyTo(changedFile, backupFile);
            }
            actions.systemOut("Backup modified files done.");
        }

        if (changeSet.getAdded().size() > 0) {
            actions.systemOut("Mark added files...");
            for (OverthereFile f : changeSet.getAdded()) {
                final String pathPrefix = stringPathPrefix(f, artifactFilePath);
                OverthereFile addedFile = remoteTargetPath.getFile(pathPrefix);
                actions.systemOut(format("Mark %s as added ", addedFile.getPath()));
                actions.markAdded(backupRemoteFolder, addedFile);
            }
            actions.systemOut("Mark added files done...");

        }
        return actions;
    }
}
