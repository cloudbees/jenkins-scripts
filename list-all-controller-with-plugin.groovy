import com.cloudbees.opscenter.server.model.*
import com.cloudbees.opscenter.server.clusterops.steps.*
import hudson.remoting.*

controllers = []
pluginToFind='checks-api'
scriptToRun = """
import Jenkins
Jenkins.instance.pluginManager.plugins.stream().sorted().findAll { it.getShortName() == "${pluginToFind}" }.each { print it.shortName }
x=null
"""

Jenkins.instance.getAllItems(ConnectedMaster.class).each {
  controllers.add(getHost(it.channel, it.class.simpleName, it.encodedName))
}

def getHost(channel, type, name){
  def hostReturnStr
  if(channel){
    def stream = new ByteArrayOutputStream()
    def listener = new StreamBuildListener(stream)
    channel.call(new MasterGroovyClusterOpStep.Script(
        scriptToRun,
        listener,
        "host-script.groovy",
        [:]))
    hostReturnStr = stream.toString()

  }
  return channel ? "${name} - ${hostReturnStr}" : "${name} - OFFLINE"
}

controllers.each { if (it) { println it } }
x=null
