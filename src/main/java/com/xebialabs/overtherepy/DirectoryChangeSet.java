/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import java.util.List;

import com.xebialabs.overthere.OverthereFile;

import static com.google.common.collect.Lists.newArrayList;


public class DirectoryChangeSet {
    private List<OverthereFile> removed = newArrayList();
    private List<OverthereFile> added = newArrayList();
    private List<OverthereFile> changed = newArrayList();

    public List<OverthereFile> getAdded() {
        return added;
    }

    public List<OverthereFile> getChanged() {
        return changed;
    }

    public List<OverthereFile> getRemoved() {
        return removed;
    }
}
