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

Jenkins.instanceOrNull.allItems(AbstractFolder.class)
        .findAll { folder -> folder.getAction(TriggerRestrictionsAction.class)?.getRestrictionsAsString() != "" }
        .each { folder ->
            println("The folder is: " + folder.name)
            println("Restrictions: " + folder.getAction(TriggerRestrictionsAction.class)?.getRestrictionsAsString())
}
