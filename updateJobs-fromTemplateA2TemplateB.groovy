/*
Author: Dario Villadiego, Carlos Rodriguez Lopez
Since: July 2017
Description: It finds all the jobs in CJE instance based on  "Template_A"  and update those to "Template_B". On its way, it migrates Properties from "Template_A" to "Template_B".
Scope: Cloudbees Jenkins Enterprise
*/

import com.cloudbees.hudson.plugins.modeling.ModelList
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.InstanceFromJobTemplate
import com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobPropertyImpl

// Job Templates - Names
def OLD_JOB_TEMPLATE = 'Template_A'
def NEW_JOB_TEMPLATE = 'Template_B'
// Job Templates - Property JOB_NAME_PROP. It is the default parameters for jobs templates
def JOB_NAME_PROP = 'name'
// Job Templates - Property - Others:
def OLD_JOB_TEMPLATE_PROP = 'Template_A_prop'
def NEW_JOB_TEMPLATE_PROP ='Template_B_prop'


def InstanceFromJobTemplate itemIntFJTempl, newIntFJTempl  = null
def numberOfJobUpdated = 0
def name2migrate = ''
def repo2migrate = ''

//1. Checking that NEW_JOB_TEMPLATE and OLD_JOB_TEMPLATE are currently included in the instance's Model List
if (ModelList.get().getItem(OLD_JOB_TEMPLATE) != null && ModelList.get().getItem(NEW_JOB_TEMPLATE) != null) {
    // 2. Retrieving all the job items from the current Jenkins instance
    Jenkins.instance.items.findAll { job ->
        itemIntFJTempl = InstanceFromJobTemplate.from(job)
        //3. Filtering job items based on templates and from those, the ones with the model name OLD_JOB_TEMPLATE
        if (itemIntFJTempl != null && itemIntFJTempl.model.name == OLD_JOB_TEMPLATE) {
            //4. Checking the properties from $OLD_JOB_TEMPLATE
            if (itemIntFJTempl.getValue(JOB_NAME_PROP)!=null && itemIntFJTempl.getValue(OLD_JOB_TEMPLATE_PROP)!=null){
                //5. Get parameters values from $OLD_JOB_TEMPLATE
                name2migrate = itemIntFJTempl.getValue(JOB_NAME_PROP)
                repo2migrate = itemIntFJTempl.getValue(OLD_JOB_TEMPLATE_PROP)
                //6. Create a new Instance from the Job Template $NEW_JOB_TEMPLATE
                newIntFJTempl = new InstanceFromJobTemplate(ModelList.get().getItem(NEW_JOB_TEMPLATE))
                //7. Assigning new template to the job item
                job.addProperty(new JobPropertyImpl(newIntFJTempl))
                //8. Migrate old parameters to the $NEW_JOB_TEMPLATE instance
                newIntFJTempl.setValue(JOB_NAME_PROP, name2migrate)
                newIntFJTempl.setValue(NEW_JOB_TEMPLATE_PROP, repo2migrate)
                //9. Save instance in order changes take place
                newIntFJTempl.save()
                //10. Checking final value
                itemIntFJTempl = InstanceFromJobTemplate.from(job)
                println "[INFO] Job '${job.name}' change template to '${itemIntFJTempl.model.name}' template\n\n"
                numberOfJobUpdated ++
            } else {
                println "[ERROR] Any of the '$OLD_JOB_TEMPLATE' properties are not correct"
            }
        } // end of step 3
    } // end of step 2
    println "[INFO] Total number of updated template-based jobs : $numberOfJobUpdated"
} else {
    println "[ERROR] The '$OLD_JOB_TEMPLATE' and/or '$NEW_JOB_TEMPLATE' are not included into the Model List"
}