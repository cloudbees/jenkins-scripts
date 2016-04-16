/*** BEGIN META {
 "name" : "Plugins Dependencies",
 "comment" : "Useful methods to get information about plugins and dependencies.",
 "parameters" : [],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import hudson.PluginWrapper
import jenkins.model.Jenkins

/**
 * Get the list of installed plugins.
 */
plugins.each {
    println "${it.getShortName()} (${it.getVersion()})"
}

println "\nFAILED:"
/**
 * Get failed plugins: getFailedPlugins()
 */
Jenkins.instance.getPluginManager().getFailedPlugins()
        .each {
    println "${it.getShortName()} (${it.getVersion()})}"
}

println "\nPINNED:"
/**
 * Get pinned plugins: isPinned()
 */
Jenkins.instance.getPluginManager().getPlugins()
        .findAll { plugin -> plugin.isPinned() }
        .each {
    println "${it.getShortName()} (${it.getVersion()})}"
}
return;

println "\nBUNDLED:"
/**
 * Get bundled plugins: isBundled()
 */
Jenkins.instance.getPluginManager().getPlugins()
        .findAll { plugin -> plugin.isBundled() }
        .each {
    println "${it.getShortName()} (${it.getVersion()})}"
};
return;

/**
 * Get failed plugins: getDependants()
 */

println "\nFORCED BY PINNING:"
/**
 * Get plugins forced to an older version because of Pinning
 */
Jenkins.instance.getPluginManager().getPlugins()
        .findAll { plugin -> plugin.isPinningForcingOldVersion() }
        .each {
    println "${it.getShortName()} (${it.getVersion()})}"
}
return;


println "\nDISABLED:"
/**
 * Get Disabled plugins.
 */
Jenkins.instance.getPluginManager().getPlugins()
        .findAll { plugin -> !plugin.isEnabled() }
        .each {
    println "${it.getShortName()} (${it.getVersion()})}"
}
return;

println "\nINACTIVE:"
/**
 * Get Inactive plugins.
 */
Jenkins.instance.getPluginManager().getPlugins()
        .findAll { plugin -> !plugin.isActive() }
        .each {
    println "${it.getShortName()} (${it.getVersion()})}"
}
return;

/**
 * Get the list of installed plugins and direct dependencies.
 */
def plugins = Jenkins.instance.getPluginManager().getPlugins()
plugins.each {
    println "${it.getShortName()} (${it.getVersion()}) - ${it.getDependencies()}"
}

/**
 * Get the dependencies of a particular plugin.
 */
def pluginByName = Jenkins.instance.getPluginManager().getPlugin('cloudbees-license');
println "${pluginByName.getShortName()} (${pluginByName.getVersion()}) - ${pluginByName.getDependencies()}"

println "\nDEPENDANTS:"
/**
 * Get the plugins that depend on a particular plugin.
 */
Jenkins.instance.getPluginManager().getPlugins()
        .findAll { plugin ->
    plugin.getDependencies().find {
        dependency -> "cloudbees-license".equals(dependency.shortName)
    }
}.each {
    println "${it.getShortName()} (${it.getVersion()})"
};
return;

/**
 * Get the detailed dependencies of a particular plugin.
 */
println "${pluginByName.getShortName()} (${pluginByName.getVersion()}) - ${pluginByName.getDependencies()}"

def void getDependencies(PluginWrapper plugin) {
    println "{${plugin.getShortName()} (${plugin.getVersion()}) - \n[";
    plugin.getDependencies().each {
        println "${it.shortName} (${it.version})";
        getDependencies(Jenkins.instance.getPluginManager().getPlugin(it.shortName));
    }
    println "]"
}
getDependencies(Jenkins.instance.getPluginManager().getPlugin('cloudbees-license'));

/**
 * Get a complete JSON object of the dependencies of a particular plugin.
 * @param plugin The Plugin
 */
def void getDependenciesJSON(PluginWrapper plugin) {
    print "{\"plugin\":\"${plugin.getShortName()}\", \"version\":\"${plugin.getVersion()}\"";
    def deps = plugin.getDependencies();
    if (!deps.isEmpty()) {
        def i;
        print ", \"dependencies\":["
        for (i = 0; i < deps.size() - 1; i++) {
            getDependenciesJSON(Jenkins.instance.getPluginManager().getPlugin(deps.get(i).shortName));
            print ","
        }
        getDependenciesJSON(Jenkins.instance.getPluginManager().getPlugin(deps.get(i).shortName));
        print "]"
    }
    print "}"
}
getDependenciesJSON(Jenkins.instance.getPluginManager().getPlugin('cloudbees-license'));

/**
 * Get a detailed set of the dependencies recursively.
 * @param plugin the plugin to analyse
 * @return {@link HashSet < PluginWrapper.Dependency >}
 */
def Set<PluginWrapper.Dependency> getDependenciesSet(PluginWrapper plugin) {
    Set<PluginWrapper.Dependency> deps = new HashSet<>();
    plugin.getDependencies().each {
        deps.add(it);
        deps.addAll(getDependenciesSet(Jenkins.instance.getPluginManager().getPlugin(it.shortName)));
    }
    return deps;
};
println(getDependenciesSet(Jenkins.instance.getPluginManager().getPlugin('cloudbees-license')));

/**
 * Get a detailed map of the dependencies recursively.
 * @param plugin the plugin to analyse
 * @return {@link TreeMap < PluginWrapper , PluginWrapper.Dependency >}
 */
def Map<PluginWrapper, PluginWrapper.Dependency> getDependenciesMap(PluginWrapper plugin) {
    Map<PluginWrapper, PluginWrapper.Dependency> deps = new TreeMap<>();
    deps.put(plugin, plugin.getDependencies());
    plugin.getDependencies().each {
        deps.putAll(getDependenciesMap(Jenkins.instance.getPluginManager().getPlugin(it.shortName)));
    }
    return deps;
};
println(getDependenciesMap(Jenkins.instance.getPluginManager().getPlugin('cloudbees-license')));