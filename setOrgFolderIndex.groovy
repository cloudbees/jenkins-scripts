//By default the script just prints out the current indexing intervals for all jobs.
//The function at the bottom can be called to set the interval to a new value.
//To call the function, change the two `each` blocks as follows:
// .each { setInterval(folder) }

import com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger
import jenkins.model.Jenkins
import jenkins.branch.OrganizationFolder

println "Organization Items\n-------"
Jenkins.get().getAllItems(jenkins.branch.OrganizationFolder.class).each { folder -> folder.triggers
       .findAll { k,v -> v instanceof com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger }
       .each { k,v -> println "Folder name: ${folder.fullName}, Interval: ${v.getInterval()}" }
}

println "Multibranch Items\n-------"
Jenkins.get().getAllItems(org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject.class).each { folder -> folder.triggers
       .findAll { k,v -> v instanceof com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger }
       .each { k,v -> println "Folder name: ${folder.fullName}, Interval: ${v.getInterval()}" }
}

return

//Acceptable values for triggers can be found here:
//https://github.com/jenkinsci/cloudbees-folder-plugin/blob/master/src/main/java/com/cloudbees/hudson/plugins/folder/computed/PeriodicFolderTrigger.java#L241
def setInterval(folder) {
  println "[INFO] : Updating ${folder.name}... "
  folder.getTriggers().find {triggerEntry ->
    def key = triggerEntry.key
    if (key instanceof PeriodicFolderTrigger.DescriptorImpl){
      println "[INFO] : Current interval : " + triggerEntry.value.getInterval()

      // Set the desired interval here
      def newInterval = new PeriodicFolderTrigger("28d")

      folder.addTrigger(newInterval)
      folder.save()
      println "[INFO] : New interval : " + newInterval.getInterval()
    }
  }
}
