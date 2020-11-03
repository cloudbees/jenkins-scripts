/**
Author: James Nord, Esther Álvarez
Since: November 2020
Description: List the views that have RBAC groups or role filters configured. Useful to report the views affected by the release of nectar-rbac 5.42
Parameters: none
Scope: Any with nectar-rbac installed
**/
 
import nectar.plugins.rbac.groups.ViewProxyGroupContainer

Set<View> allViews = new HashSet()
Set<View> viewsWithProperty = new HashSet()
Set<View> viewsWithRoleFilters = new HashSet()

// root views
allViews.addAll(Jenkins.get().allViews)

// look for views defined in folders
for (viewGroup in Jenkins.get().getAllItems(ViewGroup.class)) {
  allViews.addAll(viewGroup.allViews)
}


// filter the views to only those with groups defined
for (def v : allViews) {
  // the group will have a ViewProxyGroupContainer property that will have a non zero amount of groups.
  gc = v.properties.get(ViewProxyGroupContainer.class)
  if (gc != null && !gc.groups.empty) {
    viewsWithProperty.add(v)
  }
  if (gc != null && !gc.roleFilters.empty) {
  	viewsWithRoleFilters.add(v);
  }
}

println("Found ${allViews.size()} views, of which ${viewsWithProperty.size()} have groups defined on them, and ${viewsWithRoleFilters.size()} have role filters. \n")
if (!viewsWithProperty.empty) {
  println "The following views have groups defined on them: "
  for (def v : viewsWithProperty) {
    println(" * " + v.viewUrl + "  [" + v.class.simpleName + "]");
  }
  println ""
}
if (!viewsWithRoleFilters.empty) {
  println "The following views have role filters defined on them: "
  for (def v : viewsWithRoleFilters) {
    println(" * " + v.viewUrl + "  [" + v.class.simpleName + "]");
  }
}
