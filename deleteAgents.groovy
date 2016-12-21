import java.util.*
import jenkins.model.*
import hudson.model.*
import hudson.slaves.*
import static com.cloudbees.opscenter.server.persistence.SlaveLeaseTable.getLeases;

// Set this to false once you run the script and checked the output
dryRun = true

def goDelete(aSlave) {
    println "Agent: " + aSlave.name
    def online = false
    def busy = false
    if (!(aSlave instanceof com.cloudbees.opscenter.server.model.SharedSlave)) {
        online = aSlave.isOnline()
        busy = aSlave.countBusy() != 0
    } else {
        online = aSlave.getOfflineCause() == null
        def leases = getLeases(aSlave.getUid())
        busy = leases != null && !leases.isEmpty()
    }
    println('\tcomputer.isOnline: ' + online)
    println('\tcomputer.countBusy: ' + busy)
    if (!busy && !online && !dryRun) {
        if (aSlave instanceof com.cloudbees.opscenter.server.model.SharedSlave) {
            aSlave.doDoDelete()
        } else {
            aSlave.delete()
        }
        println('\tIs Deleted')
    } else if(dryRun) {
        println "SIMULATION MODE: Not Delete ${aSlave.name}, set dryRun variable to false if you wish to run it for real"
    } else {
        println('\tIs NOT Deleted')
    }

    println('\tIs now offline :' + !online)

}

// Jenkins Master and slaves
Jenkins.instance.computers.grep {
    it.class.superclass?.simpleName != 'AbstractCloudComputer' &&
        it.class.superclass?.simpleName != 'AbstractCloudSlave' &&
        it.class.simpleName != 'EC2AbstractSlave'
}.each {
    if (!(it instanceof jenkins.model.Jenkins.MasterComputer)) {
        goDelete(it)
    }
}

// CJOC Shared Slaves
Jenkins.instance.allItems.grep {
    it.class.name == 'com.cloudbees.opscenter.server.model.SharedSlave'
}.each {
    goDelete(it)
}
