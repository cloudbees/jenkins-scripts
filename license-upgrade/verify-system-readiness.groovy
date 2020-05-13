 /*
Script #1 - Tell you the status of your instances
Provides clear output that informs the user if their Jenkins(OC, masters, etc) are ready 
to consume the new licenses.  A user may run this script multiple times over the course of the 
upgrade process (ie to see if the system needs to be updated, to verify its ready to be updated, 
and to verify that the update has been completed successfully).
*/

// script version
def _version = "ce00ec5"

// Set slowConnection = true if the connection performance between OC and masters is not good enough.
def slowConnection = false
// Set the value of debug = "true" for additional output. The output is supposed to be consumed by a support engineer.
def debug = false
// set skipMasters = true to avoid requests to masters.
def skipMasters = false
// set onlyStatus = true to return the status array. If onlyStatus is enabled, skipMasters is automatically set to true as well.
def onlyStatus = false

// Scripts
// ------------------------------------------------------------------------------------------------
def script = '''
def _status = ["-","-","-","-","-", "-","-","-","-","-",  "-","-","-","-","-",  "-", "-"]
try {
    _status[15] = jenkins.model.Jenkins.instance.getVersion()
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
      // Is BeeKeeper enabled? 
      if (cap == "SUCCESS") {
        _status[1] = "1"  
      } else if ("WARNING") {
        _status[1] = "2"
      } else {
        _status[1] = "0"    
      }
    } catch (Exception be) {
      _status[1] = "0"
    }

    def customUpdateSite = jenkins.model.Jenkins.instance.getUpdateCenter().getSites().find{ it.class.getName() == "com.cloudbees.opscenter.updatecenters.ContextUpdateSite"} != null
    // Is Custom Update Center configured?
    if (customUpdateSite) {
        _status[2]="1"
    } else {
        _status[2]="0"
    }

    // Is your instance compatible with Incremental Upgrades?
    if (_status[1] != "0" && !customUpdateSite) {
        _status[3] ='1'
    } else {
        _status[3] ='0'
    }

    def plugin = jenkins.model.Jenkins.instance.getPlugin('cloudbees-license')
    // Is cloudbees-license plugin installed?
    if(plugin != null && plugin.getWrapper().isActive()) {
        _status[0] = '1'

        // Plugin version
        def version = plugin.getWrapper().getVersionNumber()
        _status[7] = version

        def rootCAs
        def anchors = new java.util.HashSet<java.security.cert.TrustAnchor>();

        if (hudson.license.License.metaClass.respondsTo(hudson.license.License, "loadLicenseCaCertificates").isEmpty()) {
            // cloudbees-license plugin < 9.34 and non patched
            _status[12]='1'
            _status[4]="0"
            rootCAs = new java.util.ArrayList()
            rootCAs.add(hudson.license.License.loadLicenseCaCertificate())
        } else {
            // cloudbees-license plugin >= 9.34 or patched
            _status[12]='0'
            _status[4]="1"
            rootCAs = hudson.license.License.loadLicenseCaCertificates() 
        }

        rootCAs.each { c -> 
            anchors.add(new java.security.cert.TrustAnchor(c, null));
        }

        def license = hudson.license.LicenseManager.getInstanceOrDie().getLicenseKeyData()
        if (!license.isJOCClientLicense()) {
            _status[5] = '1'

            def rootCA = null
            for (java.security.cert.X509Certificate ca : rootCAs) {
            try {
                org.jvnet.hudson.crypto.CertificateUtil.validatePath(Arrays.asList(license.getCertificate(), ca), anchors);
                rootCA = ca
            } catch (Exception e) {}
            }

            if (rootCA != null) {
                _status[6]="1"

                def format = new java.text.SimpleDateFormat("yyyy-MM-dd");
                def rootCAExpirationDate = rootCA.getNotAfter()
                def licenseExpirationDate = license.getCertificate().getNotAfter()

                _status[8] = format.format(licenseExpirationDate)
                _status[9] = format.format(rootCAExpirationDate)

                if (licenseExpirationDate < rootCAExpirationDate) {
                    _status[10]="1"
                } else {
                    _status[10]="0"
                }

                def c = java.util.Calendar.getInstance()
                c.set(2020, 5 /*JUNE*/, 23)
                def expirationDate = c.getTime()
                if (rootCAExpirationDate.compareTo(expirationDate) < 0) {
                    _status[11] = '1'
                    _status[13] = '1'
                } else {
                    _status[11] = '0'
                    _status[13] = '0'
                }
            } else {
                _status[6] = '0'
            }
        } else {
            try {
                if (license.getCertificate().info.extensions.map.get('AuthorityKeyIdentifier').names.toString().contains('O=InfraDNA Inc.')) {
                    _status[13] = '1'
                } else {
                    _status[13] = '0'
                }
            } catch (Exception e) {}
            _status[5] = '0'
        }
    } else {
        _status[0] = '0'
    }
    return _status
} catch (Exception err) { 
    _status[14] = err.getMessage()
    return _status 
}
'''

// script-status main code
// ------------------------------------------------------------------------------------------------

if (onlyStatus) {
  skipMasters = true
}

if (!onlyStatus) { println "verify-system-readiness.groovy running... [v" + _version + "]" }
if (!onlyStatus) { println "Determining the instance type..." + productType().toString() }

def _statusKey = []
_statusKey[0] = "Is cloudbees-license plugin installed?"
_statusKey[1] = "Is BeeKeeper enabled?"
_statusKey[2] = "Is Custom Update Center configured?"
_statusKey[3] = "Is your instance compatible with Incremental Upgrades?"
_statusKey[4] = "Is cloudbees-license plugin version compatible with new Certificate?"
_statusKey[5] = "Is the license managed by this instance?"
_statusKey[6] = "Is the license signed by a known certificate?"
_statusKey[7] = "Plugin version"
_statusKey[8] = "License expiration date"
_statusKey[9] = "Root CA expiration date"
_statusKey[10] = "Is the expiration date of license before than the Root CA?"
_statusKey[11] = "Is the Root CA about to expire?"
_statusKey[12] = "Plugin requires update?"
_statusKey[13] = "License requires update?"
_statusKey[14] = "Error"
_statusKey[15] = "Product Version"
_statusKey[16] = "Using wildcard license?"

def _summary = new StringBuilder()

def _status = executeScriptInLocally(script)

def manager = hudson.license.LicenseManager.getInstanceOrDie()
if (manager == null) {
  _status[16] = "?"
} else if (manager.getParsed().isWildcard()) {
  _status[16] = "1"
  _summary.append("Your instance is using a wildcard license. Contact csm-help@cloudbees.com to obtain a license.\n\n")
} else {
  _status[16] = "0"
}

_summary.append(productType())
_summary.append(" v")
_summary.append(_status[15].toString())
_summary.append(" - ")
_summary.append(printStatus(_status, false))
_summary.append("\n")

if (!onlyStatus && debug) {
  println productType().toString() + " - " + printStatus (_status, debug)
  for (i=0;i<_status.size(); i++) {
    println "\t" + _statusKey[i].toString() + " ["  + _status[i].toString() + "]"
  }
}

if (productType() == Product.OPERATIONS_CENTER) {
  int plugins = 0
  int licenses = 0
  int offline = 0
  
  if (!onlyStatus) { println("Asking for the status of the connected masters...") }

  int tries = 20
  long waitingFor = 1000
  if (slowConnection) {
    waitingFor = 3000
  }

  if (!skipMasters) {
    jenkins.model.Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.ConnectedMaster.class).each { master ->
      println "Checking status of " + master.name
      if(master.channel != null) {
        def response = executeScriptRemotely(master, script, tries, waitingFor)
        if (response == null) {
          offline++
          _summary.append(master.name)
          _summary.append(" connection performance is not good enough to determine its status.\n")
          println master.name + " connection performance is not good enough and the status cannot be determined."
        } else {
          def _return = response.minus('[').minus(']')
          String[] masterStatus = _return.tokenize(',')
          masterStatus.eachWithIndex { it,i -> masterStatus[i] = it.trim() }

          if(masterStatus.size() != 17) {
            // performance issue?
            offline++
            if (debug) {
              println "[" + master.name + "] is not returning a valid status: " + _return 
            }
          } else {

            if(masterStatus[12].trim() == '1') {
              plugins++
            } 
            if(masterStatus[13].trim() == '1') {
              licenses++
            }
            def summary = printStatus (masterStatus, debug)
            _summary.append(master.name)
            _summary.append(" v")
            _summary.append(masterStatus[15])
            _summary.append(" - ")
            _summary.append(printStatus(masterStatus, false))      
            _summary.append("\n")

            if (debug) {
              println "[" + master.name + "]" + summary
              for (i=0;i<masterStatus.size(); i++) {
                println "\t" + _statusKey[i].toString() + " ["  + masterStatus[i].toString() + "]"
              }
            }
          }
        }
      } else {
        offline++
        _summary.append(master.name)
        _summary.append(" is not online and its status cannot be determined.\n")
        println master.name + " is not online and the status cannot be determined."
      }
    }
  }
  
  if (!onlyStatus) { 
    println "verify-system-readiness.groovy complete"
    println "----------------------------------------------------------------------------------------------------------------"
    println "                                               SUMMARY"
    println "----------------------------------------------------------------------------------------------------------------"

    println _summary.toString()

    if (offline > 0) {
      println 'There are ' + offline + ' connected masters offline. Their status cannot be determined.'
    } 
    if (plugins > 0) {
      println 'You have one or more instances that need to be upgraded.'
    } else if (licenses > 0) {
      println 'Your online connected masters are ready to install the new license.'
    } else {
      println 'All your online connected masters are up to date and running the new license'
    }
  }
} else {
  if (!onlyStatus) { println _summary.toString() }
}

if (onlyStatus) { println _status}

// script-status common code
// ------------------------------------------------------------------------------------------------

def printStatus(def status, boolean debug = false) {
  def summary = new StringBuilder()
  summary.append('cloudbees-license plugin ')

  if(status[0].trim() == '1') {
    summary.append(status[7]).append(' is installed. ')

    if (status[12].trim() == '1') {
      summary.append('The plugin must be updated in this instance before installing the new license. ')
    } else if (status[12].trim() == '0' && status[13].trim() == '1') {
      summary.append('This instance is ready to install the new license.')
    } else if (status[12].trim() == '0' && status[13].trim() == '0') {
      summary.append('This instance is up to date and running the new license.')
    } else {
      summary.append('This instance is in an inconsistent state.')
    }
  } else {
    summary.append('is not installed. The plugin must be installed in this instance before updating the license.')
  }

  return summary.toString()
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
