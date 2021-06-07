/**
 Author: Harish Jangam
 Description: Get the list of offline Shared agents and bring them online
 **/

import hudson.slaves.*
import static com.cloudbees.opscenter.server.persistence.SlaveLeaseTable.getLeases;


def SharedAgentstatus(Slave) {
    def online = false
    if (Slave instanceof com.cloudbees.opscenter.server.model.SharedSlave) {
        online = Slave.getOfflineCause() == null
        if(!online){
            println(Slave.name + ' is currently Offline, hence bringing it online \t')
            //aSlave.doEnable()  // comment - to get the list of offline agents, // Uncomment - to bring the agent online
        }
    }
}

Jenkins.instance.allItems.grep {
    it.class.name == 'com.cloudbees.opscenter.server.model.SharedSlave'
}.each {
    SharedAgentstatus(it)
}
println ''
