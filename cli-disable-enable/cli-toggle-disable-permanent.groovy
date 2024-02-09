import java.nio.file.Files
import java.nio.file.Paths

def jenkins_home = Jenkins.get().getRootDir().absolutePath
def initDirPath = Paths.get(jenkins_home, "init.groovy.d")

if (!Files.exists(initDirPath)) {
    println "Creating Post-Initialization directory '${initDirPath}'..."
    Files.createDirectories(initDirPath);
}

def groovyFile = initDirPath.toRealPath().toString() + "/cli-shutdown.groovy"

def codestr="""
// disable CLI access over HTTP / Websocket

def extensionLists = [
  hudson.cli.CLIAction.class,
  hudson.ExtensionPoint.class,
  hudson.model.Action.class,
  hudson.model.ModelObject.class,
  hudson.model.RootAction.class,
  hudson.model.UnprotectedRootAction.class,
  java.lang.Object.class,
  org.kohsuke.stapler.StaplerProxy.class,
  hudson.model.Action.class
]

def archiveThenRemove = { lst ->
    def archive = lst.findAll { it.getClass().getName()?.contains("CLIAction") }
    lst.removeAll(archive)
    return archive
}

def j = jenkins.model.Jenkins.get();
def extensionListsMap = [:]
extensionLists.each { extension ->
    extensionListsMap[extension.getName()] = archiveThenRemove(j.getExtensionList(extension))
}
extensionListsMap["actions"] = archiveThenRemove(j.actions)


// disable CLI access over SSH
if (j.getPlugin('sshd')) {
  extensionListsMap["sshd_port"] = hudson.ExtensionList.lookupSingleton(org.jenkinsci.main.modules.sshd.SSHD.class).getPort()
  hudson.ExtensionList.lookupSingleton(org.jenkinsci.main.modules.sshd.SSHD.class).setPort(-1)
}

//store disabled lists in a new property of the Jenkins instance
j.metaClass.extensionListsMap = extensionListsMap
"""

try {
    println "Creating Post-Initialization script '${groovyFile}'..."
    File file = new File(groovyFile)
    file.newWriter().withWriter {it << codestr};
} catch (Exception ex) {
    println "Unable to create '$groovyFile': " + ex.getMessage() + ". If instance restarts, this script must be applied again"
}