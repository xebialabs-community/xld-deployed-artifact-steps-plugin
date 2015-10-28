/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps.action;


import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;

public class SystemOut extends BaseAction {

    public SystemOut(String message) {
        previewBuffer = message;
    }

    @Override
    public void execute(final ExecutionContext ctx) {
        ctx.logOutput(previewBuffer);
    }
}
