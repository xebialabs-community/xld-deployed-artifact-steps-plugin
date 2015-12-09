/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps.action;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.overthere.OverthereFile;


public class Added extends BaseAction {

    private final OverthereFile artifactFile;
    private final OverthereFile directory;

    public Added(final OverthereFile directory, final OverthereFile artifactFile) {
        this.directory = directory;
        this.artifactFile = artifactFile;
    }

    @Override
    public void execute(final ExecutionContext ctx) {
        Properties added = new Properties();
        try {
            final OverthereFile addedFile = getAddedFile();
            if (addedFile.exists()) {
                final InputStream inputStream = addedFile.getInputStream();
                added.load(inputStream);
                inputStream.close();
            }
            added.setProperty(artifactFile.getPath(), "ADD");
            added.store(addedFile.getOutputStream(), "");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private OverthereFile getAddedFile() {
        return directory.getFile("added.txt");
    }
}
