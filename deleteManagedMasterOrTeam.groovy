/**
 @Author carlosrodlop
 @Description example of how to delete a Managed Master or Team from groovy
 @param managedMasterName
 Tested on CJE 1.11.11 - Operations Center 2.138.2.2-rolling
**/

import com.cloudbees.jce.masterprovisioning.mesos.MesosMasterProvisioning
import com.cloudbees.opscenter.server.model.ManagedMaster
import com.cloudbees.opscenter.server.provisioning.MasterResource
import com.cloudbees.opscenter.server.util.ExternalFuture
import mesosphere.marathon.client.*
import mesosphere.marathon.client.model.v2.*

// Managed Master or Team
def managedMasterName="<MANAGED_MASTER_NAME>"

ManagedMaster.PersistedStateImpl persistedState
Jenkins.getInstance().getAllItems(ManagedMaster.class).each { mm ->
  if(mm.name == managedMasterName){
    println "Deleting Managed Master for CJOC " + mm.name
    if (mm.getState()!="STARTED"){
      persistedState = mm.getPersistedState()
      mm.setPersistedState(persistedState, new ManagedMaster.PersistedStateImpl(ManagedMaster.State.STARTED, (ExternalFuture)null, (MasterResource)null, persistedState.futureFilesystem, persistedState.filesystem))
    }
    mm.stopAction(true)
    mm.delete()
  }
}

Jenkins.getInstance().getDescriptor(MesosMasterProvisioning.class).getMarathonEndpoints().each { ep ->
  Marathon marathon = ep.getMarathon();
  marathon.getApps().getApps().each{
    if (it.getId().contains(managedMasterName)){
      println "Deleting Application from Marathon " + it.getId()
      marathon.deleteApp(it.getId())
    }
  }
}