import com.cloudbees.hudson.plugins.folder.*
import com.cloudbees.jenkins.plugins.foldersplus.*
import com.cloudbees.hudson.plugins.folder.properties.FolderProxyGroupContainer
import com.cloudbees.hudson.plugins.folder.properties.SubItemFilterProperty
  
String source = 'sub folder'
String destination = 'z'


AbstractFolder < ? > folderAbs1 = AbstractFolder.class.cast(Jenkins.instance.getAllItems(Folder.class).find{it.name.equals(source)})
AbstractFolder < ? > folderAbs2 = AbstractFolder.class.cast(Jenkins.instance.getAllItems(Folder.class).find{it.name.equals(destination)})

if(folderAbs1 == null || folderAbs2 == null){
    println 'folders not found'
} else {

  println folderAbs2
  SubItemFilterProperty propertyFPG1 = folderAbs1.getProperties().get(SubItemFilterProperty.class);
  folderAbs2.getItems().findAll{it instanceof Folder}.each { folderChild ->
    SubItemFilterProperty propertyFPG2 = folderChild.getProperties().get(SubItemFilterProperty.class);
    
    if (propertyFPG2 == null || propertyFPG2.allowedTypes == null) {
      SubItemFilterProperty c = new SubItemFilterProperty(new ArrayList<String>())
      folderChild.getProperties().replace(c)
      c.owner=folderChild
      propertyFPG2 = folderChild.getProperties().get(SubItemFilterProperty.class)
    }
    
    println "propertyFPG2 : " + propertyFPG2
    println "propertyFPG1 : " + propertyFPG1
    if (propertyFPG1 != null && propertyFPG1.allowedTypes != null) {
      propertyFPG2.allowedTypes.addAll(propertyFPG1.allowedTypes)
    }
  }
}