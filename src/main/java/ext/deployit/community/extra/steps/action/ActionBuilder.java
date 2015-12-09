/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps.action;


import java.util.List;
import com.google.common.collect.Lists;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.Preview;
import com.xebialabs.overthere.OverthereFile;

import ext.deployit.community.extra.steps.Action;

public class ActionBuilder {

    private List<Action> actions = Lists.newArrayList();

    public void systemOut(String message) {
        actions.add(new SystemOut(message));
    }

    public void mkdirs(final OverthereFile remoteTargetPath) {
        actions.add(new MkDirs(remoteTargetPath));
    }

    public void delete(final OverthereFile remoteFile) {
        actions.add(new Delete(remoteFile));
    }

    public void copyTo(final OverthereFile artifactFile, final OverthereFile remoteFile) {
        actions.add(new CopyTo(artifactFile, remoteFile));
    }

    public void deleteRecursively(final OverthereFile removedFile) {
        actions.add(new DeleteRecursively(removedFile));
    }

    public void markAdded(final OverthereFile backupRemoteFolder, final OverthereFile addedFile) {
        actions.add(new Added(backupRemoteFolder, addedFile));
    }

    public void execute(final ExecutionContext ctx) {
        for (Action action : actions) {
            action.execute(ctx);
        }
    }

    public Preview preview() {
        StringBuffer sb = new StringBuffer();
        for (Action action : actions) {
            sb.append(action.preview()).append("\n");
        }
        return Preview.withContents(sb.toString());
    }


}
