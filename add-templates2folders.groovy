/*
Author: Carlos Rodriguez Lopez
Since: September 2017
Description: It retrieves all the job templates items available in the instance and include them into the restrict list of items to create inside pFolder
Parameters: pFolders
Scope: Cloudbees Jenkins Enterprise
*/

import com.cloudbees.hudson.plugins.modeling.Model
import com.cloudbees.hudson.plugins.folder.Folder
import com.cloudbees.hudson.plugins.folder.properties.SubItemFilterProperty

// PARAMETER
// Note the structure of the Folder item in terms of file system
def pFolders = "job/TestFolder/job/testAssigment/"

Set<String> allowedTypes = new TreeSet <String>()
def processingFlag = false

// A - Getting items from the jenkins instance

def jenkins = Jenkins.instance
def jenkinsTemplates = jenkins.getAllItems(Model.class)
def jenkinsFolders = jenkins.getAllItems(Folder.class)

// B - Preparing templates element for assignating them to pFolder

if (jenkinsTemplates.size() > 0) {
    jenkinsTemplates.each{ template ->
        // println "[DEBUG]: "+template.id
        // Each job Teampletes is consider by jenkins as 1 type of jobs
        allowedTypes.add(template.id)
    }
} else {
    println "[ERROR]: There are not job templates available in this instance"
}

//c - Assigning templates  to pFolder

if (jenkinsFolders.size() > 0) {
    //if the instance contains template
    if (allowedTypes.size() > 0) {
        jenkinsFolders.each{ folder->
            //filter example
            if ((folder.url).equals(pFolders)){
                // println "[DEBUG]: "+folder.url
                folder.properties.each{ prop ->
                    if ((prop instanceof SubItemFilterProperty)==true){
                        prop = new SubItemFilterProperty (allowedTypes)
                        if (!processingFlag) processingFlag = true
                        jenkins.save()
                        println "[INFO]: This instance Templates has been added as Item Filter property for '$pFolders'"
                    }
                }
            }

        }
    }
} else {
    println "[ERROR]: There are not folders available in this instance"
}

if (!processingFlag){
    println "[ERROR]: The folder url '$pFolders' is not available in this instance"
}