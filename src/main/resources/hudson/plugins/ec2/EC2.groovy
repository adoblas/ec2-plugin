package hudson.plugins.ec2

import hudson.model.TaskListener
import hudson.model.labels.LabelAtom
import com.amazonaws.services.ec2.model.Instance
import hudson.slaves.Cloud
import jenkins.model.Jenkins

class EC2 implements Serializable {

    private org.jenkinsci.plugins.workflow.cps.CpsScript script

    public EC2 (org.jenkinsci.plugins.workflow.cps.CpsScript script) {
        this.script = script
    }

    public Node instance(String cloudName, String label) {
        new Node(this, cloudName, label)
    }

    public Node instance(String cloudName, String label, String spotMaxBidPrice) {
        new Node(this, cloudName, label, spotMaxBidPrice)
    }

    public static class Node implements Serializable {

        private final EC2 ec2
        private final String cloudName
        private final String label
        private final transient String spotMaxBidPrice
        private EC2AbstractSlave instance = null

        private Node(EC2 ec2, String cloudName, String label) {
            this.ec2 = ec2
            this.cloudName = cloudName
            this.label = label
        }

        private Node(EC2 ec2, String cloudName, String label, String spotMaxBidPrice) {
            this.ec2 = ec2
            this.cloudName = cloudName
            this.label = label
            this.spotMaxBidPrice = spotMaxBidPrice
        }

        public String boot() {
            Cloud cl = getByDisplayName(jenkins.model.Jenkins.getActiveInstance().clouds, cloudName)
            if (cl instanceof AmazonEC2Cloud) {
                cl = (AmazonEC2Cloud) cl

                def t = cl.getTemplate(this.label)
                if (t != null) {
                    t.setIsNode(false)
                    def lbl = new LabelAtom(label)
                    def universe = [SlaveTemplate.ProvisionOptions.ALLOW_CREATE] as Enum<SlaveTemplate.ProvisionOptions>[]
                    def opt = new RegularEnumSet(SlaveTemplate.ProvisionOptions, universe)
                    opt.add(SlaveTemplate.ProvisionOptions.ALLOW_CREATE)
                    if (this.spotMaxBidPrice != null) {
                        t.spotConfig = new SpotConfiguration(spotMaxBidPrice)
                    }
                    instance = t.provision(TaskListener.NULL, lbl, opt)
                } else {
                    ec2.script.error "Error in AWS Cloud. Please review AWS template defined in Jenkins configuration."
                }
            } else {
                ec2.script.error "Error in AWS Cloud. Please review EC2 settings in Jenkins configuration."
            }
        }

        public String getPrivateAddress(int timeout) {
            Instance ins = EC2AbstractSlave.getInstance(instance.getInstanceId(), instance.getCloud())
            int i = 0
            int miliTimout = timeout * 60000
            while (ins.state.name != "running" && i < miliTimout) {
                Thread.sleep(10000)
                i = i + 10000
                ins = EC2AbstractSlave.getInstance(instance.getInstanceId(), instance.getCloud())
            }
            ins.privateIpAddress
        }

        public String getPublicAddress(int timeout) {
            Instance ins = EC2AbstractSlave.getInstance(instance.getInstanceId(), instance.getCloud())
            int i = 0
            int miliTimout = timeout * 60000
            while (ins.state.name != "running" && i < miliTimout) {
                Thread.sleep(10000)
                i = i + 10000
                ins = EC2AbstractSlave.getInstance(instance.getInstanceId(), instance.getCloud())
            }
            ins.publicIpAddress
        }

        protected String getPublicAddress() {
            int timeout = 10
            getPublicAddress(timeout)
        }

        protected String getPrivateAddress() {
            int timeout = 10
            getPrivateAddress(timeout)
        }

        public Cloud getByDisplayName(Jenkins.CloudList clouds, String name) {
            Iterator i$ = clouds.iterator()
            Cloud c
            c = (Cloud) i$.next()

            while (c.getDisplayName() != name) {
                if (!i$.hasNext()) {
                    return null
                }
                c = (Cloud) i$.next()
            }
            return c
        }
    }
}