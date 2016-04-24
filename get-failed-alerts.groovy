/*** BEGIN META {
 "name" : "Get Failed Alerts",
 "comment" : "Get the failed condition (Alerts) defined by the User or in the system",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.jenkins.plugin.metrics.views.Alerter
import jenkins.model.Jenkins

//Get the Failed condition defined by default in CJP (cloudbees-monitoring)
Jenkins.instance.actions
        .findAll { action -> action instanceof Alerter.RootActionImpl }
        .each {
        //Iterate on default CJE conditions (alert)
        action -> action.conditions
        //Find all the failed conditions
                .findAll { condition -> condition.activeState }
                .each { condition -> println "name: ${condition.name}, status: ${condition.status}, activeState: ${condition.activeState}"}
}

//Get the Failed condition defined by the user
Jenkins.instance.actions
        .findAll { action -> action instanceof Alerter.RootActionImpl }
        .each {
            //Iterate on conditions (alert) defined by the user
            action -> action.alerter.conditionList
                //Find all the failed conditions
                .findAll { condition -> condition.activeState }
                .each { condition -> println "name: ${condition.name}, status: ${condition.status}, activeState: ${condition.activeState}"}
        }

/**
 * Following can be used in EnvInject to grab the Alert status message and inject it in an Environment Variable.
 */
def map = [:]
Alerter.RootActionImpl[] alerter = Jenkins.instance.actions.findAll { alerterImpl -> alerterImpl instanceof Alerter.RootActionImpl };
if (alerter.size() > 0) {
    Alerter.Condition condition = alerter[0].alerter.conditionList.find { condition -> "myCondition".equals(condition.name) }
    if (condition) {
        map['MESSAGE'] = '${condition.name} failed on ${SOURCE_JENKINS_URL} with the following status "${condition.status}" '
    } else {
        map['MESSAGE'] = "OK"
    }
}
return map
