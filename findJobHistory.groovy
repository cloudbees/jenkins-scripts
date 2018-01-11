//This script is intended to be run from a freestyle job as it may timeout from script console.  Freestyle job -> Execute system groovy script.  Imports are required.

import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*

def folder_name_pattern = /.*some_pattern*/ //Folder name pattern, determines which folders to scan
def jenkinsFolders = Jenkins.instance.getAllItems(com.cloudbees.hudson.plugins.folder.Folder)
def matchedFolders = jenkinsFolders.findAll { folder ->
    folder.name =~ folder_name_pattern
}

matchedFolders.each { folder ->
  jobsList = folder.getAllJobs()
    jobsList.each{ job ->
      allBuilds = job.getBuilds()
  	  allBuilds.each {
      def cause = it.getCauses()[0]
      def user = cause instanceof Cause.UserIdCause? cause.getUserId():""
        println "Job Name: " + folder.name + "/" + job.name + " Build Number:" + it.number +  " Build Time: " + it.time + " Username: " + user
  	  }
    }
}
