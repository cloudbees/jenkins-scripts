/**
 Create a Managed Master on PSE using code
 Notes:
 1./ This script has been tested for CJE1 (Mesosphere) -Legacy
 2./ For Managed Team Management see: https://go.cloudbees.com/docs/cloudbees-documentation/admin-cje/cje-ux/#_command_line_interface
 **/

/*** BEGIN META {
 "name" : "Create a Team Master in CloudBees Core on Modern Cloud Platform",
 "comment" : "This script creates a Kubernetes Managed Master programmatically similarly to what can be done through the UI. 
 It has been tested with version 2.176.3.2 of CloudBees Core",
 "parameters" : [],
 "core": "2.176.3.2",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.masterprovisioning.kubernetes.KubernetesImagePullSecret
import com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning
import com.cloudbees.opscenter.bluesteel.BlueSteelConstants
import com.cloudbees.opscenter.bluesteel.model.Member
import com.cloudbees.opscenter.bluesteel.model.PredefinedRecipes
import com.cloudbees.opscenter.bluesteel.model.TeamModel
import com.cloudbees.opscenter.server.bluesteel.BlueSteelHelper
import com.cloudbees.opscenter.server.bluesteel.ConnectedMasterTeamProperty
import com.cloudbees.opscenter.server.bluesteel.TeamInfo
import com.cloudbees.opscenter.server.bluesteel.TeamMasterSetupScheduler
import com.cloudbees.opscenter.server.bluesteel.security.BlueSteelDefaultRoleManager
import com.cloudbees.opscenter.server.bluesteel.security.BlueSteelSecurityUtils
import com.cloudbees.opscenter.server.bluesteel.security.xml.TeamSecurity
import com.cloudbees.opscenter.server.model.ConnectedMasterProperty
import com.cloudbees.opscenter.server.model.ConnectedMasterPropertyDescriptor
import com.cloudbees.opscenter.server.model.ManagedMaster
import com.cloudbees.opscenter.server.properties.ConnectedMasterLicenseServerProperty
import com.cloudbees.opscenter.server.properties.ConnectedMasterOwnerProperty
import hudson.Util
import hudson.model.User
import hudson.util.DescribableList

/*****************
 * INPUTS        *
 *****************/

def teamName = "team-from-groovy"

/**
 * Following attributes may be specified. The values proposed are the default from version 2.2.9 of Master Provisioning
 *
 * Note: If not setting properties explicitly, the defaults will be used.
 */
/* Master */
String  masterDescription = ""
String  masterPropertyOwners = ""
Integer masterPropertyOwnersDelay = 5

/* Master Provisioning */
Integer k8sDisk = 50
Integer k8sMemory = 3072
Double  k8sMemoryRatio = 0.7d
Double  k8sCpus = 1
String  k8sFsGroup = "1000"
Boolean k8sAllowExternalAgents = false
String  k8sClusterEndpointId = "default"
String  k8sEnvVars = ""
String  k8sJavaOptions = ""
String  k8sJenkinsOptions = ""
String  k8sImage = 'CloudBees Core - Managed Master - 2.176.4.3'
List<KubernetesImagePullSecret> k8sImagePullSecrets = Collections.emptyList()
// Example: 
//   def k8sImagePullSecret1 = new KubernetesImagePullSecret(); k8sImagePullSecret1.setValue("useast-reg")
//   List<KubernetesImagePullSecret> k8sImagePullSecrets = Arrays.asList(k8sImagePullSecret1)
Integer k8sLivenessInitialDelaySeconds = 300
Integer k8sLivenessPeriodSeconds = 10
Integer k8sLivenessTimeoutSeconds = 10
String  k8sStorageClassName = ""
String  k8sSystemProperties = ""
String  k8sNamespace = ""
String  k8sNodeSelectors = ""
Long    k8sTerminationGracePeriodSeconds = 1200L
String  k8sYaml = ""

/* Team */
String iconName = "cloudbees"
String iconNColor = "#7ac2d6"
Member [] members = [
        new Member(User.current().getId(), Arrays.asList("TEAM_ADMIN")),
        new Member("authenticated", Arrays.asList("TEAM_GUEST"))
        // ... Add more TEAM_ADMIN, TEAM_MEMBER or TEAM_GUEST
] as Member[]

/*****************
 * VALIDATION    *
 *****************/

/**
 * Validate that the master name is not empty
 */
name = Util.fixEmpty(teamName)
if (name == null) {
    println("[ERROR] A team name must be provided")
    return
}

/**
 * Validate that there is a "teams" folder
 */
AbstractFolder teamsFolder = jenkins.model.Jenkins.instanceOrNull.getItemByFullName(BlueSteelConstants.CJOC_TEAMS_FOLDER_NAME, AbstractFolder.class)
if (teamsFolder == null) {
    println("[ERROR] Could not create the 'Teams' folder on Operations Center There may be another item which is " +
            "not a folder, with the same name. No Teams can be created until this item is removed")
    return
}

teamName = BlueSteelHelper.sanitizeName(name)
String teamDisplayName = name

if(BlueSteelHelper.getTeamMaster(teamName) != null) {
    println("[ERROR] Another team is named that already!")
    return
}

/**
 * Validate there isn't any item already with the same name, only validating in the root folder where we are going to create it 
 */
if (teamsFolder.getItem(teamName) != null) {
    printf("The Team \"%s\" has a naming collision with an Item named \"%s\" in the folder \"%s\"", teamDisplayName, teamName,
            BlueSteelConstants.CJOC_TEAMS_FOLDER_NAME)
    return
}

/**********************
 * PREPARE TEAM MODEL *
 **********************/

/**
 * Provide Team configuration (similar to what is configured through the Teams UI)
 */
TeamModel teamModel = new TeamModel()
teamModel.setName(teamName)
teamModel.setDisplayName(teamDisplayName)
teamModel.setMembers(members)
teamModel.setIcon(new TeamModel.TeamIcon(iconName, iconNColor))
teamModel.setCreationRecipe(PredefinedRecipes.BASIC)

/*****************
 * CREATE MASTER *
 *****************/

/**
 * Create a Managed Masters with just a name (this will automatically fill required values for id, idName, grantId, etc...)
 * Similar to creating an item in the UI
 */
ManagedMaster master = teamsFolder.createProject(ManagedMaster.class, teamName)
master.setDisplayName(teamModel.getDisplayName())
master.setDescription(masterDescription)

/********************
 * CONFIGURE MASTER *
 ********************/

/**
 * Initialize the default MasterProvisioning configuration
 */
KubernetesMasterProvisioning masterProvisioning = new KubernetesMasterProvisioning()
masterProvisioning.setDomain(teamName.toLowerCase())

/**
 * Apply Managed Master provisioning configuration (similar to what is configured through the Managed Master UI)
 * Note: If not setting properties explicitly, the defaults will be used.
 */
masterProvisioning.setDisk(k8sDisk)
masterProvisioning.setMemory(k8sMemory)
masterProvisioning.setRatio(k8sMemoryRatio)
masterProvisioning.setCpus(k8sCpus)
masterProvisioning.setFsGroup(k8sFsGroup)
masterProvisioning.setAllowExternalAgents(k8sAllowExternalAgents)
masterProvisioning.setClusterEndpointId(k8sClusterEndpointId)
masterProvisioning.setEnvVars(k8sEnvVars)
masterProvisioning.setJavaOptions(k8sJavaOptions)
masterProvisioning.setJenkinsOptions(k8sJenkinsOptions)
masterProvisioning.setImage(k8sImage)
masterProvisioning.setImagePullSecrets(k8sImagePullSecrets)
masterProvisioning.setLivenessInitialDelaySeconds(k8sLivenessInitialDelaySeconds)
masterProvisioning.setLivenessPeriodSeconds(k8sLivenessPeriodSeconds)
masterProvisioning.setLivenessTimeoutSeconds(k8sLivenessTimeoutSeconds)
masterProvisioning.setStorageClassName(k8sStorageClassName)
masterProvisioning.setSystemProperties(k8sSystemProperties)
masterProvisioning.setNamespace(k8sNamespace)
masterProvisioning.setNodeSelectors(k8sNodeSelectors)
masterProvisioning.setTerminationGracePeriodSeconds(k8sTerminationGracePeriodSeconds)
masterProvisioning.setYaml(k8sYaml)

/**
 * Save the configuration
 */
master.setConfiguration(masterProvisioning)
master.save()

/**
 * Team Master required configuration
 */
// Force install the plugins we needs
BlueSteelHelper.replaceSystemProperty(master, BlueSteelConstants.IM_PLUGIN_WAR_PROFILE_PROPERTY_NAME, BlueSteelConstants.IM_PLUGIN_WAR_PROFILE)
// Force auto install of incremental updates during first boot
BlueSteelHelper.replaceSystemProperty(master, BlueSteelConstants.BK_AUTO_INTALL_INCREMENTAL_PROPERTY_NAME, Boolean.TRUE.toString())
// Disable full upgrades for this first boot or the incremental updates won't be taken into account
BlueSteelHelper.replaceSystemProperty(master, BlueSteelConstants.BK_NO_FULL_UPGRADE_PROPERTY_NAME, Boolean.TRUE.toString())

/**
 * Team Master properties
 */
// Properties
DescribableList<ConnectedMasterProperty, ConnectedMasterPropertyDescriptor> masterProperties = master.getProperties()
if (masterPropertyOwners != null && !masterPropertyOwners.isEmpty()) {
    masterProperties.replace(new ConnectedMasterOwnerProperty(masterPropertyOwners, masterPropertyOwnersDelay))
}
masterProperties.replace(new ConnectedMasterLicenseServerProperty(new ConnectedMasterLicenseServerProperty.DescriptorImpl().defaultStrategy()))

/**
 * Team Master required property
 */
ConnectedMasterTeamProperty property = new ConnectedMasterTeamProperty()
masterProperties.replace(property)

// Set Team information
TeamInfo teamInfo = new TeamInfo(
        teamModel.getName(),
        teamModel.getDisplayName(),
        teamModel.getIcon() != null ? new TeamInfo.TeamIcon(teamModel.getIcon()) : null,
        null,
        teamModel.getCreationRecipe())
property.setTeamInfo(teamInfo)
// Set Team security
TeamSecurity teamSecurity = new TeamSecurity(
        BlueSteelDefaultRoleManager.getDefaultRolesMap(),
        BlueSteelSecurityUtils.toUserMapping(teamModel.getMembers()),
        BlueSteelDefaultRoleManager.getDefaultRole())
BlueSteelSecurityUtils.checkTeamSecurityRule(teamSecurity)
property.setTeamSecurity(teamSecurity)

/**
 * Save the configuration
 */
master.save()

/**
 * Retrieve the master from the API and print the details of the created Managed Master
 */
def instance = jenkins.model.Jenkins.instanceOrNull.getItemByFullName(master.fullName, ManagedMaster.class)
println "${instance.name}"
println " id: ${instance.id}"
println " idName: ${instance.idName}"
println " info: ${instance.properties.get(ConnectedMasterTeamProperty.class).getTeamInfo()}"
println " security: ${instance.properties.get(ConnectedMasterTeamProperty.class).getTeamSecurity()}"

/******************************
 * PROVISION AND START MASTER *
 ******************************/

master.provisionAndStartAction()
println "Started the master..."

/**
 * Schedule the Team Master creation and setup 
 */
TeamMasterSetupScheduler.get().submitTeamSetup(master)
return