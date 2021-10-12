//testing
DownloadService.signatureCheck = true;

/**
 * The main script body, returns one of the following possible results
 * NO_CHANGE_NEEDED
 * DISABLED_CERT_VALIDATION
 * REMOVED_OFFLINE_UC
 * ERROR_CONTACT_SUPPORT: [msg]
 */

def _script = '''
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.util.PersistedList;
import jenkins.model.Jenkins;
import com.cloudbees.jenkins.plugins.license.nectar.CloudBeesUpdateSite;
import hudson.util.VersionNumber;
import net.sf.json.JSONObject;
import net.sf.json.JSONException;
import hudson.util.FormValidation;
import java.security.cert.CertificateExpiredException;
import hudson.model.DownloadService;

// parameters
// ----------------------------------------------------------------------------------------------------
_dry_run = true;
_debug = true;

//Constants - do not edit below this line
// ----------------------------------------------------------------------------------------------------
_version = "00000";
_online_uc_url_prefix = "https://jenkins-updates.cloudbees.com/update-center/";
_offline_uc_url = "file:" + Jenkins.getInstance().getRootDir() + "/war/WEB-INF/plugins/update-center.json";
_cert_error_str = "CertificateExpiredException: NotAfter: Tue Oct 19 14:31:36 EDT 2021";

// MAIN CODE BODY
info("Executing remediation check [v" + _version + "]");

info("Checking if certificate validation is already disabled")
if (!isCertificateCheckingEnabled()) {
    info("Certifcate validation was already disabled, no changes needed");
    //TODO: check the offline uc anyway, maybe we can uninstall the script
    return "NO_CHANGE_NEEDED";
}

info("Checking offline update center certificates...");
if (isAirGapped()) {
    debug("airgapped!");
    info("System appears to be airgapped, checking offline updatecenter");
    if (!checkOfflineUC()) {
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
        // remove the script since it is no longer needed for this system
        removeScript();
        return "NO_CHANGE_NEEDED";
    }
} else {
    debug("not airgapped");
    if (!checkOfflineUC()) {
        // fix is needed
        info("Offline update center failed validation, update required");
        info("removing current offline update center")

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
}

// ----------------------------------------------------------------------------------------------------
// methods below
// ----------------------------------------------------------------------------------------------------

/**
  * removes the uc-remediation.groovy script from the filesystem
  */
def removeScript() {
    File f = Jenkins.getInstance().getRootDir().getAbsolutePath() + "/init.groovy.d/uc-remediation.groovy";
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
	debug("DownlaodService.signatureCheck == " + DownloadService.signatureCheck);
    return DownloadService.signatureCheck;
}

def disableCertificateValidation() {
    debug("DownloadService.signatureCheck original value [" + DownloadService.signatureCheck + "]");
    if (!_dry_run) {
        DownloadService.signatureCheck = false;
        debug("DownloadService.signatureCheck new value [" + DownloadService.signatureCheck + "]");
    } else {
        info("dry run, not disabling signature check");
    }
}

// this currently makes the assumption that any error checking the online uc means we are airgapped
// possibly this needs to be more fine-grained?
// returns false if the online uc is able to be validated, true otherwise
def isAirGapped() {
    return(!checkOnlineUC());
}

def checkOfflineUC() {
    return(checkUpdateSite(getDefaultOfflineUC()));
}

def checkOnlineUC() {
    return(checkUpdateSite(getDefaultOnlineUc()));
}

def checkUpdateSite(UpdateSite site) {
    try {
        FormValidation v = site.updateDirectlyNow();
        debug("form validation -> "  + v);
        if (v.kind == FormValidation.Kind.OK) {
            debug(site.getUrl() + " is ok, no further action needed");
            return true;
        } else if (v.kind == FormValidation.ERROR) {
            if (v.toString().contains(_cert_error_str)) {
                debug("cert expired error found validating " + site.getUrl());
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
            return false;
        } else {
            debug("Some other error was found validating " + site.getUrl());
            return false;
        }
    }
}

def getDefaultOfflineUC() {
    PersistedList <UpdateSite> sites = Jenkins.getInstance().getUpdateCenter().getSites();
    for (UpdateSite s: sites) {
        if (s.getUrl().equals(_offline_uc_url)) {
            debug("Found default offline updatecenter " +s.getUrl());
            return s;
        }
    }
    debug("default offline updatecenter was not found");
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
            debug.add("Removed update center " +s.getUrl());
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

_init_groovy_dir = Jenkins.getInstance().getRootDir().getAbsolutePath() + "/init.groovy.d";

def writeScriptToInitGroovyFolder(String script) {
    // create the init.groovy.d folder  if it does not exist
    File folder = new File(_init_groovy_dir);
    if (!folder.exists()) {
        folder.mkdirs();
    }
    File scriptFile = new File("uc-remediation.groovy", folder);
}

String result = evaluate(_script);

if (result.equals("NO_CHANGE_NEEDED")) {
    println("System is up to date, no changes needed");    
} else if ((result.equals("DISABLED_CERT_VALIDATION")) || (result.equals("REMOVED_OFFLINE_UC"))) {
    println("persisting script");
    writeScriptToInitGroovyFolder(_script);
} else {
    // some other error occured
    println("An error occured: " + result);
    println("Please contact support (support@cloudbees.com)");
}
