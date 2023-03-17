import com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep

def retour = ''
def stream = new ByteArrayOutputStream();
def listener = new StreamBuildListener(stream);

Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.ConnectedMaster).each{
    it.channel?.call(new MasterGroovyClusterOpStep.Script("""
        class ComposedItem {
            String controllerName
            Integer numberOfJobsController
            String[] allJobsNamesController
        }
        
        jobsNameFromController = [];

        jenkins.model.Jenkins.get().allItems(hudson.model.Job).each {
        	jobsNameFromController.push(it.getFullName())
        }

        def aux = new ComposedItem();
        aux.controllerName = "${it.name}"
        aux.numberOfJobsController = jobsNameFromController.size()
        aux.allJobsNamesController = jobsNameFromController

        return aux.dump()
    """, listener, "host-script.groovy", [:]))
    retour = stream.toString().minus('Result: ')
}

return retour