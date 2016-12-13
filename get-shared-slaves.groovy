/*** BEGIN META {
 "name" : "Get Shared Slaves",
 "comment" : "Get the Shared Slaves and Shared Clouds",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.opscenter.server.model.SharedSlave;
import com.cloudbees.opscenter.server.model.SharedCloud
import jenkins.model.Jenkins;

//Get the SharedSlave(s)
println "Shared Slaves: ${Jenkins.instance.getAllItems(SharedSlave.class)}";
//Get the SharedCloud(s)
println "Shared Cloud: ${Jenkins.instance.getAllItems(SharedCloud.class)}";