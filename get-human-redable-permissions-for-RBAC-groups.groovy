import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationConfig;
import nectar.plugins.rbac.strategy.RoleMatrixAuthorizationPlugin;
import nectar.plugins.rbac.roles.Role;

RoleMatrixAuthorizationPlugin.getConfig().getRoles().each{r ->
    new Role(r).getPermissionProxies().each{p ->
        println r + " - " + p.group.title + '/' + p.name
    }
}

return null