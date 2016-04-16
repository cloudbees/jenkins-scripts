/*** BEGIN META {
 "name" : "Get all Jobs by Triggers",
 "comment" : "Get all the jobs containing a specific trigger",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import hudson.model.AbstractProject
import hudson.triggers.SCMTrigger
import jenkins.model.Jenkins

/**
 * Get all items with SCMTrigger.
 */
//Solution 1
Jenkins.instance.getAllItems(AbstractProject.class)
        .findAll { project -> project.getTrigger(SCMTrigger) }
        .each { println it.name}

//Solution 2
Jenkins.instance.getAllItems()
        .findAll { item -> item.hasProperty("triggers") }
        .findAll { item -> !item.triggers
            .findAll { it instanceof SCMTrigger}.isEmpty()
        }.each { println it.name }