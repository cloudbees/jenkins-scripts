/*
Author: Alex Taylor
Since: July 2018
Description: This script stop all or a series of running pipeline jobs
reset or became out of synch.
Parameters: None
Scope: Cloudbees Jenkins Platform
*/

/* This script can be used to kill off all running jobs, the latest build of a job, a specific job number, or jobs
which have been running for a certain amount of time. This is all based on a couple of specific settings which are marked
with comments. This script will guaranteed take 1.5 minutes to run because we want to ensure that each build command has the
time needed to run*/

import groovy.time.*;


// A couple of example map definitions:

//Used to kill all of the active jobs
//def jobMap = [:]

//Used to clean up specific builds. Put "buildNum: 0" if you want to stop the latest build
/*def jobMap = [
    1: [name:'$JOB_NAME', buildNum: $BUILD_NUM],
    2: [name:'$JOB_NAME2', buildNum: $BUILD_NUM2],
  	3: [name:'$JOB_NAME3', buildNum: $BUILD_NUM3]
]
*/

def jobMap = [:]

//Created for if the map is empty to add all the jobs
if(jobMap.isEmpty()){
  Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.job.WorkflowJob).each{
    job -> job.builds.findAll{it.isBuilding()}.each{
      build ->
      println("Adding: "+ job.fullName+ " build number " + build.getNumber().toInteger())
      jobMap.put(++jobMap.size(), [ name: job.fullName, buildNum: build.getNumber().toInteger()])
     }
   }
}
use(TimeCategory)  {
  def delay = 2.days;//Put in a Custom date here to kill anything older
  // NO MODIFICATION
  Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.job.WorkflowJob).each{
    job -> job.builds.findAll{it.isBuilding() && new Date(it.startTimeInMillis) < (new Date() - delay)}.each{
      build ->
      println("Adding: "+ job.fullName+ " build number " + build.getNumber().toInteger())
      jobMap.put(++jobMap.size(), [ name: job.fullName, buildNum: build.getNumber().toInteger()])
     }
   }
 }
println "Removing the running builds for the following jobs: "
for(int i=1; i<= jobMap.size(); i++)
{
  def currentName = jobMap.get(i).name
  def currentItem = Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.job.WorkflowJob).find{it.name.equals(currentName)}
  if (currentItem.isBuilding()){
    currentItem.builds.each{
      build ->
      if (build.isInProgress()&& jobMap.get(i).buildNum.equals(0)&& jobMap.get(i).name.equals(currentName)){
        println("Adding: "+ currentName+ " build number " + build.getNumber().toInteger())
        jobMap.put(++jobMap.size(), [ name: currentName, buildNum: build.getNumber().toInteger()])
      }
    }
    //Calling the same as the `X` in the UI
    def currentBuild = jobMap.get(i).buildNum
    if(currentBuild){
        println("Stopping " + currentName + " Build Number "+ currentBuild);
        Jenkins.instance.getItemByFullName(currentName).getBuildByNumber(currentBuild).doStop();
    }
  }
}
Thread.sleep(30000)
//Calling the same as the Terminate running build command in the console log
for(int i=1; i<= jobMap.size(); i++)
{
  def currentName = jobMap.get(i).name
  def currentItem = Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.job.WorkflowJob).find{it.name.equals(currentName)}
  if (currentItem.isBuilding()){
    def currentBuild = jobMap.get(i).buildNum
    if(currentBuild){
        println("Terminating " + currentName + " Build Number "+ currentBuild);
        Jenkins.instance.getItemByFullName(currentName).getBuildByNumber(currentBuild).doTerm();
    }
  }
}
Thread.sleep(30000)
//Calling the same as the Kill running build command in the console log
for(int i=1; i<= jobMap.size(); i++)
{
  def currentName = jobMap.get(i).name
  def currentItem = Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.job.WorkflowJob).find{it.name.equals(currentName)}
  if (currentItem.isBuilding()){
    def currentBuild = jobMap.get(i).buildNum
    if(currentBuild){
        println("Killing " + currentName + " Build Number "+ currentBuild);
        Jenkins.instance.getItemByFullName(currentName).getBuildByNumber(currentBuild).doKill();
    }
  }
}
