import com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep

def retour = ''
def stream = new ByteArrayOutputStream();
def listener = new StreamBuildListener(stream);

Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.ConnectedMaster).each{
    it.channel?.call(new MasterGroovyClusterOpStep.Script("""
        class composedItem {
            Object controllerName
            Integer numberOfJobs
            String allJobs
        }
        
        jobsController = Jenkins.instance.items.findAll()

        def aux = new composedItem();
        aux.controllerName = "${it.name}"
        aux.numberOfJobs = jobsController.size()
        aux.allJobs = jobsController

        return aux.dump()
    """, listener, "host-script.groovy", [:]))
    retour = stream.toString().minus('Result: ')
}

return retour