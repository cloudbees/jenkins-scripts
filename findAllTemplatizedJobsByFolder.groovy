//This script will find all templates in a Jenkins instance, then find all jobs using that template in the specified folder

import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobTemplate
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobPropertyImpl
import com.cloudbees.hudson.plugins.folder.Folder

def jenkinsJobs = Jenkins.instance.getAllItems(Job.class)
def folderName = 'someFolder' //Folder name

folderItem = Jenkins.instance.getAllItems(Folder.class).find{it.name.equals(folderName)}
if(folderItem == null){
  println "Folder " + folderName + " does not exist!"
  return
}

findAllTemplates(((com.cloudbees.hudson.plugins.folder.Folder) folderItem).getItems())

def findAllTemplates(items){
  def jenkinsTemplates = Jenkins.instance.getAllItems(JobTemplate.class)
  def count = 0
  jenkinsTemplates.each{ template ->
      count = findTemplatizedJobs(items, template.displayName, count)
      println "\n    " + count + "\n"
      count = 0
  }
}

def findTemplatizedJobs(items, templateName, count){
  println templateName
  def currItem
  for(item in items){
    currItem = item
    if(item instanceof AbstractProject || item instanceof org.jenkinsci.plugins.workflow.job.WorkflowJob){
      if(item.getProperty(JobPropertyImpl.class) != null){
        if(item.getProperty(JobPropertyImpl.class).getModel().name == templateName){
          count++
        }
      }
    }
  }
  if (currItem instanceof com.cloudbees.hudson.plugins.folder.Folder){
    findTemplatizedJobs(((com.cloudbees.hudson.plugins.folder.Folder) currItem).getItems(), templateName, count)
  }
  return count
}
