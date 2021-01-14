/*
Author: Félix Belzunce Arcos
Since: January 2021
Description: Decode from export-credentials-folder-level.groovy script, all the credentials of a Jenkins Master at Folder level. Paste the encoded message output from the export-credentials-folder-level.groovy script as the value in the encoded variable in this script and execute it in the Script Console on the destination Jenkins. All the credentials and domains at folder level from the source Jenkins will now be updated.
*/

import com.cloudbees.hudson.plugins.folder.Folder
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider
import com.cloudbees.plugins.credentials.domains.DomainCredentials
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.trilead.ssh2.crypto.Base64
import hudson.util.Secret
import hudson.util.XStream2
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.domains.DomainCredentials
import com.trilead.ssh2.crypto.Base64
import hudson.util.XStream2
import jenkins.model.Jenkins
import com.cloudbees.plugins.credentials.Credentials

// Paste the encoded message from the script on the source Jenkins
def encoded=[]

if (!encoded) {
    return
}

HashMap<String, List<DomainCredentials>> credentialsList;

// The message is decoded and unmarshaled
for (slice in encoded) {
    def decoded = new String(Base64.decode(slice.chars))
    domainListByFolders = new XStream2().fromXML(decoded) as HashMap<String, List<DomainCredentials>>  ;  
}

def instance = Jenkins.get()
def folderExtension = instance.getExtensionList(FolderCredentialsProvider.class)
if (!folderExtension.empty) {
  def folders = instance.getAllItems(Folder.class)
  def folderProvider = folderExtension.first()
  def store
  def domainName
  for (folder in folders) {
    store = folderProvider.getStore(folder)
    folderDomains = domainListByFolders.get(store.getContext().getFullDisplayName())
    if (folderDomains!=null) {
      for (domain in folderDomains) {
        domainName = domain.getDomain().isGlobal() ? "Global":domain.getDomain().getName()
        println "Updating domain " + domainName
        for (credential in domain.credentials) {
            println "   Updating credential: " + credential.id;
            store.updateCredentials(domain.getDomain(), credential, credential)
        }
      }
    }
  }
}