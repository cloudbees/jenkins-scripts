/*** BEGIN META {
 "name" : "Inject Short Job Name",
 "comment" : "Groovy script to use with EnvInject Plugin to inject the "short name" of a job. (see JENKINS-25164)",
 "parameters" : [],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import hudson.EnvVars
import hudson.model.Environment

JOBNAME = build.getEnvironment(null)["JOB_NAME"]
println "${JOBNAME}"
//The job name contains the path from the root if defined inside an ItemGroup. (for example: `folderA/jobA`)
def match = JOBNAME =~ /.*\/(.*)/
if (match) {
    //If this is the case, we inject the job name as a variable
    e = new EnvVars();
    e.put('SHORT_JOB_NAME', match[0][1])
    build.environments.add(Environment.create(e))
}