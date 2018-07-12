/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package com.xebialabs.overtherepy;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.ITask;
import com.xebialabs.deployit.plugin.api.inspection.InspectionContext;
import com.xebialabs.deployit.plugin.api.services.Repository;
import com.xebialabs.overthere.ConnectionOptions;
import com.xebialabs.overthere.Overthere;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overthere.ssh.SshConnectionBuilder;
import com.xebialabs.overtherepy.action.ActionBuilder;


public class PerformanceDirectorySync {

    public static void main(String[] args) throws Exception {
        DirectorySync sync = new DirectorySync(leftFile(), rightFile());
        System.out.println("Sync.......");
        ActionBuilder actions = sync.sync();
        //System.out.println(actions.previewContentString());

        actions.execute(new DummyExecutionContext());

        System.exit(0);

    }

    private static OverthereFile rightFile() {
        ConnectionOptions options = new ConnectionOptions();
        options.set(ConnectionOptions.OPERATING_SYSTEM, "UNIX");
        return Overthere.getConnection("local", options).getFile("/Users/bmoussaud/.m2/repository");
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

    private static class DummyExecutionContext implements ExecutionContext {

        @Override
        public void logOutput(String s) {
            System.out.println(s);
        }

        @Override
        public void logError(String s) {
            System.err.println(s);
        }

        @Override
        public void logError(String s, Throwable throwable) {

        }

        @Override
        public Object getAttribute(String s) {
            return null;
        }

        @Override
        public void setAttribute(String s, Object o) {

        }

        @Override
        public Repository getRepository() {
            return null;
        }

        @Override
        public InspectionContext getInspectionContext() {
            return null;
        }

        @Override
        public ITask getTask() {
            return null;
        }
    }
}
