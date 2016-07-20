/*
Author: Carlos Rodriguez Lopez
Since: July 2017
Description: It updates all the jobs in CJE instance from "Template_A" to  "Template_B"
*/

import com.cloudbees.hudson.plugins.modeling.ModelList
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.InstanceFromJobTemplate
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobPropertyImpl

def newJobTemplateName = 'Template_B'
def oldJobTemplateName = 'Template_A'
def numberOfJobUpdated = 0

Jenkins.instance.items.findAll { job ->
    def action = job.getAction(com.cloudbees.hudson.plugins.modeling.impl.entity.LinkToTemplateAction.class)
    if (action != null) {
        if (action.instance.model.name == oldJobTemplateName) {
            //Create a a Instance from the Job Template "newJobTemplateName"
            def inst = new InstanceFromJobTemplate(ModelList.get().getItem(newJobTemplateName))
            println "Job `${job.name}` currently using the `${action.instance.model.name}` template"
            //Assigning new template and saving it
            job.addProperty(new JobPropertyImpl(inst))
            inst.save()
            action = job.getAction(com.cloudbees.hudson.plugins.modeling.impl.entity.LinkToTemplateAction.class)
            println "Job `${job.name}` change template to `${action.instance.model.name}` template\n\n"
            numberOfJobUpdated ++
        }
    }
}

println 'Total number of updated jobs : ' + numberOfJobUpdated
