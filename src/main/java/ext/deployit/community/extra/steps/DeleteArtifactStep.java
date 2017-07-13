/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
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

@StepMetadata(name = "delete-artifact")
public class DeleteArtifactStep extends BaseArtifactStep {

    @StepParameter(name = "previousArtifact", description = "Previous deployed artifact.", calculated = true)
    private Artifact previousArtifact;

    @RulePostConstruct
    public void postContruct(StepPostConstructContext ctx) {
        doConfigure(ctx);

        if (previousArtifact == null) {
            previousArtifact = defaultPreviousArtifact(ctx);
        }

        if (order == 0) {
            order = 30;
        }

        if (description.isEmpty()) {
            final Deployed<?, ?> deployedOrPrevious = getDeployedOrPrevious(ctx.getDelta());
            description = String.format("Delete '%s' from '%s'", deployedOrPrevious.getName(), deployedOrPrevious.getContainer().getName());
        }
    }

    public StepExitCode execute(ExecutionContext ctx) throws Exception {
        try (OverthereConnection connection = getTargetHost().getConnection()) {
            final OverthereFile remoteTargetPath = connection.getFile(getTargetPath());
            if (!remoteTargetPath.exists()) {
                ctx.logOutput("Remote path " + getTargetPath() + " does not exists, skip");
                return StepExitCode.SUCCESS;
            }

            final OverthereFile artifactFile = previousArtifact.getFile();
            if (artifactFile.isFile()) {
                final OverthereFile remoteFile = connection.getFile(remoteTargetPath, artifactFile.getName());
                if (remoteFile.exists()) {
                    ctx.logOutput("Delete " + remoteFile);
                    remoteFile.delete();
                } else {
                    ctx.logOutput(remoteFile + "does not exist, skip");
                }
            }
            if (artifactFile.isDirectory()) {
                deleteRemoteDirectory(artifactFile, remoteTargetPath, ctx);
            }
            return StepExitCode.SUCCESS;
        }
    }


    private void deleteRemoteDirectory(final OverthereFile localFile, final OverthereFile remoteDir, ExecutionContext ctx) {
        for (OverthereFile file : localFile.listFiles()) {
            OverthereFile remoteFile = remoteDir.getFile(file.getName());
            if (file.isDirectory()) {
                if (remoteFile.exists()) {
                    deleteRemoteDirectory(file, remoteFile, ctx);
                    if (remoteFile.listFiles().isEmpty()) {
                        deleteFile(remoteFile, ctx);
                    }
                } else {
                    ctx.logOutput(remoteFile.getPath() + " does not exist on host. Will not perform delete.");
                }
            } else {
                deleteFile(remoteFile, ctx);
            }
        }
    }

    private void deleteFile(final OverthereFile file, ExecutionContext ctx) {
        ctx.logOutput("Deleting " + file.getPath());
        if (file.exists()) {
            file.deleteRecursively();
        } else {
            ctx.logOutput(file.getPath() + " does not exist on host. Will not perform delete.");
        }
    }

    private Artifact defaultPreviousArtifact(StepPostConstructContext ctx) {
        final Deployed<? extends Deployable, ? extends Container> deployed = ctx.getDelta().getPrevious();
        return deployed instanceof Artifact ? (Artifact) deployed : null;
    }

}
