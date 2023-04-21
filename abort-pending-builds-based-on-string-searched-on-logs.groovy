import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.ExecutorStepExecution

jenkins.model.Jenkins.instanceOrNull.getComputers().each { computer -> 
  computer.executors.findAll { exec -> exec.isBusy() && exec.currentExecutable }.each { exec ->
    searchStringInLogBuild(getPipelineRunFromExecutable(exec.currentExecutable), "String that you are looking for")
  }
}

/**
 * Try to infer the WorkflowRun of the executable passed in. Extracted method from https://github.com/cloudbees/jenkins-scripts/blob/master/ProperlyStopOnlyRunningPipelines.groovy
 * @param executable The executable
 * @return The WorkflowRun, or null if this is not a Pipeline run
 */
WorkflowRun getPipelineRunFromExecutable(Queue.Executable executable) {
    if (executable instanceof WorkflowRun) {
        return ((WorkflowRun) executable)
    }

    if (executable.parent instanceof ExecutorStepExecution.PlaceholderTask) {
        def executorPlaceholderTask = ((ExecutorStepExecution.PlaceholderTask) executable.parent)
        return ((WorkflowRun) executorPlaceholderTask.runForDisplay())
    }
 
    return null
}

def searchStringInLogBuild(build, string) {
  BufferedReader reader = null
  try {
    reader = new BufferedReader(build?.getLogReader());
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      if (line =~ string) {
        println "Build being executed and it's going to be killed: $build.project in build Nº$build.number"
        build.doKill()
        println "-------------"
      }
    }
  } catch (Exception e) {
    println("Error: " + e);
  } finally {
    if (reader != null) {
      reader.close();
    }
  }
}

return null