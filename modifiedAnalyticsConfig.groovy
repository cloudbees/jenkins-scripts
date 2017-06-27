/**
@Author kuisathaverat
@description This script allow to modified the Elasticsearch configuration from groovy.
**/
import com.cloudbees.opscenter.analytics.reporter.*
import com.cloudbees.opscenter.analytics.AnalyticsConfiguration
import hudson.XmlFile
import hudson.util.XStream2

def j = Jenkins.getInstance()
def config = AnalyticsConfiguration.get()

def providerES = config.getElasticsearchProvider()
def provider = config.getElasticsearchLocator().getProvider();

println config.backupInterval
println config.backupsEnabled
println config.backupNumSnapshots
println config.backupPath
println config.backupName

println providerES.urls
println providerES.credentialsId
println providerES.authScheme

config.backupInterval = 300
config.backupsEnabled = true
config.backupNumSnapshots = 5
config.backupPath = '/var/lib/somewhere'
config.backupName = 'CloudBees-Jenkins-Analytics'

providerES.urls = [new URL('http://URL_TO_ES/changed')]
//read only properties
//providerES.credentialsId = 'b312f53c-ce46-448f-9562-79d0b83e52f6'
//providerES.authScheme = 'BASIC'

config.elasticsearchProvider.ensureStopped()

/**
// hard way writing the XML to disk
try {
    def configXml = new XmlFile(new File(j.getRootDir(), "operations-center-analytics-config.xml"))
    XStream2 xs = new XStream2()
    configXml.write(config)
} catch (IOException e) {
    throw new Exception(e, "elasticsearchProvider")
}
**/

AnalyticsConfiguration.get().save()
AnalyticsConfiguration.get().elasticsearchProvider.ensureStarted()
