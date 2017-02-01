/**
Author: kuisathaverat
Description: remove the association between a folder an their parent template
Parameters: name of the folder
**/
import com.cloudbees.hudson.plugins.folder.Folder

def name = 'test'

def jenkins = Jenkins.instance
def jenkinsFolders = jenkins.getAllItems(Folder.class)
jenkinsFolders.each{ folder->
    if ((folder.name).equals(name)){
      	def template
        folder.getProperties().each{ p -> 
          if(p instanceof com.cloudbees.hudson.plugins.modeling.impl.folder.PropertyImpl){
          	template = p
          }
        }
        folder.getProperties().remove(template)
        jenkins.save()
    }
}
