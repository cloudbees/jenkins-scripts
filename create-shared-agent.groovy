/**
Author: kuisathaverat
create a shared Agent JNLP from groovy
**/

import com.cloudbees.opscenter.server.properties.EnvironmentVariablesNodePropertyCustomizer
import com.cloudbees.opscenter.server.properties.NodePropertyCustomizer
import com.cloudbees.opscenter.server.properties.SharedSlaveNodePropertyCustomizer
import com.cloudbees.opscenter.server.properties.ToolLocationNodePropertyCustomizer
import com.cloudbees.opscenter.server.jnlp.slave.JocJnlpSlaveLauncher;
import hudson.model.Node
import hudson.slaves.ComputerLauncher
import hudson.slaves.EnvironmentVariablesNodeProperty
import hudson.slaves.JNLPLauncher
import hudson.tasks.Maven

import hudson.tools.ToolLocationNodeProperty
import hudson.tools.ToolProperty
import com.cloudbees.opscenter.server.model.*

SharedSlave instance = jenkins.model.Jenkins.getInstance().createProject(SharedSlave.class,"ShareAgentName")
//Agent startup options, Tunnel connection through, and JVM options respectively
instance.setLauncher(new JocJnlpSlaveLauncher(null, null, null))
instance.setNumExecutors(5)
instance.setLabelString("foo bar manchu")
instance.setMode(Node.Mode.EXCLUSIVE)
instance.setRemoteFS("/home/foo")
instance.getProperties().add(
    new SharedSlaveNodePropertyCustomizer(
        Arrays. < NodePropertyCustomizer > asList(
            new EnvironmentVariablesNodePropertyCustomizer(
                new EnvironmentVariablesNodeProperty(
                    Arrays.asList(
                        new EnvironmentVariablesNodeProperty.Entry("x", "y"),
                        new EnvironmentVariablesNodeProperty.Entry("a", "b")
                    )
                )
            )
        )
    )
);
instance.setRetentionStrategy(new SharedNodeRetentionStrategy())
instance.save()
