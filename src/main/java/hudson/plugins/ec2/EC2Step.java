/*
 * The MIT License
 *
 * Copyright (c) 2004-, Kohsuke Kawaguchi, Sun Microsystems, Inc., and a number of other of contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.ec2;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.slaves.Cloud;
import hudson.slaves.WorkspaceList;

import java.util.*;
import java.util.concurrent.TimeUnit;

import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Returns the working directory path.
 *
 * Used like:
 *
 * <pre>
 * node {
 *     def x = ec2 cloud: 'myCloud', template: 'aws-CentOS-7'
 * }
 * </pre>
 */
public class EC2Step extends Step {

    private final String cloudName;
    private final String label;

    @DataBoundConstructor public EC2Step(String cloudName,  String label) {
        this.cloudName = cloudName;
        this.label = label;
    }

    public String getCloudName() {
        return this.cloudName;
    }
    public String getLabel() {
        return this.label;
    }


    public StepExecution start(StepContext context) throws Exception {
        return new EC2Step.Execution( this, context);
    }

    @Extension public static final class DescriptorImpl extends StepDescriptor {

        @Override public String getFunctionName() {
            return "EC2";
        }

        @Override public String getDisplayName() {
            return "EC2 machine provisioning";
        }


        public ListBoxModel doFillCloudItems() {
            ListBoxModel r = new ListBoxModel();
            Jenkins.CloudList clouds = jenkins.model.Jenkins.getActiveInstance().clouds;
            for (Cloud cList : clouds) {
                r.add(cList.getDisplayName());
            }
            return r;
        }

        public ListBoxModel doFillTemplateItems(@QueryParameter String cloud) {
            cloud = Util.fixEmpty(cloud);
            ListBoxModel r = new ListBoxModel();
            for (Cloud cList : jenkins.model.Jenkins.getActiveInstance().clouds) {
                if (cList.getDisplayName().equals(cloud)) {
                    List<SlaveTemplate> templates = ((AmazonEC2Cloud) cList).getTemplates();
                    for (SlaveTemplate template : templates) {
                        String[] labList = template.labels.split(" ");
                        if (labList.length > 1) { //Several labels defined for the same template
                            for (int i = 0; i < labList.length; i++) {
                                r.add(labList[i] + " , " + template.getAmi() + " , " + ((AmazonEC2Cloud) cList).getRegion() + " , " + template.type.name());
                            }
                        }
                        else {
                                r.add(template.getLabelString() + " , " + template.getAmi() + " , " + ((AmazonEC2Cloud) cList).getRegion() + " , " + template.type.name());
                            }
                    }
                }
            }
            return r;
        }


        @Override public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Instance> {
        private final String cloudName;
        private final String label;

        Execution(EC2Step step, StepContext context) {
            super(context);
            this.cloudName = step.cloudName;
            this.label = step.label;
        }

        public Cloud getByDisplayName(Jenkins.CloudList clouds, String name) {
            Iterator i$ = clouds.iterator();
            Cloud c;
            c = (Cloud) i$.next();

            while (!c.getDisplayName().equals(name)) {
                if (!i$.hasNext()) {
                    return null;
                }
                c = (Cloud) i$.next();
            }
            return c;
        }

        @Override protected Instance run() throws Exception {
            Cloud cl = getByDisplayName(jenkins.model.Jenkins.getActiveInstance().clouds, this.cloudName);
            if (cl instanceof AmazonEC2Cloud) {
                SlaveTemplate t;
                t = ((AmazonEC2Cloud) cl).getTemplate(this.label);
                if (t != null) {
                    t.setIsNode(false);
                    LabelAtom lbl = new LabelAtom(this.label);
                    SlaveTemplate.ProvisionOptions universe = SlaveTemplate.ProvisionOptions.ALLOW_CREATE;
                    EnumSet<SlaveTemplate.ProvisionOptions> opt = EnumSet.noneOf(SlaveTemplate.ProvisionOptions.class);
                    opt.add(universe);

                    EC2AbstractSlave instance = t.provision(TaskListener.NULL, lbl, opt);
                    Instance myInstance = EC2AbstractSlave.getInstance(instance.getInstanceId(), instance.getCloud());
                    return myInstance;
                } else {
                    throw new IllegalArgumentException("Error in AWS Cloud. Please review AWS template defined in Jenkins configuration.");
                }
            } else {
                throw new IllegalArgumentException("Error in AWS Cloud. Please review EC2 settings in Jenkins configuration.");
            }
        }
    }

}
