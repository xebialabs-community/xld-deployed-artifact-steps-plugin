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
import com.xebialabs.overtherepy.DirectoryDiff;

import static java.lang.String.format;

@StepMetadata(name = "upload-artifact")
public class UploadArtifactStep extends BaseArtifactStep {

    @StepParameter(name = "artifact", description = "Artifact that has been uploaded to the target host.")
    private Artifact artifact;

    @StepParameter(name = "previousArtifact", description = "Previous deployed artifact.", calculated = true, required = false)
    private Artifact previousArtifact;

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
            final OverthereFile remoteTargetPath = connection.getFile(getTargetPath());
            if (!remoteTargetPath.exists()) {
                ctx.logOutput("Remote path " + getTargetPath() + " does not exists, create it");
                remoteTargetPath.mkdirs();
            }

            final OverthereFile artifactFile = artifact.getFile();
            final String artifactFilePath = artifactFile.getPath();
            if (artifactFile.isFile()) {
                ctx.logOutput("Artifact: file");
                final OverthereFile remoteFile = connection.getFile(remoteTargetPath, artifactFile.getName());
                if (remoteFile.exists()) {
                    ctx.logError(format("Remote file '%s' exists, it will be removed", remoteFile.getPath()));
                    remoteFile.delete();
                }
                ctx.logOutput(format("Copy %s -> %s", artifactFilePath, remoteFile.getPath()));
                artifactFile.copyTo(remoteFile);
            }


            if (artifactFile.isDirectory()) {
                ctx.logOutput("Artifact: Folder");
                DirectoryDiff diff = new DirectoryDiff(remoteTargetPath, artifactFile);
                final DirectoryDiff.DirectoryChangeSet changeSet = diff.diff();
                ctx.logOutput(format("%d files to be removed.", changeSet.getRemoved().size()));
                ctx.logOutput(format("%d new files to be copied.", changeSet.getAdded().size()));
                ctx.logOutput(format("%d modified files to be copied.", changeSet.getChanged().size()));


                if (changeSet.getRemoved().size() > 0) {
                    ctx.logOutput("Start removal of files...");
                    DirectoryDiff.DirectoryChangeSet previousChangeSet = null;
                    if (isSharedTarget() && previousArtifact != null) {
                        previousChangeSet = new DirectoryDiff(remoteTargetPath, previousArtifact.getFile()).diff();
                        ctx.logOutput(format("Shared options is on and we have a previous artifact"));
                        ctx.logOutput(format("%d file(s) not managed by this artifact, should be skipped: %s", previousChangeSet.getRemoved().size(), previousChangeSet.getRemoved()));
                        ctx.logOutput(format("/Previous Change Set....."));
                    }

                    for (OverthereFile f : changeSet.getRemoved()) {
                        OverthereFile removedFile = remoteTargetPath.getFile(stringPathPrefix(f, getTargetPath()));
                        String fileType = (f.isDirectory() ? "directory" : "file");
                        if (!removedFile.exists()) {
                            ctx.logOutput(format("File %s does not exist. Ignoring.", removedFile.getPath()));
                            continue;
                        }
                        if (isSharedTarget() && previousArtifact != null && previousChangeSet.getRemoved().contains(f)) {
                            ctx.logOutput(format("Skipping (1) %s %s", fileType, removedFile.getPath()));
                            continue;
                        }
                        if (isSharedTarget() && previousArtifact == null) {
                            ctx.logOutput(format("Skipping (2) %s %s", fileType, removedFile.getPath()));
                            continue;
                        }

                        ctx.logOutput(format("Removing %s %s", fileType, removedFile.getPath()));
                        removedFile.deleteRecursively();
                    }
                    ctx.logOutput("Removal of files done.");
                }


                if (changeSet.getAdded().size() > 0) {
                    ctx.logOutput("Start copying of new files...");
                    for (OverthereFile f : changeSet.getAdded()) {
                        OverthereFile addFile = remoteTargetPath.getFile(stringPathPrefix(f, artifactFilePath));
                        String fileType = "file";
                        if (f.isDirectory()) {
                            fileType = "directory";
                            if (!f.exists())
                                f.mkdirs();
                        } else {
                            if (!addFile.getParentFile().exists()) {
                                addFile.getParentFile().mkdirs();
                            }
                        }
                        ctx.logOutput(format("Copying %s %s", fileType, addFile.getPath()));
                        f.copyTo(addFile);
                    }
                    ctx.logOutput("Copying of new files done.");
                }

                if (changeSet.getChanged().size() > 0) {
                    ctx.logOutput("Start copying of modified files...");
                    for (OverthereFile f : changeSet.getChanged()) {
                        OverthereFile changedFile = remoteTargetPath.getFile(stringPathPrefix(f, artifactFilePath));
                        ctx.logOutput(format("Updating file %s", changedFile.getPath()));
                        f.copyTo(changedFile);
                    }
                    ctx.logOutput("Copying of modified files done.");
                }

            }
            return StepExitCode.SUCCESS;
        }
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
