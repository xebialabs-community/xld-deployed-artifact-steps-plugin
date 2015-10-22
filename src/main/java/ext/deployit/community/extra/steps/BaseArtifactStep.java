/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.community.extra.steps;

import com.xebialabs.deployit.plugin.api.flow.Step;
import com.xebialabs.deployit.plugin.api.rules.Scope;
import com.xebialabs.deployit.plugin.api.rules.StepParameter;
import com.xebialabs.deployit.plugin.api.rules.StepPostConstructContext;
import com.xebialabs.deployit.plugin.api.udm.Container;
import com.xebialabs.deployit.plugin.api.udm.Deployable;
import com.xebialabs.deployit.plugin.api.udm.Deployed;
import com.xebialabs.deployit.plugin.api.udm.artifact.Artifact;
import com.xebialabs.deployit.plugin.overthere.Host;
import com.xebialabs.deployit.plugin.overthere.HostContainer;
import com.xebialabs.overthere.OverthereFile;


public abstract class BaseArtifactStep implements Step {

    @StepParameter(name = "artifact", description = "Artifact that has been uploaded to the target host.")
    private Artifact artifact;

    @StepParameter(name = "target-host", description = "A target host where the step is applied.", calculated = true)
    private Host targetHost;

    @StepParameter(name = "targetPath", description = "Path of the file or folder where the artifact has been uploaded.")
    private String targetPath;

    @StepParameter(name = "order", description = "The execution order of this step", calculated = true)
    int order = 0;

    @StepParameter(name = "description", description = "The description of this step", calculated = true)
    String description = "";

    @StepParameter(name = "deployed", description = "Deployed on which the step will be applied on.", calculated = true)
    private Deployed deployed;


    public int getOrder() {
        return order;
    }

    public String getDescription() {
        return description;
    }


    protected void doConfigure(final StepPostConstructContext ctx) {
        if (ctx.getScope() != Scope.DEPLOYED)
            throw new RuntimeException("upload-artifact and delete-artifact step can be used only using the 'deployed' scope");

        if (deployed == null) {
            deployed = defaultDeployed(ctx);
        }

        if (targetHost == null) {
            targetHost = defaultHost(ctx);
        }

        if (artifact == null) {
            artifact = defaultArtifact(ctx);
        }
    }

    public Host getTargetHost() {
        return targetHost;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public Deployed getDeployed() {
        return deployed;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    Host defaultHost(final StepPostConstructContext context) {
        final Container container = defaultTargetContainer(context);
        if (container instanceof HostContainer) {
            HostContainer hostContainer = (HostContainer) container;
            return hostContainer.getHost();
        } else if (container.hasProperty("host")) {
            return container.getProperty("host");
        }
        return null;
    }

    Container defaultTargetContainer(StepPostConstructContext ctx) {
        if (ctx.getScope() == Scope.DEPLOYED) {
            return getDeployed().getContainer();
        }
        throw new RuntimeException("Cannot find defaultTargetContainer ");
    }

    Artifact defaultArtifact(StepPostConstructContext ctx) {
        if (ctx.getScope() == Scope.DEPLOYED) {
            final Deployed<? extends Deployable, ? extends Container> deployed = getDeployed();
            if (deployed instanceof Artifact) {
                Artifact artifact = (Artifact) deployed;
                return artifact;
            }
        }
        return null;
    }

    abstract protected Deployed<? extends Deployable, ? extends Container> defaultDeployed(final StepPostConstructContext ctx);

    protected String stringPathPrefix(final OverthereFile file, final String prefix) {

        final String path = file.getPath();
        final int path_length = path.length();
        final int prefix_length = prefix.length();
        String relativePath = path.substring(prefix_length + 1, path_length);
        return relativePath.replace('\\', '/');
    }

}
