import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.util.PersistedList;
import jenkins.model.Jenkins;
import com.cloudbees.jenkins.plugins.license.nectar.CloudBeesUpdateSite;
import net.sf.json.JSONObject;
import net.sf.json.JSONException;
import hudson.util.FormValidation;
import java.security.cert.CertificateExpiredException;
import hudson.model.DownloadService;

/**
 * What does this script do?
 *  - This script is intended to detect and provide a short-term fix for CloudBees CI instances which 
 *    are using an offline update center that was signed by a certificate which is now expired.
 *    https://www.cloudbees.com/r/oct-21-ci-uc-cert-update-kb
 *
 * Who should run this script?
 *  - Air-gapped customers (i.e. without any Internet access)
 *      CloudBees CI or CloudBees Jenkins Platform customers on these versions
 *        - 2.303.2.5 or lower on the rolling release, or 
 *        - 2.277.42.0.1 or lower on the 2.277.x fixed release, or 
 *        - 2.249.33.0.1 or lower on the 2.249.x fixed release
 *      who are deployed in an environment with no external network access 
 *      AND are using the default off-line update center should run this script in
 *      order to disable certificate validation for the update center until they can be upgraded
 *      to these CloudBees CI or CloudBees Jenkins Platform versions
 *        - 2.303.2.6 or newer on the rolling release, or
 *        - 2.277.42.0.3 or newer on the 2.277.x fixed release, or
 *        - 2.249.33.0.2 on the 2.249.x fixed release

 *
 *  - Non-air-gapped customers
 *      CloudBees CI or CloudBees Jenkins Platform customers on these versions
 *        - 2.303.2.5 or lower on the rolling release, or 
 *        - 2.277.42.0.1 or lower on the 2.277.x fixed release, or 
 *        - 2.249.33.0.1 or lower on the 2.249.x fixed release
 *      who are deployed in an environment with external network access 
 *      should run this script in order to disable the off-line update center until they 
 *      can be upgraded to these CloudBees CI or CloudBees Jenkins Platform versions
 *        - 2.303.2.6 or newer on the rolling release, or
 *        - 2.277.42.0.3 or newer on the 2.277.x fixed release, or
 *        - 2.249.33.0.2 on the 2.249.x fixed release
 *
 * How to use this script
 *  - This script can be run using the script console on any individual operations center or controller.  It may also be run via
 *    a cluster-operation (https://docs.cloudbees.com/docs/cloudbees-ci/latest/cloud-admin-guide/cluster-operations)
 *    using the `Execute Groovy Script on Controller` step.
 *
 * Technical Details
 *  - It is safe to run this script multiple times on any CloudBees CI or CloudBees Jenkins Platform instance.  
 *  - For non-air-gapped systems, if a problem is detected with the certificate used to sign the off-line
 *    update center then the off-line update center will be removed.  This will prevent an error message from
 *    being displayed in the plugin manager.
 *  - For air-gapped systems, if a problem is detected with the certificate used to sign the off-line update center
 *    then update center certificate validation will be disabled.  This will allow air-gapped systems to continue
 *    being able to manage plugins using the plugin manager
 *  - For both air-gapped and online systems, a copy of this script will be installed to 
 *    <JENKINS_HOME>/init.d.groovy/ucCertRemediation.groovy.  This is needed because the fixes applied by this
 *    script are not persistent across restarts and need to be re-applied.
 *  - The proper solution for this problem is to upgrade to CloudBees CI or CloudBees Jenkins Platform versions listed above.
 *    We would like to help prepare a customized update plan, and guide you through update testing via an Assisted Update: https://support.cloudbees.com/hc/en-us/articles/115001919212
 *  - If the script detects that the off-line update center is no longer using an invalid certificate
 *    then it will automatically remove itself.
 *    
 *       
 * This script returns one of the following possible results:
 * NO_CHANGE_NEEDED
 * DEFAULT_OFFLINC_UC_NOT_FOUND
 * DISABLED_CERT_VALIDATION
 * REMOVED_OFFLINE_UC
 * UNINSTALLED_SCRIPT
 * ERROR_CONTACT_SUPPORT: [msg]
 */

def _script = '''
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.util.PersistedList;
import jenkins.model.Jenkins;
import com.cloudbees.jenkins.plugins.license.nectar.CloudBeesUpdateSite;
import net.sf.json.JSONObject;
import net.sf.json.JSONException;
import hudson.util.FormValidation;
import java.security.cert.CertificateExpiredException;
import hudson.model.DownloadService;

/**
 * parameters:
 * 
 * _dry_run  If set to true, no changes will actually be made to the instance
 * _debug    If set to true, additional information will be logged to the console
 *
 */

_debug = false;
_dry_run = false;

//Constants - do not edit below this line
// ----------------------------------------------------------------------------------------------------
_version = "00010";

_online_uc_url_prefix = "https://jenkins-updates.cloudbees.com/update-center/";
_retry_time = 30000;   // how long to wait before checking for an update site to be loaded
_cert_error_str = "CertificateExpiredException: NotAfter: Tue Oct 19 14:31:36 EDT 2021";

// MAIN CODE BODY
info("Executing remediation check [v" + _version + "]");
if (System.properties['_CLOUDBEES_UC_CERT_REMEDIATION_INSTALL'] == "TRUE") {
    info("Running bootstrap install, disabling retry interval");
    _retry_time = 0;
    System.properties['_CLOUDBEES_UC_CERT_REMEDIATION_INSTALL'] = '';
}

info("Checking if certificate validation is already disabled");

if (!isCertificateCheckingEnabled()) {
    info("Certificate validation was already disabled, no changes needed");
    //check the offline uc anyway, maybe we can uninstall the script
    if (checkOfflineUC(true)) {
        info("Offline update center is ok, removing script");
        if (removeScript()) {
            debug("script has been uninstalled");
            enableCertificateValidation();
            return "UNINSTALLED_SCRIPT";
        } else {
            info("Problem removing script");
        }
    }
    info("Offline update center is invalid, but signature checking has already been disabled");
    return "NO_CHANGE_NEEDED";
}

info("Checking offline update center certificates...");
if (isAirGapped()) {
    debug("airgapped!");
    info("System appears to be airgapped, checking offline updatecenter");
    if (hasDefaultOfflineUC(_retry_time) && !checkOfflineUC()) {
        info("Offline update center has invalid certificate, disabling certificate validation");
        disableCertificateValidation();
        // sanity check, should pass validation now
        if (!checkOfflineUC()) {
            info("ERROR - offline uc is still invalid after disabling certificate validation");
            return "ERROR_CONTACT_SUPPORT: offline uc is still invalid after disabling certificate validation"; 
        } else {
            info("Sanity check complete, offline updatecenter is valid");
            return "DISABLED_CERT_VALIDATION";
        }
    } else {
        info("Offline update center is ok, no update needed");
        debug("removing the script since it is no longer needed for this system");
        removeScript();
        return "NO_CHANGE_NEEDED";
    }
} else {
    debug("not airgapped");
    if (hasDefaultOfflineUC(_retry_time)) {
        if (!checkOfflineUC()) {
            // fix is needed
            info("Offline update center failed validation, update required");
            info("removing current offline update center");

            if (!removeUpdateCenter(getDefaultOfflineUC())) {
                info("Error removing current offline update center");
                return "ERROR_CONTACT_SUPPORT: There was a problem removing the default offline update center";
            } else {
                return "REMOVED_OFFLINE_UC";
            }
        } else {
            info("Offline update center is ok, no update needed");
            // remove the script since it is no longer needed for this system
            removeScript();
            return "NO_CHANGE_NEEDED";
        }
    } else {
        info("default offline updatecenter was not found, if this is not expected you may need to increase the value for _retry_time");
        return("DEFAULT_OFFLINC_UC_NOT_FOUND");
    }
}

// ----------------------------------------------------------------------------------------------------
// methods below
// ----------------------------------------------------------------------------------------------------

/**
  * removes the ucCertRemediation.groovy script from the filesystem
  */
def removeScript() {
    File f = new File(Jenkins.getInstance().getRootDir().getAbsolutePath() + File.separator + "init.groovy.d" + File.separator + "ucCertRemediation.groovy");

    if (f.exists()) {
        debug("Removing script " + f.getAbsolutePath());
        if (!_dry_run) {
            f.delete();
            info("Script has been uninstalled");
        } else {
            info("dry run, not removing script file");
        }
        return true;
    } else {
        info(f.getAbsolutePath() + " not found, skipping uninstall");
        return false;
    }
}

def isCertificateCheckingEnabled() {
    debug("DownloadService.signatureCheck == " + DownloadService.signatureCheck);
    return DownloadService.signatureCheck;
}

def disableCertificateValidation() {
    info("disabling certificate validation");
    setCertificateValidation(false);
}

def enableCertificateValidation() {
    info("enabling certificate validation");
    setCertificateValidation(true);
}

def setCertificateValidation(boolean check) {
    debug("DownloadService.signatureCheck original value [" + DownloadService.signatureCheck + "]");
    if (!_dry_run) {
        DownloadService.signatureCheck = check;
        debug("DownloadService.signatureCheck new value [" + DownloadService.signatureCheck + "]");
    } else {
        info("dry run, not changing signature check");
    }
}

// this currently makes the assumption that any error checking the online uc means we are airgapped
// possibly this needs to be more fine-grained?
// returns false if the online uc is able to be validated, true otherwise
def isAirGapped() {
    return(!checkOnlineUC(false));
}


def checkOfflineUC(boolean validate) {
    return checkUpdateSite(getDefaultOfflineUC(), validate);
}

def checkOfflineUC() {
    return checkUpdateSite(getDefaultOfflineUC(), DownloadService.signatureCheck);
}

def checkOnlineUC(boolean validate) {
    return checkUpdateSite(getDefaultOnlineUc(), validate);
}

def checkOnlineUC() {
    return checkUpdateSite(getDefaultOnlineUc(), DownloadService.signatureCheck);
}

def checkUpdateSite(UpdateSite site, boolean validate) {
    if(site  == null){
        debug("update site is already null");
        return true;
    }
    // ugly to accomodate the logic in nectar-license plugin
    originalCheckValue = isCertificateCheckingEnabled();
    if (validate && !isCertificateCheckingEnabled()) {
        setCertificateValidation(true);
    }

    try {
        debug("checking update site " + site + " with validate == " + validate);
        FormValidation v = site.updateDirectlyNow(validate);
        debug("form validation -> "  + v);
        if (v.kind == FormValidation.Kind.OK) {
            debug(site.getUrl() + " is ok");
            setCertificateValidation(originalCheckValue);
            return true;
        } else if (v.kind == FormValidation.ERROR) {
            if (v.toString().contains(_cert_error_str)) {
                debug("cert expired error found validating " + site.getUrl());
                setCertificateValidation(originalCheckValue);
                return false;
            } else {
                debug("Some other error was found validating " + site.getUrl());
            }
        } else {
            debug("Found a warning validating " + site.getUrl());
        }
    }
    catch (Exception e) {
        debug("Caught exception " + e.class + " validating cert from " + site.getUrl());
        if (e.toString().contains(_cert_error_str)) {
            debug("cert expired error found validating " + site.getUrl());
            setCertificateValidation(originalCheckValue);
            return false;
        } else {
            debug("Some other error was found validating " + site.getUrl());
            setCertificateValidation(originalCheckValue);
            return false;
        }
    }
}

def hasDefaultOfflineUC() {
    return hasDefaultOfflineUC(0);
}

def hasDefaultOfflineUC(int retryTime) {    
    UpdateSite s = getDefaultOfflineUC(retryTime);
    debug("default offline updatecenter = " + s);
    if (s != null) {
        debug("AA");
        return true;
    } else {
        debug("BB");
        return false;
    }
}

def getDefaultOfflineUC() {
    return getDefaultOfflineUC(0);
}

def getDefaultOfflineUC(int retryTime) {
    PersistedList <UpdateSite> sites = Jenkins.getInstance().getUpdateCenter().getSites();
    for (UpdateSite s: sites) {
        if (s.getId().contains("-offline")) {
            debug("Found default offline updatecenter " +s.getUrl());
            return s;
        }
    }
    debug("default offline updatecenter was not found");
    if (retryTime > 0) {
        debug("default offline updatecenter was not found, sleeping for " + retryTime + " ms and will try again");
        Thread.sleep(retryTime);
        for (UpdateSite s: sites) {
            debug("checking " + s.getUrl());
            if (s.getId().contains("-offline")) {
                debug("Found default offline updatecenter " +s.getUrl() + " on second attempt");
                return s;
            }
        }
    }
    debug("getDefaultOfflineUC returns null");
    return null;
}

def getDefaultOnlineUc() {
    PersistedList <UpdateSite> sites = Jenkins.getInstance().getUpdateCenter().getSites();
    for (UpdateSite s: sites) {
        if (s.getUrl().startsWith(_online_uc_url_prefix)) {
            debug("Found default online updatecenter " +s.getUrl());
            return s;
        }
    }
    debug("default offline updatecenter was not found");
    return null;
}

def removeUpdateCenter(UpdateSite s) {
    if (!_dry_run) {
        if (Jenkins.getInstance().getUpdateCenter().getSites().remove(s)) {
            debug("Removed update center " +s.getUrl());

            return true;
        } else {
            debug("update center was not found, nothing removed");
            return false;
        }
    } else {
        info("dry run, not removing update center " + s.getUrl());
        return true;
    }
}

def debug(String msg) {
    if (_debug) {
        println("DEBUG: " + msg);
    }
}

def info(String msg) {
    println("INFO: " + msg);
}
'''
// end script

/**
  * installer
  */

_init_groovy_dir = Jenkins.getInstance().getRootDir().getAbsolutePath() + File.separator +"init.groovy.d";

def writeScriptToInitGroovyFolder(String script) {
    // create the init.groovy.d folder  if it does not exist
    File folder = new File(_init_groovy_dir);
    if (!folder.exists()) {
        folder.mkdirs();
    }
    File scriptFile = new File("ucCertRemediation.groovy", folder);
    scriptFile.write(script);
}

System.properties['_CLOUDBEES_UC_CERT_REMEDIATION_INSTALL'] = 'TRUE';

String result = evaluate(_script);

if (result.equals("NO_CHANGE_NEEDED")) {
    println("SUCCESS: System is up to date, no changes needed"); 
} else if (result.equals("DEFAULT_OFFLINC_UC_NOT_FOUND")) {
    println("INFO: If you still see issues (though you should not), please increase '_retry_time', set '_debug = true;' and run this script again, then share the output with CloudBees support (https://support.cloudbees.com/)");
    println("SUCCESS: The remediation appears to have already been run successfully in the past, or this instance does not have an offline update center.");
} else if (result.equals("DISABLED_CERT_VALIDATION") || result.equals("REMOVED_OFFLINE_UC")) {
    println("persisting script");
    writeScriptToInitGroovyFolder(_script);
    println("Reloading update center data");
    Jenkins.getInstance().pluginManager.doCheckUpdatesServer();
    println("SUCCESS: The remediation is now complete and successful");
} else if (result.equals("UNINSTALLED_SCRIPT")) {
    println("SUCCESS: No issues detected, script has been uninstalled");
} else {
    // some other error occured
    println("ERROR: An error occured: " + result);
    println("ERROR: Please set '_debug = true;' and run this script again, then share the output with CloudBees support (https://support.cloudbees.com/)");
}
