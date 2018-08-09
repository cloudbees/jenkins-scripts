/**
Author: kuisathaverat
create a shared Agent JNLP from groovy
**/

import com.cloudbees.opscenter.server.jnlp.slave.JocJnlpSlaveLauncher
import com.cloudbees.opscenter.server.model.SharedNodeRetentionStrategy
import com.cloudbees.opscenter.server.model.SharedSlave
import com.cloudbees.opscenter.server.properties.EnvironmentVariablesNodePropertyCustomizer
import com.cloudbees.opscenter.server.properties.NodePropertyCustomizer
import com.cloudbees.opscenter.server.properties.SharedSlaveNodePropertyCustomizer
import hudson.model.Node
import hudson.slaves.EnvironmentVariablesNodeProperty

import hudson.tools.ToolLocationNodeProperty
import hudson.tools.ToolProperty
import com.cloudbees.opscenter.server.model.*

SharedSlave instance = jenkins.model.Jenkins.getInstance().createProject(SharedSlave.class,"ShareAgentName")
instance.setLauncher(new JocJnlpSlaveLauncher("tunnel:10000", "-Xmx256m", "-noCertificateCheck"))
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
