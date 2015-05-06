/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.Step;
import com.xebialabs.deployit.plugin.api.flow.StepExitCode;
import com.xebialabs.deployit.plugin.api.rules.RulePostConstruct;
import com.xebialabs.deployit.plugin.api.rules.StepMetadata;
import com.xebialabs.deployit.plugin.api.rules.StepParameter;
import com.xebialabs.deployit.plugin.api.rules.StepPostConstructContext;
import com.xebialabs.deployit.plugin.api.udm.ConfigurationItem;
import com.xebialabs.deployit.plugin.api.udm.artifact.Artifact;

@StepMetadata(name = "delete-from-artifact")
public class DeleteFromArtifact implements Step {

    @StepParameter(name = "artifact", description = "Artifact that has been uploaded to the target host.")
    private Artifact artifact;

    @StepParameter(name = "target-host", description = "A target host where the step is applied.", calculated = true)
    private ConfigurationItem targetHost;

    @StepParameter(name = "targetPath", description = "Path of the file or folder where the artifact has been uploaded.")
    private String targetPath;

    @StepParameter(name = "order", description = "The execution order of this step", calculated = true)
    private int order = 0;

    public int getOrder() {
        return order;
    }

    public String getDescription() {
        return "Performing MyStep...";
    }

    @RulePostConstruct
    public void postContruct(StepPostConstructContext context) {
        if (order == 0) {
            order = 30;
        }
    }

    public StepExitCode execute(ExecutionContext ctx) throws Exception {
        return StepExitCode.SUCCESS;
    }
}
