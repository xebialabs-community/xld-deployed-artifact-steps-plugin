/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import com.xebialabs.overthere.ConnectionOptions;
import com.xebialabs.overthere.Overthere;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overthere.util.OverthereUtils;
import org.junit.Test;

import java.io.IOException;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class DirectoryDiffTest {


    public OverthereFile content_A() {
        return getLocalConnection().getFile("src/test/resources/content/A");
    }

    public OverthereFile content_B() {
        return getLocalConnection().getFile("src/test/resources/content/B");
    }

    private OverthereConnection getLocalConnection() {
        ConnectionOptions options = new ConnectionOptions();
        options.set(ConnectionOptions.OPERATING_SYSTEM, "UNIX");
        return Overthere.getConnection("local", options);
    }

    private OverthereFile getTemporaryDirectory() {
        return getLocalConnection().getTempFile("test", "tst").getParentFile();
    }


    @Test
    public void testNewFolder() throws IOException {
        OverthereFile content = content_A();
        OverthereFile remoteDirectory = getTemporaryDirectory();
        DirectoryDiff diff = new DirectoryDiff(remoteDirectory, content);


        final DirectoryChangeSet changeSet = diff.diff();
        assertThat(2, is(equalTo(changeSet.getAdded().size())));
        assertThat(changeSet.getAdded(), hasItem(getLocalConnection().getFile("src/test/resources/content/A/Level1")));
        assertThat(changeSet.getAdded(), hasItem(getLocalConnection().getFile("src/test/resources/content/A/Level2")));
        assertThat(0, is(equalTo(changeSet.getRemoved().size())));
        assertThat(0, is(equalTo(changeSet.getChanged().size())));

        dump(changeSet);
    }

    @Test
    public void testSameFolder() throws IOException {
        OverthereFile content = content_A();
        OverthereFile remoteDirectory = getTemporaryDirectory();
        DirectoryDiff diff = new DirectoryDiff(content, content);


        final DirectoryChangeSet changeSet = diff.diff();
        assertThat(0, is(equalTo(changeSet.getAdded().size())));
        assertThat(0, is(equalTo(changeSet.getRemoved().size())));
        assertThat(0, is(equalTo(changeSet.getChanged().size())));

        dump(changeSet);

    }

    @Test
    public void testFolderWithOneMoreFile() throws IOException {
        OverthereFile content = content_A();
        OverthereFile remoteDirectory1 = getTemporaryDirectory();
        content.copyTo(remoteDirectory1);

        OverthereFile remoteDirectory2 = getTemporaryDirectory();
        content.copyTo(remoteDirectory2);
        OverthereFile file22c = remoteDirectory2.getConnection().getFile(remoteDirectory2, "Level2").getFile("Level22").getFile("22c.txt");
        OverthereUtils.write("BENOIT", "UTF-8", file22c);

        DirectoryDiff diff = new DirectoryDiff(remoteDirectory1, remoteDirectory2);

        final DirectoryChangeSet changeSet = diff.diff();
        dump(changeSet);
        assertThat(1, is(equalTo(changeSet.getAdded().size())));
        assertThat(changeSet.getAdded(), hasItem(file22c));
        assertThat(0, is(equalTo(changeSet.getRemoved().size())));
        assertThat(0, is(equalTo(changeSet.getChanged().size())));
    }

    @Test
    public void testFolderWithOneFileLess() throws IOException {
        OverthereFile content = content_A();
        OverthereFile remoteDirectory1 = getTemporaryDirectory();
        content.copyTo(remoteDirectory1);

        OverthereFile remoteDirectory2 = getTemporaryDirectory();
        content.copyTo(remoteDirectory2);
        OverthereFile file22c = remoteDirectory2.getConnection().getFile(remoteDirectory2, "Level2").getFile("Level22").getFile("22c.txt");
        OverthereUtils.write("BENOIT", "UTF-8", file22c);

        DirectoryDiff diff = new DirectoryDiff(remoteDirectory2, remoteDirectory1);

        final DirectoryChangeSet changeSet = diff.diff();
        dump(changeSet);
        assertThat(0, is(equalTo(changeSet.getAdded().size())));
        assertThat(1, is(equalTo(changeSet.getRemoved().size())));
        assertThat(changeSet.getRemoved(), hasItem(file22c));
        assertThat(0, is(equalTo(changeSet.getChanged().size())));
    }

    @Test
    public void testFolderWithSamefileNames() throws IOException {
        OverthereFile content_a = content_A();
        OverthereFile content_b = content_B();

        DirectoryDiff diff = new DirectoryDiff(content_a, content_b);

        final DirectoryChangeSet changeSet = diff.diff();
        dump(changeSet);
        assertThat(0, is(equalTo(changeSet.getAdded().size())));
        assertThat(0, is(equalTo(changeSet.getRemoved().size())));
        assertThat(8, is(equalTo(changeSet.getChanged().size())));
    }


    private void dump(DirectoryChangeSet changeSet) {
        System.out.println(format("%d files to be removed.", changeSet.getRemoved().size()));
        System.out.println(format("%d new files to be copied.", changeSet.getAdded().size()));
        System.out.println(format("%s new files to be copied.", changeSet.getAdded()));
        System.out.println(format("%d modified files to be copied.", changeSet.getChanged().size()));
    }

}
