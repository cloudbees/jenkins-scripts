/*** BEGIN META {
  "name" : "Builds Cleaner",
  "comment" : "Delete all builds keeping ${MAX_BUILDS} from all job of the Jenkins Instance. It checks build is not building and it also keeps the keep forever, lastStableBuild, lastSuccessfulBuild, lastSuccessfulBuild, lastUnstableBuild, lastUnsuccessfulBuild in case are not included in the ${MAX_BUILDS}.",
  "parameters" : [],
  "core": "2.107.3",
  "authors" : [
    { name : "Marco Davalos" }
  ]
} END META**/

import jenkins.model.Jenkins
import hudson.model.Job

def MAX_BUILDS = 5 // max builds to keep

Jenkins.instance.allItems.findAll { it instanceof hudson.model.Job }.each { job ->

    job.builds.drop(MAX_BUILDS).findAll {

        !it.keepLog &&
        !it.building &&
        it != job.lastStableBuild &&
        it != job.lastSuccessfulBuild &&
        it != job.lastUnstableBuild &&
        it != job.lastUnsuccessfulBuild

    }.each { build ->
        build.delete()
    }
}
