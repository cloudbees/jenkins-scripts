/*
 * Configure HTTP proxy settings available at http://your-jenkins-instance:port/pluginManager/advanced
 * proxyUrl is your proxy address, equivalent to http.proxyHost parameter
 * proxyPort is your proxy port, equivalent to http.proxyPort parameter
 * userName and password are your proxy credentials
 * Set the noProxyHost list to bypass the proxy for these URLs
 */

import jenkins.model.*

def proxyUrl = "proxy.example.com"
def proxyPort = 8080
def userName = "user"
def password = "pass"
def noProxyHost = "localhost"
def jenkins = Jenkins.getInstance()

jenkins.proxy = new hudson.ProxyConfiguration(proxyUrl, proxyPort, userName, password, noProxyHost)
jenkins.save()

return
