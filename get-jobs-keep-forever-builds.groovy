/*** BEGIN META {
 "name" : "Get all Job with Keep Forever Builds",
 "comment" : "Get all the jobs that are configured to keep Build logs forever",
 "parameters" : [ 'templateName' ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import hudson.model.Build
import hudson.model.Job
import hudson.model.Run
import jenkins.model.Jenkins

/**
 * Solution1: Get all builds with "Keep Forever" property.
 */
Jenkins.instance.getAllItems(Job.class)
        .findAll { job -> job.builds.isEmpty() }
        .findAll { job -> !job.builds
            .findAll { Build build -> build.isKeepLog()}.isEmpty()
        }

/**
 * Solution2: Get all builds with "Keep Forever" property.
 */
Jenkins.instance.getAllItems(Job.class)
        .findAll { job -> !job.builds.isEmpty() }
        .each { job -> job.builds
            .findAll { Build build -> build.isKeepLog()}
            .each { println ((Run)it);}
        }