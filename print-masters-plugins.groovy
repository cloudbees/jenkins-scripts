/*
Author: Jean-Philippe Briend
This script prints all the installed plugins and their version for every online Client Master.
It must be launched as a Groovy script from the CJOC server.
*/
import com.cloudbees.opscenter.server.model.*
import com.cloudbees.opscenter.server.clusterops.steps.*

def retour = '\n'
// Loop over all online Client Masters
Jenkins.instance.getAllItems(ConnectedMaster.class).each {
  if(it.channel) {
    def stream = new ByteArrayOutputStream();
    def listener = new StreamBuildListener(stream);
    // Execute remote Groovy script in the Client Master
    // Result of the execution must be a String
    it.channel.call(new MasterGroovyClusterOpStep.Script("""
        result = ''
        for (plugin in Jenkins.instance.pluginManager.plugins) {
          result = result + "\${plugin.displayName} \${plugin.version}\\n"
        }
       return result
    """, listener, "host-script.groovy"))
    retour = retour << "Master ${it.name}:\n${stream.toString().minus('Result: ')}"
  }
}

return retour
