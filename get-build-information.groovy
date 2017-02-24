//This script allows you to get build information for selected jobs
/*
 You can also add closures to filter the jobs or the builds...
 .find{job->job.name.contains('dsl')}
 .find{ [whatever]}

*/
Jenkins.instance.getAllItems(Job).each{

  def jobBuilds=it.getBuilds()

	//for each of such jobs we can get all the builds (or you can limit the number at your convenience)
    jobBuilds.each { build ->
      def runningSince = groovy.time.TimeCategory.minus( new Date(), build.getTime() )
      def currentStatus = build.buildStatusSummary.message
      def cause = build.getCauses()[0] //we keep the first cause
      //This is a simple case where we want to get information on the cause if the build was 
      //triggered by an user
      def user = cause instanceof Cause.UserIdCause? cause.getUserId():""
      //This is an easy way to show the information on screen but can be changed at convenience
      println "Build: ${build} | Since: ${runningSince} | Status: ${currentStatus} | Cause: ${cause} | User: ${user}" 
     
      // You can get all the information available for build parameters.
      def parameters = build.getAction(ParametersAction)?.parameters
      parameters.each {
        println "Type: ${it.class} Name: ${it.name}, Value: ${it.dump()}" 
      
		}
    }
}
