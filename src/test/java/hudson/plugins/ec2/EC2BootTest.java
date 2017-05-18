package hudson.plugins.ec2;


import com.amazonaws.services.ec2.model.InstanceType;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.ec2.util.PluginTestRule;
import hudson.slaves.CommandLauncher;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by adoblas on 16/5/17.
 */
public class EC2BootTest {
        @Rule
        public PluginTestRule r = new PluginTestRule();
        AmazonEC2Cloud cl;
        @Before
        public void setup () throws Exception {
            AmazonEC2Cloud.testMode = true;
            List<EC2Tag> type = new ArrayList<>();
            EC2Tag tag = new EC2Tag("jenkins_slave_type","demand_aws-CentOS-7");
            type.add(tag);
            InstanceType instanceT = InstanceType.M4Xlarge;
            Node.Mode modeT = Node.Mode.NORMAL;
            AMITypeData amyData = new AMITypeData() {
                @Override
                public boolean isWindows() {
                    return false;
                }

                @Override
                public boolean isUnix() {
                    return true;
                }
            };
            List nodeProps = new ArrayList();
            nodeProps.add("dummy");
            EC2AbstractSlave absSlave = new EC2AbstractSlave("name", "1", "us-east-1", "/home/centos", 2, modeT, "aws-CentOS-7",
                    new CommandLauncher("ls -la"), new EC2RetentionStrategy("-5"), "la -la", "/", nodeProps, "cenntos", "", true,
                    "-5", type, "dummyCloud", false, false, 5, amyData) {
                @Override
                public void terminate() {

                }

                @Override
                public String getEc2Type() {
                    return null;
                }
            };
            SlaveTemplate template = new SlaveTemplate("ami-7abd0209", "us-east-1", new SpotConfiguration("1"),
                    "default","/home/centos", instanceT, true, "aws-CentOS-7",
                    modeT, "aws-CentOS-7", "ls -la", "/", null, "2", "centos",
                    amyData, null,true, null, type, "-5", false,
                    null, null, false, false, null,
                    false, null, false, false, false);
            SlaveTemplate tmpl = mock(SlaveTemplate.class);

            when(tmpl.provision(any(TaskListener.class),any(Label.class), Matchers.<EnumSet<SlaveTemplate.ProvisionOptions>>any())).thenReturn(absSlave);
//        when(tmpl.provision(any(), any(), any())).thenReturn(absSlave);




            List<SlaveTemplate> templates = new ArrayList<SlaveTemplate>();
            templates.add(template);
            cl = mock(AmazonEC2Cloud.class);
            when(cl.getCloudName()).thenReturn("dummyCloud");
            when(cl.getTemplates()).thenReturn(templates);
            when(cl.getTemplate(Mockito.anyString())).thenReturn(template);
            r.addCloud(cl);
        }

        @Test
        public void boot() throws Exception {

            WorkflowJob boot = r.jenkins.createProject(WorkflowJob.class, "EC2Test");
            boot.setDefinition(new CpsFlowDefinition(
                    " node('master') {\n" +
                            "    def X = ec2.instance('us-east-1', 'aws-CentOS-7')\n" +
                            "    X.boot()\n" +
                            "}" , true));
            WorkflowRun b = r.assertBuildStatusSuccess(boot.scheduleBuild2(0));
            r.assertLogContains("SUCCESS", b);
        }

        @After
        public void teardown () {
            AmazonEC2Cloud.testMode = false;
            r.jenkins.clouds.clear();
        }


    }