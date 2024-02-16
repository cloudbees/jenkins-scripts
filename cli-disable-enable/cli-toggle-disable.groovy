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

try {
  if (j.extensionListsMap != null && !j.extensionListsMap.isEmpty() ) {
    println "CLI is already disabled"
    return 
  }
} catch (groovy.lang.MissingPropertyException e) {/*Continue*/}

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

println "CLI disabled"
