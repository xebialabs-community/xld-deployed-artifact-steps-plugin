/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.xebialabs.overthere.OverthereFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Compares 2 directories and determines which files were added, removed or changed.
 */
public class DirectoryDiff {

    private final boolean parallelStream;
    private final OverthereFile leftSide;
    private final OverthereFile rightSide;

    public DirectoryDiff(OverthereFile leftSide, OverthereFile rightSide) {
        this(leftSide, rightSide, false);
    }

    /**
     * Constructor
     *
     * @param leftSide  directory to compare
     * @param rightSide directory to compare
     */
    public DirectoryDiff(OverthereFile leftSide, OverthereFile rightSide, boolean parallelStream) {
        checkArgument(leftSide.isDirectory(), "File [%s] must be a directory.", leftSide);
        checkArgument(rightSide.isDirectory(), "File [%s] must be a directory.", rightSide);
        this.leftSide = leftSide;
        this.rightSide = rightSide;
        this.parallelStream = parallelStream;
    }

    public OverthereFile getLeftSide() {
        return leftSide;
    }

    public OverthereFile getRightSide() {
        return rightSide;
    }

    /**
     * Calculate the differences between the two directories that this class was constructed with.
     *
     * @return differences
     * @throws IOException
     */
    public DirectoryChangeSet diff() throws IOException {
        DirectoryChangeSet changeSet = new DirectoryChangeSet();
        compareDirectoryRecursive(leftSide, rightSide, changeSet);
        changeSet.process(parallelStream);
        return changeSet;
    }


    /**
     * Intermediate method for recursion, so that objects created in the compareDirectory method can be
     * garbage collected.
     */
    private void compareDirectoryRecursive(OverthereFile left, OverthereFile right, DirectoryChangeSet changeSet) throws IOException {
        List<OverthereFile[]> dirsToRecurse = compareDirectory(left, right, changeSet);
        for (OverthereFile[] leftAndRightDir : dirsToRecurse) {
            compareDirectoryRecursive(leftAndRightDir[0], leftAndRightDir[1], changeSet);
        }
    }

    private List<OverthereFile[]> compareDirectory(OverthereFile left, OverthereFile right, DirectoryChangeSet changeSet) throws IOException {
        Set<FileWrapper> leftFiles = listFiles(left);
        Set<FileWrapper> rightFiles = listFiles(right);

        //find new files
        Set<FileWrapper> filesAdded = Sets.difference(rightFiles, leftFiles);
        //find removed files
        Set<FileWrapper> filesRemoved = Sets.difference(leftFiles, rightFiles);

        //find changed files
        Set<FileWrapper> potentialChangedFiles = newHashSet(leftFiles);
        potentialChangedFiles.removeAll(filesRemoved);

        //filter out directories
        Map<FileWrapper, FileWrapper> rightFilesIndex = newHashMap();
        for (FileWrapper file : rightFiles) {
            rightFilesIndex.put(file, file);
        }

        Set<FileWrapper> potentialChangedFilesSet = Sets.filter(potentialChangedFiles, FileWrapperPredicates.FILE);
        for (FileWrapper potentialChangedFile : potentialChangedFilesSet) {
            FileWrapper rightFile = rightFilesIndex.get(potentialChangedFile);
            changeSet.addChangedFiles(rightFile, potentialChangedFile);
        }

        changeSet.addAddedFiles(filesAdded);
        changeSet.addRemovedFiles(filesRemoved);

        Set<FileWrapper> potentialChangedDirectories = Sets.filter(potentialChangedFiles, FileWrapperPredicates.DIRECTORY);
        List<OverthereFile[]> directoriesStillToCheck = newArrayList();
        for (FileWrapper potentialChangedDirectory : potentialChangedDirectories) {
            directoriesStillToCheck.add(new OverthereFile[]{potentialChangedDirectory.getFile(), rightFilesIndex.get(potentialChangedDirectory).getFile()});
        }

        return directoriesStillToCheck;
    }

    private Set<FileWrapper> listFiles(OverthereFile dir) {
        return dir.listFiles().stream().map(file -> new FileWrapper(file)).collect(Collectors.toSet());
    }

    enum FileWrapperPredicates implements Predicate<FileWrapper> {
        FILE {
            @Override
            public boolean apply(final FileWrapper input) {
                return input.getFile().isFile();
            }
        },
        DIRECTORY {
            @Override
            public boolean apply(final FileWrapper input) {
                return input.getFile().isDirectory();
            }
        }
    }

}
