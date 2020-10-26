// This script can be ran from a jenkins job or via the script console
// Sometimes jobs get hung on an executor and register as null which makes killing it programmatically more difficult
// This loops through the executors and kills the hung process, regardless of the job name

// minutes
def int timeoutThreshold = 60

def busyExecutors = Jenkins.instance.computers
                                .collect {
                                  c -> c.executors.findAll { it.isBusy() }
                                }
                                .flatten()

busyExecutors.each { executor ->
  def long jobRunningMinutes = (executor.getElapsedTime() / 1000) / 60
  // If build is likely stuck and time elapsed is greater than defined threshold
  if (executor.isLikelyStuck() && jobRunningMinutes >= timeoutThreshold) {
    // Cancel build on the executor
    executor.doStopBuild()
    // Print build and methods available
    println(executor.metaClass.methods*.name.sort().unique())
  } else {
    println("No stuck builds")
  }
}
