/*** BEGIN META {
 "name" : "List controlled agents / approved folders",
 "comment" : "This script list all controlled agents and their approved folders. 
 It has been tested with version 2.289.3.2 of CloudBees Core",
 "parameters" : [],
 "core": "2.289.3.2",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

jenkins.model.Jenkins.instanceOrNull.nodes.each { node ->
    node.nodeProperties
      .findAll {(it.class.name == 'com.cloudbees.jenkins.plugins.foldersplus.SecurityTokensNodeProperty')}
      .each {
        println "Node:\t${node.nodeName}"
        println "Folders:"
          ((com.cloudbees.jenkins.plugins.foldersplus.SecurityTokensNodeProperty)it).getSecurityTokens()
          .collect {it.getFolders()}.flatten()
          .each {println "\t${it.fullName}"}
    }
}
return