findItems(jenkins.model.Jenkins.instance.items.findAll());

def findItems(items) {
  for (item in items) {
    switch(item) {
        case org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject:
            item.getItems()?.each { itemMultibranch ->
                searchStringInLogBuild(itemMultibranch, "String that you are looking for")
            }
            break;
        case org.jenkinsci.plugins.workflow.job.WorkflowJob:
            searchStringInLogBuild(item, "String that you are looking for")
            break;

        // you can add other kinds of Items using a case clause, or even a default value.

        case com.cloudbees.hudson.plugins.folder.Folder:
            findItems(item.getItems());
            break;
    }
  }
}

def searchStringInLogBuild(item, string) {
  item.getBuilds()?.each { build ->
    if (build.isBuilding()) {
      build.getLog()?.eachLine { line ->
      if (line =~ string) {
        println "Build being executed and it's going to be aborted: $build.project in build Nº$build.number"
        build.finish(hudson.model.Result.ABORTED, new java.io.IOException("Aborting build"))
        println "-------------"
      }
    }
    }
  }
}

return null