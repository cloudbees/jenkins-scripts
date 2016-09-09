/*
Author: Carlos Rodriguez Lopez
Since: September 2017
Description: It retrieves all the job templates items available in pFolder and include them into the restrict list of items to create inside all Folder plus of the instance
Scope: Cloudbees Jenkins Enterprise
*/

import com.cloudbees.hudson.plugins.modeling.Model
import com.cloudbees.hudson.plugins.folder.Folder
import com.cloudbees.hudson.plugins.folder.properties.SubItemFilterProperty

def jenkins = Jenkins.instance
def jenkinsTemplates = jenkins.getAllItems(Model.class)
def jenkinsFolders = jenkins.getAllItems(Folder.class)
// Note the structure of the Folder item in terms of file system
def pFolders = "job/TestFolder/job/testAssigment/"
def Set<String> allowedTypes = new TreeSet <String>()

jenkinsTemplates.each{ template ->
    allowedTypes.add(template.id)
}

jenkinsFolders.each{ folder->
    //filter example
    if ((folder.url).equals(pFolders)){
        println "[DEBUG]: "+folder.url
        folder.properties.each{ prop ->
            if ((prop instanceof SubItemFilterProperty)==true){
                prop = new SubItemFilterProperty (allowedTypes)
                jenkins.save()
            }
        }
    }
}