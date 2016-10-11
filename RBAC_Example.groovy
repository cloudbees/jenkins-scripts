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
    println '\t\t Group Memberships'
    g.getGroupMembership().each{mg -> println '\t\t\t' + mg}
    println '\t\t Group Members'
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

println 'create a new group'
String groupName = "newGroup"
GroupContainer container = GroupContainerLocator.locate(Jenkins.getInstance())
Group group = new Group(container, groupName)
group.doAddMember('tesla')
group.doAddMember('userToDelete')
group.doRemoveMember('userToDelete')
group.doGrantRole('roleToRevoke', 0, Boolean.TRUE)
group.doRevokeRole('roleToRevoke')
group.doGrantRole(roleName, 0, Boolean.TRUE)
container.addGroup(group)
