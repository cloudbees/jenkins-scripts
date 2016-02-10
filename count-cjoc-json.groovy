import com.cloudbees.opscenter.server.model.*;
import com.cloudbees.opscenter.server.clusterops.steps.*;
import hudson.remoting.*;

def cjoc = getHost(new LocalChannel(), OperationsCenter.class.simpleName, OperationsCenter.class.simpleName)

cjoc.masters = []
Jenkins.instance.getAllItems(ConnectedMaster.class).each {
  cjoc.masters.add(getHost(it.channel, it.class.simpleName, it.encodedName))
}

try{
  cjoc.summary = [
    masters:cjoc.masters.size() + 1, //masters + cjoc
    masterCores:cjoc.masters*.cores.sum() + cjoc.cores,
    executors:cjoc.nodes*.executors.sum() + cjoc.masters*.nodes*.executors.sum().sum(),
    knownCloudExecutors:cjoc.masters*.clouds*.executorsCap.sum().findAll{it}.sum(0) + cjoc.clouds*.executorsCap.findAll{it}.sum(0)
  ]
}catch(e){}

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

      //clouds
      def clouds = []
      Jenkins.instance.clouds.each {
        def cloud = [type:it.descriptor.displayName, name:it.displayName]
        try{
          cloud.executorsCap = it.templates?.inject(0, {a, c -> a + (c.numExecutors * c.instanceCap)})
        }catch(e){}
        try{
          cloud.executorsPerNode = it.numExecutors
        }catch(e){}
        clouds.add(cloud)
      }

      //shared clouds
      Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.SharedCloud.class).each {
        //TODO may need to check either numExectors or numExecutors * instance caps
        clouds.add([type:it.class.simpleName, name:it.displayName, executorsPerNode:it.cloud.numExecutors])
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