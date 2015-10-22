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
import com.xebialabs.deployit.plugin.api.rules.StepPostConstructContext;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overtherepy.DirectoryDiff;

import static java.lang.String.format;

@StepMetadata(name = "upload-artifact")
public class UploadArtifactStep extends BaseArtifactStep {

    @RulePostConstruct
    public void postContruct(StepPostConstructContext ctx) {
        doConfigure(ctx);
        if (description.isEmpty()) {
            description = format("Upload '%s' to '%s'", getArtifact().getName(), getTargetHost().getName());
        }
        if (order == 0) {
            order = 60;
        }
    }

    public StepExitCode execute(ExecutionContext ctx) throws Exception {
        try (OverthereConnection connection = getTargetHost().getConnection()) {
            final OverthereFile remoteTargetPath = connection.getFile(getTargetPath());
            if (!remoteTargetPath.exists()) {
                ctx.logOutput("Remote path " + getTargetPath() + " does not exists, create it");
                remoteTargetPath.mkdirs();
            }

            final OverthereFile artifactFile = getArtifact().getFile();
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
                    if (isSharedTarget() && getPreviousArtifact() != null) {
                        previousChangeSet = new DirectoryDiff(remoteTargetPath, getPreviousArtifact().getFile()).diff();
                        ctx.logOutput(format("Shared options is on and we have a previous artifact"));
                        ctx.logOutput(format("previous %d files to be removed:    %s", previousChangeSet.getRemoved().size(), previousChangeSet.getRemoved()));
                        ctx.logOutput(format("previous %d new files to be copied: %s", previousChangeSet.getAdded().size(), previousChangeSet.getAdded()));
                        ctx.logOutput(format("previous %d modified files to be copied.", previousChangeSet.getChanged().size()));
                        ctx.logOutput(format("/Previous Change Set....."));
                    }

                    for (OverthereFile f : changeSet.getRemoved()) {
                        OverthereFile removedFile = remoteTargetPath.getFile(stringPathPrefix(f, getTargetPath()));
                        String fileType = (f.isDirectory() ? "directory" : "file");
                        if (removedFile.exists()) {
                            if (isSharedTarget() && previousChangeSet.getRemoved().contains(f)) {
                                ctx.logOutput(format("Skipping %s %s", fileType, removedFile.getPath()));
                            } else {
                                ctx.logOutput(format("Removing %s %s", fileType, removedFile.getPath()));
                                removedFile.deleteRecursively();
                            }
                        } else {
                            ctx.logOutput(format("File %s does not exist. Ignoring.", removedFile.getPath()));
                        }
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


}
