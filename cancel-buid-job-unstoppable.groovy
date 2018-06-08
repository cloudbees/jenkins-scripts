/*
Use this script to stop an unstoppable zombie job on
Jenkins without restarting the server.

Access Manage Jenkins > Script Console

jobName should be full job name from root if mutliple levels down(for example "Folder1/Folder2/Job")
 */
def jobname = '<Job Name>'

def job = Jenkins.instance.getItemByFullName(jobname)


for (build in job.builds) {
    def number = build.getNumber().toInteger()
    println(build)
    println(build.getNumber().toInteger())
    Jenkins.instance.getItemByFullName("${jobname}").getBuildByNumber(number).finish(hudson.model.Result.ABORTED,
            new java.io.IOException("Aborting build"))
}