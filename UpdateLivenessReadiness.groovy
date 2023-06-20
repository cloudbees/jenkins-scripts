import com.cloudbees.opscenter.server.model.ManagedMaster
import com.cloudbees.masterprovisioning.kubernetes.KubernetesMasterProvisioning

//Script to programmatically set values for Liveness and Readiness probes

String mycontroller_name="circular"
int l_initial_delay=300
int l_period_seconds=10
int l_timeout_seconds=10
int r_initial_delay=30
int r_failure_threshold=100
int r_timeout_seconds=5

Jenkins.instance.getAllItems(ManagedMaster.class).each{
  println "Adjusting liveness/readiness values for the controller named: " + it.name
    KubernetesMasterProvisioning config=it.getConfiguration()
  if (it.name==mycontroller_name){
       config.setLivenessInitialDelaySeconds(l_initial_delay)
       config.setLivenessPeriodSeconds(l_period_seconds)
   	   config.setLivenessTimeoutSeconds(l_timeout_seconds)
       config.setReadinessInitialDelaySeconds(r_initial_delay)
       config.setReadinessFailureThreshold(r_failure_threshold)
      config.setReadinessTimeoutSeconds(r_timeout_seconds)
     it.setConfiguration(config)
  }
  if (config!=null){
   
   println "\t Liveness Initial Delay" + config.getLivenessInitialDelaySeconds()
   println "\t Liveness Period seconds" + config.getLivenessPeriodSeconds()
   println "\t Liveness Timeout seconds" + config.getLivenessTimeoutSeconds()
   println "\t Readiness Initial Delay" + config.getReadinessInitialDelaySeconds()
   println "\t Readiness Failure Threshold" + config.getReadinessFailureThreshold()
   println "\t Readiness Timeout in seconds" + config.getReadinessTimeoutSeconds()
  }
}
return null
