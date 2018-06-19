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
  
def changeInterval(interval) {
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
