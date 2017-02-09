/**
Author: kuisathaverat
Description: list groups at top level on Master and CJOC from Master Console Script
**/
import hudson.model.*
import nectar.plugins.rbac.strategy.*;
import nectar.plugins.rbac.groups.*;
import com.cloudbees.opscenter.security.*
  
def rbac = RoleMatrixAuthorizationPlugin.getInstance()
def proxy = rbac.getRootProxyGroupContainer()
//Master Groups
proxy.getGroups().each{ println it.displayName }
//CJOC groups
proxy?.getParent().getGroups().each{ println it.displayName }
