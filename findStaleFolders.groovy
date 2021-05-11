import groovy.time.TimeCategory
import com.cloudbees.hudson.plugins.folder.Folder

now = new Date()
definitionOfStaleInDays = 100

def jenkinsFolders = Jenkins.instance.getAllItems(Folder)
jenkinsFolders.each { folder ->
  if(isStaleFolder(folder)){
    println "${folder.name} IS A STALE FOLDER ${constructParentPath(folder)}"
  }
}

boolean isStaleFolder(folder){
    boolean returnValue
    use(TimeCategory) {
        for(job in folder.getAllJobs()) {
            Run lastBuild = job.getLastBuild();
            if (lastBuild == null) {
                //Job has never run
                continue
            }
            if (lastBuild.getTime() > now - definitionOfStaleInDays.days) {
                returnValue = false
                break
            }else{
                returnValue = true
            }
        }
    }
    return returnValue
}

String constructParentPath(Folder folder){
    if(folder.getParent().getDisplayName() == "Jenkins"){
        return "That is under ROOT"
    }
    parentPath = 'That is under '
    def parent = folder.getParent()
    String displayName = parent.getDisplayName()
    while(displayName != "Jenkins") {
       parentPath <<= "${displayName} under "
       parent = parent.getParent()
       displayName = parent.getDisplayName()
    }
  
    parentPath <<= "ROOT"
    return parentPath
}

return