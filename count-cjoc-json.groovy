import com.cloudbees.opscenter.server.model.*;
import com.cloudbees.opscenter.server.clusterops.steps.*;
import hudson.remoting.*;

def hosts = []

hosts.add(getHost(new LocalChannel(), OperationsCenter.class.simpleName, OperationsCenter.class.simpleName)) // for CJOC

Jenkins.instance.getAllItems(ConnectedMaster.class).each {
  hosts.add(getHost(it.channel, it.class.simpleName, it.encodedName)) //for Client Masters
}

def getHost(channel, type, name){
  if(channel == null){
    return """{"offline":true, "type":"$type", "name":"$name"}"""
  } else {
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

      //clouds
      def clouds = []
      Jenkins.instance.clouds.each {
        Integer executorsCap
        try{
          executorsCap = it.templates?.inject(0, {a, c -> a + (c.numExecutors * c.instanceCap)})
        }catch(e){}
        clouds.add([type:it.descriptor.displayName, name:it.displayName, executorsCap:executorsCap])
      }

      def host = [type:'$type', name:'$name', url:Jenkins.instance.rootUrl, cores:Runtime.runtime.availableProcessors(), nodes:nodes, clouds:clouds]

      return new groovy.json.JsonBuilder(host).toPrettyString()
    """, listener, "host-script.groovy"));
    return stream.toString().minus("Result: ");
  }
}

return hosts