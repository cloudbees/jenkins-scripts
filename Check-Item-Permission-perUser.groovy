/*
Author: Alex Taylor
Since: June 2019
Description: This script will get all items and then print every user's permission on the item itself
*/

iimport org.acegisecurity.*
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.ACLContext;
import hudson.model.User
import jenkins.security.*
import java.util.Date

Set failedUsers = [] 

for (i in Jenkins.instance.allItems) {
  println(i.getFullDisplayName())
  for (u in User.getAll()){
    if (failedUsers.contains(u)){
      break;
    }
    def permissionsList = u.getId().toString() + ": ";
    //READ, CONFIGURE, CREATE, DELETE, UPDATE
    for(p in permissions.values()){
      try{
        if(i.getACL().hasPermission(u.impersonate(), Item.(p.name()))){
          permissionsList = permissionsList + p.toString() + " "
        }
      }
      catch(Exception e){
        println("Failed to impersonate user: " + u.getId())
        failedUsers.add(u);
        break;
      }
    }
    println(permissionsList)
  }
}

enum permissions{
  READ, CONFIGURE, CREATE, DELETE
}
