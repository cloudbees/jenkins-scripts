import com.cloudbees.hudson.plugins.folder.*
import com.cloudbees.jenkins.plugins.foldersplus.*
import com.cloudbees.hudson.plugins.folder.properties.FolderProxyGroupContainer
  
String name1 = 'test'
String name2 = 'test2'


AbstractFolder < ? > folderAbs1 = AbstractFolder.class.cast(Jenkins.instance.getAllItems(Folder.class).find{it.name.equals(name1)})
AbstractFolder < ? > folderAbs2 = AbstractFolder.class.cast(Jenkins.instance.getAllItems(Folder.class).find{it.name.equals(name2)})

if(folderAbs1 == null || folderAbs2 == null){
    println 'folders not found'
} else {
  /* 
  // CloudBess Folder > 5.2.2
  SecurityGrantsFolderProperty property1 = SecurityGrantsFolderProperty.of(folderAbs1)
  SecurityGrantsFolderProperty property2 = SecurityGrantsFolderProperty.of(folderAbs2,true)

  println "property2 : " + property2
  println "property1 : " + property1
  if (property1 != null) {
      property1.getSecurityGrants().each {
          println "SecurityGrant : " + it
          property2.addSecurityGrant(it)
      }
  }
  
  */
  // CloudBees Folder <=5.2.2
  FolderProxyGroupContainer propertyFPG1 = folderAbs1.getProperties().get(FolderProxyGroupContainer.class);
  FolderProxyGroupContainer propertyFPG2 = folderAbs2.getProperties().get(FolderProxyGroupContainer.class);
  
  if (propertyFPG2 == null) {
    	FolderProxyGroupContainer c = new FolderProxyGroupContainer()
      	folderAbs2.getProperties().replace(c)
        c.owner=folderAbs2
      	propertyFPG2 = folderAbs2.getProperties().get(FolderProxyGroupContainer.class)
  }
  
  println "propertyFPG2 : " + propertyFPG2
  println "propertyFPG1 : " + propertyFPG1
  if (propertyFPG1 != null) {
    	propertyFPG1.getGroups().findAll{it != null}.each {
          println "Group : " + it
          propertyFPG2.addGroup(it)
      	}
       	propertyFPG1.getRoleFilters().findAll{it != null}.each {
          println "RoleFilter : " + it
          propertyFPG2.addRoleFilter(it)
      	}
	 	propertyFPG2.save()
  }
}