#!/bin/env groovy
/*  A system script which will gather node items
    about any system its being run from and will
    remove the workspace folders of each of the
    folders it holds recursively based on whether
    it have ever been started, when the last build
    was kicked off, if it is currently building,
    while setting the node offline.
    partly derived from source: http://blog.ehrnhoefer.com/2016-06-14-jenkins-workspace-cleanup/
 */

import hudson.model.*;
import hudson.matrix.*;
import hudson.maven.*;
import hudson.util.*;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;

Jenkins.instance.nodes.each { node ->
    computer = node.toComputer()
    if (computer.getChannel() == null) continue

    rootPath = node.getRootPath()
    size = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
    roundedSize = size / (1024 * 1024 * 1024) as int

    println("node: " + node.getDisplayName() + ", free space: " + roundedSize + "GB")
    if (roundedSize < 10) {
        computer.setTemporarilyOffline(true, new hudson.slaves.OfflineCause.ByCLI("disk cleanup"))
        Jenkins.instance.getAllItems(Job.class).each { item ->

            // MavenModule is superfluous project returned by getAllItems()
            if (!(item instanceof MatrixConfiguration || item instanceof MavenModule)) {
                println item

                jobName = item.getFullDisplayName()

                if (item.isBuilding()) {
                    println(".. job " + jobName + " is currently running, workspace skipped")
                } else {
                    numbuilds = item.builds.size()
                    if (numbuilds == 0) {
                        println 'JOB: ' + item.fullName
                        println '  -> no build'

                    } else {
                        println(".. wiping out workspace of job " + jobName)

                        workspacePath = node.getWorkspaceFor(item)
                        lastbuild = item.builds[numbuilds - 1]
                        pathAsString = workspacePath.getRemote()
                            if (workspacePath.exists()) {
                                workspacePath.deleteRecursive()
                                println(".... deleted from location " + pathAsString)
                            }
                        println(".... workspace = " + workspacePath)
                    }
                }
            }
        }
        computer.setTemporarilyOffline(false, null)
    }
}
