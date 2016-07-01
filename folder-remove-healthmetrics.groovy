import com.cloudbees.hudson.plugins.folder.health.FolderHealthMetric

def folders = Jenkins.instance.getAllItems(com.cloudbees.hudson.plugins.folder.Folder.class)
folders.each{ folder ->
  folder.healthMetrics.each{ metric ->
    if (!(metric instanceof com.cloudbees.hudson.plugins.folder.health.ProjectEnabledHealthMetric)) {
      println "Removing ${metric.class.simpleName} from ${folder.name}"
      folder.healthMetrics.remove(metric)
      folder.save()
    }
  }
}
return null
