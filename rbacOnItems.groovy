import java.util.List
import java.util.Map
import java.util.ArrayList
import java.util.HashMap


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
      if (!v.name.equals("All") && !vc.groups.isEmpty()) {
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


/*
 * Main 
 */
Map<String,Object> yaml = new HashMap()

List<Map<String,Object>> items = new ArrayList()
Jenkins.instance.getItems().each{ item -> 
  items.add(processItem(item))
}

if(!items.isEmpty()) {
  yaml.put("items", items)
}


//def folder = Jenkins.instance.getItemByFullName('development')
//Map<String,Object> first = processItem(folder)
//items.add(first)


def builder = new groovy.json.JsonBuilder()
builder.call(yaml)
return builder.toPrettyString()
