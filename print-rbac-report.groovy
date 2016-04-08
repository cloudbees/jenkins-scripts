/**
 * Author: Jean-Philippe Briend <jbriend@cloudbees.com>
 * This script outputs the RBAC configuration across all the Folders.
 * It must be executed on an Operations Center server.
 * It reports Folder name, Groups and Roles attached.
 * This script also enters in connected Client Masters and reports RBAC infos.
 *
 * Output looks like:
 * Jenkins
     Granted groups:
       + Administrators
          * Members: [admin]
          * Roles: [administer (propagates)]
       + Developers
          * Members: []
          * Roles: [develop (propagates)]
       + Browsers
          * Members: [user1, user2]
          * Roles: [browse (propagates)]
   Jenkins/Admin jobs
     Filters:
       - develop
       - browse
   Jenkins/Admin jobs » Masters Daily backup
   Jenkins/Admin jobs » Masters Weekly backup
   Jenkins/Admin jobs » test
   Jenkins/MyOrg
   ...
 */
import nectar.plugins.rbac.groups.*
import java.util.*
import com.cloudbees.opscenter.server.model.*
import com.cloudbees.opscenter.server.clusterops.steps.*

// Container used to handle connected Client masters
class ExploredObject {
  GroupContainer groupContainer
  Boolean isMaster
  Item instance
}

Map containers = new TreeMap();

// Add the root container
def root = new ExploredObject()
root.groupContainer = GroupContainerLocator.locate(Jenkins.instance)
root.isMaster = false
containers.put(Jenkins.instance.displayName, root)

// Add all the items that are be containers
for (i in Jenkins.instance.allItems) {
  if (GroupContainerLocator.isGroupContainer(i.getClass())) {
    GroupContainer g = GroupContainerLocator.locate(i)

    if (g != null) {
      def exploredObject = new ExploredObject()
      exploredObject.groupContainer = g
      exploredObject.isMaster = i instanceof ConnectedMaster
      exploredObject.instance = i
      containers.put("${Jenkins.instance.displayName}/${i.fullDisplayName}", exploredObject)
    }
  }
}

// Add all the nodes, as they are containers also (but be safe about it)
for (i in Jenkins.instance.nodes) {
  if (GroupContainerLocator.isGroupContainer(i.getClass())) {
    GroupContainer g = GroupContainerLocator.locate(i);
    if (g != null) {
      def exploredObject = new ExploredObject()
      exploredObject.groupContainer = g
      exploredObject.isMaster = i instanceof ConnectedMaster
      exploredObject.instance = i
      containers.put("${Jenkins.instance.displayName}/${i.fullDisplayName}", exploredObject)
    }
  }
}

for (cont in containers) {
  def c = cont.value.groupContainer
  println(cont.key)

  if (c.roleFilters.size() > 0) {
    println("  Filters:")
    for (filter in c.roleFilters) {
      println("    - ${filter}")
    }
  }

  if (c.groups.size() > 0) {
    println("  Granted groups:")

    for (g in c.groups) {
      println("    + ${g.name}")
      println("      * Members: ${g.members}")
      println("      * Roles: ${g.roles.collect {it + (g.doesPropagateToChildren(it) ?' (propagates)':'(pinned)')}}")
    }
  }

  /*
  If this container is a connected Client Master, execute a remote Groovy script on this Master
   */
  if (cont.value.isMaster && cont.value.instance.channel) {
    try {
      def retour = '\n'
      def stream = new ByteArrayOutputStream();
      def listener = new StreamBuildListener(stream);
      // Execute remote Groovy script in the Client Master
      // Result of the execution must be a String
      cont.value.instance.channel.call(new MasterGroovyClusterOpStep.Script("""
            import nectar.plugins.rbac.groups.*;
            import java.util.*;

            result = ''

            Map containers = new TreeMap();
            containers.put("${cont.key}", GroupContainerLocator.locate(Jenkins.instance));
            for (i in Jenkins.instance.allItems) {
              if (GroupContainerLocator.isGroupContainer(i.getClass())) {
                GroupContainer g = GroupContainerLocator.locate(i);
                if (g != null) containers.put("${cont.key} » \${i.fullDisplayName}", g);
              }
            }

            for (c in containers) {
              result = result + "\${c.key}\\n"
              if (c.value.roleFilters.size() > 0) {
                result = result + "  Filters:\\n"
                for (filter in c.value.roleFilters) {
                  result = result + "    - \${filter}\\n"
                }
              }

              if (c.value.groups.size() > 0) {
                result = result + "  Granted groups:\\n"

                for (g in c.value.groups) {
                  result = result + "    + \${g.name}\\n"
                  result = result + "      * Members: \${g.members}\\n"
                  result = result + "      * Roles: \${g.roles.collect {it + (g.doesPropagateToChildren(it) ?' (propagates)':'(pinned)')}}\\n"
                }
              }

            }

            return result
        """, listener, "host-script.groovy"))
      retour = retour << stream.toString().minus('Result: ').minus('\n\n')
      println(retour)
    } catch (hudson.remoting.ProxyException exception) {
      println " ***** Exception ***** : ${exception.message }"
    } catch (org.acegisecurity.userdetails.UsernameNotFoundException noSecurityException) {
      println "***** ^ This master has security disable, thus no RBAC configuration is available. *****"
    }
  }

}