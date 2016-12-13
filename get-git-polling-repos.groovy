/*** BEGIN META {
 "name" : "Get all Polling Repos",
 "comment" : "Print all the Git branches and repositories of jobs that have polling configured",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/


import hudson.plugins.git.GitSCM
import hudson.triggers.SCMTrigger
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

def activeJobs = Jenkins.instance.getAllItems(AbstractProject.class)
        .findAll { job -> job.isBuildable()}
        .findAll { job -> job.getTrigger(SCMTrigger)}
        .findAll { job -> job.scm != null && job.scm instanceof GitSCM}
        .collect();

//Solution 1
for (project in activeJobs) {
    scm = project.scm;
    println("${project.name}, repositories: ${scm.repositories.collect{ it.getURIs() }}, branches: ${scm.branches.collect{ it.name }}")
}

//Solution 2
for (project in activeJobs) {
    scm = project.scm;
    def reposNames = []
    for (RemoteConfig cfg : scm.getRepositories()) {
        for (URIish uri : cfg.getURIs()) {
            reposNames.add(uri.toString())
        }
    }
    println("${project.name}, repositories: ${reposNames}, branches: ${scm.branches.collect{ it.name }}")
}