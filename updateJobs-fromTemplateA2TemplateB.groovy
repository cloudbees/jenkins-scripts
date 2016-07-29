/*
Author: Dario Villadiego, Carlos Rodriguez Lopez
Since: July 2017
Description: It finds all the jobs in CJE instance based on  "Template_A"  and update those to "Template_B"
Scope: Cloudbees Jenkins Enterprise
*/

import com.cloudbees.hudson.plugins.modeling.ModelList
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.InstanceFromJobTemplate
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobPropertyImpl

def String NEW_JOB_TEMPLATE = 'Template_B'
def String OLD_JOB_TEMPLATE = 'Template_A'
def InstanceFromJobTemplate itemIntFJTempl, newIntFJTempl  = null
def int numberOfJobUpdated = 0

Jenkins.instance.items.findAll { job ->
    itemIntFJTempl = InstanceFromJobTemplate.from(job)
    //Filtering Jobs based on templates and from those, the ones with the model name OLD_JOB_TEMPLATE
    if (itemIntFJTempl != null && itemIntFJTempl.model.name == OLD_JOB_TEMPLATE) {
        //Checking NEW_JOB_TEMPLATE is currently included in the instance's Model List
        if (ModelList.get().getItem(NEW_JOB_TEMPLATE) != null) {
            //Create a a Instance from the Job Template $NEW_JOB_TEMPLATE
            newIntFJTempl = new InstanceFromJobTemplate(ModelList.get().getItem(NEW_JOB_TEMPLATE))
            //Assigning new template to the job and saving it
            job.addProperty(new JobPropertyImpl(newIntFJTempl))
            newIntFJTempl.save()
            //Checking final value
            itemIntFJTempl = InstanceFromJobTemplate.from(job)
            println "Job '${job.name}' change template to '${itemIntFJTempl.model.name}' template\n\n"
            numberOfJobUpdated ++
        } else {
            println "The template model '${NEW_JOB_TEMPLATE}' is not included in the Template List of this instance. Please assign another value for $NEW_JOB_TEMPLATE"
        }
    }
}
println "Total number of updated jobs : ${numberOfJobUpdated}"