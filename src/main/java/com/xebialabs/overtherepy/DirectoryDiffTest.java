/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import com.xebialabs.overthere.ConnectionOptions;
import com.xebialabs.overthere.Overthere;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overthere.ssh.SshConnectionBuilder;

import static java.lang.String.format;


public class DirectoryDiffTest {

    public static void main(String[] args) throws Exception {
        DirectoryDiff diff = new DirectoryDiff(leftFile(), rightFile());
        System.out.println("Start Diff Analysis...");
        long  start = System.currentTimeMillis();
        final DirectoryChangeSet changeSet = diff.diff();
        long  end = System.currentTimeMillis();
        System.out.println(format("End Diff Analysis...%d seconds",((end-start)/1000)));
        System.out.println(format("%d files to be removed.", changeSet.getRemoved().size()));
        System.out.println(format("%d new files to be copied.", changeSet.getAdded().size()));
        System.out.println(format("%d modified files to be copied.", changeSet.getChanged().size()));

        System.exit(0);

    }

    private static OverthereFile rightFile() {
        ConnectionOptions options = new ConnectionOptions();
        options.set(ConnectionOptions.OPERATING_SYSTEM, "UNIX");
        return Overthere.getConnection("local", options).getFile("/Users/bmoussaud/Workspace/xebialabs/poc/amundi/xl-deploy-7.0.0-server/importablePackages/repo/repository");
    }

    private static OverthereFile leftFile() {
        ConnectionOptions options = new ConnectionOptions();
        options.set(ConnectionOptions.OPERATING_SYSTEM, "UNIX");
        options.set(SshConnectionBuilder.CONNECTION_TYPE, "SFTP");
        options.set(ConnectionOptions.ADDRESS, "deployit.vm");
        options.set(ConnectionOptions.USERNAME, "ubuntu");
        options.set(ConnectionOptions.PASSWORD, "ubuntu");

        return Overthere.getConnection("ssh", options).getFile("/tmp/a/container");
    }
}
