/**
Author: carlosrodlop
Since: August 2018
Description: Make a report of License Entitlement
Scope: Cloudbees Jenkins Operations Center
Test on: CloudBees Jenkins Operations Center 2.121.2.1-rolling, CloudBees License Manager 9.26
**/

import hudson.license.*
import java.util.Date;
import java.text.SimpleDateFormat;
import org.apache.commons.lang.StringUtils;


def key = LicenseManager.instance.key
def certificate = LicenseManager.instance.certificate

License lic = new License(key, certificate)

StringBuilder message = new StringBuilder("Licensed to ").append(lic.getCustomerName()).append("\nValid until ")
    .append(SimpleDateFormat.getDateInstance().format(lic.getExpirationDate()));

if (lic.getStringAttribute(License.OID_OPSCENTER_CLIENT) != null) {
    message.append('\n').append(Messages.LicenseManager_operationsCenterClient());
}

if (StringUtils.isNotBlank(lic.getEditionDisplayName())) {
    message.append('\n').append(lic.getEditionDisplayName());
}
if (!lic.getFeaturePacks().isEmpty()) {
    for (String name: lic.getFeaturePacks().values()) {
        message.append('\n').append(Messages.LicenseManager_featurePack(name));
    }
}
if (lic.isTestMaster()) {
    message.append("\nTest Master License");
}
for (LicenseProperty property: lic.getProperties()) {
    String description = property.getDescription();
    if (description != null) {
        message.append('\n').append(description);
    }
}
println message.toString()