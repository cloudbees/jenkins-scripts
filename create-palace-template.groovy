/**
@Author: Kuisathaverat
@Description: Create a Palace template from a groovy script.
**/

def config = [
        templateName:"custom-palace-template", 
        imageName:"maven", 
        dockerRepo:"registry.hub.docker.com/library", 
        tag:"3.3-jdk-8"]

def templateName = "${config.templateName}"
def instance = jenkins.model.Jenkins.getInstance().createProject(com.cloudbees.tiger.plugins.palace.templates.DockerSlaveTemplate.class, templateName)
instance.setDisplayName(templateName)
instance.setLabelString(templateName)
instance.setCpus(config["cpus"] ? config["cpus"] : 0.1)
instance.setMemory(config["memory"] ? config["memory"] : 512)
instance.setJvmMemory(config["jvmMemory"] ? config["jvmMemory"] : 256)
instance.setJvmArgs('')
instance.setRemoteFS("/jenkins")
instance.setImage("${config["dockerRepo"]}/${config["imageName"]}:${config["tag"]}")
def containerPropertiesList = []
//    containerPropertiesList.add(new com.cloudbees.tiger.plugins.palace.model.URISpec("https://s3.amazonaws.com/S3BUCKET/docker.tar.gz",false,true))
containerPropertiesList.add(new com.cloudbees.tiger.plugins.palace.model.EnvironmentVariableContainerProperty("TZ","America/New_York"))
instance.setContainerProperties(containerPropertiesList)
instance.save()
