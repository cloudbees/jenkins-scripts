/**
Author: Alex Taylor
Since: July 2019
Description: Show the Trigger restrictions that you have on ever folder item
Scope: Any
**/
import com.cloudbees.jenkins.plugins.trigger.*
import com.cloudbees.jenkins.plugins.trigger.restrictions.property.TriggerRestrictionsAction;
import com.cloudbees.jenkins.plugins.trigger.restrictions.property.TriggerRestrictionsData;
import com.cloudbees.jenkins.plugins.trigger.restrictions.TriggerRestriction;
import com.cloudbees.jenkins.plugins.trigger.restrictions.Restrictions;
import com.cloudbees.hudson.plugins.folder.AbstractFolder;



import com.cloudbees.hudson.plugins.folder.Folder

for(item in Jenkins.instance.getAllItems())
{
  if (item instanceof AbstractFolder) {
    if (item.getAction(TriggerRestrictionsAction.class).getRestrictionsAsString()!=""){
      println("The folder is: " + item.name)
      println("Restrictions: " + item.getAction(TriggerRestrictionsAction.class).getRestrictionsAsString())
    }
  }
}