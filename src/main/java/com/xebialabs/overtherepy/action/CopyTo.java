/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy.action;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.overthere.OverthereFile;


public class CopyTo extends BaseAction {

    private final OverthereFile artifactFile;
    private final OverthereFile remoteFile;

    public CopyTo(final OverthereFile artifactFile, final OverthereFile remoteFile) {
        this.artifactFile = artifactFile;
        this.remoteFile = remoteFile;
    }
    @Override
    public void execute(final ExecutionContext ctx) {
        artifactFile.copyTo(remoteFile);
    }
}
