import hudson.triggers.*
import hudson.maven.MavenModuleSet
import org.jenkinsci.plugins.workflow.job.*
 
def logSpec = { it, getTrigger -> String spec = getTrigger(it)?.getSpec(); if (spec) println (it.getFullName() + " with spec " + spec)}
 
println("--- SCM Polling for Pipeline jobs ---")
Jenkins.getInstance().getAllItems(WorkflowJob.class).each() { logSpec(it, {it.getSCMTrigger()}) }
 
println("\n--- SCM Polling for FreeStyle jobs ---")
Jenkins.getInstance().getAllItems(FreeStyleProject.class).each() { logSpec(it, {it.getSCMTrigger()}) }
 
println("\n--- SCM Polling for Maven jobs ---");
Jenkins.getInstance().getAllItems(MavenModuleSet.class).each() { logSpec(it, {it.getTrigger(SCMTrigger.class)}) }
 
println '\nDone.'
