/*
Author: carlosrodlop
Since: October 2018
Parameters: <Master_Update_Center-ID>
Tested on: Managed Master 2.107.1.2-rolling
*/


/**
Get <Master_Update_Center-ID>
Jenkins.getInstance().getUpdateCenter().getSiteList().each {
  println "Master_Update_Center-ID: ${it.id}"
}
*/

import hudson.model.UpdateSite

Set<String> plugins_to_install = []
long installerCounter = 0
Boolean dynamicLoad = false
UpdateSite updateSite = Jenkins.getInstance().getUpdateCenter().getById('<Master_Update_Center-ID>')

def availablePlugins = Jenkins.instance.updateCenter.getCategorizedAvailables().each {
  plugins_to_install.add(it.plugin.name)
}

plugins_to_install.each {
  println "Installing ${it} - ${++installerCounter} from ${plugins_to_install.size()}"
  UpdateSite.Plugin plugin = updateSite.getPlugin(it)
  Throwable error = plugin.deploy(dynamicLoad).get().getError()
  if(error != null) {
    println "ERROR installing ${it}, ${error}"
  }
}

if(plugins_to_install.size() != 0 && installerCounter == plugins_to_install.size()) {
   jenkins.model.Jenkins.instance.safeRestart()
}