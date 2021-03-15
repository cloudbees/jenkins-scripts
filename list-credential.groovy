/**
Author: kuisathaverat, carlosrodlop
Description: List ID and Description of all credentials on a Jenkins Instance.
It includes the fingerprints under the following considerations: 
   1) Job needs to have enable the Fingerprints. See https://www.jenkins.io/doc/book/using/fingerprints/ 
   2) Usage tracking requires the cooperation of plugins and consequently may not track every use.
Test on CloudBees CI Cloud Operations Center 2.277.1.2
**/
import com.cloudbees.plugins.credentials.Credentials
import com.cloudbees.plugins.credentials.CredentialsProvider

Set<Credentials> allCredentials = new HashSet<Credentials>();
Jenkins.instance.getAllItems(com.cloudbees.hudson.plugins.folder.Folder.class).each{ f ->
 creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
      com.cloudbees.plugins.credentials.Credentials.class, f)
  allCredentials.addAll(creds)

}
for (c in allCredentials) {
  if (CredentialsProvider.FINGERPRINT_ENABLED) {
    fp = CredentialsProvider.getFingerprintOf(c)
    if (fp) {  
    	fp_str = "Fingerprinted jobs: " + fp.getJobs()
  	} else {
    	fp_str = "(No Fingerprints)"
    }  
  }
  println(c.id + " : " + c.description  + " | " + fp_str)
}