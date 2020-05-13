/*
Script #2 - Upgrade your instances
Checks for and applies incremental updates for you jenkins instances so that they are ready to consume the new license
*/

// script version
def _version = "ce00ec5"

// Set restart = true to automatically restart jenkins after the update is applied. 
// A restart is always required after plugin upgrade. It can be done either manually or
// automatically using the script. 
def restart = false
// Set slowConnection = true if the connection performance between OC and masters is not good enough.
def slowConnection = false
// set debug = true for additional debug ouput. The output is supposed to be consumed by a support engineer.
def debug = false
// set skipMasters = true to avoid requests to masters. Please, don't enable it unless you know what you are doing. <
def skipMasters = false
// set direct = true to enable directly updating the cloudbees-license-plugin if no incremental update is available (should not be needed).
// direct method is useful when BeeKeeper is disabled or the instance cannot reach the public update site. It only replaces the current version 
// of cloudbees-license plugin by its patched version.
def direct = false

// Scripts
// ------------------------------------------------------------------------------------------------
def script_status = '''
def _status = ["-","-","-","-","-", "-"]
try {

    try {
      def assurance = com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get()
      def cap
      if (assurance.metaClass.respondsTo(assurance, "getBeekeeperState",null).isEmpty()) {
        if (assurance.metaClass.respondsTo(assurance, "getBeekeeper", null).isEmpty()) {
          cap = assurance.getReport().getStatus()
        } else {
          cap = assurance.getBeekeeper().getStatus()
        }
      } else {
        cap = assurance.getBeekeeperState().getStatus()
      }
      // [2] Is BeeKeeper enabled? 
      if (cap == "SUCCESS") {
        _status[2] = "1"  
      } else if ("WARNING") {
        _status[2] = "2"
      } else {
        _status[2] = "0"    
      }
    } catch (Exception be) {
      _status[2] = "0"
    }

    def customUpdateSite = jenkins.model.Jenkins.instance.getUpdateCenter().getSites().find{ it.class.getName() == "com.cloudbees.opscenter.updatecenters.ContextUpdateSite"} != null
    // [3] Is Custom Update Center configured?
    if (customUpdateSite) {
        _status[3]="1"
    } else {
        _status[3]="0"
    }

    // [1] Is your instance compatible with Incremental Upgrades?
    if (_status[2] != "0" && !customUpdateSite) {
        _status[1] ='1'
    } else {
        _status[1] ='0'
    }

    def plugin = jenkins.model.Jenkins.instance.getPlugin('cloudbees-license')
    // [0] Plugin requires update?
    // [4] Is cloudbees-license plugin version compatible with new Root Ca?
    if(plugin != null && plugin.getWrapper().isActive()) {
        if (hudson.license.License.metaClass.respondsTo(hudson.license.License, "loadLicenseCaCertificates").isEmpty()) {
            // cloudbees-license plugin < 9.34 and non patched
            _status[0]='1'
            _status[4]='0'
        } else {
            // cloudbees-license plugin >= 9.34 or patched
            _status[0]='0'
            _status[4]='1'
        }
    } else {
        _status[0]='1'
    }

return _status
} catch (Exception err) { 
    _status[5] = err.getMessage()
    return _status 
}
'''

/*
 * Asks for a safe restart.
 */
script_restart = '''
jenkins.model.Jenkins.instance.doSafeRestart(null)
'''

/*
 * Applies an incremental release if available
 * returns: 
 *    * RESTART_REQUIRED if the incremental has been picked for the installation after the restart
 *    * NO if there is no incremental available
 *    * ERROR plus the cause if there is a problem with the execution
 */
script_incremental = '''try {
def tries = 10
def waitingFor = 2000

def assurance = com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get()

com.cloudbees.jenkins.plugins.assurance.props.BeekeeperProp.get().NO_FULL_UPGRADES.set()
if (assurance.metaClass.respondsTo(assurance, "refreshOfferedUpgrade").isEmpty()) {
    assurance.refreshStateSync()
} 
if (!assurance.ucRefresher.metaClass.respondsTo(assurance.ucRefresher, "awaitRefresh").isEmpty()) {
    def ucFuture = assurance.refreshUpdateCenters()
    for(int i=1; i<=tries; i++) {
        if(ucFuture.isDone()) {
            break;
        } else {
            sleep(waitingFor)
        }
    }
    assurance.refreshStateSync()
} else {
    assurance.ucRefresher.refresh()
    for(int i=1; i<=tries; i++) {
    if (com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get().getOfferedUpgrade().getClass().getName() == 'com.cloudbees.jenkins.plugins.assurance.OfferedUpgrade$Incremental') {
            break;
        } else {
            sleep(waitingFor)
        }
    }
}

def incrementalUpgrade = false

if (assurance.metaClass.respondsTo(assurance, "getUpgradeAction").isEmpty()) {
    // == 2.60.xxx
    if (com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get().getOfferedUpgrade().getClass().getName() == 'com.cloudbees.jenkins.plugins.assurance.OfferedUpgrade$Incremental') {
        incrementalUpgrade = true
      	def offered = com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get().getOfferedUpgrade()
        if (!offered.metaClass.respondsTo(offered, "pick", null).isEmpty()) {
          offered.pick()
        } else if (!offered.metaClass.respondsTo(offered, "pick", boolean).isEmpty()) {
          offered.pick(false)
        } else {
          incrementalUpgrade = false
        }
    }
} else {
    // != 2.60.xxx
    if (com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get().getUpgradeAction().getUpgrade().isIncrementalUpgrade()) {
        incrementalUpgrade = true
        com.cloudbees.jenkins.plugins.assurance.CloudBeesAssurance.get().getUpgradeAction().getUpgrade().pick(false, null)
    }
}
return incrementalUpgrade ? "RESTART_REQUIRED" : "NO"
} catch (Exception fatal) { return "ERROR. Cause: " + fatal.getMessage()}'''


/*
 * Looks for an update available for cloudbees-license plugin in the update centers
 * returns: 
 *    * RESTART_REQUIRED if the plugin has been upgraded (no effect until the restart)
 *    * NO if there is plugin update available
 *    * ERROR plus the cause if there is a problem with the execution
 */
script_custom = '''try {
jenkins.model.Jenkins.instance.updateCenter.sites.each { us -> 
  if (us.hasUpdates()) {
    us.getUpdates().each { update ->
      if (update.name == "cloudbees-license") {
       update.deploy() 
       return "RESTART_REQUIRED"
      }
    }
  }
}
return "NO"
} catch (Exception fatal) { return "ERROR. Cause: " + fatal.getMessage()}'''


/*
 * Looks for an update available for cloudbees-license plugin in the update centers
 * returns: 
 *    * RESTART_REQUIRED if the plugin has been upgraded (no effect until the restart)
 *    * NO if there is plugin update available
 *    * ERROR plus the cause if there is a problem with the execution
 */
script_download = '''
def plugin = 'cloudbees-license'
def backports = ['9.9'  : ['9.9.1',''],
                 '9.10' : ['9.10.1',''],
                 '9.11' : ['9.11.1',''],
                 '9.13' : ['9.13.1','http://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.13.1/cloudbees-license.hpi'],
                 '9.14' : ['9.14.1',''],
                 '9.17' : ['9.17.1',''],
                 '9.18' : ['9.18.0.1',''],
                 '9.18.1' : ['9.18.1.1',''],
                 '9.20' : ['9.20.1',''],
                 '9.24' : ['9.24.1',''],
                 '9.26' : ['9.26.1',''],
                 '9.27' : ['9.27.1',''],
                 '9.28' : ['9.28.1',''],
                 '9.31' : ['9.31.1',''],
                 '9.32' : ['9.32.1',''],
                 '9.33' : ['9.33.1','']]
def base = 'http://jenkins-updates.cloudbees.com/download/plugins/'
def tries = 5
def waitingFor = 5000

//----------------------------------------
try {
def _timeout = tries*waitingFor
def _restart = false
def _plugin = jenkins.model.Jenkins.instance.getPlugin(plugin)
if(_plugin != null && _plugin.getWrapper().isActive()) {
    def _version = _plugin.getWrapper().getVersionNumber().toString()
    def _backport = backports[_version]

    if (_backport != null && _backport[0] != null) {
        def _url = _backport[1] != '' ? _backport[1] : base + plugin + '/' + _backport[0] + '/' + plugin + '.hpi'

        //println "cloudbees-license " + _version + " backport " + _backport[0] + " url " + _url

        def _httpClient = new com.ning.http.client.AsyncHttpClient(new com.ning.http.client.AsyncHttpClientConfig.Builder()
                                                               .setRequestTimeoutInMs(_timeout)
                                                               .setProxyServer(jenkins.plugins.asynchttpclient.AHCUtils.getProxyServer()).build())
  
        def _future = _httpClient.prepareGet(_url).execute();
  
        for(int i=1; i<=tries; i++) {
            if(_future.isDone()) {
                break;
            } else {
                sleep(waitingFor)
            }
        }
        if (_future.isDone()){
            def _response = _future.get()
            if (_response.getStatusCode() == 200) {
                def rootDir = jenkins.model.Jenkins.instance.getRootDir()
                def pluginsDir = new java.io.File(rootDir, 'plugins')
                def pluginFile = new java.io.File(pluginsDir, plugin + '.hpi')

                def jpi = new java.io.File(pluginsDir, plugin + '.jpi')
                def bak = new java.io.File(pluginsDir, plugin + '.jpi.bak')

                org.apache.commons.io.FileUtils.copyInputStreamToFile(_response.getResponseBodyAsStream(), pluginFile);
                if (bak.isFile()) {
                    org.apache.commons.io.FileUtils.deleteQuietly(bak)
                }
                if (jpi.isFile()) {
                    org.apache.commons.io.FileUtils.moveFile(jpi, bak)
                }
                org.apache.commons.io.FileUtils.moveFile(pluginFile, jpi)
                _restart = true
            }
        }
    }
} 
return _restart ? "RESTART_REQUIRED" : "NO"
} catch (Exception fatal) { return "ERROR. Cause: " + fatal.getMessage()}
'''


// script-upgrade main code
// ------------------------------------------------------------------------------------------------

println "upgrade-license-plugin.groovy running... [v" + _version + "]"


def _statusKey = []
_statusKey[0] = "Plugin requires update?"
_statusKey[1] = "Is your instance compatible with Incremental Upgrades?"
_statusKey[2] = "Is BeeKeeper enabled?"
_statusKey[3] = "Is Custom Update Center configured?"
_statusKey[4] = "Is cloudbees-license plugin version compatible with new Root Ca?"
_statusKey[5] = "Error message"

def _summary = new StringBuilder()
def _summary2 = new StringBuilder()

def type = productType()

println "Determine the instance type: " + type
boolean all = true
if (type == Product.OPERATIONS_CENTER && !skipMasters) {
    int plugins = 0
    int offline = 0
    int tries = 20
    long waitingFor = 1000
    if (slowConnection) {
        waitingFor = 3000
    }

    jenkins.model.Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.ConnectedMaster.class).each { master ->
        println "Analyzing " + master.name + "... "
        if(master.channel != null) {
            def _masterResult = executeScriptRemotely(master, script_status, tries, waitingFor)
            if (_masterResult != null) {
               
                def _masterStatus = parseMasterStatus(_masterResult)
                if (debug) {
                    print " " + _masterStatus
                }
                if (_masterStatus.size() != 6) {
                    offline++
                    _summary.append(master.name)
                    _summary.append(" - Connected master status is not valid.\n")
                } else if (_masterStatus[0] == '1') {
                    // If plugin requires update
                    _summary.append(master.name)
                    _summary.append(" - ")
                    boolean upgraded = performPluginUpdate(_masterStatus, direct, master)
                    if(upgraded) {
                        if (debug) {
                            print " Plugin upgraded successfully. "
                        }
                        _summary.append(" Plugin upgraded successfully, ")
                        if (restart) { 
                            if (debug) {
                                print "Restarting the instance..."
                            }
                            _summary.append(" restarting the instance...\n")
                            executeScript(script_restart,master) 
                        } else {
                            if (debug) {
                                print "Manual restart required."
                            }
                            _summary.append(" manual restart required.\n")
                        }
                        println ""
                    } else {
                        plugins++
                        if (debug) {
                            println "Plugin cannot be upgraded. Please contact CloudBees support."
                        }
                        _summary.append("Plugin cannot be upgraded. Please contact CloudBees support.\n")
                        _summary.append("Additional information for ")
                        _summary.append(master.name)
                        _summary.append("\n")
                        if (_masterStatus[1] == '0') {
                            _summary.append("\tInstance cannot apply incremental upgrade\n")
                        }
                        if (_masterStatus[2] == '0') {
                            _summary.append("\tBeekeeper is not enabled\n")
                        }
                        if (_masterStatus[3] == '1') {
                            _summary.append("\tInstance is using a custom update center")
                        }
                        _summary.append("\n")
                    } 
                } else {
                    _summary.append(master.name)
                    _summary.append(" - Connected master has already been upgraded, no further action needed\n")
                }
                if (debug) {
                    println("Additional Info - " + master.name)
                    for (int i=0;i<_masterStatus.size();i++) {
                        print _statusKey[i] + " = " + _masterStatus[i].toString() + "\n"
                    }
                }
            } else {
                offline++
                _summary.append(master.name)
                _summary.append(" - Connected master performance is not enough to determine the status.\n")
            }
        } else {
            offline++
            _summary.append(master.name)
            _summary.append(" - Connected master is offline, its status can not be determined.\n")
        }
        _summary.append("\n")
    }

    if(offline > 0) {
        if (debug) {
            println 'There are ' + offline +' connected masters offline. Their status cannot be determined.'
        }
        _summary2.append("There are ")
        _summary2.append(offline)
        _summary2.append(" connected masters offline. Their status cannot be determined.\n")
    }
    if(plugins > 0) {
        if (debug) {
            println 'You have one or more connected master instances that need to be upgraded.'
        }
        _summary2.append("You have one or more connected master instances that need to be upgraded.\n")
        all = false
    }

}

// After upgrading masters (in case of OC)...
def _status = executeScript(script_status)
// If plugin requires update
if (_status[0] == '1') {
    println "Analyzing " + type + "... "
    _summary.append(type)
    _summary.append(" - ")
    boolean upgraded = performPluginUpdate(_status, direct)

    if(upgraded) {
        print " Plugin upgraded successfully. "
        _summary.append(" Plugin upgraded successfully")
        if (restart) { 
            print "Restarting the instance..."
            _summary.append(" Restarting the instance...\n")
            executeScript(script_restart) 
        } else {
            print "Manual restart required."
            _summary.append(" Manual restart required.\n")
        }
        println ""
    } else {
        _summary.append(" Plugin cannot be upgraded. Please contact CloudBees support.\n")
        all = false
    } 
} else {
    println "cloudbees-license plugin is updated on your " + type + " instance."
    _summary.append("cloudbees-license plugin is updated on your ")
    _summary.append(type)
    _summary.append(" instance.\n")
}

println ""
if (all) {
    _summary2.append("\nAll instances haven been upgraded sucessfully\n")
} else {
    _summary2.append("\nYou have one or more instances that need to be upgraded.\n")
}

println("------------------------------------------- SUMMARY -----------------------------------------")
println _summary.toString()
print "\n\n"
println _summary2.toString()

// script-upgrade common code
// ------------------------------------------------------------------------------------------------

def parseMasterStatus(String response) {
    def _return = response.minus('[').minus(']')
    String[] masterStatus = _return.tokenize(',')
    masterStatus.eachWithIndex { it,i -> masterStatus[i] = it.trim() }

    return masterStatus
}

def performPluginUpdate(def _status, boolean directDownload = false, master = null) {
    if (_status[0] != '1') {
        return false
    }

    boolean upgraded = false
    if(directDownload) {
        if (executeScript(script_download, master) == 'RESTART_REQUIRED') {
            print "[direct]"
            upgraded = true
        }
    }

    // Incremental upgrade
    if (!upgraded && _status[1] == '1') {
        if (executeScript(script_incremental, master) == 'RESTART_REQUIRED') {
            print "[incremental]"
            upgraded = true
        }
    }

    // Custom update center
    if(!upgraded && _status[3] == '1') {
        if (executeScript(script_custom, master) == 'RESTART_REQUIRED') {
            print "[custom]"
            upgraded = true
        }
    }

    return upgraded
}

def executeScript(String script, master = null) {
    if (master == null) {
        return executeScriptInLocally(script)
    } else {
        return executeScriptRemotely(master, script)
    }
}

// Common code
// ------------------------------------------------------------------------------------------------

enum Product {
  CONNECTED_MASTER('Connected Master'), STANDALONE_MASTER('Standalone Master'), OPERATIONS_CENTER('Operations Center'), CJD('CJD')
  Product(String detail) {
    this.detail = detail
  }
  final String detail

  public String toString() {
    return detail
  }
}

def productType() {
  def _plugin_oc_server = jenkins.model.Jenkins.instance.getPlugin('operations-center-server')
  if(_plugin_oc_server != null && _plugin_oc_server.getWrapper().isActive()) {
    return Product.OPERATIONS_CENTER
  } else {
    def _plugin_oc_client = jenkins.model.Jenkins.instance.getPlugin('operations-center-client')
    if(_plugin_oc_client != null && _plugin_oc_client.getWrapper().isActive()) {
      def _descriptor = com.cloudbees.opscenter.client.plugin.OperationsCenterRootAction.descriptor()
      if (_descriptor != null && _descriptor.isEnabled()) {
        return Product.CONNECTED_MASTER
      } else {
        return Product.STANDALONE_MASTER
      }
    } else {
      def _plugin_oc_context = jenkins.model.Jenkins.instance.getPlugin('operations-center-context')
      def _plugin_folder_plus = jenkins.model.Jenkins.instance.getPlugin('cloudbees-folders-plus')
      if((_plugin_oc_context != null && _plugin_oc_context.getWrapper().isActive()) || 
        (_plugin_folder_plus != null && _plugin_folder_plus.getWrapper().isActive())) {
        return Product.STANDALONE_MASTER
      } else {
        return Product.CJD
      }
    }
  }
}

def executeScriptRemotely(def master, String script, int tries = 20, long waitingFor = 1000) {
  def _plugin_oc_clusterops = jenkins.model.Jenkins.instance.getPlugin('operations-center-clusterops')
  if(_plugin_oc_clusterops != null && _plugin_oc_clusterops.getWrapper().isActive()) {
    def _stream = new ByteArrayOutputStream();
    def _listener = new hudson.model.StreamBuildListener(_stream);
    def _future
    if (com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep.Script.metaClass.hasProperty(com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep.Script, "parameters") != null) {
        //_future = master.channel.callAsync(new com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep.Script(script, _listener, "host-script.groovy", [:]))
        _future = master.channel.callAsync(Class.forName('com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep$Script').newInstance(script, _listener, "host-script.groovy", [:]))
    } else {
        //_future = master.channel.callAsync(new com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep.Script(script, _listener, "host-script.groovy"))
        _future = master.channel.callAsync(Class.forName('com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep$Script').newInstance(script, _listener, "host-script.groovy"))
    }

    for(int i=1; i<=tries; i++) {
        if(_future.isDone()) {
            break;
        } else {
            sleep(waitingFor)
        }
    }
    if (_future.isDone()) {
      def _return = _stream.toString().minus('Result: ').minus('\n')
      _listener.close()
      _stream.close()
      return _return
    }
  }
  return null
}

def executeScriptInLocally(String script) {
  def _groovy = new groovy.lang.GroovyShell(jenkins.model.Jenkins.getActiveInstance().getPluginManager().uberClassLoader)
  return _groovy.run(script, "local.script", new java.util.ArrayList())
}

