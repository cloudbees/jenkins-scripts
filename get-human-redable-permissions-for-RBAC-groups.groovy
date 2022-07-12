import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationStrategyImpl;
import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationConfig;
import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationPlugin;
import hudson.security.PermissionGroup;
import nectar.plugins.rbac.roles.Role;

//Obtain security configuration
RoleMatrixAuthorizationStrategyImpl strategy = RoleMatrixAuthorizationStrategyImpl.getInstance()
RoleMatrixAuthorizationConfig config = RoleMatrixAuthorizationPlugin.getConfig()

def map = [:]

//Populate map
PermissionGroup.getAll().each { permissionGroup ->
  permissionGroup.permissions.each { permission ->
    if (permission.enabled && permissionGroup.id != "N/A") {
      map[permission.id] = permissionGroup.id + '/' + permission.name
    }
  }
}

config.getRoles().each{r ->
    DESCRIPTION = r
    Role rc = new Role(r)

    rc.getPermissionProxies().each{p ->
        println DESCRIPTION + ' - ' + map[p.id]
    }
}

return null