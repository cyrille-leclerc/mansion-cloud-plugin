package com.cloudbees.jenkins.plugins.mtslavescloud;

import com.cloudbees.jenkins.plugins.mtslavescloud.client.BrokerRef;
import com.cloudbees.jenkins.plugins.mtslavescloud.client.VirtualMachineRef;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.slaves.AbstractCloudImpl;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner.PlannedNode;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kohsuke Kawaguchi
 */
public class MansionCloud extends AbstractCloudImpl {
    private final URL broker;

    @DataBoundConstructor
    public MansionCloud(String name, URL broker) {
        super(name, "0"/*unused*/);
        this.broker = broker;
    }

    /**
     * End point that we talk to.
     */
    public URL getBroker() {
        return broker;
    }

    @Override
    public boolean canProvision(Label label) {
        // TODO: we'll likely support multiple kinds of slaves like EC2 plugin does in the future
        return true;
    }

    @Override
    public Collection<PlannedNode> provision(Label label, int excessWorkload) {
        List<PlannedNode> r = new ArrayList<PlannedNode>();
        try {
            for (int i=0; i<excessWorkload; i++) {
                final VirtualMachineRef vm = new BrokerRef(broker).createVirtualMachine(JSONObject.fromObject("{\"configs\":[]}"));

                Future<Node> f = Computer.threadPoolForRemoting.submit(new Callable<Node>() {
                    public Node call() throws Exception {
                        vm.boot();
                        return null;
                    }
                });
                r.add(new PlannedNode(vm.getId(),f,1));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to provision from "+this,e);
        }
        return r;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {
        @Override
        public String getDisplayName() {
            return "Multi-tenancy Slave Cloud";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(MansionCloud.class.getName());
}