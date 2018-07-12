/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overtherepy.action.ActionBuilder;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class DirectorySync {
    private final OverthereFile remoteTargetPath;
    private final OverthereFile artifactFile;
    private final OverthereFile previousArtifact;
    private final boolean sharedRemoteDirectory;
    private final boolean optimizedDiff;

    private DirectoryChangeSet changeSet;


    public DirectorySync(OverthereFile remoteTargetPath, OverthereFile artifactFile) {
        this(remoteTargetPath, artifactFile, null, false, false);
    }

    public DirectorySync(OverthereFile remoteTargetPath, OverthereFile artifactFile, OverthereFile previousArtifact, boolean sharedRemoteDirectory, boolean optimizedDiff) {
        this.remoteTargetPath = remoteTargetPath;
        this.artifactFile = artifactFile;
        this.previousArtifact = previousArtifact;
        this.sharedRemoteDirectory = sharedRemoteDirectory;
        this.optimizedDiff = optimizedDiff;
    }


    public ActionBuilder sync() throws IOException {
        final ActionBuilder actions = new ActionBuilder();
        actions.systemOut(format("Synchronize..."));

        DirectoryDiff diff = new DirectoryDiff(remoteTargetPath, artifactFile, optimizedDiff);
        actions.systemOut("Start Diff Analysis...");
        long start = System.currentTimeMillis();
        changeSet = diff.diff();
        long end = System.currentTimeMillis();
        actions.systemOut(format("End Diff Analysis...%d seconds", ((end - start) / 1000)));
        actions.systemOut(format("%d files to be removed.", changeSet.getRemoved().size()));
        actions.systemOut(format("%d new files to be copied.", changeSet.getAdded().size()));
        actions.systemOut(format("%d modified files to be changed.", changeSet.getChanged().size()));

        actions.addAll(remove());
        actions.addAll(copy());
        actions.addAll(change());

        return actions;
    }

    public ActionBuilder update() throws IOException {

        final ActionBuilder actions = new ActionBuilder();
        actions.systemOut(format("Update..."));

        DirectoryDiff diff = new DirectoryDiff(remoteTargetPath, artifactFile, optimizedDiff);
        actions.systemOut("Start Diff Analysis...");
        long start = System.currentTimeMillis();
        changeSet = diff.diff();
        long end = System.currentTimeMillis();
        actions.systemOut(format("End Diff Analysis...%d seconds", ((end - start) / 1000)));

        actions.systemOut(format("%d new files to be copied.", changeSet.getAdded().size()));
        actions.systemOut(format("%d modified files to be changed.", changeSet.getChanged().size()));

        actions.addAll(copy());
        actions.addAll(change());

        return actions;
    }

    private List<Action> change() {
        final ActionBuilder actions = new ActionBuilder();
        actions.systemOut("Start copying of modified files...");
        for (OverthereFile f : changeSet.getChanged()) {
            OverthereFile changedFile = remoteTargetPath.getFile(stringPathPrefix(f, artifactFile.getPath()));
            actions.systemOut(format("Updating file %s", changedFile.getPath()));
            actions.copyTo(f, changedFile);
        }
        actions.systemOut("Copying of modified files done.");
        return actions.getActions();
    }

    private List<Action> copy() {
        final ActionBuilder actions = new ActionBuilder();

        if (changeSet.getAdded().size() > 0) {
            actions.systemOut("Start copying of new files...");
            for (OverthereFile f : changeSet.getAdded()) {
                OverthereFile addFile = remoteTargetPath.getFile(stringPathPrefix(f, artifactFile.getPath()));
                String fileType = "file";
                if (f.isDirectory()) {
                    fileType = "directory";
                    if (!f.exists())
                        actions.mkdirs(f);
                } else {
                    if (addFile.getParentFile() != null && !addFile.getParentFile().exists()) {
                        actions.mkdirs(addFile.getParentFile());
                    }
                }
                actions.systemOut(format("Copying %s %s", fileType, addFile.getPath()));
                actions.copyTo(f, addFile);
            }
            actions.systemOut("Copying of new files done.");
        }

        return actions.getActions();
    }

    private List<Action> remove() throws IOException {
        final ActionBuilder actions = new ActionBuilder();

        if (changeSet.getRemoved().size() > 0) {
            actions.systemOut("Start removal of files...");
            DirectoryChangeSet previousChangeSet = null;
            if (sharedRemoteDirectory && previousArtifact != null) {
                actions.systemOut(format("Shared option is 'on' and have a previous artifact"));
                previousChangeSet = new DirectoryDiff(remoteTargetPath, previousArtifact, optimizedDiff).diff();
                actions.systemOut(format("%d file(s) not managed by this artifact, should be skipped: %s", previousChangeSet.getRemoved().size(), previousChangeSet.getRemoved()));
            }

            for (OverthereFile f : changeSet.getRemoved()) {
                OverthereFile removedFile = remoteTargetPath.getFile(stringPathPrefix(f, remoteTargetPath.getPath()));
                String fileType = (f.isDirectory() ? "directory" : "file");
                if (!removedFile.exists()) {
                    actions.systemOut(format("File %s does not exist. Ignoring.", removedFile.getPath()));
                    continue;
                }
                if (sharedRemoteDirectory && previousArtifact != null && previousChangeSet.getRemoved().contains(f)) {
                    actions.systemOut(format("Skipping (1) %s %s", fileType, removedFile.getPath()));
                    continue;
                }
                if (sharedRemoteDirectory && previousArtifact == null) {
                    actions.systemOut(format("Skipping (2) %s %s", fileType, removedFile.getPath()));
                    continue;
                }

                actions.systemOut(format("Removing %s %s", fileType, removedFile.getPath()));
                actions.deleteRecursively(removedFile);

            }
            actions.systemOut("Removal of files done.");
        }

        return actions.getActions();

    }

    private String stringPathPrefix(final OverthereFile file, final String prefix) {
        final String path = file.getPath();
        final int path_length = path.length();
        final int prefix_length = prefix.length();
        final String relativePath = path.substring(prefix_length + 1, path_length);
        return relativePath.replace('\\', '/');
    }


}
