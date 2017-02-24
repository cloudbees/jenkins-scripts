//This script will find all templates in a Jenkins instance, and find the number of times the template is used.

import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobTemplate
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobPropertyImpl

def jenkinsTemplates = Jenkins.instance.getAllItems(JobTemplate.class)
def jenkinsJobs = Jenkins.instance.getAllItems(Job.class)
def count = 0

jenkinsTemplates.each{ template ->
  println template.displayName
  jenkinsJobs.each { job ->
    if(job.getProperty(JobPropertyImpl.class) != null){
      if(job.getProperty(JobPropertyImpl.class).getModel().name == template.displayName){
        count++
      }
    }
  }
  println "\n    " + count + "\n"
  count = 0
}
