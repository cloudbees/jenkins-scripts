// This outputs the user token data as a csv for all controllers
// CONTROLLER,USER,TOKEN_LAST_USED,IS_LEGACY,TOKEN_NAME


import com.cloudbees.opscenter.server.model.*
import com.cloudbees.opscenter.server.clusterops.steps.*
import hudson.remoting.*
import hudson.security.ACL
import jenkins.model.Jenkins

controllers = []
scriptToRun = '''
import org.acegisecurity.*
import jenkins.security.*
import java.util.Date
User.getAll().each{ u ->
  def tProp = u.getProperty(jenkins.security.ApiTokenProperty)
  // https://javadoc.jenkins.io/jenkins/security/ApiTokenProperty.TokenInfoAndStats.html
  tProp.tokenList.each { println  "${u}, ${it.lastUseDate?.format("yyyy-MM-dd HH:mm:ss")}, ${it.isLegacy}, ${it.name.replaceAll(',','_COMMA_')} " }
}
null
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
  }
  return host
}

ACL.impersonate(ACL.SYSTEM, new Runnable() {
  @Override
  public void run() {

    Jenkins.instance.getAllItems(ConnectedMaster.class).each {
      def hostRes = getHost(it.channel, it.class.simpleName, it.encodedName)
      if (hostRes) {
        controllers.add("${it.encodedName},${hostRes}")
      }
    }
  }
})
println "CONTROLLER,USER,TOKEN_LAST_USED,IS_LEGACY,TOKEN_NAME"
controllers.each { println it }
null