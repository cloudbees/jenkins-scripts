import com.cloudbees.opscenter.server.model.*
import com.cloudbees.opscenter.server.clusterops.steps.*
import hudson.remoting.*
import hudson.security.ACL
import jenkins.model.Jenkins

controllerName = "r23322"
controllers = []
scriptToRun = '''
import jenkins.model.Jenkins

// Start Jenkins in maintenance mode
// Jenkins.instance.doQuietDown()

return Jenkins.instance.getRootUrl()
'''

def getHost(channel, type, name){
  def host
  if(channel) {
    def stream = new ByteArrayOutputStream()
    def listener = new StreamBuildListener(stream)
    channel.call(new MasterGroovyClusterOpStep.Script(
        scriptToRun,
        listener,
        "host-script.groovy",
        [:])
    )
    host = stream.toString().minus("Result: ")
  } else {
    host = [type:type, name:name, offline:true]
  }
  return host
}

ACL.impersonate(ACL.SYSTEM, new Runnable() {
  @Override
  public void run() {

    Jenkins.instance.getAllItems(ConnectedMaster.class).each {
      if (it.encodedName == (controllerName)) {
        controllers.add(getHost(it.channel, it.class.simpleName, it.encodedName))
      }
    }
  }
})
println controllers
