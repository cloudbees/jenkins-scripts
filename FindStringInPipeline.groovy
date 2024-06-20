/*
Author: Alex Taylor
Since: March 2020
Description: This searches over all pipeline scripts locally and on the last build if run via Jenkinsfile for a string
This is helpful for when you need to do a mass fix on a bunch of pipelines
Parameters: searchString
Scope: Jenkins
*/

import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*

def searchString = "Looking for this String"

Jenkins.getInstance().getAllItems(WorkflowJob.class).each(){pipeline ->
  if(pipeline.getDefinition() instanceof CpsFlowDefinition
     && pipeline.getDefinition() != null
     && pipeline.getDefinition().script.contains(searchString)){
    println("Need to fix: " + pipeline.getDisplayName())
  } else {
    if(pipeline.getDefinition() instanceof CpsFlowDefinition
     && pipeline.getLastBuild() != null
     && pipeline.getLastBuild().getExecution() != null
     && pipeline.getLastBuild().getExecution().script.contains(searchString)){
      println("Need to fix Jenkinsfile: " + pipeline.getDisplayName())
    }
  }
}