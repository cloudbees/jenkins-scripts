import com.cloudbees.opscenter.server.model.*
import com.cloudbees.opscenter.server.clusterops.steps.*
import hudson.remoting.*

controllers = []
scriptToRun = '''
import jenkins.model.Jenkins

// Start Jenkins in maintenance mode
Jenkins.instance.doQuietDown()

return Jenkins.instance.pluginManager.plugins.stream().sorted().collect(java.util.stream.Collectors.toList()).each { plugin -> println (plugin.getShortName()) }
x=""
'''

Jenkins.instance.getAllItems(ConnectedMaster.class).each {
  controllers.add(getHost(it.channel, it.class.simpleName, it.encodedName))
}

def getHost(channel, type, name){
  def host
  if(channel){
    def stream = new ByteArrayOutputStream()
    def listener = new StreamBuildListener(stream)
    channel.call(new MasterGroovyClusterOpStep.Script(
        scriptToRun,
        listener,
        "host-script.groovy",
        [:]))
    host = stream.toString().minus("Result: ")

  } else {

    host = [type:type, name:name, offline:true]
  }
  return host
}

println controllers