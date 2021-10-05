/*** BEGIN META {
 "name" : "Create a Managed Controller in CloudBees Core on Modern Cloud Platform",
 "comment" : "This script creates a Kubernetes Managed Controller programmatically similarly to what can be done through the UI.
 It has been tested with version 2.289.1.2 of CloudBees Core",
 "parameters" : [],
 "core": "2.289.1.2",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.masterprovisioning.kubernetes.KubernetesImagePullSecret
import com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning
import com.cloudbees.opscenter.server.model.ManagedMaster
import com.cloudbees.opscenter.server.properties.ConnectedMasterLicenseServerProperty
import com.cloudbees.opscenter.server.properties.ConnectedMasterOwnerProperty

/*****************
 * INPUTS        *
 *****************/

/**
 * The controller name is mandatory
 */
String controllerName = "mm-from-groovy"
/**
 * Location of the controller
 * Leave empty to add to the top level
 * Provide full name of the folder to place controller in the folder, e.g.
 * String controllerParent = "Controllers"
 * String controllerParent = "Division/Sub-division"
 */
String controllerParent = ""

/**
 * Following attributes may be specified. The values proposed are the default from version 2.2.9 of Controller Provisioning
 *
 * Note: If not setting properties explicitly, the defaults will be used.
 */
/* Controller */
String  controllerDisplayName = ""
String  controllerDescription = ""
String  controllerPropertyOwners = ""
Integer controllerPropertyOwnersDelay = 5

/* Controller Provisioning */
Integer k8sDisk = 50
Integer k8sMemory = 3072
/**
 * Since version 2.235.4.1, we recommend not using the Heap Ratio. Instead add `-XX:MinRAMPercentage` and 
 * `-XX:MaxRAMPercentage` to the Java options. For example, a ratio of 0.5d translate to a percentage of 50: 
 * `-XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=50.0`
 *
 * See https://support.cloudbees.com/hc/en-us/articles/204859670-Java-Heap-settings-best-practice and 
 * https://docs.cloudbees.com/docs/release-notes/latest/cloudbees-ci/modern-cloud-platforms/2.235.4.1.
 */
Double  k8sMemoryRatio = null
Double  k8sCpus = 1
String  k8sFsGroup = "1000"
Boolean k8sAllowExternalAgents = false
String  k8sClusterEndpointId = "default"
String  k8sEnvVars = ""
String  k8sJavaOptions = "-XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=50.0"
String  k8sJenkinsOptions = ""
String  k8sImage = 'CloudBees CI - Managed Master - 2.289.1.2'
List<KubernetesImagePullSecret> k8sImagePullSecrets = Collections.emptyList()
// Example: 
//   def k8sImagePullSecret1 = new KubernetesImagePullSecret(); k8sImagePullSecret1.setValue("useast-reg")
//   List<KubernetesImagePullSecret> k8sImagePullSecrets = Arrays.asList(k8sImagePullSecret1)
Integer k8sLivenessInitialDelaySeconds = 300
Integer k8sLivenessPeriodSeconds = 10
Integer k8sLivenessTimeoutSeconds = 10
Integer k8sReadinessInitialDelaySeconds = 30
Integer k8sReadinessFailureThreshold = 100
Integer k8sReadinessTimeoutSeconds = 5
String  k8sStorageClassName = ""
String  k8sSystemProperties = ""
String  k8sNamespace = ""
String  k8sNodeSelectors = ""
Long    k8sTerminationGracePeriodSeconds = 1200L
String  k8sYaml = ""

/**
 * cascBundle (optional). Configuration as Code Configuration Bundle
 */
String cascBundle = ""

/*****************
 * CREATE CONTROLLER *
 *****************/
def jenkins = Jenkins.get()
ModifiableTopLevelItemGroup parent = controllerParent?.trim() ? jenkins.getItemByFullName(controllerParent) : jenkins
if (!parent) {
    println("Cannot find parent '${controllerParent}'.")
    return
}
/**
 * Create a Managed Controllers with just a name (this will automatically fill required values for id, idName, grantId, etc...)
 * Similar to creating an iten in the UI
 */
ManagedMaster newInstance = parent.createProject(jenkins.getDescriptorByType(ManagedMaster.DescriptorImpl.class), controllerName, true)
newInstance.setDescription(controllerDescription)
newInstance.setDisplayName(controllerDisplayName)

/************************
 * CONFIGURATION BUNDLE *
 ************************/
if(cascBundle?.trim()) {
    // Properties must follow this order
    // Note: ConnectedMasterTokenProperty supported since 2.289.1.2. For earlier version, comment out the following.
    newInstance.getProperties().replace(new com.cloudbees.opscenter.server.casc.config.ConnectedMasterTokenProperty(hudson.util.Secret.fromString(UUID.randomUUID().toString())))
    // Note: ConnectedMasterCascProperty supported since 2.277.4.x. For earlier version, comment out the following.
    newInstance.getProperties().replace(new com.cloudbees.opscenter.server.casc.config.ConnectedMasterCascProperty(cascBundle))
}

/********************
 * CONFIGURE CONTROLLER *
 ********************/

/**
 * Configure the Controller provisioning details. Refer to the `config.xml` for more details.
 * Similar to configuring a Managed Controller from the UI
 */
KubernetesMasterProvisioning controllerProvisioning = new KubernetesMasterProvisioning()
controllerProvisioning.setDomain(controllerName.toLowerCase())

/**
 * Apply Managed Controller provisioning configuration (similar to what is configured through the Managed Controller UI)
 * Note: If not setting properties explicitly, the defaults will be used.
 */
controllerProvisioning.setDisk(k8sDisk)
controllerProvisioning.setMemory(k8sMemory)
if(k8sMemoryRatio) {
    controllerProvisioning.setHeapRatio(new com.cloudbees.jce.masterprovisioning.Ratio(k8sMemoryRatio))
    /**
     * For versions earlier than 2.235.4.1 (Master Provisioning plugin 2.5.6), use setRatio
     * controllerProvisioning.setRatio(k8sMemoryRatio)
     */
}
controllerProvisioning.setCpus(k8sCpus)
controllerProvisioning.setFsGroup(k8sFsGroup)
controllerProvisioning.setAllowExternalAgents(k8sAllowExternalAgents)
controllerProvisioning.setClusterEndpointId(k8sClusterEndpointId)
controllerProvisioning.setEnvVars(k8sEnvVars)
controllerProvisioning.setJavaOptions(k8sJavaOptions)
controllerProvisioning.setJenkinsOptions(k8sJenkinsOptions)
controllerProvisioning.setImage(k8sImage)
controllerProvisioning.setImagePullSecrets(k8sImagePullSecrets)
controllerProvisioning.setLivenessInitialDelaySeconds(k8sLivenessInitialDelaySeconds)
controllerProvisioning.setLivenessPeriodSeconds(k8sLivenessPeriodSeconds)
controllerProvisioning.setLivenessTimeoutSeconds(k8sLivenessTimeoutSeconds)
controllerProvisioning.setReadinessInitialDelaySeconds(k8sReadinessInitialDelaySeconds)
controllerProvisioning.setReadinessFailureThreshold(k8sReadinessFailureThreshold)
controllerProvisioning.setReadinessTimeoutSeconds(k8sReadinessTimeoutSeconds)
controllerProvisioning.setStorageClassName(k8sStorageClassName)
controllerProvisioning.setSystemProperties(k8sSystemProperties)
controllerProvisioning.setNamespace(k8sNamespace)
controllerProvisioning.setNodeSelectors(k8sNodeSelectors)
controllerProvisioning.setTerminationGracePeriodSeconds(k8sTerminationGracePeriodSeconds)
controllerProvisioning.setYaml(k8sYaml)

/**
 * Provide Controller item general configuration (similar to what is configured through the Controller UI)
 * Note: If not setting properties explicitly, the defaults will be used.
 */
if (controllerPropertyOwners != null && !controllerPropertyOwners.isEmpty()) {
    newInstance.getProperties().replace(new ConnectedMasterOwnerProperty(controllerPropertyOwners, controllerPropertyOwnersDelay))
}
newInstance.getProperties().replace(new ConnectedMasterLicenseServerProperty(new ConnectedMasterLicenseServerProperty.DescriptorImpl().defaultStrategy()))

/**
 * Save the configuration
 */
newInstance.setConfiguration(controllerProvisioning)
newInstance.save()

/**
 * Retrieve the controller from the API and print the details of the created Managed Controller
 */
def instance = jenkins.getItemByFullName(newInstance.fullName, ManagedMaster.class)
println "${instance.fullName}"
println " id: ${instance.id}"
println " idName: ${instance.idName}"

/******************************
 * PROVISION AND START CONTROLLER *
 ******************************/

instance.provisionAndStartAction()
println "Started the controller..."
return
