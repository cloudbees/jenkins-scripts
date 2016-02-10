//master and regular slaves
def nodes = []
(Jenkins.instance.computers.grep { 
  it.class.superclass?.simpleName != 'AbstractCloudComputer' &&
    it.class.superclass?.simpleName != 'AbstractCloudSlave' &&
    it.class.simpleName != 'EC2AbstractSlave'
}
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
def host = [type:Jenkins.instance.displayName, name:Jenkins.instance.displayName, url:Jenkins.instance.rootUrl, cores:Runtime.runtime.availableProcessors(), nodes:nodes, clouds:clouds, offline:false]
return new groovy.json.JsonBuilder(host).toPrettyString()