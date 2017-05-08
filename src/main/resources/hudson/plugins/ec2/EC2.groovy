package hudson.plugins.ec2

import hudson.model.TaskListener
import hudson.model.labels.LabelAtom
import com.amazonaws.services.ec2.model.Instance
import hudson.slaves.Cloud

class EC2 implements Serializable {

    private org.jenkinsci.plugins.workflow.cps.CpsScript script

    public EC2 (org.jenkinsci.plugins.workflow.cps.CpsScript script) {
        this.script = script
    }

    public Node instance(String cloudName, String label) {
        new Node(this, cloudName, label)
    }

    public static class Node implements Serializable {

        private final EC2 ec2
        private final String cloudName
        private final String label
        private EC2AbstractSlave instance = null

        private Node(EC2 ec2, String cloudName, String label) {
            this.ec2 = ec2
            this.cloudName = cloudName
            this.label = label
        }

        public String boot() {
            Cloud cl = jenkins.model.Jenkins.getActiveInstance().clouds.get(0)
            if (cl instanceof AmazonEC2Cloud) {
                cl = (AmazonEC2Cloud) cl
                def t = cl.getTemplate(this.label)
                t.setIsNode(false)
                def lbl = new LabelAtom(label)
                Enum<SlaveTemplate.ProvisionOptions>[] universe = [SlaveTemplate.ProvisionOptions.ALLOW_CREATE]
                EnumSet opt = new RegularEnumSet(SlaveTemplate.ProvisionOptions, universe)
                this.instance = t.provisionOndemand(TaskListener.NULL, lbl, opt)
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
            ins.getPrivateIpAddress()
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
            ins.getPublicIpAddress()
        }

        protected String getPublicAddress() {
            int timeout = 10
            getPublicAddress(timeout)
        }
        protected String getPrivateAddress() {
            int timeout = 10
            getPrivateAddress(timeout)
        }
    }
}