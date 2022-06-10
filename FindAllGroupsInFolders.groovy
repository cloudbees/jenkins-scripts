import com.cloudbees.hudson.plugins.folder.AbstractFolder
import nectar.plugins.rbac.groups.GroupContainer
import nectar.plugins.rbac.groups.GroupContainerLocator

//Parent folder name to start with
String folderName = 'EmptyFolder'
AbstractFolder folderItem = Jenkins.instance.getAllItems(AbstractFolder.class).find{ (it.name == folderName) }
print "Folder : " + folderItem.name + "\n"

GroupContainer container = GroupContainerLocator.locate(folderItem);
findAllGroups(container)

def findAllGroups(GroupContainer fpgc) {
  if (fpgc != null) {
    fpgc.getGroups().findAll { it != null }.each {
      println "  Group: " + it.name
      println '  Memberships: '
      // For RBAC Plugin < 5.66, use the following
      // it.getMembers().each{ println '    Member : ' + it }

      // For RBAC Plugin 5.66 or later, use the following
      it.getUsers().each { println '    User: ' + it }
      it.getGroups().each { println '    Group: ' + it }
      it.getMembers().each { println '    Ambiguous Member: ' + it }
    }
  }
}

findAllItems(folderItem.getItems())

def findAllItems(items){
  for(item in items)
  {
    if (item instanceof AbstractFolder) {
      GroupContainer container = GroupContainerLocator.locate(item);
      println "Folder: " + item.name
      findAllGroups(container)
      //Drill into folders
      findAllItems(((AbstractFolder) item).getItems())
    }
  }
}
return