/*** BEGIN META {
 "name" : "Store or Promote with dependencies in Custom Update Center",
 "comment" : "Calculate required dependencies for a Plugin in Custom Update Center and performs an action (STORE or PROMOTE). The script ",
 "parameters" : [pluginName, pluginVersion, updateCenterFullName, updateCenterAction, updateCenterActionDryRun, updateCenterStrategy],
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
import com.google.common.collect.Sets

/*************************************************
 * Variables:
 * Change the following variable to suit your need.
 * Default configuration provide detailed output
 * and do not apply any changes
 *************************************************/

/**
 * The plugin name.
 * Example: 'workflow-aggregator'
 */
String pluginName = "${pluginName}"
/**
 * The plugin version. Note: if the EXACT version cannot be found in store or for download, the latest
 * available version will be picked by the script.
 * This can be controlled with ${updateCenterStrategy} but ONLY higher versions are considered suitable.
 * Example: '2.5' or leave empty '' to pick up the latest
 */
String pluginVersion = "${pluginVersion}"
/**
 * Full Name of the custom update center.
 * Example: 'myFolder/myUc'
 */
String updateCenterFullName = "${updateCenterFullName}"
/**
 * Action to undertake after calculating the dependencies.
 *
 * - 'STORE': Download/Store the plugin with its Dependencies
 * - 'PROMOTE': Promote the plugin with its Dependencies
 * - 'NOOP' or empty|null: do nothing
 */
String updateCenterAction = "NOOP"
/**
 * Control whether to apply the updateCenterAction of just execute a test run:
 *
 * - true: Print the action to undertake (store or promote)
 * - false: Execute the action to undertake
 */
boolean updateCenterActionDryRun = true


/*************************************************
 * Further Variables:
 * Change the following variable to tweak the
 * script's behavior. Default configuration is
 * recommended.
 *************************************************/
/**
 * Strategy for picking available version. When checking version available in store, the script picks the exact version
 * if it can find it, otherwise the default behavior is to pick the latest version available. This can be controlled
 * with this variable:
 *
 * - 'LATEST': pick the latest version available in store EVEN IF AN EXACT MATCH IS FOUND
 * - 'DEFAULT' or empty|null: if NO EXACT MATCH found for download, pick the latest version found in store (example: checking for 1.2, found 1.3 and 1.5 -> pick 1.5)
 *
 * Note: With either LATEST / DEFAULT, the output shows all the required dependencies
 */
String updateCenterStoreStrategy = "LATEST"
/**
 * Strategy for picking available version. When checking version available for download, the script picks the exact
 * version if it can find it, otherwise the default behavior is to pick the latest version available. This can be
 * controlled with this variable:
 *
 * - 'LATEST': pick the latest version available for download EVEN IF AN EXACT MATCH IS FOUND
 * - 'DEFAULT' or empty|null: if NO EXACT MATCH found for download, pick the latest version found for download (example: checking for 1.2, found 1.3 and 1.5 -> pick 1.5)
 *
 * Note: With either LATEST / DEFAULT, the output shows all the required dependencies
 */
String updateCenterDownloadStrategy = "LATEST"

/**
 * Fill a map with required dependencies.
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param pickStrategy strategy to apply when exact version is not available
 * @param checkedDeps the dependencies already checked
 * @param optionalDeps the optional dependencies already checked
 * @param requiredDeps the required dependencies (to be stored/promoted)
 * @param indent indent for log output
 */
boolean fillRequiredDependencies(
        UpdateCenter updateCenter,
        String name,
        String version,
        String pickStrategy,
        String downloadStrategy,
        Collection<DependencyEntry> checkedDeps,
        Map<String, VersionNumber> optionalDeps,
        Map<String, PluginEntry> requiredDeps,
        String indent) {

    PluginData pluginData = updateCenter.getPlugin(name)
    if(pluginData == null) {
        println "${indent}[ERROR]Plugin [${name}] does not exists in this update center!"
        return false
    }

    VersionNumber pluginVersionNumber = version == null ? null : new VersionNumber(version)
    println "${indent}[${pluginData.name}:${pluginVersionNumber}]"

    PluginEntry pluginStoredEntry, pluginDownloadableEntry = null

    println "${indent} Checking available versions in store..."
    pluginStoredEntry = getSuitableVersionInStore(pluginData, pluginVersionNumber, pickStrategy,"${indent} ")

    if(pluginStoredEntry == null || downloadStrategy == "LATEST") {
        println "${indent} Checking available versions for download..."
        pluginDownloadableEntry = getSuitableVersionToDownload(pluginData, pluginVersionNumber, downloadStrategy,"${indent} ")
    }

    PluginEntry pluginEntry = pluginDownloadableEntry != null ? pluginDownloadableEntry : pluginStoredEntry

    boolean consistent = true
    if(pluginEntry != null) {
        println "${indent} [CANDIDATE] Suitable version(s) found [${pluginEntry.name}:${pluginEntry.versionNumber}]"
        requiredDeps.put(pluginData.name, pluginEntry)
        /* Retrieve required dependencies */
        pluginEntry.dependencies.findAll{!checkedDeps.contains(it)}.each { dep ->
            checkedDeps.add(dep)
            VersionNumber depVersionNumber = new VersionNumber(dep.version)
            println "${indent} Checking dependency: \"name\": \"${dep.name}\", \"version\": \"${dep.version}\", \"optional\": \"${dep.getOptional()}\""
            /*
             * Only pick an optional dependency if it is already promoted. Because the plugin is likely to be installed
             * in a client master in which case it is not "optional" anymore.
             */
            if(dep.getOptional()) {
                // Record the optional dependency requirement
                VersionNumber currentOptionalVersion = optionalDeps.get(dep.getName())
                if (currentOptionalVersion == null || currentOptionalVersion.isOlderThan(depVersionNumber)) {
                    optionalDeps.put(dep.getName(), depVersionNumber)
                }

                if(updateCenter.getPlugin(dep.name)?.versions == null) {
                    println "${indent}  [DISCARD] optional and not in store"
                    return
                } else if(updateCenter.getPlugin(dep.name).getPromotedVersion() == null) {
                    println "${indent}  [DISCARD] optional and not promoted"
                    return
                }
            }

            /*
             * Only check for a dependency if the requirement is higher than what we already found (may
             * include optional dependencies)
             */
            VersionNumber currentVersionNumber = requiredDeps.get(dep.name)?.versionNumber
            Set<VersionNumber> requiredVersions = Sets.newHashSet(currentVersionNumber, optionalDeps.get(dep.getName()), depVersionNumber)
            requiredVersions.remove(null)
            // Required version is the maximum version of required versions found so far
            VersionNumber requiredVersion = Collections.max(requiredVersions)

            if (currentVersionNumber == null || currentVersionNumber.isOlderThan(requiredVersion)) {
                consistent = fillRequiredDependencies(updateCenter, dep.name, dep.version, pickStrategy,
                        downloadStrategy, checkedDeps, optionalDeps, requiredDeps,"${indent} ")
            } else {
                println "${indent}  [DISCARD] (Already found a requirement for exact or higher version: ${currentVersionNumber})"
            }
        }
    } else {
        println "${indent} [ERROR] No suitable version(s) found in store or for download"
        return consistent = false
    }
    return consistent
}

/**
 * Return a suitable stored version of a plugin (one equal or more recent).
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param downloadStrategy strategy to apply when exact version is not available
 * @param indent indent for log output
 * @return The corresponding entry
 */
PluginEntry getSuitableVersionInStore(
        PluginData pluginData,
        VersionNumber pluginVersionNumber,
        String storeStrategy,
        String indent) {

    PluginEntry toStore = null
    //Check for more recent versions
    List<PluginEntry> pluginVersionsStored = pluginData.versions.findAll {
        entry -> entry.key >= pluginVersionNumber
    }.collect {
        entry -> entry.value
    }

    if (pluginVersionsStored == null || pluginVersionsStored.isEmpty()) {
        println "${indent}Does not have any suitable version in store"
    } else {
        toStore = pluginVersionsStored.find { it.versionNumber == pluginVersionNumber}
        println "${indent}Requested version ${pluginVersionNumber} ${toStore != null ? "" : "not "}available in store"
        if (toStore == null || storeStrategy == "LATEST") {
            print "${indent}Found suitable versions in store:"
            //Take the latest by default
            println " using the latest version ${pluginVersionsStored.get(0).version}"
            toStore = pluginVersionsStored.get(0)
        }
    }
    return toStore
}

/**
 * Return a suitable download version of a plugin (one equal or more recent).
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param downloadStrategy strategy to apply when checking updates
 * @param indent indent for log output
 * @return The corresponding entry
 */
PluginEntry getSuitableVersionToDownload(
        PluginData pluginData,
        VersionNumber pluginVersionNumber,
        String downloadStrategy,
        String indent) {

    PluginEntry toDownload = null
    List<PluginEntry> pluginUpdates  = pluginData.updates.findAll {
        entry -> entry.key >= pluginVersionNumber
    }.collect {
        entry -> entry.value
    }

    if (pluginUpdates == null || pluginUpdates.isEmpty()) {
        println "${indent}Does not have any suitable updates available for download"
    } else {
        toDownload = pluginUpdates.find { it.versionNumber == pluginVersionNumber}
        println "${indent}Requested version ${pluginVersionNumber} ${toDownload != null ? "" : "not"} available for download"
        if (toDownload == null || downloadStrategy == "LATEST") {
            print "${indent}Found suitable versions for download:"
            //Take the latest by default
            println " Using the latest version ${pluginUpdates.get(0).version}"
            toDownload = pluginUpdates.get(0)
        }
    }
    return toDownload
}

/*************************************************************
 * Store With Dependencies: Based on the list calculated above
 *************************************************************/

/**
 * Download plugin if not already stored
 * @param updateCenter the update center item
 * @param pluginEntries the list of plugin entries to download
 * @return
 */
def downloadPlugins(UpdateCenter updateCenter, Collection<PluginEntry> pluginEntries, boolean dryRun) {
    pluginEntries.each { pluginEntry ->
        println "[${pluginEntry.name}:${pluginEntry.versionNumber}]"
        PluginData pluginData = updateCenter.getPlugin(pluginEntry.name)
        if(pluginData.versions.find { entry -> entry.key == pluginEntry.versionNumber} != null) {
            println " Is already in store."
        } else {
            println " Pulling plugin version"
            if(!dryRun) {
                updateCenter.downloadPlugin(pluginEntry.getUrl(), pluginEntry.name, pluginEntry.version, pluginEntry.sha1)
            }
        }
    }
}

/***************************************************************
 * Promote With Dependencies: Based on the list calculated above
 ***************************************************************/

/**
 * Promote plugin if not already promoted
 * @param updateCenter the update center item
 * @param pluginEntries the list of plugin entries to promote
 * @return
 */
def promotePlugins(UpdateCenter updateCenter, Collection<PluginEntry> pluginEntries, boolean dryRun) {
    pluginEntries.each { pluginEntry ->
        println "[${pluginEntry.name}:${pluginEntry.versionNumber}]"
        PluginData pluginData = updateCenter.getPlugin(pluginEntry.name)
        if(pluginData.versions.find { entry -> entry.key == pluginEntry.versionNumber} != null) {
            if (pluginData.promotedVersionNumber != pluginEntry.versionNumber) {
                println " Promoting plugin version"
                if(!dryRun) {
                    pluginData.setPromotedVersion(pluginEntry.version)
                }
            } else {
                println " Is already promoted."
            }
            // Following can be used to promote to the latest
            // pluginData.setPromotedVersion(PluginData.LATEST_VERSION_STRING)
        } else {
            println " Is not in store. Run the STORE action first"
        }
    }
}

/*************************************************
 * Execution
 *************************************************/

UpdateCenter myUC = jenkins.model.Jenkins.instance.getItemByFullName(updateCenterFullName, UpdateCenter.class)
if(myUC == null) {
    println "Cannot find UC '${updateCenterFullName}'!"
    return
}

println "\n----------------------------------\nCalculate required dependencies:\n----------------------------------"
// Construct the dependencies map
Map<String, PluginEntry> requiredDeps = new TreeMap<>()
boolean isConsistent = fillRequiredDependencies(
        myUC,
        pluginName,
        pluginVersion,
        updateCenterStoreStrategy,
        updateCenterDownloadStrategy,
        new HashSet<DependencyEntry>(),
        new HashMap<String, VersionNumber>(),
        requiredDeps,
        "")

println "\n----------------------------------\nResult\n----------------------------------"

if(!requiredDeps.isEmpty()) {
    println "\nRequired Dependencies\n-----------------"
    requiredDeps.values().each {
        println "${it.getName()}:${it.getVersionNumber()}"
    }
}

if(!isConsistent) {
    println "\n!!! Found inconsistencies during the calculation of dependencies. Have a look at '[ERROR]' messages) !!!"
    if(!updateCenterActionDryRun) {
        return
    }
}

if(updateCenterAction == "STORE") {
    println "\n----------------------------------\nDownload required dependencies:\n----------------------------------"
    downloadPlugins(myUC, requiredDeps.values(), updateCenterActionDryRun)
} else if (updateCenterAction == "PROMOTE") {
    println "\n----------------------------------\nPromote required dependencies:\n----------------------------------"
    promotePlugins(myUC, requiredDeps.values(), updateCenterActionDryRun)
}
return