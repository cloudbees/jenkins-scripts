/**
Author:  escoem
Since: November 2018 
Description: Activate an existing license for a specific instance
Parameters:  
    key             License Key
    certificate     License Certificate
Scope: Cloudbees Jenkins Operations Center
Test on: CloudBees Jenkins Operations Center 2.190.3.2-rolling, CloudBees License Manager 9.35
**/

import java.util.logging.Logger
import hudson.license.LicenseManager

Logger logger = Logger.getLogger("008-license.groovy")

logger.info("-----> LICENSE <-----")

LicenseManager lm = LicenseManager.getInstance()

String key = """-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAqPdN72dwL3BV21fDTIRAFR+e7Tx86iT699FeGGMhHFtqs0S6
[...]
d7OLnYcV0mYectBGblNFs/eVJEHM9uK3xjzNkaQCGSmoNsIrHZevDA==
-----END RSA PRIVATE KEY-----
"""

String certificate = """-----BEGIN CERTIFICATE-----
MIIGFTCCBP2gAwIBAgIEASFTGTANBgkqhkiG9w0BAQUFADCBsTELMAkGA1UEBhMC
[...]
HRof/9mcnnDlyC+1iPRomWaIMaVus2JOdQ==
-----END CERTIFICATE-----
"""

lm.setLicense(key, certificate)