/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.StepExitCode;
import com.xebialabs.deployit.plugin.api.rules.RulePostConstruct;
import com.xebialabs.deployit.plugin.api.rules.Scope;
import com.xebialabs.deployit.plugin.api.rules.StepMetadata;
import com.xebialabs.deployit.plugin.api.rules.StepPostConstructContext;
import com.xebialabs.deployit.plugin.api.udm.Container;
import com.xebialabs.deployit.plugin.api.udm.Deployable;
import com.xebialabs.deployit.plugin.api.udm.Deployed;
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
            description = format("Upload '%s' to '%s'", defaultArtifact(ctx).getName(), defaultTargetContainer(ctx).getName());
        }
        if (order == 0) {
            order = 60;
        }
    }

    protected Deployed<? extends Deployable, ? extends Container> defaultDeployed(final StepPostConstructContext ctx) {
        if (ctx.getScope() == Scope.DEPLOYED) {
            return ctx.getDelta().getDeployed();
        }
        throw new RuntimeException("delete-artifact step can be used only using the 'deployed' scope");
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
                com.xebialabs.overtherepy.DirectoryDiff diff = new DirectoryDiff(remoteTargetPath, artifactFile);
                final DirectoryDiff.DirectoryChangeSet changeSet = diff.diff();
                ctx.logOutput(format("%d files to be removed.", changeSet.getRemoved().size()));
                ctx.logOutput(format("%d new files to be copied.", changeSet.getAdded().size()));
                ctx.logOutput(format("%d modified files to be copied.", changeSet.getChanged().size()));

                if (changeSet.getRemoved().size() > 0) {
                    ctx.logOutput("Start removal of files...");
                    for (OverthereFile f : changeSet.getRemoved()) {
                        OverthereFile removedFile = remoteTargetPath.getFile(stringPathPrefix(f, getTargetPath()));
                        String fileType = (f.isDirectory() ? "directory" : "file");
                        if (removedFile.exists()) {
                            ctx.logOutput(format("Removing %s %s", fileType, removedFile.getPath()));
                            removedFile.deleteRecursively();
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
