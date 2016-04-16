/*** BEGIN META {
 "name" : "Schedule All Jobs",
 "comment" : "Schedule all jobs with few parameters such as quiet period.",
 "parameters" : [ 'templateName', 'throttleLimit', 'sleepInterval', 'quietPeriod'],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobPropertyImpl
import hudson.model.Job
import jenkins.model.Jenkins

/**
 * This example schedule all job templatized via the Template `templateName`. If more than 100 jobs are to be scheduled,
 * the script schedules 100 jobs every 2.1 seconds. Each Job is scheduled with a quiet period of 2 seconds.
 */
//Sleep interval in milliseconds
int sleepInterval = 1000;
//Number of jobs to schedules every sleep interval
int throttleLimit = 2;
//Quiet period in seconds
int quietPeriod = 2;

Jenkins.instance.getAllItems(Job.class)
        .findAll { item -> item.getProperty(JobPropertyImpl.class)}
        .findAll { item -> item.getProperty(JobPropertyImpl.class).getModel().name == "templateName"}
        .each {
    while(Jenkins.instance.getQueue().getItems().size() > throttleLimit) {
        print "."
        sleep(sleepInterval);
    }
    print "\nScheduling ${it.name}";
    it.scheduleBuild2(quietPeriod);
}