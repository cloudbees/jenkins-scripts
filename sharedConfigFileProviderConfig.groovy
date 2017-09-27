/*** BEGIN META {
 "name" : "Push Config File Provider configuration to Client Masters",
 "comment" : "Run from the Script Console. This script read the global configuration of the Config File Provider in CJOC and push it to all Client Masters. If a config file with identical ID already exists in the Client Master, it will be overridden.",
 "parameters" : [],
 "core": "1.642",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.opscenter.server.clusterops.steps.MasterGroovyClusterOpStep
import com.cloudbees.opscenter.server.model.ConnectedMaster
import hudson.model.StreamBuildListener
import jenkins.model.Jenkins
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles

def result = '\n'

// Marshall the GlobalConfigFiles object to XML
def cfpConfigXml = Jenkins.instance.XSTREAM2.toXML(GlobalConfigFiles.get())

// Loop over all online Client Masters
Jenkins.instance.getAllItems(ConnectedMaster.class).eachWithIndex{ it, index ->
    if(it.channel) {
        def stream = new ByteArrayOutputStream();
        def listener = new StreamBuildListener(stream);
        // Execute remote Groovy script in the Client Master
        // Result of the execution must be a String
        it.channel.call(new MasterGroovyClusterOpStep.Script("""
        import org.jenkinsci.plugins.configfiles.GlobalConfigFiles
        import org.jenkinsci.lib.configprovider.ConfigProvider

        GlobalConfigFiles currentStore = GlobalConfigFiles.get()
        GlobalConfigFiles globalConfig = Jenkins.instance.XSTREAM2.fromXML('''${cfpConfigXml}''')
        globalConfig.configs.each {config ->
            if(currentStore.getById(config.id) != null) {
                println "Overriding config file '\${config.id}'"
            }
            println "Saving config file '\${config.id}' of type '\${config.provider.id}'"
            ConfigProvider.all().get(config.provider.class).save(config)
        }
        
        return
        """, listener, "configFileProviderPush.groovy"))
        result = result << "Master ${it.name}:\n${stream.toString()}"

        stream.toString().eachLine { line, count ->
            print line + "\n"
        }
    }
}