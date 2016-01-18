import com.cloudbees.opscenter.server.model.*;
import com.cloudbees.opscenter.server.clusterops.steps.*;
import hudson.remoting.*;

def cjoc = getHost(new LocalChannel(), OperationsCenter.class.simpleName, OperationsCenter.class.simpleName)

cjoc.masters = []
Jenkins.instance.getAllItems(ConnectedMaster.class).each {
  cjoc.masters.add(getHost(it.channel, it.class.simpleName, it.encodedName))
}

def getHost(channel, type, name){
  def host
  if(channel){
    def stream = new ByteArrayOutputStream();
    def listener = new StreamBuildListener(stream);
    channel.call(new MasterGroovyClusterOpStep.Script("""
      //master, regular slaves, and shared slaves
      def nodes = []
      (Jenkins.instance.computers.grep { 
          it.class.superclass?.simpleName != 'AbstractCloudComputer' &&
          it.class.superclass?.simpleName != 'AbstractCloudSlave' &&
          it.class.simpleName != 'EC2AbstractSlave'
        } + Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.SharedSlave.class)
      ).each {
        nodes.add([type:it.class.simpleName, name:it.displayName, executors:it.numExecutors])
      }

      //shared clouds
      Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.SharedCloud.class).each {
        nodes.add([type:it.class.simpleName, name:it.displayName, executors:it.cloud.numExecutors])
      }

      //clouds
      def clouds = []
      Jenkins.instance.clouds.each {
        Integer executorsCap
        try{
          executorsCap = it.templates?.inject(0, {a, c -> a + (c.numExecutors * c.instanceCap)})
        }catch(e){}
        clouds.add([type:it.descriptor.displayName, name:it.displayName, executorsCap:executorsCap])
      }

      def host = [type:'$type', name:'$name', url:Jenkins.instance.rootUrl, cores:Runtime.runtime.availableProcessors(), nodes:nodes, clouds:clouds, offline:false]

      return new groovy.json.JsonBuilder(host).toString()
    """, listener, "host-script.groovy"));
    host = new groovy.json.JsonSlurper().parseText(stream.toString().minus("Result: "));
  } else {
    host = [type:type, name:name, offline:true]
  }
  return host;
}

return new groovy.json.JsonBuilder(cjoc).toPrettyString()