/**
 * This script is used to enable the build strategy SkipInitialBuildOnFirstIndexingResetRevision for all multibranch projects.
 * Use Case: Migration of Jobs across CloudBees CI instances to prevent Build Storm.
 * Requirement: https://docs.cloudbees.com/docs/release-notes/latest/plugins/cloudbees-build-strategies-plugin/
 * Tested on: CloudBees CI 2.462.3.3 
 */

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import com.cloudbees.jenkins.plugins.buildstrategies.SkipInitialBuildOnFirstIndexingResetRevision

def dryRun = true


def enableBuildStrategy(dryRun) {
    def modifiedCount = 0
    def emptyCount = 0
    def nonEmptyCount = 0

    Jenkins.instance.getAllItems(WorkflowMultiBranchProject).each { multibranchProject ->
		
        multibranchProject.getSourcesList().each { branchSource ->
            if (branchSource !=null) { 
                def buildStrategies = branchSource.getBuildStrategies()
                if (buildStrategies.isEmpty()) {
                    if (dryRun){
                        println "Dryrun: ${multibranchProject.fullName} is a candidate to add Build Strategy."
                    } else {
                        branchSource.setBuildStrategies(Arrays.asList(new SkipInitialBuildOnFirstIndexingResetRevision()))
                        println "Adding Build Strategy SkipInitialBuildOnFirstIndexingResetRevision for ${multibranchProject.fullName}" 
                        modifiedCount++
                    }
                    emptyCount++
                } else {
                    println "${multibranchProject.fullName} has already enabled Build Strategy ${buildStrategies}."
                    nonEmptyCount++
                }
            }
        }
    }
    println "===================================================================================="
    println "Non-empty build strategies count: ${nonEmptyCount}"
    println "Empty build strategies count: ${emptyCount}"
    println "Added build strategies SkipInitialBuildOnFirstIndexingResetRevision: ${modifiedCount}"
    println "===================================================================================="
}

enableBuildStrategy(dryRun)
null