/*** BEGIN META {
 "name" : "Interrupt Polling Threads",
 "comment" : "Interrupt Polling Threads currently running or running for a certain amount of time",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import jenkins.model.Jenkins

/**
 * Interrupt any running Polling Threads. This script is used as a workaround for SCM plugins that have problems
 * freeing Polling threads under some circumstances (Perforce for example).
 */
Thread.getAllStackTraces().keySet().each() {
    item ->
        if (item.getName().contains("SCM polling")) {
            println "Interrupting thread " + item.getId() + " " + item.getName();
            item.interrupt()
        }
}
return;

/**
 * Interrupt any running Polling Threads that are currently running for more than 3 minutes. This can be tuned.
 */
Jenkins.instance.getTrigger("SCMTrigger").getRunners().each() {
    item ->
        println(item.getTarget().name)
        println(item.getDuration())
        println(item.getStartTime())
        long millis = Calendar.instance.time.time - item.getStartTime()

        // 1000 millis in a second * 60 seconds in a minute * 3 minutes
        if (millis > (1000 * 60 * 3)) {
            Thread.getAllStackTraces().keySet().each() {
                tItem ->
                    if (tItem.getName().contains("SCM polling") && tItem.getName().contains(item.getTarget().name)) {
                        println "Interrupting thread " + tItem.getId() + " " + tItem.getName();
                        tItem.interrupt()
                    }
            }
        }
}