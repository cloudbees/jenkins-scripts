/*** BEGIN META {
 "name" : "Get Shared Clouds status JSON",
 "comment" : "Run this script to save the output as variable to process in the client 
 "parameters" : [],
 "core": "2.73.2.1",
 "authors" : [
 { name : "Carlos Rodriguez Lopez" }
 ]
 } END META**/

import groovy.json.JsonOutput

def shareCloud = []
Jenkins.instance.allItems.grep {
   it.class.name == 'com.cloudbees.opscenter.server.model.SharedCloud'
}.each {
    def jnlpCloud = it?.cloud
    def connected = jnlpCloud?.countConnectedSlaves() ?: 0
    def available = jnlpCloud?.countAvailableSlaves() ?: 0
    def inuse = connected - available
    def inuse_ratio = 0
    if (inuse != 0 || connected != 0) {
        inuse_ratio = (connected - available) / connected
    }
    def availablenodes = []
    jnlpCloud?.channelStates.each { c ->
    if (c.available) {
        availablenodes.add(c?.name)
      }
    }
    def shareCloudItem = [
          name: "${it?.name}",
        connected: "$connected",
        available: "$available",
        inuse_ratio: "$inuse_ratio",
        free_vms: availablenodes
    ]
    shareCloud << shareCloudItem
}

def jsonShareCloud = JsonOutput.toJson(shareCloud)
println jsonShareCloud
return this