/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy.action;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.overthere.OverthereFile;


public class MkDirs extends BaseAction {

    private final OverthereFile remoteTargetPath;

    public MkDirs(final OverthereFile remoteTargetPath) {
        super();
        this.remoteTargetPath = remoteTargetPath;
    }

    @Override
    public void execute(final ExecutionContext ctx) {
        if (!remoteTargetPath.exists())
            remoteTargetPath.mkdirs();
    }
}
