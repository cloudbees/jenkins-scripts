/*** BEGIN META {
 "name" : "Check Node Online",
 "comment" : "Check if a particular Node is online/offline. Check if nodes of a particular Label are online/offline",
 "parameters" : [ 'nodeName', 'nodeLabel'],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import hudson.model.Node
import jenkins.model.Jenkins

def nodeName = 'osx-1'
println "Check by name ${nodeName}"
Node myNode = Jenkins.instance.nodes.find{item -> item.getNodeName()== nodeName}
if(myNode.toComputer().isOffline()) {
    println "${myNode.getNodeName()} is offline"
} else {
    println "${myNode.getNodeName()} is online"
}

def nodeLabel = 'osx'
println "Check by label ${nodeLabel}"
Node [] myLabelNodes = Jenkins.instance.nodes.find{item -> item.getLabelString()== nodeLabel}
myLabelNodes.each {
    myLabelNode -> if(myLabelNode.toComputer().isOffline()) {
        println "${myLabelNode.getNodeName()} is offline"
    } else {
        println "${myLabelNode.getNodeName()} is online"
    }
}