/**
Author: kuisathaverat
Description: List ID and Description of all credentials on a Jenkins Instance.
**/

def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
      com.cloudbees.plugins.credentials.Credentials.class,
      Jenkins.instance,
      null,
      null
  );
  for (c in creds) {
       println(c.id + ": " + c.description)
  }
