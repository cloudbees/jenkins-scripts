/**
 ** Create an Update Center that can be used as a source for Controllers and other update centers.
 ** Requires the CloudBees Update Center Plugin
 */
import com.cloudbees.plugins.updatecenter.*

/*
 * Set your Update Center name
 */
def updateCenterName = 'MY-UPDATE-CENTER'

Jenkins instance = jenkins.model.Jenkins.getInstance()
TopLevelItem updateCenter = instance.createProject(com.cloudbees.plugins.updatecenter.UpdateCenter.class, updateCenterName)

/*
 * Plugin versioning
 * - strategy is one of those:
 *     - NewPluginVersionStrategy.NONE (Require explicit configuration before publishing)
 *     - NewPluginVersionStrategy.LATEST (Implicitely publish the latest version unless configured otherwise)
 */
updateCenter.setNewPluginVersionStrategy(strategy)

/*
 * Certification
 * Self-certified signature is set by default, use below method if you want to go Unsigned
 * Use updateCenter.setCertificationProvider() to match the "Unsigned (incompatible with Jenkins 1.433+)" checkbox (not recommended)
 */

/*
 * Upstream sources
 */
List<com.cloudbees.plugins.updatecenter.sources.UpdateSource> sourcesList = new ArrayList<>()
// If you want to add the CloudBees Update Center
// - maxAge is the Maximum cache age
// - boolean verifySignature is the Verify signature flag
// - "version" is the Jenkins Version
sourcesList.add(new com.cloudbees.plugins.updatecenter.sources.JenkinsEnterpriseUpdateSource(maxAge, verifySignature, "version"))
// If you want to add the Jenkins Open Source Update Center
// - maxAge is the Maximum cache age
// - boolean verifySignature is the Verify signature flag
// - "url" is the URL depending on which repository you want to scan
//         URLS are : Latest Release - http://mirrors.jenkins-ci.org/updates/update-center.json
//                    Long-Term Support Release - http://mirrors.jenkins-ci.org/updates/stable/update-center.json
//                    Latest Release with Experimental Plugins - http://mirrors.jenkins-ci.org/updates/experimental/update-center.json
sourcesList.add(new com.cloudbees.plugins.updatecenter.sources.JenkinsOSSUpdateSource(maxAge, true, "url"))
// If you want to add the Jenkins Third Party Source Update Center
// - maxAge is the Maximum cache age
// - boolean verifySignature is the Verify signature flag
// - "url" is the URL of your repository
// - "rootCertificate" is the Third Party Update Center CA certificate
sourcesList.add(new com.cloudbees.plugins.updatecenter.sources.JenkinsThirdPartyUpdateSource(maxAge, true, "url", "rootCertificate"))
// If you want to add an existing Update Center hosted on your Jenkins instance
// - "name" is the name of your existing Update Center
sourcesList.add(new com.cloudbees.plugins.updatecenter.sources.LocalUpdateSource("name"))
updateCenter.setSources(sourcesList)

/*
 * Maintenance tasks
 */
List<com.cloudbees.plugins.updatecenter.UpdateCenterTrigger> tasksList = new ArrayList()
// If you want to add a "Pull everything" task
// - "cron" is the cron expression defining the frequency of the trigger (example "")
tasksList.add(new com.cloudbees.plugins.updatecenter.PullEverythingTrigger("cron"))
// If you want to add a "Pull new versions" task
// - "cron" is the cron expression defining the frequency of the trigger (example "")
tasksList.add(new com.cloudbees.plugins.updatecenter.PullUpdatesTrigger("cron"))
updateCenter.setMaintenanceTriggers(tasksList)
