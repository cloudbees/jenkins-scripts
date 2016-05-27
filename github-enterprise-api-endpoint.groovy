/*
Author: Kurt Madel
This script configures a GitHub Enterprise API Endpoint 
It may be used as an init.groovy.d to preconfigure your Jenkins master at startup
*/
import jenkins.model.*;
import org.jenkinsci.plugins.github_branch_source.*;
import java.util.*;

import java.util.logging.Logger

Logger logger = Logger.getLogger("github-enterprise-api-endpoint")

logger.info("about to add GHE API endpoint")
GitHubConfiguration gitHubConfig = GlobalConfiguration.all().get(GitHubConfiguration.class)

Endpoint gheApiEndpoint = new Endpoint("https://{replace with your GHE host}/api/v3/","GHE API Endpoint Descriptive Name")
List<Endpoint> endpointList = new ArrayList<Endpoint>()
endpointList.add(gheApiEndpoint)
gitHubConfig.setEndpoints(endpointList)
logger.info("added GHE API endpoint")