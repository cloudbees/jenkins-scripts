/**
The script was created on 2019-07-21
by Kirill Gostaf, kgostaf@cloudbees.com

This script will iterate over all the projects, and check whether a job has Build periodically trigger enabled.
If the project type is Freestyle job the Build periodically trigger will be disabled.
Otherwise, it prints the name of the project without modifying the trigger settings.
For a pipeline project, i.e. org.jenkinsci.plugins.workflow.job.WorkflowJob, you may want to modify
the Jenkinsfile that contains `triggers { cron(H/4 * * * *) }` instructions.
*/


import hudson.model.*
import hudson.triggers.*
TriggerDescriptor TIMER_TRIGGER_DESCRIPTOR = Hudson.instance.getDescriptorOrDie(TimerTrigger.class)

for(item in Jenkins.instance.getAllItems(Job))
{
  def timertrigger = item.getTriggers().get(TIMER_TRIGGER_DESCRIPTOR)
  if (timertrigger) {
    if (item instanceof FreeStyleProject) {
      item.removeTrigger(TIMER_TRIGGER_DESCRIPTOR)
      println(item.name + " Build periodically trigger disabled successfully");
    }
    else {
      println(item.name + " not a Freestyle project. Build periodically trigger is still enabled");
    }
  }
}
