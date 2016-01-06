/*
Author: Isaac S Cohen
This script works with workflow to cancel other running builds for the same job
Use case: many build may go to QA, but only the build that is accepted is needed,
the other builds in the workflow should be aborted
*/

def jobname = env.JOB_NAME
def buildnum = env.BUILD_NUMBER.toInteger()

def job = Jenkins.instance.getItemByFullName(jobname)
 for (build in job.builds) {
     if (!build.isBuilding()) { continue; }
     if (buildnum == build.getNumber().toInteger()) { continue; println "equals" } 
    build.doStop();
}
