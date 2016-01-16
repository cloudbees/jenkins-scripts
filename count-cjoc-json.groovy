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
    return "{offline:true}"
  } else {
    return "{offline:true}"
    def baos = new ByteArrayOutputStream();
    def s = new StreamBuildListener(baos);
    channel.call(new MasterGroovyClusterOpStep.Script("""
      import groovy.json.*
      class Host {
        String name
        String type
        String url
        Integer cores
        List<Node> nodes
      }
      class Node {
        String name
        String type
        String meta
        Integer executors
        Integer instanceCaps
      }
      
      def nodes = []

      //regular slaves and master
      Jenkins.instance.computers.grep{ 
        it.class.superclass?.simpleName != 'AbstractCloudComputer' &&
        it.class.superclass?.simpleName != 'AbstractCloudSlave' &&
        it.class.simpleName != 'EC2AbstractSlave'
      }.each{
        nodes.add(new Node(name: it.displayName, type: it.class.simpleName, executors: it.numExecutors, meta: "Computer"))
      }

      //shared slaves
      Jenkins.instance.allItems.grep{
        it.class.name == 'com.cloudbees.opscenter.server.model.SharedSlave'
      }.each{
        nodes.add(new Node(name: it.displayName, type: it.class.simpleName, executors: it.numExecutors, meta: "SharedSlave"))
      }

      //clouds
      Jenkins.instance.clouds.each {
        Integer instanceCaps
        try{
          instanceCaps = it.templates?.inject(0, {a, c -> a + (c.numExecutors * c.instanceCap)})
        }catch(e){}
        nodes.add(new Node(name: it.displayName, type: it.descriptor.displayName, instanceCaps: instanceCaps, meta: "Cloud"))
      }

      def host = new Host(name: '$name', type: '$type', url: Jenkins.instance.rootUrl, cores:Runtime.runtime.availableProcessors(), nodes:nodes)

      return new JsonBuilder(host).toPrettyString()
      
    """, s, "host-script.groovy"));
    def o = baos.toString().minus("Result: ");
    return o
  }
}

return hosts