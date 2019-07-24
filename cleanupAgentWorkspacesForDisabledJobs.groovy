import hudson.model.*;
import hudson.util.*;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;

/**
  NOTE: Doesn't support custom workspaces
**/


def performCleanup(def node, def items) {

  for (item in items) {
    jobName = item.getFullDisplayName()

    println("Cleaning " + jobName)

    if(item instanceof com.cloudbees.hudson.plugins.folder.AbstractFolder) {
        performCleanup(node, item.items)
        continue
    }

    if (item.buildable) {
      println("Job " + jobName + " is currently enabled. Skipping.")
      continue
    }


    println("Wiping out workspaces of job " + jobName)

    workspacePath = node.getWorkspaceFor(item)
    if (workspacePath == null) {
      println("Could not get workspace path")
      continue
    }

    println("Workspace = " + workspacePath)

    pathAsString = workspacePath.getRemote()
    if (workspacePath.exists()) {
      workspacePath.deleteRecursive()
      println("Deleted from location " + pathAsString)
    } else {
      println("Nothing to delete at " + pathAsString)
    }
  }  
}


for (node in Jenkins.instance.nodes) {
    computer = node.toComputer()
    if (computer.getChannel() == null) continue

    rootPath = node.getRootPath()
    size = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
    roundedSize = size / (1024 * 1024 * 1024) as int

    println("node: " + node.getDisplayName() + ", free space: " + roundedSize + "GB")
    computer.setTemporarilyOffline(true, new hudson.slaves.OfflineCause.ByCLI("disk cleanup"))

    performCleanup(node, Jenkins.instance.items)

    computer.setTemporarilyOffline(false, null)

}