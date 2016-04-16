/*** BEGIN META {
 "name" : "Get all Jobs recursively",
 "comment" : "Get all the jobs recursively into ItemGroup (such as Folders)",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import hudson.model.AbstractProject
import hudson.model.ItemGroup
import hudson.triggers.SCMTrigger
import jenkins.model.Jenkins

def findAllItems(items){
    for(item in items)
    {
        if (!(item instanceof ItemGroup)) {
            if(item instanceof AbstractProject && item.getTrigger(SCMTrigger)) {
                println(item.name); //Print scheduled job
            }
        } else {
            //Drill into folders
            //println(item.name); //Print folder name
            findAllItems(((ItemGroup) item).getItems())
        }
    }
}
findAllItems(Jenkins.instance.items);
