/*** BEGIN META {
 "name" : "Get Queue State",
 "comment" : "Get current Queue/Executors state. Useful to troubleshoot queue and check that tasks are correctly handled",
 "parameters" : [ ],
 "core": "1.642",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

def all = [ Jenkins.instance ]
all.addAll(Jenkins.instance.nodes)
all.each {
    def c = it.toComputer()
    println "[$it.nodeName] - $it.numExecutors - $it.assignedLabels - $it.acceptingTasks - $it.nodeProperties - $c.offline"
}
println "----"
Jenkins.instance.queue.items.each {
    println "$it.id $it.blocked $it.buildable $it.stuck $it.assignedLabel $it.causes"
}