/*** BEGIN META {
 "name" : "Store or Promote with dependencies in Custom Update Center",
 "comment" : "Calculate required dependencies for a Plugin in Custom Update Center. Provide also methods to either 1) Download the plugin with its dependencies or 2) Promote the plugin with its dependencies (Don't use both at once)",
 "parameters" : [pluginName, pluginVersion, updateCenterFullName],
 "core": "1.642",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.jenkins.updates.data.DependencyEntry
import com.cloudbees.jenkins.updates.data.PluginEntry
import com.cloudbees.jenkins.updates.versioning.VersionNumber
import com.cloudbees.plugins.updatecenter.PluginData
import com.cloudbees.plugins.updatecenter.UpdateCenter

/*************************************************
 * START Variables:
 * Change the following variable to suit your need
 *************************************************/

// Plugin name (example: 'cloudbees-template')
String pluginName = "${pluginName}"
// Plugin version (example: '4.26')
String pluginVersion = "${pluginVersion}"
// Update center full name (example: 'myFolder/myUc')
String updateCenterFullName = "${updateCenterFullName}"

/*************************************************
 * END Variables
 *************************************************/

/**
 * Fill a map with required dependencies.
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param checkedDeps the dependencies already checked
 * @param requiredDeps the required dependencies (Map being filled)
 * @param indent indent for log output
 */
void fillRequiredDependencies(
        UpdateCenter updateCenter,
        String name,
        String version,
        Collection<DependencyEntry> checkedDeps,
        Map<String, PluginEntry> requiredDeps,
        String indent) {

    PluginData pluginData = updateCenter.getPlugin(name)
    VersionNumber pluginVersionNumber = new VersionNumber(version)

    /* Retrieve the plugin entry */
    PluginEntry pluginEntry = pluginData.getVersions().find { entry -> entry.getKey() == pluginVersionNumber}?.getValue()

    if(pluginEntry == null) {
        println "${indent} Plugin ${pluginData.getName()}:${pluginVersionNumber} not pulled yet. Checking on store and updates."
        pluginEntry = getSuitableVersion(updateCenter, name, version,"${indent} ")
    }

    if(pluginEntry != null) {
        requiredDeps.put(pluginData.getName(), pluginEntry)
        /* Retrieve required dependencies */
        pluginEntry.dependencies.findAll{ !it.getOptional() }?.each { dep ->
            if(!checkedDeps.contains(dep)) {
                println "${indent} [PULL] Checking dependency: \"name\": \"${dep.getName()}\", \"version\": \"${dep.getVersion()}\", \"optional\": \"${dep.getOptional()}\""
                checkedDeps.add(dep)

                VersionNumber currentVersionNumber = requiredDeps.get(dep.getName())?.getVersionNumber()
                if(currentVersionNumber == null || currentVersionNumber.isOlderThan(new VersionNumber(dep.getVersion()))) {
                    fillRequiredDependencies(updateCenter, dep.getName(), dep.getVersion(), checkedDeps, requiredDeps,"${indent} ")
                } else {
                    println "${indent}  Already found a requirement for higher version: ${currentVersionNumber} "
                }
            }
        }
    }
}

/**
 * Return a suitable version/update of a plugin (one equal or more recent). It will pick the most recent one if no exact match found.
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param indent indent for log output
 * @return The corresponding entry
 */
PluginEntry getSuitableVersion(UpdateCenter updateCenter, String name, String version, String indent) {

    PluginData pluginData = updateCenter.getPlugin(name)
    VersionNumber pluginVersionNumber = new VersionNumber(version)

    /* Retrieve the plugin entry */
    PluginEntry pluginEntry = pluginData.getVersions().find { entry -> entry.getKey() == pluginVersionNumber}?.getValue()

    if(pluginEntry != null) {
        //Found exact match
        return pluginEntry
    } else {
        //Check for more recent versions
        List<PluginEntry> moreRecentVersions = pluginData.getVersions().findAll {
            entry -> entry.getKey() >= pluginVersionNumber
        }.collect {
            entry -> entry.getValue()
        }

        if (moreRecentVersions == null || moreRecentVersions.isEmpty()) {
            println "${indent} Plugin ${pluginData.getName()} does not have any suitable version stored"
        } else {
            println "${indent} Requested version not available in store but more recent versions found. Using the latest version ${moreRecentVersions.get(0).getVersion()}."
            return moreRecentVersions.get(0)
        }

        // Need to pull, check for updates
        List<PluginEntry> pluginUpdates  = pluginData.getUpdates().findAll {
            entry -> entry.getKey() >= pluginVersionNumber
        }.collect {
            entry -> entry.getValue()
        }

        if (pluginUpdates == null || pluginUpdates.isEmpty()) {
            println "${indent} [ERROR] Plugin ${pluginData.getName()} does not have any suitable updates available for download"
        } else {
            PluginEntry toDownload = pluginUpdates.find { it.getVersionNumber() == pluginVersionNumber}
            if (toDownload == null) {
                println "${indent} [WARNING] Requested version ${pluginVersionNumber} not available for download but more recent versions found. Using the latest version"
                toDownload = pluginUpdates.get(0)
            }
            println "${indent} [INFO] Requested version ${toDownload.getVersion()} available for download"
            return toDownload
        }
        return null
    }
}

UpdateCenter myUC = jenkins.model.Jenkins.instance.getItemByFullName(updateCenterFullName, UpdateCenter.class)
if(myUC == null) {
    println "Cannot find UC '${updateCenterFullName}'!"
}

println "##########\nCalculate required dependencies:\n##########"
// Construct the dependencies map
Map<String, PluginEntry> requiredDeps = new HashMap<>()
fillRequiredDependencies(myUC, pluginName, pluginVersion, new HashSet<DependencyEntry>(), requiredDeps, "")

/*************************************************************
 * Store With Dependencies: Based on the list calculated above
 *************************************************************/

/**
 * Download plugin if not already stored
 * @param updateCenter the update center item
 * @param pluginEntries the list of plugin entries to download
 * @return
 */
def downloadPlugins(UpdateCenter updateCenter, Collection<PluginEntry> pluginEntries) {
    pluginEntries.each { pluginEntry ->
        PluginData pluginData = updateCenter.getPlugin(pluginEntry.getName())
        if(pluginData.getVersions().find { entry -> entry.getKey() == pluginEntry.getVersionNumber()} != null) {
            println " Plugin ${pluginEntry.getName()}:${pluginEntry.getVersionNumber()} is already pulled."
        } else {
            println " Pulling plugin version ${pluginEntry.getName()}:${pluginEntry.getVersionNumber()}"
            updateCenter.downloadPlugin(pluginEntry.getUrl(), pluginEntry.getName(), pluginEntry.getVersion(), pluginEntry.getSha1())
        }
    }
}

println "##########\nDownload required dependencies:\n##########"
downloadPlugins(myUC, requiredDeps.values())

/***************************************************************
 * Promote With Dependencies: Based on the list calculated above
 ***************************************************************/

/**
 * Promote plugin if not already promoted
 * @param updateCenter the update center item
 * @param pluginEntries the list of plugin entries to promote
 * @return
 */
def promotePlugins(UpdateCenter updateCenter, Collection<PluginEntry> pluginEntries) {
    pluginEntries.each { pluginEntry ->
        PluginData pluginData = updateCenter.getPlugin(pluginEntry.getName())
        if(pluginData.getVersions().find { entry -> entry.getKey() == pluginEntry.getVersionNumber()} != null) {
            if (pluginData.getPromotedVersionNumber() != pluginEntry.getVersionNumber()) {
                println " Promoting plugin version ${pluginEntry.getName()}:${pluginEntry.getVersionNumber()}"
                pluginData.setPromotedVersion(pluginEntry.getVersion())
            } else {
                println " ${pluginEntry.getName()}:${pluginEntry.getVersionNumber()} is already promoted."
            }
        } else {
            println " Plugin ${pluginEntry.getName()}:${pluginEntry.getVersionNumber()} is not pulled yet."
        }
    }
}

println "##########\nPromote required dependencies:\n##########"
promotePlugins(myUC, requiredDeps.values())

return