/*** BEGIN META {
 "name" : "Get Update Sites",
 "comment" : "Get all the Update Sites configured in Jenkins Update Center.",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import jenkins.model.Jenkins

println Jenkins.getInstance().getUpdateCenter().getSiteList().each {
    println "Update Site '${it.id}':${it.url}"
}