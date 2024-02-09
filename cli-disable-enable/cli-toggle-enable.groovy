
// re-enable CLI access over HTTP / Websocket
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

j = jenkins.model.Jenkins.get();
def extensionListsMap = j.extensionListsMap

extensionLists.each { extension ->
    if (extensionListsMap[extension.getName()] != null ) {
      j.getExtensionList(extension).addAll(extensionListsMap[extension.getName()])
    }
}

if (extensionListsMap["actions"] != null) {
  if (j.actions == null) {
    j.actions = extensionListsMap["actions"]
  } else {
    j.actions.addAll(extensionListsMap["actions"])
  }
}

//re-enable CLI access over SSH
if (j.getPlugin('sshd') && extensionListsMap["sshd_port"] != null) {
  hudson.ExtensionList.lookupSingleton(org.jenkinsci.main.modules.sshd.SSHD.class).setPort(extensionListsMap["sshd_port"])
}