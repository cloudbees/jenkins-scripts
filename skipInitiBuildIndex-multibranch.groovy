/**
 * This script is used to enable the build strategy SkipInitialBuildOnFirstIndexingResetRevision for all existing multibranch and organization projects that have NOT set a Build Strategy definition.
 * Use Case: Migration of Jobs across CloudBees CI instances to prevent Build Storm.
 * Requirement: https://docs.cloudbees.com/docs/release-notes/latest/plugins/cloudbees-build-strategies-plugin/
 * Tested on: CloudBees CI 2.462.3.3 
 */

import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import com.cloudbees.jenkins.plugins.buildstrategies.SkipInitialBuildOnFirstIndexingResetRevision
import jenkins.branch.OrganizationFolder

def dryRun = true

def enableBuildStrategy(dryRun) {
    def mPModifiedCount = 0
    def mPEmptyCount = 0
    def mPNonEmptyCount = 0
    def oFModifiedCount = 0
    def oFEmptyCount = 0
    def oFNonEmptyCount = 0

    def jenkins = Jenkins.instance

    jenkins.allItems(WorkflowMultiBranchProject).each { multibranchProject ->
		
        multibranchProject.getSourcesList().each { branchSource ->
            if (branchSource !=null) { 
                def buildStrategies = branchSource.getBuildStrategies()
                if (buildStrategies.isEmpty()) {
                    if (dryRun){
                        println "Dryrun: Multibranch: ${multibranchProject.fullName} is a candidate to add Build Strategy."
                    } else {
                        branchSource.setBuildStrategies(Arrays.asList(new SkipInitialBuildOnFirstIndexingResetRevision()))
                        println "Multibranch: Adding Build Strategy SkipInitialBuildOnFirstIndexingResetRevision for ${multibranchProject.fullName}" 
                        mPModifiedCount++
                        multibranchProject.save()
                    }
                    mPEmptyCount++
                } else {
                    println "Multibranch: ${multibranchProject.fullName} has already enabled Build Strategy ${buildStrategies}."
                    mPNonEmptyCount++
                }
            }
        }
    }

    jenkins.allItems(OrganizationFolder).each { orgFolder ->
            def buildStrategies = orgFolder.getBuildStrategies()
            if (buildStrategies.isEmpty()) {
                if (dryRun) {
                    println "Dryrun: Org Folder: ${orgFolder.fullName} is a candidate to add Build Strategy."
                } else {
                    orgFolder.getBuildStrategies().add(new SkipInitialBuildOnFirstIndexingResetRevision())
                    println "Org Folder: Adding Build Strategy SkipInitialBuildOnFirstIndexingResetRevision for ${orgFolder.fullName}"
                    oFModifiedCount++
                    orgFolder.save()
                }
                oFEmptyCount++
            } else {
                println "Org Folder: ${orgFolder.fullName} has already enabled Build Strategy ${buildStrategies}."
                oFNonEmptyCount++
            }
    }

    println """
====================================================================================
TOTALS
    Multibranch:
    	Non-empty build strategies count: ${mPNonEmptyCount}
    	Empty build strategies count: ${mPEmptyCount}
    	Added build strategies SkipInitialBuildOnFirstIndexingResetRevision: ${mPModifiedCount}
    Organization Folder:
    	Non-empty build strategies count: ${oFNonEmptyCount}
    	Empty build strategies count: ${oFEmptyCount}
    	Added build strategies SkipInitialBuildOnFirstIndexingResetRevision: ${oFModifiedCount}
===================================================================================="""
}


enableBuildStrategy(dryRun)

null