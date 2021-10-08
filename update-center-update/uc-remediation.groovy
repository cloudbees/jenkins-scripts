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


// VARS & CONSTANTS
// ----------------------------------------------------------------------------------------------------
_version = "00000";
_dry_run = true;
_debug = true;
//_min_version = "2.303.2.2"
_offline_uc_url = "file:" + Jenkins.getInstance().getRootDir() + "/war/WEB-INF/plugins/update-center.json";
_cert_error_str = "CertificateExpiredException: NotAfter: Tue Oct 19 14:31:36 EDT 2021"
_results = [];


// MAIN CODE BODY
println("Executing remediation check [v" + _version + "]");
// there is a small possibilty that the validation will fail for a reason other than the cert being expired.
// should this script abort if that is true?

info("Checking offline update center certificates...");
if (!checkOfflineUC()) {
    // fix is needed
    info("Offline update center failed validation, update required");
    info("removing current offline update center")

    if (!removeUpdateCenter(getDefaultOfflineUC())) {
        info("Error removing current offline update center");
        return false;
    }
     
    info("installing the new offline update center");
    if (!installNewOfflineUpdateCenter()) {
        info("Error installing the new offline update center");
        return false;
    }
     
    info("validating the offline update center after installation");
    if (!checkOfflineUC()) {
        // bad - the new uc should be valid!
        info("offline update center failed validation after update, please contact support");
        return false;
    } else {
        info("Validation complete, no problem detected");
    }   
    // now validate that the offline update center that was just installed
} else {
    info("Offline update center is ok, no update needed");
}

return true;


// methods below
// ----------------------------------------------------------------------------------------------------
def installNewOfflineUpdateCenter() {
    return false;
}

def checkOfflineUC() {
    UpdateSite site = getDefaultOfflineUC();
    try {
        FormValidation v = site.updateDirectlyNow(true);
        println("form validation -> "  + v);
        if (v.kind == FormValidation.Kind.OK) {
            debug("current offline uc is ok, no further action needed");
            return true;
        } else if (v.kind == FormValidation.ERROR) {
            if (v.toString().contains(_cert_error_str)) {
                debug("cert expired error found validating offline uc");
                return false;
            } else {
                debug("Some other error was found validating the offline uc");
            }
        } else {
            debug("Found a warning validating offline uc");
        }
    }
    catch (Exception e) {
        debug("Caught exception " + e.class + " validating cert");
        if (e.toString().contains(_cert_error_str)) {
            debug("cert expired error found validating offline uc");
            return false;
        } else {
            debug("Some other error was found validating the offline uc");
            return false;
        }
    }
}

def checkCbciVersion() {
       VersionNumber cur_version = new VersionNumber(jenkins.model.Jenkins.instance.getVersion());
       VersionNumber min_version = new VersionNumber(_min_version);
       return min_version.isNewerThan(cur_version);
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
        debug("dry run, not removing update center " + s.getUrl());
        return true;
    }
}

def debug(String msg) {
    println("DEBUG: " + msg);
}

def info(String msg) {
        println("INFO: " + msg);
}