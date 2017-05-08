package hudson.plugins.ec2;

import groovy.lang.Binding;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
@Extension
public class EC2DSL extends GlobalVariable {

    @Override public String getName() {
        return "ec2";
    }

    @Override public Object getValue(CpsScript script) throws Exception {
        Binding binding = script.getBinding();
        Object ec2;
        if (binding.hasVariable(getName())) {
            ec2 = binding.getVariable(getName());
        } else {
            ec2 = script.getClass().getClassLoader().loadClass("hudson.plugins.ec2.EC2").getConstructor(CpsScript.class).newInstance(script);
            binding.setVariable(getName(), ec2);
        }
        return ec2;
    }

}
