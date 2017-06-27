/*
Author: kuisathaverat
configure Mesos Cloud from a groovy script
*/
import org.jenkinsci.plugins.mesos.MesosCloud;
import org.jenkinsci.plugins.mesos.MesosSlaveInfo;
import org.apache.mesos.Protos;
import hudson.slaves.NodeProperty;

def createMesosCloud(
      nativeLibraryPath,
      master,
      description,
      frameworkName,
      role,
      slavesUser,
      credentialsId,
      slaveInfos,
      checkpoint,
      onDemandRegistration,
      jenkinsURL,
      declineOfferDuration,
      cloudID) {
    return new MesosCloud(
           nativeLibraryPath,
           master,
           description,
           frameworkName,
           role,
           slavesUser,
           credentialsId,
           null,
           null,
           slaveInfos,
           checkpoint,
           onDemandRegistration,
           jenkinsURL,
           declineOfferDuration,
           cloudID)
}

def createMesosSlaveInfo(
      labelString, mode, slaveCpus, slaveMem,
      minExecutors, maxExecutors, executorCpus, executorMem,
      remoteFSRoot, idleTerminationMinutes, slaveAttributes,
      jvmArgs, jnlpArgs, defaultSlave,
      containerInfo, additionalURIs, nodeProperties){
    return new MesosSlaveInfo(labelString, mode, slaveCpus, slaveMem, minExecutors,
        maxExecutors, executorCpus, executorMem, remoteFSRoot, idleTerminationMinutes,
        slaveAttributes, jvmArgs, jnlpArgs, defaultSlave, containerInfo, additionalURIs,
        nodeProperties)
}

def createContainerInfo(
    type, dockerImage, dockerPrivilegedMode, dockerForcePullImage,
    dockerImageCustomizable, useCustomDockerCommandShell, customDockerCommandShell,
    volumes, parameters, networking, portMappings, networkInfos){
    return new MesosSlaveInfo.ContainerInfo(type, dockerImage,dockerPrivilegedMode,
        dockerForcePullImage,dockerImageCustomizable, useCustomDockerCommandShell,
        customDockerCommandShell,volumes,parameters, networking,portMappings,networkInfos)
}

def createPortMapping(containerPort, hostPort, protocol){
    return new MesosSlaveInfo.PortMapping(containerPort,hostPort,protocol)
}

def createNetworkInfo(networkName){
    return new MesosSlaveInfo.NetworkInfo(networkName)
}

def createVolume(containerPath, hostPath, readOnly){
    return new MesosSlaveInfo.Volume(containerPath,hostPath,readOnly)
}

def createParameter(key, value){
    return new MesosSlaveInfo.Parameter(key, value)
}

//configured
MesosCloud cloud = MesosCloud.get();

//new cloud from scratch
/*
def cloud = createMesosCloud('/native/Library/Path',
        '127.0.0.1:8080',
       'description',
       'Jenkins Scheduler',
       '*',
       'slavesUser',
       'credentialsId',
       null,
       true,
       false,
       Jenkins.getInstance().getRootUrl(),
       '600000',
       null)

Jenkins.instance.clouds.add(cloud)
*/



if (cloud != null) {
    def volumes = new LinkedList<MesosSlaveInfo.Volume>()
    def parameters = new LinkedList<MesosSlaveInfo.Parameter>()
    def portMappings = new LinkedList<MesosSlaveInfo.PortMapping>()
    def networkInfos = new LinkedList<MesosSlaveInfo.NetworkInfo>()
    
    def nodeProperties = new LinkedList<? extends NodeProperty<?>>()
    def additionalURIs = new LinkedList<URI>()
    
    volumes.add(createVolume('/mnt/folder','/mnt',false))
    parameters.add(createParameter('key','value'))
    portMappings.add(createPortMapping(8080,80,'http'))
    networkInfos.add(createNetworkInfo('sampleNet'))
    
    def containerInfo = createContainerInfo("",
                "organization/image:version",
                false,
                false,
                false,
                false,
                "",
                volumes,
                parameters,
                Protos.ContainerInfo.DockerInfo.Network.BRIDGE.name(),
                portMappings,
                networkInfos)
    
    def mesosSlaveInfo = createMesosSlaveInfo('labelString', 
                      Node.Mode.NORMAL, 
                      '0.1', 
                      '512',
                      '1', 
                      '2', 
                      '0.1', 
                      '128',
                      '/jenkins', 
                      '3', 
                      '',
                      '-Xms16m -XX:+UseConcMarkSweepGC -Djava.net.preferIPv4Stack=true', 
                      '-noReconnect', 
                      'false',
                      containerInfo, 
                      additionalURIs, 
                      nodeProperties)
                      
    cloud.getSlaveInfos().add(mesosSlaveInfo)
    cloud.getSlaveInfos().each {
        t ->
            println t
    }
}
