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
import com.xebialabs.overthere.cifs.CifsConnectionBuilder;
import com.xebialabs.overtherepy.action.ActionBuilder;


public class PerformanceDirectoryWindowsSync {

    public static void main(String[] args) throws Exception {
        DirectorySync sync = new DirectorySync(leftFile(), rightFile(), null, true, false, false, false);
        System.out.println("Sync.......");
        ActionBuilder actions = sync.sync();

        actions.execute(new DummyExecutionContext());

        System.exit(0);

    }

    private static OverthereFile rightFile() {
        ConnectionOptions options = new ConnectionOptions();
        options.set(ConnectionOptions.OPERATING_SYSTEM, "UNIX");
        return Overthere.getConnection("local", options).getFile("/Users/bmoussaud/Workspace/xebialabs/poc/amundi/xl-deploy-7.0.0-server-amundi/lib");
    }


    private static OverthereFile leftFile() {
        ConnectionOptions options = new ConnectionOptions();
        options.set(ConnectionOptions.OPERATING_SYSTEM, "WINDOWS");
        options.set(CifsConnectionBuilder.CONNECTION_TYPE, "WINRM_INTERNAL");
        options.set(ConnectionOptions.ADDRESS, "192.168.34.223");
        options.set(ConnectionOptions.USERNAME, "Admin");
        options.set(ConnectionOptions.PASSWORD, "xebiaLabs2015");

        return Overthere.getConnection(CifsConnectionBuilder.CIFS_PROTOCOL, options).getFile("C:\\AMUNDI\\xl-deploy-7.0.0-server-amundi\\lib\\");
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
