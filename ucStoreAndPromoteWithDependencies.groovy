/*** BEGIN META {
 "name" : "Store or Promote with dependencies in Custom Update Center",
 "comment" : "Calculate required dependencies for a Plugin in Custom Update Center. Provide also methods to either 1) Download the plugin with its dependencies or 2) Promote the plugin with its dependencies (Don't use both at once)",
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

/**
 * Fill a map with required dependencies.
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param pickStrategy strategy to apply when exact version is not available
 * @param checkedDeps the dependencies already checked
 * @param requiredDeps the required dependencies (Map being filled)
 * @param indent indent for log output
 */
boolean fillRequiredDependencies(
        UpdateCenter updateCenter,
        String name,
        String version,
        String pickStrategy,
        Collection<DependencyEntry> checkedDeps,
        Map<String, PluginEntry> requiredDeps,
        String indent) {

    boolean consistent = true
    PluginData pluginData = updateCenter.getPlugin(name)
    if(pluginData == null) {
        println "${indent}[ERROR]Plugin [${name}] does not exists in this update center!"
        return false
    }
    VersionNumber pluginVersionNumber = new VersionNumber(version)
    println "${indent}[${pluginData.name}:${pluginVersionNumber}]"

    /* Retrieve the plugin entry */
    println "${indent} Checking available versions in store..."
    PluginEntry pluginEntry = getSuitableVersionInStore(updateCenter, name, version, pickStrategy,"${indent} ")

    if(pluginEntry == null || pickStrategy == "LATEST_STRICT") {
        println "${indent} Checking available versions for download..."
        pluginEntry = getSuitableVersionToDownload(updateCenter, name, version, pickStrategy,"${indent} ")
    }

    if(pluginEntry != null) {
        println "${indent} [PICK] Suitable version(s) found [${pluginEntry.name}:${pluginEntry.versionNumber}]"
        /* Retrieve required dependencies */
        pluginEntry.dependencies.findAll{!checkedDeps.contains(it)}.each { dep ->
            checkedDeps.add(dep)
            println "${indent} Checking dependency: \"name\": \"${dep.name}\", \"version\": \"${dep.version}\", \"optional\": \"${dep.getOptional()}\""
            /*
             * Only pick an optional dependency if it is already promoted. Because the plugin is likely to be installed
             * in a client master in which case it is not "optional" anymore.
             */
            if(dep.getOptional()) {
                println updateCenter.getPlugin(dep.name)?.versions
                if(updateCenter.getPlugin(dep.name)?.versions == null) {
                    println "${indent}  [DISCARD] optional and not in store"
                    return
                } else if(updateCenter.getPlugin(dep.name).getPromotedVersion() == null) {
                    println "${indent}  [DISCARD] optional and not promoted"
                    return
                }
            }
            // Only check for a dependency if the requirement is higher than what we already found
            VersionNumber currentVersionNumber = requiredDeps.get(dep.name)?.versionNumber
            if (currentVersionNumber == null || currentVersionNumber.isOlderThan(new VersionNumber(dep.version))) {
                consistent = fillRequiredDependencies(updateCenter, dep.name, dep.version, pickStrategy, checkedDeps,
                        requiredDeps, "${indent} ")
            } else {
                println "${indent}  [DISCARD] (Already found a requirement for higher version: ${currentVersionNumber})"
            }
        }
        requiredDeps.put(pluginData.name, pluginEntry)
    } else {
        println "${indent} [ERROR] No suitable version(s) found in store / or for download"
        consistent = false
    }
    return consistent
}

/**
 * Return a suitable stored version of a plugin (one equal or more recent).
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param pickStrategy strategy to apply when exact version is not available
 * @param indent indent for log output
 * @return The corresponding entry
 */
PluginEntry getSuitableVersionInStore(
        UpdateCenter updateCenter,
        String name,
        String version,
        String pickStrategy,
        String indent) {

    PluginData pluginData = updateCenter.getPlugin(name)
    VersionNumber pluginVersionNumber = new VersionNumber(version)

    /* Retrieve the plugin entry */
    PluginEntry pluginEntry = pluginData.versions.find { entry -> entry.key == pluginVersionNumber}?.value

    if (pluginEntry == null || pickStrategy == "LATEST_STRICT") {
        //Check for more recent versions
        List<PluginEntry> moreRecentVersions = pluginData.versions.findAll {
            entry -> entry.key >= pluginVersionNumber
        }.collect {
            entry -> entry.value
        }

        if (moreRecentVersions == null || moreRecentVersions.isEmpty()) {
            println "${indent}Does not have any suitable version in store"
        } else {
            print "${indent}Requested version ${pluginVersionNumber} not available in store. Found more recent versions though."
            if (pickStrategy == "CLOSEST") {
                println " Using the closest version ${moreRecentVersions.get(moreRecentVersions.size() - 1).version}"
                pluginEntry = moreRecentVersions.get(moreRecentVersions.size() - 1)
            } else {
                //Take the latest by default
                println " Using the latest version ${moreRecentVersions.get(0).version}"
                pluginEntry = moreRecentVersions.get(0)
            }
        }
    } else {
        println "${indent}Requested version ${pluginEntry.version} available in store"
    }
    return pluginEntry
}

/**
 * Return a suitable download version of a plugin (one equal or more recent).
 * @param updateCenter The update center item
 * @param name the name of the plugin
 * @param version the version of the plugin
 * @param pickStrategy strategy to apply when exact version is not available
 * @param indent indent for log output
 * @return The corresponding entry
 */
PluginEntry getSuitableVersionToDownload(
        UpdateCenter updateCenter,
        String name,
        String version,
        String pickStrategy,
        String indent) {

    PluginData pluginData = updateCenter.getPlugin(name)
    VersionNumber pluginVersionNumber = new VersionNumber(version)

    List<PluginEntry> pluginUpdates  = pluginData.updates.findAll {
        entry -> entry.key >= pluginVersionNumber
    }.collect {
        entry -> entry.value
    }

    if (pluginUpdates == null || pluginUpdates.isEmpty()) {
        println "${indent}[ERROR] Plugin ${pluginData.name} does not have any suitable updates available for download"
    } else {
        PluginEntry toDownload = pluginUpdates.find { it.versionNumber == pluginVersionNumber}
        if (toDownload == null) {
            print "${indent}Requested version ${pluginVersionNumber} not available for download. Found more recent versions though."
            if(pickStrategy == "CLOSEST") {
                println " Using the closest version ${pluginUpdates.get(pluginUpdates.size()-1).version}"
                toDownload = pluginUpdates.get(pluginUpdates.size()-1)
            } else {
                //Take the latest by default
                println " Using the latest version ${pluginUpdates.get(0).version}"
                toDownload = pluginUpdates.get(0)
            }
        } else {
            println "${indent}Requested version ${toDownload.version} available for download"
        }
        return toDownload
    }
    return null
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
        } else {
            println " Is not in store."
        }
    }
}

/*************************************************
 * Variables:
 * Change the following variable to suit your need.
 * Default configuration provide detailed output
 * and do not apply any changes
 *************************************************/

/**
 * The plugin name.
 * Example: 'cloudbees-template'
 */
String pluginName = "${pluginName}"
/**
 * The plugin version. Note that if the EXACT version cannot be found in store or for download, the latest
 * available version will be picked by the script.
 * This can be controlled with ${updateCenterStrategy} but ONLY higher versions are considered suitable.
 * Example: '4.28'
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
String updateCenterAction = "STORE"
/**
 * Control whether to apply the updateCenterAction of just execute a test run:
 */
boolean updateCenterActionDryRun = true
/**
 * Strategy for picking available version when an exact match is not found. When checking version available in store
 * or for download, the script picks the exact version if it can find it, otherwise the default behavior is to pick
 * the latest version available. This can be controlled with this variable:
 *
 * - 'LATEST_STRICT': pick the latest version available for download EVEN IF AN EXACT MATCH IS FOUND
 * - 'LATEST' or empty|null: pick the latest version found (example: checking for 1.2, found 1.3 and 1.5 -> pick 1.5)
 * - 'CLOSEST': pick the closest higher version found (example: checking for 1.2, found 1.3 and 1.5 -> pick 1.3)
 */
String updateCenterStrategy = "LATEST_STRICT"

/*************************************************
 * Execution
 *************************************************/

UpdateCenter myUC = jenkins.model.Jenkins.instance.getItemByFullName(updateCenterFullName, UpdateCenter.class)
if(myUC == null) {
    println "Cannot find UC '${updateCenterFullName}'!"
}

println "\nCalculate required dependencies:\n----------------------------------"
// Construct the dependencies map
Map<String, PluginEntry> requiredDeps = new HashMap<>()
boolean isConsistent = fillRequiredDependencies(
        myUC,
        pluginName,
        pluginVersion,
        updateCenterStrategy,
        new HashSet<DependencyEntry>(),
        requiredDeps, "")

println "\nResult\n----------------------------------"
requiredDeps.values().each {
    println "${it.getName()}:${it.getVersionNumber()}"
}

if(!isConsistent) {
    println "\n[ERROR]: Found inconsistencies during the calculation of dependencies. Have a look at '[ERROR]' messages."
}

if(updateCenterAction == "STORE") {
    println "\nDownload required dependencies:\n----------------------------------"
    downloadPlugins(myUC, requiredDeps.values(), updateCenterActionDryRun)
} else if (updateCenterAction == "PROMOTE") {
    println "\nPromote required dependencies:\n----------------------------------"
    promotePlugins(myUC, requiredDeps.values(), updateCenterActionDryRun)
}
return