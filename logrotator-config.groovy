jobs = Jenkins.instance.getAllItems();
jobs.each { j ->
  if (j instanceof com.cloudbees.hudson.plugins.folder.Folder
     || j instanceof com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobTemplate
     || j instanceof org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
     || j instanceof jenkins.branch.OrganizationFolder
     || j instanceof com.cloudbees.hudson.plugins.modeling.impl.auxiliary.AuxModel
     || j instanceof com.cloudbees.hudson.plugins.modeling.impl.builder.BuilderTemplate
     || j instanceof com.cloudbees.hudson.plugins.modeling.impl.folder.FolderTemplate) { return }
  if (j.isBuildable() && j.logRotator==null) {
        println j.name
        j.logRotator = new hudson.tasks.LogRotator ( '', '5', '2', '5')
  }
}
