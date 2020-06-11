def plugin = 'cloudbees-license'
def backports = ['9.9'  : ['9.9.1',''],
                 '9.10' : ['9.10.1',''],
                 '9.11' : ['9.11.1',''],
                 '9.13' : ['9.13.1','http://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.13.1/cloudbees-license.hpi'],
                 '9.14' : ['9.14.1',''],
                 '9.17' : ['9.17.1',''],
                 '9.18' : ['9.18.0.1',''],
                 '9.18.1' : ['9.18.1.1',''],
                 '9.20' : ['9.20.1',''],
                 '9.24' : ['9.24.1',''],
                 '9.26' : ['9.26.1',''],
                 '9.27' : ['9.27.1',''],
                 '9.28' : ['9.28.1',''],
                 '9.31' : ['9.31.1',''],
                 '9.32' : ['9.32.1',''],
                 '9.33' : ['9.33.1','']]
def base = 'http://jenkins-updates.cloudbees.com/download/plugins/'
def tries = 5
def waitingFor = 5000

java.util.logging.Logger logger = java.util.logging.Logger.getLogger("initd-unbreak.groovy")

//----------------------------------------
try {
    logger.info("Verifying cloudbees-license plugin readiness...")
    def _timeout = tries*waitingFor
    def _restart = false
    def _plugin = jenkins.model.Jenkins.instance.getPlugin(plugin)
    if(_plugin != null && _plugin.getWrapper().isActive()) {
        logger.info("cloudbees-license plugin is installed.")
        def _version = _plugin.getWrapper().getVersionNumber().toString()
        def _backport = backports[_version]

        if (_backport != null && _backport[0] != null) {
            logger.info("Backport " + _backport + " found for cloudbees-license:" + _version + " plugin.")
            logger.info("Jenkins should start in the state that doesn't do any build.")
            jenkins.model.Jenkins.instance.doQuietDown()

            def _url = _backport[1] != '' ? _backport[1] : base + plugin + '/' + _backport[0] + '/' + plugin + '.hpi'

            logger.info("Downloading plugin from " + _url)

            def _httpClient = new com.ning.http.client.AsyncHttpClient(new com.ning.http.client.AsyncHttpClientConfig.Builder()
                                                                .setRequestTimeoutInMs(_timeout)
                                                                .setProxyServer(jenkins.plugins.asynchttpclient.AHCUtils.getProxyServer()).build())
    
            def _future = _httpClient.prepareGet(_url).execute();
    
            for(int i=1; i<=tries; i++) {
                if(_future.isDone()) {
                    break;
                } else {
                    sleep(waitingFor)
                }
            }
            if (_future.isDone()){
                def _response = _future.get()
                if (_response.getStatusCode() == 200) {
                    logger.info("Plugin downloaded.")
                    def rootDir = jenkins.model.Jenkins.instance.getRootDir()
                    def pluginsDir = new java.io.File(rootDir, 'plugins')
                    def pluginFile = new java.io.File(pluginsDir, plugin + '.hpi')

                    def jpi = new java.io.File(pluginsDir, plugin + '.jpi')
                    def bak = new java.io.File(pluginsDir, plugin + '.jpi.bak')

                    org.apache.commons.io.FileUtils.copyInputStreamToFile(_response.getResponseBodyAsStream(), pluginFile);
                    logger.info("Plugin downloaded into " + pluginFile)
                    logger.info("Looking for bak file " + bak)
                    if (bak.isFile()) {
                        logger.info("Trying to delete bak file...")
                        org.apache.commons.io.FileUtils.deleteQuietly(bak)
                        logger.info("Bak file deleted.")
                    }
                    logger.info("Looking for jpi file " + jpi)
                    if (jpi.isFile()) {
                        logger.info("Moving to bak file...")
                        org.apache.commons.io.FileUtils.moveFile(jpi, bak)
                        logger.info("Moved")
                    }
                    logger.info("Renaming hpi to jpi...")
                    org.apache.commons.io.FileUtils.moveFile(pluginFile, jpi)
                    logger.info("Renamed.")
                    _restart = true
                } else {
                    logger.info("The server returns " + _response.getStatusCode() + " status code trying to download the plugin.")
                }
            } else {
                logger.info("The plugin couldn't be downloaded.")
            }
        } else {
            logger.info("There is no backport available for cloudbees-license:"+_version)
        }
    }
    if (_restart) {
        logger.info("Performing master restart...")
        jenkins.model.Jenkins.instance.doSafeRestart(null)
    } else {
        logger.info("No restart required!")
    }
} catch (Exception fatal) { 
    logger.warning("Error! Cause: " + fatal.getMessage())
}