import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import java.util.*

def days = 1
def cutOffDate = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * days
def dayInMillis = 86400000

for (job in Jenkins.instance.getAllItems(Job.class)) {

 build = job.getLastSuccessfulBuild()

 if (build != null && build.getTimeInMillis() > cutOffDate && build.duration > dayInMillis){
    def cause = build.getCauses()[0]
    def user = cause instanceof Cause.UserIdCause? cause.getUserId():""
    println job.fullName + "\nUsername: " + user  + "\nBuildNumber:" + build.number +  "\nBuildTime: " + build.duration/3600000 + " hours" + "\nStart time:" + build.timestampString2 + "\n"
 }

}
