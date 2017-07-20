 import jenkins.model.Jenkins
 import hudson.slaves.*
 import hudson.plugins.sshslaves.*

 def createLauncher(agentLauncher){
     if(agentLauncher.class.name ==  'hudson.plugins.sshslaves.SSHLauncher'){
         return "new hudson.plugins.sshslaves.SSHLauncher('${agentLauncher.host}',${agentLauncher.port},'${agentLauncher.credentialsId}','${agentLauncher?.jvmOptions}','${agentLauncher?.javaPath}','${agentLauncher?.prefixStartSlaveCmd}','${agentLauncher?.suffixStartSlaveCmd}',${agentLauncher?.launchTimeoutSeconds},${agentLauncher?.maxNumRetries},${agentLauncher?.retryWaitTime},${agentLauncher?.sshHostKeyVerificationStrategy})"         
     } else if(agentLauncher.class.name == 'com.cloudbees.jenkins.plugins.sshslaves.SSHLauncher'){
         return "new com.cloudbees.jenkins.plugins.sshslaves.SSHLauncher('${agentLauncher.host}',new com.cloudbees.jenkins.plugins.sshslaves.SSHConnectionDetails('${agentLauncher.connectionDetails.credentialsId}',${agentLauncher.connectionDetails.port},'${agentLauncher.connectionDetails?.javaPath}','${agentLauncher.connectionDetails?.jvmOptions}','${agentLauncher.connectionDetails?.prefixStartSlaveCmd}','${agentLauncher.connectionDetails?.suffixStartSlaveCmd}',${agentLauncher.connectionDetails?.displayEnvironment},${agentLauncher.connectionDetails?.keyVerificationStrategy}))"
     } else if (agentLauncher.class.name == 'hudson.slaves.JNLPLauncher' ||agentLauncher.class.name == 'com.cloudbees.opscenter.server.jnlp.slave.JocJnlpSlaveLauncher'){
         def tunnel = agentLauncher?.tunnel == null ? "" : agentLauncher?.tunnel
         def vmargs = agentLauncher?.vmargs == null ? "" : agentLauncher?.vmargs
         return "new hudson.slaves.JNLPLauncher('${tunnel}','${vmargs}')"
     }
     
     return "NOT SUPPORTED - " + agentLauncher.getClass().getCanonicalName() + " - NOT SUPPORTED"
 } 

 def createAgent(agentName, agentDescription, agentHome, agentExecutors, agentLabels, launcher) {
     println "//Create Agent ${agentName}"
     //DumbSlave(String name, String nodeDescription, String remoteFS, String numExecutors, Node.Mode mode, String labelString, ComputerLauncher launcher, RetentionStrategy retentionStrategy)
     println "Jenkins.instance.addNode(new DumbSlave('${agentName}',  '${agentDescription}', '${agentHome}', '${agentExecutors}', Mode.NORMAL, '${agentLabels}', ${launcher}, new RetentionStrategy.Always()))"
 }


println '''
import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import hudson.plugins.sshslaves.*
import com.cloudbees.opscenter.server.model.*
import hudson.model.Node.Mode
'''

 // Jenkins Master and slaves
 Jenkins.instance.computers.grep {
    it.class.superclass?.simpleName != 'AbstractCloudComputer' &&
    it.class.superclass?.simpleName != 'AbstractCloudSlave' &&
    it.class.simpleName != 'EC2AbstractSlave'
 }.each {
     if (!(it instanceof jenkins.model.Jenkins.MasterComputer)) {
         createAgent(it.name, it.displayName, it.getNode().getRemoteFS(), it.numExecutors, it.getNode().getLabelString(), createLauncher(it.getLauncher()))
     }
 }

 // CJOC Shared Slaves
 Jenkins.instance.allItems.grep {
     it.class.name == 'com.cloudbees.opscenter.server.model.SharedSlave'
 }.each {
     createAgent(it.name, it.displayName, it.getRemoteFS(), it.numExecutors, it.getLabelString(), createLauncher(it.getLauncher()))
 }

println '''
//END_OF_SCRIPT

'''
