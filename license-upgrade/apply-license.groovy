/*
Script #3 - Apply new license
Checks the license server for the new license, and if available, installs the license
*/

// script version
def _version = "ce00ec5"

// Set slowConnection = true if the connection performance between OC and masters is not good enough.
def slowConnection = false
// set debug = true for additional debug ouput. The output is supposed to be consumed by a support engineer.
def debug = false
// set skipMasters = true to avoid requests to masters. Please, don't enable it unless you know what you are doing. The license
// is going to be upgraded even if there are uncompatible plugins installed on masters.
def skipMasters = false
// Set forceRestart = true to automatically restart masters with cloudbees-license < 9.17 after OC license is installed. 
forceRestart = false
// Set forceSublicenseRefresh = true for refreshing connected master licenses even if there is no upgrade for OC license.
// You may need to do it if you have cloudbees-license < 9.17
forceSublicenseRefresh = false

// Scripts
// ------------------------------------------------------------------------------------------------

 def _statusKey = []
 _statusKey[0] = "Is cloudbees-license plugin version compatible with new Root Ca?"
 _statusKey[1] = "License requires update?"
 _statusKey[2] = "Is sublicense?"
 _statusKey[3] = "Is the license signed by a known certificate?"
 _statusKey[4] = "Is the Root CA about to expire?"
 _statusKey[5] = "Error message"

def script_status = '''
def _status = ["-","-","-","-","-", "-"]
try {
    def plugin = jenkins.model.Jenkins.instance.getPlugin('cloudbees-license')
    // Is cloudbees-license plugin installed?
    if(plugin != null && plugin.getWrapper().isActive()) {

        def rootCAs
        def anchors = new java.util.HashSet<java.security.cert.TrustAnchor>();


        if (hudson.license.License.metaClass.respondsTo(hudson.license.License, "loadLicenseCaCertificates").isEmpty()) {
            // cloudbees-license plugin < 9.34 and non patched
            _status[0]='0'
            rootCAs = new java.util.ArrayList()
            rootCAs.add(hudson.license.License.loadLicenseCaCertificate())
        } else {
            // cloudbees-license plugin >= 9.34 or patched
            _status[0]='1'
            rootCAs = hudson.license.License.loadLicenseCaCertificates() 
        }

        rootCAs.each { c -> 
            anchors.add(new java.security.cert.TrustAnchor(c, null));
        }

        def license = hudson.license.LicenseManager.getInstanceOrDie().getLicenseKeyData()
        if (!license.isJOCClientLicense()) {
            _status[2] = '0'

            def rootCA = null
            for (java.security.cert.X509Certificate ca : rootCAs) {
            try {
                org.jvnet.hudson.crypto.CertificateUtil.validatePath(Arrays.asList(license.getCertificate(), ca), anchors);
                rootCA = ca
            } catch (Exception e) {}
            }

            if (rootCA != null) {
                _status[3]="1"

                def rootCAExpirationDate = rootCA.getNotAfter()
                def c = java.util.Calendar.getInstance()
                c.set(2020, 5 /*JUNE*/, 23)
                def expirationDate = c.getTime()
                if (rootCAExpirationDate.compareTo(expirationDate) < 0) {
                    _status[4] = '1'
                    _status[1] = '1'
                } else {
                    _status[4] = '0'
                    _status[1] = '0'
                }
            } else {
                _status[3] = '0'
            }
        } else {
            try {
                if (license.getCertificate().info.extensions.map.get('AuthorityKeyIdentifier').names.toString().contains('O=InfraDNA Inc.')) {
                    _status[1] = '1'
                } else {
                    _status[1] = '0'
                }
            } catch (Exception e) {}
            _status[2] = '1'
        }
    } else {
        _status[0]='0'
    }
return _status
} catch (Exception err) { 
    _status[5] = err.getMessage()
    return _status 
}
'''

def script_license_renewal = '''
com.cloudbees.jenkins.plugins.license.Renewal.request()
'''

script_license_purge = '''
hudson.license.LicenseManager.getInstance().setLicense("","")
hudson.license.LicenseManager.getInstance().data = null
'''

script_restart = '''
jenkins.model.Jenkins.instance.doSafeRestart(null)
'''

def script_license_set = '''try {
      def endpoint = com.cloudbees.EndPoints.licenses()
      def tries = 5
      def waitingFor = 1000
  
      def manager = hudson.license.LicenseManager.getInstance()
      def certificate = manager.getCertificate()
      def builder = new com.ning.http.client.RequestBuilder("PUT");
      def request = builder.setUrl(endpoint + "/license/refresh")
                    .addHeader("content-type", "application/pkix-cert")
                    .addHeader("X-Jenkins-groovy", "apply-license.groovy")
                    .setBody(certificate)
                    .setBodyEncoding("utf-8")
                    .build();

      def future = jenkins.plugins.asynchttpclient.AHC.instance().executeRequest(request)
 
      for(int i=1; i<=tries; i++) {
        if(future.isDone()) {
          break;
        } else {
          sleep(waitingFor)
        }
      }
      
      def result
      if (future.isDone) {
        def response = future.get()

        def json = net.sf.json.JSONObject.fromObject(response.getResponseBody());
  
        // CBDC-1461 (remove when is resolved!)
        def details 

        try {
          details = json.get("licenseDetails")
        } catch (Exception e) {}
  
        if (details == null) {
          details = json
        }
  
        def key = manager.getKey();
        def newKey = details.getString("key");
        def newCertificate = details.getString("cert");

        def _key = org.apache.commons.lang.StringUtils.substringBetween(key, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----").trim().replaceAll("\\\\s","")
        def _newKey = org.apache.commons.lang.StringUtils.substringBetween(newKey, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----").trim().replaceAll("\\\\s","")
        def _certificate = org.apache.commons.lang.StringUtils.substringBetween(certificate, "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----").trim().replaceAll("\\\\s","")
        def _newCertificate = org.apache.commons.lang.StringUtils.substringBetween(newCertificate, "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----").trim().replaceAll("\\\\s","")

        if (!org.apache.commons.lang.StringUtils.equals(_key, _newKey) ||!org.apache.commons.lang.StringUtils.equals(_certificate, _newCertificate)) {
          try {
            manager.setLicense(newKey, newCertificate);
            result = "NEW_LICENSE"
          } catch(Exception e) {
            result = e.getMessage() 
          }
        } else {
          result = "NO_LICENSE_UPDATE"
        }
      } else {
        result = "SERVER_ERROR"
      }
return result
} catch (Exception fatal) { return "NO. Cause: " + fatal.getMessage()}'''


// script-license main code
// ------------------------------------------------------------------------------------------------

println "apply-license.groovy running... [v" + _version + "]"
println "Checks the license server for the new license, and if available, installs the license."

def type = productType()

println "Determine the instance type: " + type
def manager = hudson.license.LicenseManager.getInstanceOrDie()
if ((manager != null) && (manager.getParsed().isWildcard())) {
  println type.toString() + " currently has a wildcard license installed.  Contact csm-help@cloudbees.com to obtain a license."
} else {
  if (type == Product.OPERATIONS_CENTER) {
      // update operations center license
      // update online masters license

      def status = executeScript(script_status)
      
      if (debug) {
        for (int i=0;i<status.size();i++) {
          println _statusKey[i] + " [" + status[i] + "]"
        }
      }
      
      if(status[0] == '1') {
        if (!skipMasters) {
          def map = new java.util.HashMap()
          def offline = 0
          def plugins = 0
          int tries = 20
          long waitingFor = 1000
          if (slowConnection) {
            waitingFor = 3000
          }
          jenkins.model.Jenkins.instance.getAllItems(com.cloudbees.opscenter.server.model.ConnectedMaster.class).each { master ->
              if(master.channel != null) {
                  def resultState = executeScriptRemotely(master, script_status, tries, waitingFor)
                  if (resultState != null) {
                    def masterStatus = parseMasterStatus(resultState)
                    if (debug) { println "    " + master.name + " " + masterStatus}
                    map.put(master.name, [master, masterStatus])
                    if (masterStatus.size() != 6) {
                      if (debug) { println "    " + master.name + "  is not returning a valid status."}
                      offline++
                    } else if (masterStatus[0] != '1') {
                      plugins++
                    }
                  } else {
                    if (debug) { println "    " + master.name + " timeout"}
                    offline++
                  }
              } else {
                  offline++
              }
          }

          if (plugins == 0) {
              def result = executeScript(script_license_set)
              if (result == "NEW_LICENSE" || forceSublicenseRefresh) { 
                  println "New license installed on Operations Center. Asking for refresh sublicense on masters."
                  map.keySet().each { name -> 
                      print "   License requested for " + name
                      def master = map.get(name)[0]
                      def masterStatus = map.get(name)[1]

                      def prop = master.getProperties().find{ it.getClass().getName() == "com.cloudbees.opscenter.server.properties.ConnectedMasterLicenseServerProperty"}
                      if (prop != null) {
                        // pre-CJP-6313 (<9.17)
                        if (prop.metaClass.respondsTo(prop, "check", boolean).isEmpty()) {
                          def suc = sublicenseRefresh(master, forceRestart)
                          if (suc && forceRestart) {
                            println ". Done!"
                          } else if (suc && !forceRestart) {
                            println ". The sub-license will be refreshed in 24 hours or less."
                          } else {
                            println ". Failure!"
                          }                        
                        } else {
                          if (prop.check(true)) {
                            println ". Done!"
                          } else {
                            println ". Failure!"
                          }
                        }
                      }
                  }
              } else {
                  println "No new license available.  Contact csm-help@cloudbees.com to obtain a license."
                  if (debug) {
                    println "  Debug: " + result
                  }
              }
          } else {
              println "One or more masters have an incompatible version of cloudbees-license plugin. Please update them before applying the new license:"
              map.findAll { k,v -> v[1][0] != "1"}.keySet().each { name ->
                  println " * ${name}"
              }
          }
        } else {
          def result = executeScript(script_license_set)
          if (result == "NEW_LICENSE") {
              println "New license installed on Operations Center."
          } else {
              println "No new license available.  Contact csm-help@cloudbees.com to obtain a license."
          }
        }
      } else {
          println "cloudbees-license plugin installed on operations center is not compatible with new license. Please update it."
      }
  } else {
      def status = executeScript(script_status)
      if (debug) {
        for (int i=0;i<status.size();i++) {
          println _statusKey[i] + " [" + status[i] + "]"
        }
      }
      if(status[0] == '1' && status[1] == '1') {
          if (type == Product.CONNECTED_MASTER) {
              println "Requesting license renewal in connected master."
              // this is not deterministic at all
              com.cloudbees.jenkins.plugins.license.Renewal.request()
          } else {
              def result = executeScript(script_license_set)
              if (result == "NEW_LICENSE") {
                  println "New license installed on Standalone Master."
              } else {
                  println "No new license available.  Contact csm-help@cloudbees.com to obtain a license."
              }
          }
      } else if (status[0] == "1" && status[1] == "0") {
        println "License for this Standalone Master is already updated.  No further action required."
      } else {
          println "cloudbees-license plugin installed on " + type + " is not compatible with new license. Please update it."
      }
  }
}



// script-license common code
// ------------------------------------------------------------------------------------------------

def sublicenseRefresh(master, restart) {
    try {
        if (restart) {
          executeScriptRemotely(master, script_license_purge)
          executeScriptRemotely(master, script_restart)
        }
        return true
    } catch(Exception err) {
        return false
    }
}


def parseMasterStatus(String response) {
    def _return = response.minus('[').minus(']')
    String[] masterStatus = _return.tokenize(',')
    masterStatus.eachWithIndex { it,i -> masterStatus[i] = it.trim() }

    return masterStatus
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
