package hudson.plugins.ec2;


import com.amazonaws.services.ec2.model.Instance;
import hudson.model.Label;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.ec2.util.PluginTestRule;
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

    @Before
    public void setup () throws Exception {
        List<SlaveTemplate> templates = new ArrayList<SlaveTemplate>();
        templates.add(st);

        when(cl.getCloudName()).thenReturn("myCloud");
        when(cl.getDisplayName()).thenReturn("myCloud");
        when(cl.getTemplates()).thenReturn(templates);
        when(cl.getTemplate(anyString())).thenReturn(st);
        r.addCloud(cl);
    }

    @Test
    public void boot_PublicIP() throws Exception {

        when(st.getZone()).thenReturn("us-east-1");
        when(st.provision(any(TaskListener.class),any(Label.class),any(EnumSet.class))).thenReturn(instance);


        when(instance.getInstanceId()).thenReturn("1");
        when(instance.getCloud()).thenReturn(cl);

        mockStatic(EC2AbstractSlave.class);
        when(EC2AbstractSlave.getInstance(anyString(),any(EC2Cloud.class))).thenReturn(myInstance);

        com.amazonaws.services.ec2.model.InstanceState myState = new com.amazonaws.services.ec2.model.InstanceState();
        myState.setName("running");

        when(myInstance.getState()).thenReturn(myState);
        when(myInstance.getPublicIpAddress()).thenReturn("10.20.30.40");

        WorkflowJob boot = r.jenkins.createProject(WorkflowJob.class, "EC2Test");
        boot.setDefinition(new CpsFlowDefinition(
                " node('master') {\n" +
                        "    def X = ec2.instance('myCloud', 'aws-CentOS-7')\n" +
                        "    X.boot()\n" +
                        "    echo X.getPublicAddress(5)\n" +
                        "}" , true));
        WorkflowRun b = r.assertBuildStatusSuccess(boot.scheduleBuild2(0));
        r.assertLogContains("10.20.30.40", b);
        r.assertLogContains("SUCCESS", b);
    }

    @Test
    public void boot_privateIP() throws Exception {

        when(st.provision(any(TaskListener.class),any(Label.class),any(EnumSet.class))).thenReturn(instance);

        when(instance.getInstanceId()).thenReturn("1");
        when(instance.getCloud()).thenReturn(cl);

        mockStatic(EC2AbstractSlave.class);
        when(EC2AbstractSlave.getInstance(anyString(),any(EC2Cloud.class))).thenReturn(myInstance);

        com.amazonaws.services.ec2.model.InstanceState myState = new com.amazonaws.services.ec2.model.InstanceState();
        myState.setName("running");

        when(myInstance.getState()).thenReturn(myState);
        when(myInstance.getPrivateIpAddress()).thenReturn("1.2.3.4");

        WorkflowJob boot = r.jenkins.createProject(WorkflowJob.class, "EC2Test");
        boot.setDefinition(new CpsFlowDefinition(
                " node('master') {\n" +
                        "    def X = ec2.instance('myCloud', 'aws-CentOS-7')\n" +
                        "    X.boot()\n" +
                        "    echo X.getPrivateAddress(5)\n" +
                        "}" , true));
        WorkflowRun b = r.assertBuildStatusSuccess(boot.scheduleBuild2(0));
        r.assertLogContains("1.2.3.4", b);
        r.assertLogContains("SUCCESS", b);
    }

    @Test
    public void boot_timeout() throws Exception {

        when(st.provision(any(TaskListener.class),any(Label.class),any(EnumSet.class))).thenReturn(instance);

        when(instance.getInstanceId()).thenReturn("1");
        when(instance.getCloud()).thenReturn(cl);

        mockStatic(EC2AbstractSlave.class);
        when(EC2AbstractSlave.getInstance(anyString(),any(EC2Cloud.class))).thenReturn(myInstance);

        com.amazonaws.services.ec2.model.InstanceState myState = new com.amazonaws.services.ec2.model.InstanceState();
        myState.setName("stopped");

        when(myInstance.getState()).thenReturn(myState);
        when(myInstance.getPublicIpAddress()).thenReturn("10.20.30.40");

        WorkflowJob boot = r.jenkins.createProject(WorkflowJob.class, "EC2Test");
        boot.setDefinition(new CpsFlowDefinition(
                " node('master') {\n" +
                        "    def X = ec2.instance('myCloud', 'aws-CentOS-7')\n" +
                        "    X.boot()\n" +
                        "    echo X.getPublicAddress(1)\n" +
                        "}" , true));
        WorkflowRun b = r.assertBuildStatusSuccess(boot.scheduleBuild2(0));
        r.assertLogContains("10.20.30.40", b);
        r.assertLogContains("SUCCESS", b);
    }


    @Test
    public void boot_noCloud() throws Exception {

        when(st.provision(any(TaskListener.class),any(Label.class),any(EnumSet.class))).thenReturn(instance);

        when(instance.getInstanceId()).thenReturn("1");
        when(instance.getCloud()).thenReturn(cl);

        mockStatic(EC2AbstractSlave.class);
        when(EC2AbstractSlave.getInstance(anyString(),any(EC2Cloud.class))).thenReturn(myInstance);


        WorkflowJob boot = r.jenkins.createProject(WorkflowJob.class, "EC2Test");
        boot.setDefinition(new CpsFlowDefinition(
                " node('master') {\n" +
                        "    def X = ec2.instance('dummyCloud', 'aws-CentOS-7')\n" +
                        "    X.boot()\n" +
                        "}" , true));
        WorkflowRun b = r.assertBuildStatus(Result.FAILURE, boot.scheduleBuild2(0).get());
        r.assertLogContains("Error in AWS Cloud. Please review EC2 settings in Jenkins configuration.", b);
        r.assertLogContains("FAILURE", b);
    }


    @Test
    public void boot_noTemplate() throws Exception {

        when(cl.getTemplate(anyString())).thenReturn(null);
        when(st.provision(any(TaskListener.class),any(Label.class),any(EnumSet.class))).thenReturn(instance);

        when(instance.getInstanceId()).thenReturn("1");
        when(instance.getCloud()).thenReturn(cl);

        mockStatic(EC2AbstractSlave.class);
        when(EC2AbstractSlave.getInstance(anyString(),any(EC2Cloud.class))).thenReturn(myInstance);

        WorkflowJob boot = r.jenkins.createProject(WorkflowJob.class, "EC2Test");
        boot.setDefinition(new CpsFlowDefinition(
                " node('master') {\n" +
                        "    def X = ec2.instance('myCloud', 'dummyTemplate')\n" +
                        "    X.boot()\n" +
                        "}" , true));
        WorkflowRun b = r.assertBuildStatus(Result.FAILURE, boot.scheduleBuild2(0).get());
        r.assertLogContains("Error in AWS Cloud. Please review AWS template defined in Jenkins configuration.", b);
        r.assertLogContains("FAILURE", b);
    }

    @After
    public void teardown () {
        r.jenkins.clouds.clear();
    }


}
