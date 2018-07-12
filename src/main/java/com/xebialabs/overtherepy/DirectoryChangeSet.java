/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import com.xebialabs.overthere.OverthereFile;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;


public class DirectoryChangeSet {
    private List<OverthereFile> removed = newArrayList();
    private List<OverthereFile> added = newArrayList();
    private List<OverthereFile> changed = newArrayList();

    private List<FileWrapper> wrappedAdded = newArrayList();
    private List<FileWrapper> wrappedRemoved = newArrayList();
    private List<FileWrapper> rightWrappedChanged = newArrayList();
    private List<FileWrapper> leftWrappedChanged = newArrayList();


    public List<OverthereFile> getAdded() {
        return added;
    }

    public List<OverthereFile> getChanged() {
        return changed;
    }

    public List<OverthereFile> getRemoved() {
        return removed;
    }


    public void addAddedFiles(Collection<? extends FileWrapper> wrappedFile) {
        wrappedAdded.addAll(wrappedFile);
    }

    public void addRemovedFiles(Collection<? extends FileWrapper> wrappedFile) {
        wrappedRemoved.addAll(wrappedFile);
    }

    public void addChangedFiles(FileWrapper right, FileWrapper left) {
        rightWrappedChanged.add(right);
        leftWrappedChanged.add(left);
    }


    public void process(boolean parallelStream) {
        added.clear();
        removed.clear();
        changed.clear();

        added.addAll(wrappedAdded.stream().map(file -> file.getFile()).collect(Collectors.toList()));
        removed.addAll(wrappedRemoved.stream().map(file -> file.getFile()).collect(Collectors.toList()));

        if (parallelStream) {
            rightWrappedChanged.parallelStream().forEach(file -> file.getHashCode());
            leftWrappedChanged.parallelStream().forEach(file -> file.getHashCode());
        }else {
            rightWrappedChanged.forEach(file -> file.getHashCode());
            leftWrappedChanged.forEach(file -> file.getHashCode());
        }

        for (int i = 0; i < rightWrappedChanged.size(); i++) {
            FileWrapper right = rightWrappedChanged.get(i);
            FileWrapper left = leftWrappedChanged.get(i);
            if (!right.getHashCode().equals(left.getHashCode())) {
                changed.add(right.getFile());
            }
        }
    }
}
