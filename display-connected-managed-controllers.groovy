/**
 * List Managed Masters with URLs
 */

def mms = Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.ManagedMaster)

String outputFormat = "%-32s%-50s%s"
println String.format(outputFormat,"Display Name", "Managed Master Full Display Name", "URL")

mms.each {
  it.getPersistedState().resource.each { p -> 
    println String.format(outputFormat, it.getDisplayName(), it.getFullDisplayName(), p.getEndpoint())
  }
}
null
