import jenkins.model.Jenkins;
import nectar.plugins.rbac.strategy.*;
import hudson.security.*;
import nectar.plugins.rbac.groups.*;
import nectar.plugins.rbac.roles.*;

//Obtain security configuration
RoleMatrixAuthorizationStrategyImpl strategy = RoleMatrixAuthorizationStrategyImpl.getInstance()
RoleMatrixAuthorizationConfig config = RoleMatrixAuthorizationPlugin.getConfig()

println 'Groups'
config.getGroups().each{ g ->
    println '\t' + g.name
    println '\t\t Group Roles'
    g.getAllRoles().each{rg -> println '\t\t\t' + rg}
    
    // RBAC Plugin < 5.66
    // println '\t\t Group Members'
    // g.getMembers().each{mg -> println '\t\t\t' + mg}
    
    // RBAC Plugin >= 5.66
    println '\t\t Group User Members'
    g.getUsers().each{mg -> println '\t\t\t' + mg}
    println '\t\t Group Group Members'
    g.getGroups().each{mg -> println '\t\t\t' + mg}
    println '\t\t Group Ambiguous Members'
    g.getMembers().each{mg -> println '\t\t\t' + mg}
}

println '*Roles*'
config.getRoles().each{r ->
    println '\t' + r
    println '\t\t Role Permissions'
    Role rc = new Role(r)
    rc.getPermissionProxies().each{p -> println '\t\t' + p.id + " - " + p.name}
    }

println '*Permissions*'
Permission.getAll().each{p -> println '\t' + p.id + " - " + p.name}

println 'create a new Role'
String roleName = "NewRole"
strategy.addRole(roleName)

println 'add all permission to NewRole'
Role rc = new Role(roleName)
for (Permission p: Permission.getAll()) {
    if(p.getEnabled() && p.owner == null){
        rc.doGrantPermissions(p.id)
    }
}

println 'remove permission from role'
rc.doRevokePermissions("hudson.model.Hudson.Read")

println 'create a new groups at different container levels'

// Get location for ClientMaster
locationCM = Jenkins.get().getAllItems().find{it.name.equals("ClientMaster")}
// Get location for a FolderA/FolderB
locationFolder = Jenkins.get.getAllItems().find{it.fullName.equals("FolderA/FolderB")}
// Get location at Root Level 
locationRoot = Jenkins.get()

// For the following example the group is created at root container (locationRoot) 
String groupName = "newGroup"
GroupContainer container = GroupContainerLocator.locate(locationRoot)
Group group = new Group(container, groupName)
Group groupToDelete = new Group(container, "groupToDelete")
// RBAC Plugin < 5.66
//group.doAddMember('userToDelete')
//group.doRemoveMember('userToDelete')
//group.doAddMember(groupToDelete.name)
//group.doRemoveMember(groupToDelete.name)
// RBAC Plugin >= 5.66
group.doAddUser('userToDelete')
group.doRemoveUser('userToDelete')
group.doAddGroup(groupToDelete.name)
group.doRemoveGroup(groupToDelete.name)
group.doAddMember('ambiguousMember')
group.doRemoveMember('ambiguousMember')
group.doGrantRole('roleToRevoke', 0, Boolean.TRUE)
group.doRevokeRole('roleToRevoke')
group.doGrantRole(roleName, 0, Boolean.TRUE)
container.addGroup(group)
