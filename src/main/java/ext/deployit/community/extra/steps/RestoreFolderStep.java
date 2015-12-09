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
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overtherepy.DirectoryChangeSet;
import com.xebialabs.overtherepy.DirectoryDiff;

import ext.deployit.community.extra.steps.action.ActionBuilder;

import static java.lang.String.format;

@StepMetadata(name = "restore-folder")
public class RestoreFolderStep extends BaseArtifactStep {

    @StepParameter(name = "from-folder", description = "the source directory on the remote-host")
    private String fromFolder;

    @RulePostConstruct
    public void postContruct(StepPostConstructContext ctx) {
        doConfigure(ctx);

        if (order == 0) {
            order = 40;
        }

        if (description.isEmpty()) {
            description = String.format("Restore '%s' folder to '%s'", fromFolder, getTargetPath());
        }
    }

    @Override
    public StepExitCode execute(final ExecutionContext executionContext) throws Exception {
        try (OverthereConnection connection = getTargetHost().getConnection()) {
            analyze(connection).execute(executionContext);
            return StepExitCode.SUCCESS;
        }
    }

    private ActionBuilder analyze(final OverthereConnection connection) throws Exception {
        ActionBuilder actions = new ActionBuilder();
        final OverthereFile remoteTargetPath = connection.getFile(getTargetPath());
        final OverthereFile fromTargetPath = connection.getFile(fromFolder);

        DirectoryDiff diff = new DirectoryDiff(fromTargetPath, remoteTargetPath);
        final DirectoryChangeSet changeSet = diff.diff();

        if (changeSet.getChanged().size() > 0) {
            actions.systemOut("Restore modified files...");
            for (OverthereFile f : changeSet.getChanged()) {
                final String pathPrefix = stringPathPrefix(f, remoteTargetPath.getPath());
                OverthereFile sourceFile = fromTargetPath.getFile(pathPrefix);
                actions.systemOut(format("Restore  file %s -> %s", sourceFile.getPath(), f.getPath()));
                actions.mkdirs(sourceFile.getParentFile());
                actions.copyTo(sourceFile, f);
            }
            actions.systemOut("Restore modified files done.");
        }

        return actions;
    }

    protected String stringPathPrefix(final OverthereFile file, final String prefix) {
        final String path = file.getPath();
        final int path_length = path.length();
        final int prefix_length = prefix.length();
        final String relativePath = path.substring(prefix_length, path_length);
        return relativePath.replace('\\', '/');
    }

}
