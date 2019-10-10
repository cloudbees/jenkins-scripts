import groovy.time.TimeCategory

now = new Date()
definitionOfStaleInDays = 100

def jenkinsFolders = Jenkins.instance.getAllItems(com.cloudbees.hudson.plugins.folder.Folder)
jenkinsFolders.each { folder ->
  if(isStaleFolder(folder)){
  	println "${folder.name} IS A STALE FOLDER"
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
