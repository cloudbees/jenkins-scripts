import nectar.plugins.rbac.*
import hudson.model.*
import hudson.slaves.*
import nectar.plugins.rbac.groups.NodeProxyGroupContainer
  
//Simple script to copy groups and roles from a source Node to a destination Node.

String name1 = 'Source_Node_name'
String name2 = 'Destination_Node_name'


Node nodeAbs1 = Jenkins.instanceOrNull.getNode(name1)
Node  nodeAbs2 = Jenkins.instance.nodes.find{it.name.equals(name2)}
println nodeAbs1
println nodeAbs2

if(nodeAbs1 == null || nodeAbs2 == null){
    println 'Nodes not found'
} else {
  NodeProxyGroupContainer propertyNodeProxyGroup1 = nodeAbs1.getNodeProperties().get(NodeProxyGroupContainer.class);
  NodeProxyGroupContainer propertyNodeProxyGroup2 = nodeAbs2.getNodeProperties().get(NodeProxyGroupContainer.class);
  
    if (propertyNodeProxyGroup2 == null) {
    	NodeProxyGroupContainer c = new NodeProxyGroupContainer()
      	nodeAbs2.getNodeProperties().add(c)
        //c.owner=nodeAbs2
      	propertyNodeProxyGroup2 = nodeAbs2.getNodeProperties().get(NodeProxyGroupContainer.class)
  }
  println "propertyNodeProxyGroup2 : " + propertyNodeProxyGroup2
  println "propertyNodeProxyGroup1 : " + propertyNodeProxyGroup1
  if (propertyNodeProxyGroup1 != null) {
    	propertyNodeProxyGroup1.getGroups().findAll{it != null}.each {
          try{
            propertyNodeProxyGroup2.addGroup(it)}
          catch(Exception ex){
            println "Exception----------"
            println ex
          }
          println "Group : " + it
      	}
       	propertyNodeProxyGroup1.getRoleFilters().findAll{it != null}.each {
          println "RoleFilter : " + it
          propertyNodeProxyGroup2.addRoleFilter(it)
      	}
        println "Saving..."
	 	nodeAbs2.save()
  }
}
