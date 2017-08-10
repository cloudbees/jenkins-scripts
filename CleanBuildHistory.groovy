import com.cloudbees.hudson.plugins.folder.AbstractFolder
import hudson.model.AbstractItem
import groovy.transform.Field

def jobName = "LatestJob1/AnotherOne/anotherFolder" // jobName can be set to a Job or a Folder. If a folder, it will clean all jobs in that folder
//jobName should be full job name from root if mutliple levels down(for example "Folder1/Folder2/Job")
def resetBuildNumber = false // If this is true, the build number will be set back to 1
@Field def cleanedJobsTotal = 0
removeBuilds(Jenkins.instance.getItemByFullName(jobName), resetBuildNumber)
def removeBuilds(job, resetBuildNumber) {
  def buildTotal = 5
  def count
  if (job instanceof AbstractFolder) {
    cleanedJobsLimit = 2 //Maximum number of jobs to clean in one run, useful for large systems or slow disks
    for (subJob in job.getItems()) {
      if(cleanedJobsTotal >= cleanedJobsLimit){ 
         println "The cleaned jobs limit of " + cleanedJobsTotal + " has been reached.  Exiting..."
         return
      }
      else{
        removeBuilds(subJob, resetBuildNumber)
      }
    }
  } else if (job instanceof Job) {
    count = 0
    buildsDeleted = false
    job.getBuilds().each { 
      if(count < buildTotal){
        count++
      }
      else{
        it.delete()
        buildsDeleted = true
      } 
    }
    if(buildsDeleted){
    	println "Job " + job.name + " cleaned successfully.\n"
      	cleanedJobsTotal++
    }
    if (resetBuildNumber) {
      job.nextBuildNumber = 1
      job.save()
    }
  } else {
    //Do nothing, next job
    //throw new RuntimeException("Unsupported job type ${job.getClass().getName()}!")
  }
}
