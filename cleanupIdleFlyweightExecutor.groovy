/*** BEGIN META {
 "name" : "Clean up idle flyweight executors",
 "comment" : "This script cleans up flyweight executors. Those executors may not be properly removed for version older 
 than 2.208 due to https://issues.jenkins-ci.org/browse/JENKINS-57304",
 "parameters" : [],
 "core": "2.204.1.3",
 "authors" : [
 { name : "Pierre Beitz", "Allan Burdajewicz" }
 ]
 } END META**/


// This script will remove any OneOffExecutors that has no executable, no async execution, and whose thread state is terminated

Computer computer = jenkins.model.Jenkins.getInstance().toComputer()
List<OneOffExecutor> executors = computer.getOneOffExecutors()
println "Examining " + executors.size() + " OneOffExecutor instances on the Jenkins master."
for (OneOffExecutor executor : executors) {
    println "* ${executor.displayName}(${executor.id})"
    Queue.Executable executable = executor.getCurrentExecutable();
    if (executable == null) {
        print "   Executor has no executable";
        if (executor.getAsynchronousExecution() == null && executor.getState() == Thread.State.TERMINATED) {
            println ", no async execution, and has already terminated as a thread. Deleting the executor.";
            computer.remove(executor);
        } else {
            println ", async exection: " + executor.getAsynchronousExecution() + ", thread state: " + executor.getState();
        }
    } else {
        println "   [SKIPPING] Executor has an executable: ${executor.getCurrentExecutable()}";
    }
}
return