import jenkins.model.GlobalConfiguration
import com.cloudbees.opscenter.server.security.SecurityEnforcer
import com.cloudbees.opscenter.server.sso.SecurityEnforcerImpl
import com.cloudbees.opscenter.server.security.RestrictedEquivalentRAMF

/*
Author: Philip Cheong

This script will set the Security Setting Enforcement to Single Sign-On (security realm and authorization strategy)
The authentication mapping can be changed from RestrictedEquivalentRAMF to either TrustedEquivalentRAMF
or UntrustedEquivalentRAMF
It will also enforce all the security policies such as preventing XSS

javadoc to help understand this code:
https://repo.cloudbees.com/content/repositories/dev-connect/com/cloudbees/operations-center/server/operations-center-sso/2.222.0.2/operations-center-sso-2.222.0.2-javadoc.jar
https://repo.cloudbees.com/content/repositories/dev-connect/com/cloudbees/operations-center/server/operations-center-server/2.222.0.2/operations-center-server-2.222.0.2-javadoc.jar
*/

// get the current global security config
SecurityEnforcer.GlobalConfigurationImpl secEnfImpl = GlobalConfiguration.all().get(SecurityEnforcer.GlobalConfigurationImpl.class)

// There appear to be 3 different options that we can set using the SSO plugin.
secEnfImpl.setGlobal(new SecurityEnforcerImpl(
            false,                              // Allow client masters to opt-out
            false,                              // Allow per-master configuration of authentication mapping
            new RestrictedEquivalentRAMF()))    // or TrustedEquivalentRAMF or UntrustedEquivalentRAMF

SecurityEnforcer secEnf = SecurityEnforcer.getCurrent()
// Enforce Cross Site Request Forgery exploits prevention settings
secEnf.setCrumbIssuer(true)
// Enforce markup formatter settings
secEnf.setMarkupFormatter(true)
// Enforce slave → master security settings
secEnf.setMasterKillSwitch(true)
// Enforce remember me settings
secEnf.setRememberMe(true)
