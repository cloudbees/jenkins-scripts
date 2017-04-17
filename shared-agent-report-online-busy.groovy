/**
Author: Kuisathaverat
Since: April 2017
Description: Check the status of all Shared Agents 
Scope: Cloudbees Jenkins Operations Center
**/

import java.util.*
import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import static com.cloudbees.opscenter.server.persistence.SlaveLeaseTable.getLeases;

boolean reportError = false

def checkSharedAgentOffline(aSlave) {
    println "Agent: " + aSlave.name
    def online = false
    def busy = false
    if (aSlave instanceof com.cloudbees.opscenter.server.model.SharedSlave) {
        online = aSlave.getOfflineCause() == null
        def leases = getLeases(aSlave.getUid())
        busy = leases != null && !leases.isEmpty()
    }
    println('\tIs Online: ' + online)
    println('\tIs Busy: ' + busy)
  
  	if(!online){
      reportError = true
  	}
}

// CJOC Shared Slaves
Jenkins.instance.allItems.grep {
    it.class.name == 'com.cloudbees.opscenter.server.model.SharedSlave'
}.each {
    checkSharedAgentOffline(it)
}

if(reportError){
	println 'Some Shared Agents are offline!!!' 
}
