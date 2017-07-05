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
import com.xebialabs.overtherepy.DirectoryChangeSet;
import com.xebialabs.overtherepy.DirectoryDiff;

import ext.deployit.community.extra.steps.action.ActionBuilder;

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
            analyze(connection).execute(ctx);
            return StepExitCode.SUCCESS;
        }
    }

    /*
    @Override
    public Preview getPreview() {
        try (OverthereConnection connection = getTargetHost().getConnection()) {
            return analyze(connection).preview();
        } catch (Exception e) {
            e.printStackTrace();
            return Preview.withContents(e.getMessage());
        }
    }
    */

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
            DirectoryDiff diff = new DirectoryDiff(remoteTargetPath, artifactFile);
            actions.systemOut("Start Diff Analysis...");
            long  start = System.currentTimeMillis();
            final DirectoryChangeSet changeSet = diff.diff();
            long  end = System.currentTimeMillis();
            actions.systemOut(format("End Diff Analysis...%d seconds",((end-start)/1000)));
            actions.systemOut(format("%d files to be removed.", changeSet.getRemoved().size()));
            actions.systemOut(format("%d new files to be copied.", changeSet.getAdded().size()));
            actions.systemOut(format("%d modified files to be copied.", changeSet.getChanged().size()));

            if (changeSet.getRemoved().size() > 0) {
                actions.systemOut("Start removal of files...");
                DirectoryChangeSet previousChangeSet = null;
                if (isSharedTarget() && previousArtifact != null) {
                    actions.systemOut(format("Shared option is 'on' and have a previous artifact"));
                    previousChangeSet = new DirectoryDiff(remoteTargetPath, previousArtifact.getFile()).diff();
                    actions.systemOut(format("%d file(s) not managed by this artifact, should be skipped: %s", previousChangeSet.getRemoved().size(), previousChangeSet.getRemoved()));
                }

                for (OverthereFile f : changeSet.getRemoved()) {
                    OverthereFile removedFile = remoteTargetPath.getFile(stringPathPrefix(f, getTargetPath()));
                    String fileType = (f.isDirectory() ? "directory" : "file");
                    if (!removedFile.exists()) {
                        actions.systemOut(format("File %s does not exist. Ignoring.", removedFile.getPath()));
                        continue;
                    }
                    if (isSharedTarget() && previousArtifact != null && previousChangeSet.getRemoved().contains(f)) {
                        actions.systemOut(format("Skipping (1) %s %s", fileType, removedFile.getPath()));
                        continue;
                    }
                    if (isSharedTarget() && previousArtifact == null) {
                        actions.systemOut(format("Skipping (2) %s %s", fileType, removedFile.getPath()));
                        continue;
                    }

                    actions.systemOut(format("Removing %s %s", fileType, removedFile.getPath()));
                    actions.deleteRecursively(removedFile);

                }
                actions.systemOut("Removal of files done.");
            }


            if (changeSet.getAdded().size() > 0) {
                actions.systemOut("Start copying of new files...");
                for (OverthereFile f : changeSet.getAdded()) {
                    OverthereFile addFile = remoteTargetPath.getFile(stringPathPrefix(f, artifactFilePath));
                    String fileType = "file";
                    if (f.isDirectory()) {
                        fileType = "directory";
                        if (!f.exists())
                            actions.mkdirs(f);
                    } else {
                        if (!addFile.getParentFile().exists()) {
                            actions.mkdirs(addFile.getParentFile());
                        }
                    }
                    actions.systemOut(format("Copying %s %s", fileType, addFile.getPath()));
                    actions.copyTo(f, addFile);
                }
                actions.systemOut("Copying of new files done.");
            }

            if (changeSet.getChanged().size() > 0) {
                actions.systemOut("Start copying of modified files...");
                for (OverthereFile f : changeSet.getChanged()) {
                    OverthereFile changedFile = remoteTargetPath.getFile(stringPathPrefix(f, artifactFilePath));
                    actions.systemOut(format("Updating file %s", changedFile.getPath()));
                    actions.copyTo(f, changedFile);
                }
                actions.systemOut("Copying of modified files done.");
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
