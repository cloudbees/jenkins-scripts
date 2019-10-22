/**
 Create a Managed Master on PSE using code
 Notes:
 1./ This script has been tested for CJE1 (Mesosphere) -Legacy
 2./ For Managed Team Management see: https://go.cloudbees.com/docs/cloudbees-documentation/admin-cje/cje-ux/#_command_line_interface
 **/

/*** BEGIN META {
 "name" : "Create a Team Master in CloudBees Jenkins Enterprise",
 "comment" : "This script creates a Mesos Managed Master programmatically similarly to what can be done through the UI. 
 It has been tested with version 1.11.22 of CloudBees Jenkins Enterprise",
 "parameters" : [],
 "core": "2.176.3.2",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.jce.masterprovisioning.mesos.HealthCheckConfiguration
import com.cloudbees.jce.masterprovisioning.mesos.MesosMasterProvisioning
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
Integer mesosDisk = 50
Integer mesosMemory = 3072
Double  mesosMemoryRatio = 0.7d
Double  mesosCpus = 1
Boolean mesosAllowExternalAgents = false
String  mesosClusterEndpointId = "default"
String  mesosEnvVars = ""
String  mesosJavaOptions = ""
String  mesosJenkinsOptions = ""
String  mesosSystemProperties = ""
String  mesosImage = 'CloudBees Jenkins Enterprise 2.176.4.3'
Integer mesosGracePeriodSeconds = 1200
Integer mesosIntervalSeconds = 60

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
MesosMasterProvisioning masterProvisioning = new MesosMasterProvisioning()
masterProvisioning.setDomain(teamName.toLowerCase())

/**
 * Apply Managed Master provisioning configuration (similar to what is configured through the Managed Master UI)
 * Note: If not setting properties explicitly, the defaults will be used.
 */
masterProvisioning.setDisk(mesosDisk)
masterProvisioning.setMemory(mesosMemory)
masterProvisioning.setRatio(mesosMemoryRatio)
masterProvisioning.setCpus(mesosCpus)
masterProvisioning.setAllowExternalAgents(mesosAllowExternalAgents)
masterProvisioning.setClusterEndpointId(mesosClusterEndpointId)
masterProvisioning.setEnvVars(mesosEnvVars)
masterProvisioning.setJavaOptions(mesosJavaOptions)
masterProvisioning.setJenkinsOptions(mesosJenkinsOptions)
masterProvisioning.setImage(mesosImage)
masterProvisioning.setSystemProperties(mesosSystemProperties)
HealthCheckConfiguration hc = new HealthCheckConfiguration()
hc.setGracePeriodSeconds(mesosGracePeriodSeconds)
hc.setIntervalSeconds(mesosIntervalSeconds)
masterProvisioning.setHealthCheckConfiguration(hc)

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