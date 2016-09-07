/*
Author: Carlos Rodriguez Lopez
Since: September 2017
Description: It retrieves all the job templates items available in pFolder and include them into the restrict list of items to create inside all Folder plus of the instance
Scope: Cloudbees Jenkins Enterprise
*/

import com.cloudbees.hudson.plugins.modeling.Model
import com.cloudbees.hudson.plugins.folder.properties

def jenkinsTemplates = Jenkins.instance.getAllItems(com.cloudbees.hudson.plugins.modeling.Model.class)
def jenkinsFolders = Jenkins.instance.getAllItems(com.cloudbees.hudson.plugins.folder.Folder.class)
def pFolders2exclude = []
def Set<String> allowedTypes = new TreeSet <String>()

jenkinsTemplates.each{ template ->
    allowedTypes.add(template.id)
}

jenkinsFolders.each{ folder ->
    folder.properties.each{ prop ->
        if (prop instanceof com.cloudbees.hudson.plugins.properties.SubItemFilterProperty) {
            prop.allowedTypes = allowedTypes
            folder.save()
        }
    }
}