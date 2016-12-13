/*** BEGIN META {
 "name" : "Get all Templatized Jobs by Template",
 "comment" : "Get all the jobs created from a specified template",
 "parameters" : [ 'templateName' ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobPropertyImpl
import hudson.model.Job
import jenkins.model.Jenkins


def templatizedJobs = Jenkins.instance.getAllItems(Job.class)
        .findAll { item -> item.getProperty(JobPropertyImpl.class)}
        .findAll { item -> item.getProperty(JobPropertyImpl.class).getModel().name == "${templateName}"};
return templatizedJobs;
