package hudson.plugins.ec2;


import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.InstanceState;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

//@PowerMockIgnore({"javax.crypto.*", "org.hamcrest.*", "javax.net.ssl.*","javax.management.*"})
@PowerMockIgnore({"javax.crypto.*", "org.hamcrest.*", "javax.net.ssl.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({EC2AbstractSlave.class, SlaveTemplate.class})
public class EC2StepTest {
    @Rule
    public PluginTestRule r = new PluginTestRule();

    @Mock
    private AmazonEC2Cloud cl;

    @Mock
    private SlaveTemplate st;

    @Mock
    private EC2AbstractSlave instance;

    @Mock
    private Instance myInstance;

//    EC2AbstractSlave absSlave;

    @Before
    public void setup () throws Exception {
            //        List<EC2Tag> type = new ArrayList<>();
            //        EC2Tag tag = new EC2Tag("jenkins_slave_type","demand_aws-CentOS-7");
            //        type.add(tag);
            //        InstanceType instanceT = InstanceType.M4Xlarge;
            //        Node.Mode modeT = Node.Mode.NORMAL;
            //        AMITypeData amyData = new AMITypeData() {
            //            @Override
            //            public boolean isWindows() {
            //                return false;
            //            }
            //
            //            @Override
            //            public boolean isUnix() {
            //                return true;
            //            }
            //        };
            //        List nodeProps = new ArrayList();
            //        nodeProps.add("dummy");
            //        absSlave = new EC2AbstractSlave("name", "1", "us-east-1", "/home/centos", 2, modeT, "aws-CentOS-7",
            //                new CommandLauncher("ls -la"), new EC2RetentionStrategy("-5"), "la -la", "/", nodeProps, "centos", "", true,
            //                "-5", type, "myCloud", false, false, 5, amyData) {
            //            @Override
            //            public void terminate() {
            //
            //            }
            //
            //            @Override
            //            public String getEc2Type() {
            //                return Messages.EC2OndemandSlave_OnDemand();
            //            }
            //        };
//        SlaveTemplate template = new SlaveTemplate("ami-7abd0209", "us-east-1", null,
//                "default","/home/centos", instanceT, true, "aws-CentOS-7",
//                 modeT, "aws-CentOS-7", "ls -la", "/", null, "2", "centos",
//                 amyData, null,true, null, type, "-5", false,
//                null, null, false, false, null,
//                false, null, false, false, false);
//
//        EnumSet<ProvisionOptions> provEnum = EnumSet.noneOf(ProvisionOptions.class);
//        ProvisionOptions opt = ProvisionOptions.ALLOW_CREATE;
//        provEnum.add(opt);

        when(st.getAmi()).thenReturn("SlaveTemplate MOCKED");
//        when(st.get)


        List<SlaveTemplate> templates = new ArrayList<SlaveTemplate>();
        templates.add(st);

        when(cl.getCloudName()).thenReturn("myCloud");
        when(cl.getTemplates()).thenReturn(templates);
        when(cl.getTemplate(anyString())).thenReturn(st);
        r.addCloud(cl);
    }

    @Test
    public void boot() throws Exception {

        when(st.provision(any(TaskListener.class),any(Label.class),any(EnumSet.class))).thenReturn(instance);


        when(instance.getInstanceId()).thenReturn("1");
        when(instance.getCloud()).thenReturn(cl);

        mockStatic(EC2AbstractSlave.class);
        when(EC2AbstractSlave.getInstance(anyString(),any(EC2Cloud.class))).thenReturn(myInstance);

        when(myInstance.getPublicIpAddress()).thenReturn("10.20.30.40");
        when(myInstance.getPrivateIpAddress()).thenReturn("1.2.3.4");

        InstanceState myState = new InstanceState();
        myState.setName("running");
        myInstance.setState(myState);


        WorkflowJob boot = r.jenkins.createProject(WorkflowJob.class, "EC2Test");
        boot.setDefinition(new CpsFlowDefinition(
                " node('master') {\n" +
                        "    echo \"RUNNING MI TEST.THIS IS CRAZY!!!\"\n" +
                        "    def X = ec2.instance('myCloud', 'aws-CentOS-7')\n" +
                        "    X.boot()\n" +
//                        "    echo X.getPublicAddress(5)\n" +
                        "}" , true));
        WorkflowRun b = r.assertBuildStatusSuccess(boot.scheduleBuild2(0));
//        r.assertLogContains("1.2.3.4", b);
        r.assertLogContains("SUCCESS", b);
    }

    @After
    public void teardown () {
        r.jenkins.clouds.clear();
    }


}
