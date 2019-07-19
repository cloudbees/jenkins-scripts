//This is for organization folders or multibranch project indexing
//The script as is just prints out intervals if they exist
//The function at the bottom can be called to set the interval

import com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger
import jenkins.model.Jenkins
import jenkins.branch.OrganizationFolder

println "Organization Items\n-------"
Jenkins.instance.getAllItems(jenkins.branch.OrganizationFolder.class).each { it.triggers
       .findAll { k,v -> v instanceof com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger }
       .each { k,v -> println "Folder name: ${it.fullName}, Interval: ${v.getInterval()}" }
}
println "Multibranch Items\n-------"
Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject.class).each { it.triggers
       .findAll { k,v -> v instanceof com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger }
       .each { k,v -> println "Folder name: ${it.fullName}, Interval: ${v.getInterval()}" }                                                                                                            
}
return
  
//This function is not called above
//Acceptable values for triggers can be found here: 
//https://github.com/jenkinsci/cloudbees-folder-plugin/blob/master/src/main/java/com/cloudbees/hudson/plugins/folder/computed/PeriodicFolderTrigger.java#L241
def setInterval(folder) {
  println "[INFO] : Updating ${folder.name}... " 
  folder.getTriggers().find {triggerEntry ->
    def key = triggerEntry.key
    if (key instanceof PeriodicFolderTrigger.DescriptorImpl){
      println "[INFO] : Current interval : " + triggerEntry.value.getInterval()
      def newInterval = new PeriodicFolderTrigger("28d")
      folder.addTrigger(newInterval)
      folder.save()
      println "[INFO] : New interval : " + newInterval.getInterval()
    }
  }
}  
