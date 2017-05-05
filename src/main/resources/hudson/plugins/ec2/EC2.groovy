package hudson.plugins.ec2

import hudson.model.labels.LabelAtom
import com.amazonaws.services.ec2.model.Instance
import hudson.slaves.Cloud

class EC2 implements Serializable {

    private org.jenkinsci.plugins.workflow.cps.CpsScript script

    public EC2 (org.jenkinsci.plugins.workflow.cps.CpsScript script) {
        this.script = script
    }

    public Node instance(String cloudName, String ami) {
        new Node(this, cloudName, ami)
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
            def Cloud cl = jenkins.model.Jenkins.getActiveInstance().clouds.getByName(cloudName)
            def t
            if (cl instanceof EC2Cloud) {
                cl = (EC2Cloud) cl
                t = cl.getTemplate()
                t.setIsNode(false)
            }
            def lbl
            lbl = new LabelAtom(label)
            t.provisionOndemand(null, lbl, SlaveTemplate.ProvisionOptions.ALLOW_CREATE)
//            this.instance = t.provisionOndemand(null, lbl, SlaveTemplate.ProvisionOptions.ALLOW_CREATE)
        }

        public String getAddress() {
            Instance ins = EC2AbstractSlave.getInstance(instance.getInstanceId(), instance.getCloud())
            ins.getPrivateIpAddress()
        }
    }
}