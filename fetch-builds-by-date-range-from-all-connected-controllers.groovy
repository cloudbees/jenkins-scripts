import com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep

def retour = ''
def stream = new ByteArrayOutputStream();
def listener = new StreamBuildListener(stream);

Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.ConnectedMaster).each{
    it.channel?.call(new MasterGroovyClusterOpStep.Script("""
          import jenkins.model.Jenkins
          import java.time.Instant

          // dates should use the ISO8601 format
          def startDate = Instant.parse("2022-06-24T16:10:43.000Z")
          def endDate = Instant.now()

          def builds = Jenkins.instance.allItems(hudson.model.Job.class)
                  .collect { it.builds.byTimestamp(startDate.toEpochMilli(), endDate.toEpochMilli()) }
                  .flatten()

          return builds
    """, listener, "host-script.groovy", [:]))
    retour = stream.toString().minus('Result: ')
}

return retour