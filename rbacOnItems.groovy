import hudson.model.User
import jenkins.model.Jenkins

import java.util.List
import java.util.Map
import java.util.Set
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet


def getExternalGroup(String name) {
  try {
    hudson.security.GroupDetails groupDetails = nectar.plugins.rbac.strategy.RoleMatrixAuthorizationPlugin.getGroupDetails(name)
    if (groupDetails != null) {
      return new nectar.plugins.rbac.assignees.ExternalGroupAssignee(groupDetails)
    }
  } catch (org.acegisecurity.userdetails.UsernameNotFoundException e) {
    // ignore
  } catch (org.springframework.dao.DataAccessException e) {
    // ignore
  }
  return null;
}

def put(Map map, String key, String value) {
  if (!map.containsKey(key)) {
    map.put(key,new HashSet())
  } 
  
  if (value != null) {
    map.get(key).add(value)
  }
}

def getExternalsInformation() {
  Map<String,Object> yaml = new HashMap()

  Map<String,String> users = new HashMap()
  Map<String,Set<String>> userGroups = new HashMap()
  Map<String,Set<String>> groupMembers = new HashMap()

  User.getAll().each{ user -> 
    if (user.getProperty(jenkins.security.LastGrantedAuthoritiesProperty.class)) {
      users.put(user.getId(), user.getFullName())
      put(userGroups, user.getId(), null)
      
      try {
      Jenkins.instance.getSecurityRealm().loadUserByUsername(user.getId()).getAuthorities().each{ group ->     

        nectar.plugins.rbac.assignees.ExternalGroupAssignee external = getExternalGroup(group.getAuthority())
        if (external != null) {
          put(userGroups, user.getId(), external.getId())
          
          if (external.getMembers() != null) {
            external.getMembers().each { member -> 
              String value = member
              if (!users.containsValue(member)) {
                User externalUser = User.get(member, false)
                if (externalUser != null) {
                  users.put(externalUser.getId(), externalUser.getFullName())
                  value = externalUser.getId()
                }
              }
              put(groupMembers, external.getId(), value)
            }
          }
        }
      }
      } catch (err) {
      }
    }
  }

  if (!userGroups.isEmpty()) {
    Map<String,Object> externalYaml = new HashMap()
    List<Map<String,Object>> usersYaml = new ArrayList()

    userGroups.entrySet().each { entry -> 
      Map map = new HashMap()
      map.put(entry.getKey(), entry.getValue())
      usersYaml.add(map)
    }
    externalYaml.put("users", usersYaml)

    List<Map<String,Object>> groupsYaml = new ArrayList()
    groupMembers.entrySet().each { entry -> 
      Map map = new HashMap()
      map.put(entry.getKey(), entry.getValue())
      groupsYaml.add(map)
    }
    externalYaml.put("groups", groupsYaml)

    yaml.put("external", externalYaml)
  }

  return yaml
}


/*
 * Returns the groups information from the container
 */
def getGroups(nectar.plugins.rbac.groups.GroupContainer gc) {
  List<Map<String,Object>> groups = new ArrayList()
  
  gc.groups.each{ g -> 
    Map<String,Object> map = new HashMap()
    map.put("name",g.name)
    if(!g.roles.isEmpty()) {
      map.put("roles", g.roles)
    }
    if(!g.getMembership().isEmpty()) {
      List<Map<String,Object>> list = new ArrayList() 
    
      g.getMembership().each{ m -> 
        Map<String,String> assignee = new HashMap()
        if (m instanceof nectar.plugins.rbac.assignees.UserAssignee) {
          assignee.put("kind", "user")
          assignee.put("name", m.getId())
        } else if (m instanceof nectar.plugins.rbac.assignees.GroupAssignee) {
          assignee.put("kind", "internal")
          assignee.put("name", m.getId())
        } else if (m instanceof nectar.plugins.rbac.assignees.ExternalGroupAssignee) {
          assignee.put("kind", "external")
          assignee.put("name", m.getId())
        } else {
          assignee.put("kind", "unknown")
          assignee.put("name", m.getId())
        }
        list.add(assignee)
      } 
      map.put("members", list)
    }
    groups.add(map)
  
  }
  
  return groups
}


/*
 * Returns the map definition of the item
 */
def processItem(item) {
  Map<String,Object> yaml = new HashMap()

  yaml.put("kind", item.getClass().getName())
  yaml.put("name", item.getName())
  yaml.put("fullName", item.getFullName())
  
  nectar.plugins.rbac.groups.GroupContainer gc = nectar.plugins.rbac.groups.GroupContainerLocator.locate(item);
  List<Map<String,Object>> groups = getGroups(gc)
  if(!groups.isEmpty()) {
    yaml.put("groups", groups)
  }

  if (!gc.getRoleFilters().isEmpty()){
    yaml.put("roleFilters", gc.getRoleFilters())
  }
  

  // Views and items
  if(item instanceof com.cloudbees.hudson.plugins.folder.AbstractFolder){
    
    // Views
    List<Map<String,Object>> views = new ArrayList()
    item.getViews().each{v ->   
      nectar.plugins.rbac.groups.GroupContainer vc = nectar.plugins.rbac.groups.GroupContainerLocator.locate(v);
      if (vc != null && !v.name.equals("All") && !vc.groups.isEmpty()) {
        Map<String,Object> map = new HashMap()
        map.put("name", v.name)

        List<Map<String,Object>> viewgroups = getGroups(gc)
        if(!viewgroups.isEmpty()) {
          map.put("groups", viewgroups)
        }
     
        views.add(map)
      }
    }
    if(!views.isEmpty()) {
      yaml.put("views", views)
    }

    // Items
    List<Map<String,Object>> children = new ArrayList()
    item.getItems().each{ i -> 
      Map<String,Object> child = processItem(i)
      children.add(child)
    }
    if(!children.isEmpty()) {
      yaml.put("items", children)
    }
  }
  return yaml  
}

/**
 * Returns all items if fullName is null or the structure starting in fullName.
 */
def rbacOnItems(String fullName){
  Map<String,Object> yaml = new HashMap()

  List<Map<String,Object>> items = new ArrayList()

  if (fullName != null) {
    def folder = Jenkins.instance.getItemByFullName(fullName)
    Map<String,Object> first = processItem(folder)
    items.add(first)
  } else {
    Jenkins.instance.getItems().each{ item -> 
      items.add(processItem(item))
    }
  }

  if(!items.isEmpty()) {
    yaml.put("items", items)
  }

  return yaml
}

def globalGroups() {
  Map<String,Object> yaml = new HashMap()

  nectar.plugins.rbac.groups.GroupContainer gc = nectar.plugins.rbac.groups.GroupContainerLocator.locate(Jenkins.instance);
  List<Map<String,Object>> groups = getGroups(gc)
  if(!groups.isEmpty()) {
    yaml.put("groups", groups)
  }

  return yaml
}

/*
 * Main 
 */
Map<String,Object> yaml = new HashMap()

yaml.put("userIdStrategy", Jenkins.instance.getSecurityRealm().getUserIdStrategy().toString())
yaml.put("groupIdStrategy", Jenkins.instance.getSecurityRealm().getGroupIdStrategy().toString())

getExternalsInformation().entrySet().each{ entry ->
  yaml.put(entry.getKey(), entry.getValue())
}

globalGroups().entrySet().each{ entry ->
  yaml.put(entry.getKey(), entry.getValue())
}

rbacOnItems().entrySet().each{ entry ->
  yaml.put(entry.getKey(), entry.getValue())
}


def builder = new groovy.json.JsonBuilder()
builder.call(yaml)
def prettyString = builder.toPrettyString() 

new java.io.File(Jenkins.instance.getRootDir(), "rbacOnItems.txt").withWriter('utf-8') { writer ->
    writer.write(prettyString)
} 


println prettyString 
return prettyString
