//only runs on CJOC

import com.cloudbees.opscenter.server.casc.BundleStorage
import hudson.ExtensionList

String masterName = "master1"
String masterPath = "folder1/master1"
boolean regenerateBundleToken = false

setBundleConfig(masterName, masterPath, regenerateBundleToken)

// set the masterPath for a master, and optionally regenerate the bundle token
private static void setBundleConfig(String masterName, String masterPath, boolean regenerateBundleToken) {
    sleep(100)
    ExtensionList.lookupSingleton(BundleStorage.class).initialize()
    BundleStorage.AccessControl accessControl = ExtensionList.lookupSingleton(BundleStorage.class).getAccessControl()
    accessControl.updateMasterPath(masterName, masterPath)
    if (regenerateBundleToken) {
        accessControl.regenerate(masterName)
    }
}



