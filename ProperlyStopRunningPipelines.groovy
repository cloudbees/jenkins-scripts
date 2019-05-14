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
//def jobMap = []

//Used to clean up specific builds. Put "buildNum: 0" if you want to stop all of the running builds for that job
/*def jobMap = [
    //[name:'$JOB_NAME', buildNum: $BUILD_NUM2],
    //[name:'$JOB_NAME2', buildNum: $BUILD_NUM2],
  	//[name:'$JOB_NAME3', buildNum: $BUILD_NUM3]
]*/


def jobMap = []

//Created for if the map is empty to add all the jobs
if(jobMap.isEmpty()){
  Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.job.WorkflowJob).each{
    job -> job.builds.findAll{it.isBuilding()}.each{
      build ->
      println("Adding: "+ job.fullName+ " build number " + build.getNumber().toInteger())
      jobMap.add([ name: job.fullName, buildNum: build.getNumber().toInteger()])
     }
   }
}
use(TimeCategory)  {
  def delay = 2.days;//Put in a Custom date here to kill anything older
  def refDate = (new Date()- delay).time
  // NO MODIFICATION
  Jenkins.instance.getAllItems(org.jenkinsci.plugins.workflow.job.WorkflowJob).each{
    job -> job.builds.byTimestamp(refDate, new Date().time).each{
      build ->
      println("Adding: "+ job.fullName+ " build number " + build.getNumber().toInteger())
      jobMap.add([ name: job.fullName, buildNum: build.getNumber().toInteger()])
     }
   }
 }
println "Removing the running builds for the following jobs: "
for(int i=0; i< jobMap.size(); i++)
{
  def currentName = jobMap.get(i).name
  def currentItem = Jenkins.instance.getItemByFullName(currentName)
  if (currentItem.isBuilding()){
    def currentBuild = jobMap.get(i).buildNum
    currentItem.builds.each{
      build ->
      if (build.isInProgress()&& jobMap.get(i).buildNum.equals(0)&& jobMap.get(i).name.equals(currentName)){
        println("Adding: "+ currentName+ " build number " + build.getNumber().toInteger())
        jobMap.add([ name: currentName, buildNum: build.getNumber().toInteger()])
      }
      //Adding description for reason to abort builds forcefully
       if (build.isInProgress()&& jobMap.get(i).buildNum.equals(currentBuild)&& jobMap.get(i).name.equals(currentName)){
          build.setDescription("Build was suspended for taking too much time. Contact to your Jenkins administrators")
          build.save()
       }
    }
    //Calling the same as the `X` in the UI
    if(currentBuild){
        println("Stopping " + currentName + " Build Number "+ currentBuild);
        Jenkins.instance.getItemByFullName(currentName).getBuildByNumber(currentBuild).doStop();
    }
  }
}
Thread.sleep(30000)
//Calling the same as the Terminate running build command in the console log
for(int i=0; i< jobMap.size(); i++)
{
  def currentName = jobMap.get(i).name
  def currentItem = Jenkins.instance.getItemByFullName(currentName)
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
for(int i=0; i< jobMap.size(); i++)
{
  def currentName = jobMap.get(i).name
  def currentItem = Jenkins.instance.getItemByFullName(currentName)
  if (currentItem.isBuilding()){
    def currentBuild = jobMap.get(i).buildNum
    if(currentBuild){
        println("Killing " + currentName + " Build Number "+ currentBuild);
        Jenkins.instance.getItemByFullName(currentName).getBuildByNumber(currentBuild).doKill();
    }
  }
}
