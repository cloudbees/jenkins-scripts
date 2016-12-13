/*** BEGIN META {
 "name" : "Get all Jobs by Builder",
 "comment" : "Get all the jobs containing a specific builder",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import jenkins.model.Jenkins
import hudson.model.Job
import hudson.tasks.Shell

/**
 * Get all items with Shell tasks whose the command contains "mate".
 */
Jenkins.instance.getAllItems(Job.class)
        .findAll { item -> item.hasProperty("builders") }
        .findAll { item -> !item.builders
            .findAll { it instanceof Shell && it.getContents().contains("mate") }.isEmpty()
}