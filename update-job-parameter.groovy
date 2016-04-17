/*** BEGIN META {
 "name" : "Update Job Parameters",
 "comment" : "Update the definition of Job Parameters (add, remove, update)",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import hudson.model.Job
import hudson.model.ParametersDefinitionProperty
import hudson.model.StringParameterDefinition
import jenkins.model.Jenkins

def jobName = 'myJobName'
def paramName = 'myParamName'
def paramDefaultValue = 'myParamDefaultValue'

/**
 * Add a parameter. Override if it does already exist.
 */
//Retrieve the Job by name
Job job = Jenkins.instance.getAllItems(Job.class).find { job -> "myJob" == job.name }
//Retrieve the ParametersDefinitionProperty that contains the list of parameters.
ParametersDefinitionProperty jobProp = job.getProperty(ParametersDefinitionProperty.class);
if (jobProp != null) {
    //Retrieve the ParameterDefinition by name
    def param = jobProp.getParameterDefinition('myParamName');
    //If the parameter exists, remove it
    if (param) {
        println("--- Parameter ${paramName} already exists, removing it ---")
        jobProp.getParameterDefinitions().remove(param);
    }
    //Add the parameter (here a StringParameter)
    println("--- Add Parameter(key=${jobName}, defaultValue=${paramName})  ---")
    jobProp.getParameterDefinitions().add(new StringParameterDefinition(paramName, paramDefaultValue))
    //Save the job
    job.save();
}

