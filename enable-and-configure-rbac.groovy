import jenkins.model.Jenkins
import hudson.security.Permission
import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationStrategyImpl
import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationConfig
import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationPlugin
import nectar.plugins.rbac.groups.Group
import nectar.plugins.rbac.groups.GroupContainer
import nectar.plugins.rbac.groups.GroupContainerLocator
import nectar.plugins.rbac.groups.RootProxyGroupContainer
import nectar.plugins.rbac.roles.Role
import nectar.plugins.rbac.roles.Roles
import java.io.IOException
import org.yaml.snakeyaml.Yaml
/*
 * Some important lessons I learned writing this code:
 * 1. This is a cached repository https://repo.cloudbees.com/content/repositories/dev-connect
 *    So if the version of the plugin you are looking for does not appear, you need to try and type it into the URL
 *    directly, and as long as you get it correct, the plugin and it's files should appear as needed.
 * 2. From the Operations Center you need to find out what version of the
 *    Operations Center Server Role Based Access Control plugin is installed.
 *    Here are the releases https://release-notes.cloudbees.com/product/58
 * 3. Then you will need to look up the pom.xml for that version of the plugin here
 *    https://repo.cloudbees.com/content/repositories/dev-connect/com/cloudbees/operations-center/server/operations-center-rbac/
 *    to find what version of the nectar-rbac plugin is being used. (Which contains the nectar.plugin.rbac classes)
 * 4. You can then go here to download the javadocs for the plugin to see how it's changed when this code breaks
 *    https://repo.cloudbees.com/content/repositories/dev-connect/com/cloudbees/nectar/nectar-rbac/
 *
 * This script has been verified to work on Cloudbees Core 2.190.2.2 and nectar-rbac 5.25
 *
*/

// You definitely want to use javadocs instead of this, but it's still here just in case
void printAllMethods( obj ){
    if( !obj ){
        println( "Object is null\r\n" );
        return;
    }
    if( !obj.metaClass && obj.getClass() ){
        printAllMethods( obj.getClass() );
        return;
    }
    def str = "class ${obj.getClass().name} functions:\r\n";
    obj.metaClass.methods.name.unique().each{
        str += it+"(); ";
    }
    println "${str}\r\n";
}

// simple wrapper around addRole() that returns the Role object that was just created
Role createRole(String roleName) {
    RoleMatrixAuthorizationStrategyImpl strategy = RoleMatrixAuthorizationStrategyImpl.getInstance()
    strategy.addRole( roleName )
    return getRoleByName( roleName )
}

// A wrapper around removeRole that attempts to remove all the permissions and delete the role
// This will not succeed if the role is still associated to any groups
void deleteRole(String roleName) {
    RoleMatrixAuthorizationStrategyImpl strategy = RoleMatrixAuthorizationStrategyImpl.getInstance()
    Role roleToDelete = getRoleByName(roleName)
    List<String> permissionsToRemove = roleToDelete.getPermissionProxyIds()
    permissionsToRemove.each {
        roleToDelete.doRevokePermissions(it)
    }
    strategy.removeRole( roleToDelete.getDisplayName() )
}

// Permissions change depending on the plugins that are installed not just in the CJOC
// but also the Managed Masters. I think this is what are called "proxy permissions".
// Since the permissions will change over time, we need a dynamic way to list all of them
List<String> listAllPermissions() {
    List<String> validPermissions = []

    // We must add the role to the strategy in order to discover all the permissions
    Role tempRole = createRole('tempRole')
    // getAll() does not include proxy permission
    for (Permission p: Permission.getAll()) {
        try {
            p.setEnabled(true)
            tempRole.doGrantPermissions(p.id)
            validPermissions.add(p.id)
        } catch (IOException message) {
            println 'ignoring ' + p.id
            // at the time of writing these seem to be all the ignored permissions
            // hudson.security.Permission.FullControl
            // hudson.security.Permission.GenericRead
            // hudson.security.Permission.GenericWrite
            // hudson.security.Permission.GenericCreate
            // hudson.security.Permission.GenericUpdate
            // hudson.security.Permission.GenericDelete
            // hudson.security.Permission.GenericConfigure
        }
    }

    // Likely these permissions are from plugins in Managed Masters
    tempRole.getPermissionProxyIds().each { r ->
        tempRole.doGrantPermissions(r)
        validPermissions.add(r)
        //println 'added ' +r
    }

    deleteRole( tempRole.getDisplayName() )

    return validPermissions.unique()
}

// We need to be careful about how we change and update the group permissions.
// At the time of writing this code only expects a group called 'administrators'
// to be present in the rbac_config.yaml - meaning that all other groups must be
// created and updated by the 'onboarding' code
Map validateRootGroups(Map rootGroups) {
    List groupsFromConfig = rootGroups.keySet() as List
    boolean valid = true

    // Validate groups only contain administrators
    if (groupsFromConfig.size() != 1 ) {
        println 'some other group configuration is present'
        valid = false
    }

    if (!groupsFromConfig.contains() == 'administrators') {
        println 'root_groups config does not contain administrators group'
        valid = false
    }

    if (!valid) {
        throw new IOException()
    }
}

// We attempt to validate the permissions in the role_config look correct to the best
// of our knowledge, and we will throw an IOException if anything looks fishy
Map validateRolePermisisons(Map rolePermissions) {
    List rolesFromConfig = rolePermissions.keySet() as List
    boolean valid = true

    // Validate role_permissions
    rolesFromConfig.each {
        Map keyValidation = rolePermissions[it]
        List validKey = keyValidation.keySet() as List

        if (validKey.size() != 1) {
            println 'something else than permissions found?' + validKey
            valid = false
        }
        if (validKey[0] != 'permissions') {
            println 'invalid key = ' + validKey[0]
            valid = false
        }
    }

    if (!valid) {
        throw new IOException()
    }
}

// a wrapper around the methods validateRolePermissions and validateRootGroups
Map validateRbacConfig(configFile) {
    Yaml parser = new Yaml()
    Map role_config = [:]
    Map root_groups = [:]
    Map rbac_config = parser.load((configFile as File).text)
    if (rbac_config.containsKey('role_config')) {
        validateRolePermisisons(rbac_config.role_config)
    }
    else {
        throw new IOException()
    }

    if (rbac_config.containsKey('root_groups')) {
        validateRootGroups(rbac_config.root_groups)
    }
    else {
        throw new IOException()
    }

    return rbac_config
}

// return the Role object of an existing role from it's name
Role getRoleByName( String roleName ) {
    List<Role> allRoles = new Roles().getRoles()
    Role roleToReturn = null
    allRoles.each {
        if (roleName == it.getDisplayName()) {
            roleToReturn = it
        }
    }
    return roleToReturn
}

// Attempt to create any new roles in the role_config and remove any that are not in the role_config
void addOrRemoveRoles(List<String> rolesFromConfig) {
    List<String> existingRoleNames = []
    List<String> rolesToRemove = []
    List<String> rolesToAdd = []

    // get all the existing roles
    List<Role> allRoles = new Roles().getRoles()
    allRoles.each {
        existingRoleNames.add(it.getDisplayName())
    }
    // add any new roles in the config file
    rolesToAdd = rolesFromConfig - existingRoleNames
    println 'rolesToAdd = ' + rolesToAdd
    rolesToAdd.each {
        createRole(it)
    }
    // delete any roles not in the config file
    rolesToRemove = existingRoleNames - rolesFromConfig
    println 'rolesToRemove = ' + rolesToRemove
    rolesToRemove.each {
        deleteRole(it)
    }
}

// Add any permissions that exist in the role_config and remove any permissions not included in the role_config.
// There are two magic keywords "all" and "none" to make things slightly easier for admins.
void applyRolesAndPermissions(Map rbac_config) {
    List<String> allPermissions = listAllPermissions()
    println "allPermissions: "
    allPermissions.each { println it }

    List<String> rolesFromConfig = rbac_config.role_config.keySet() as List

    // update our roles before assigning permissions
    addOrRemoveRoles(rolesFromConfig)

    rolesFromConfig.each{
        println 'role: ' + it
        List<String> permissionsInRole = rbac_config.role_config[it].permissions
        // 'none' has top priority in the list
        if (permissionsInRole.contains('none')) {
            Role noPermsRole = getRoleByName(it)
            allPermissions.each {
                noPermsRole.doRevokePermissions(it)
            }
            println '  remove all permissions'
        }
        // 'all' has second highest priority in the list
        else if (permissionsInRole.contains('all')) {
            Role allPermsRole = getRoleByName(it)
            println '  add all permissions'
            allPermissions.each {
                try {
                    allPermsRole.doGrantPermissions(it)
                } catch (IOException message) {
                    println '  ignoring permission ' + it
                }
            }
        }
        else {
            List<String> removePermissions = allPermissions - permissionsInRole
            Role modifyPermsRole = getRoleByName(it)
            removePermissions.each {
                modifyPermsRole.doRevokePermissions(it)
                println '  removed permission: ' + it
            }
            permissionsInRole.each {
                try {
                    modifyPermsRole.doGrantPermissions(it)
                    println '  granted permission: ' + it
                } catch (IOException message) {
                    println '  ignoring permission ' + it
                }
            }
        }
    }
}

// Root groups exist only on the cjoc level, and this code is only concerned with who should have admin on CJOC.
void applyRootGroups(Map rbac_config) {

    RoleMatrixAuthorizationConfig config = RoleMatrixAuthorizationPlugin.getConfig()
    RootProxyGroupContainer cjocRootContainer = RoleMatrixAuthorizationPlugin.getInstance().getRootProxyGroupContainer()
    List<String> groupsFromConfig = rbac_config.root_groups.keySet() as List

    List<Group> existingRootGroups = config.getGroups()
    List<Group> updatedRootGroups = new ArrayList<Group>()
    String adminGroupName = 'administrators'
    Group adminGroup = new Group(adminGroupName)

    for ( Group group : existingRootGroups ) {
        if ( adminGroupName != group.getName() ) {
            println "Existing non-admin group found: " + group
            updatedRootGroups.add(group)
        }
    }

    // create the new administrators group
    List<String> adminMembers = new ArrayList<String>()
    groupsFromConfig.each {
        // any extra validation needed?
        rbac_config.root_groups[it].each {
            adminMembers.add(it)
        }
    }
    // this will overwrite any axisting members!
    adminGroup.setMembers(adminMembers)

    updatedRootGroups.add(adminGroup)

    // This will overwrite the existing groups, so it must be a complete set
    config.setGroups(updatedRootGroups)

    // add role to group
    adminGroup.doGrantRole('cjoc_admin', 0, true)

    // set the top-level cjoc for administrators
    //locationFolder = Jenkins.instance.getItemByFullName('')
    GroupContainer container = GroupContainerLocator.locate(cjocRootContainer)
    container.addGroup(adminGroup)
}

void doit() {
    // enable RBAC
    RoleMatrixAuthorizationStrategyImpl rbac = new RoleMatrixAuthorizationStrategyImpl();
    Jenkins.instance.setAuthorizationStrategy(rbac)

    // check that yaml looks ok
    Map rbac_config = validateRbacConfig('/tmp/rbac_config.yaml' )

    // The groups applied in this rbac_config will overwrite the existing configuration!
    // It will not change any other groups
    // Do this first before modifying the roles and permissions!
    applyRootGroups(rbac_config)

    // add or remove all roles and permissions according to what is in the config
    applyRolesAndPermissions(rbac_config)
}

doit()

