//This script will clean build history and can reset build numbers
//It can take a job or folder
//Total builds can also be set

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import hudson.model.AbstractItem

// jobName can be set to a Job or a Folder.  If a folder, it will clean all jobs in that folder
def jobName = "LatestJob"
// If this is true, the build number will be set back to 1, only works if you wipe all builds
def resetBuildNumber = false
def removeBuilds(job, resetBuildNumber) {
  def buildTotal = 5 //number of builds to keep
  def count
  if (job instanceof AbstractFolder) {
    for (subJob in job.getItems()) {
      removeBuilds(subJob, resetBuildNumber)
    }
  } else if (job instanceof AbstractItem) {
    count = 0
    job.getBuilds().each { 
      if(count < buildTotal){
        count++
      }
      else{
        it.delete()
      } 
    }
    if (resetBuildNumber && buildTotal == 0) {
      job.nextBuildNumber = 1
      job.save()
    }
  } else {
    throw new RuntimeException("Unsupported job type ${job.getClass().getName()}!")
  }
}
removeBuilds(Jenkins.instance.getItem(jobName), resetBuildNumber)
