/*
Author: Félix Belzunce Arcos
Since: January 2021
Description: Encode in Base64 all the credentials of a Jenkins Master at Folder level. The script is used to update all the credentials at folder level, so they can be re-encrypted in another Jenkins Master. 

The script should be executed in the Script Console. It will output an encoded message containing a flattened list of all the folder credentials. Copy that encoded message.

The encoded message can be used to update the credentials in a new Jenkins Master.
*/

import com.cloudbees.hudson.plugins.folder.AbstractFolder
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

def instance = Jenkins.get()
def credentials = []
HashMap<String, List<DomainCredentials>> domainsFromFolders = new HashMap<String, List<DomainCredentials>>();

// Each folder contains a Store. A Store contains one or more Domains 
// and each Domain might contain Credentials defined. 
def folderExtension = instance.getExtensionList(FolderCredentialsProvider.class)
if (!folderExtension.empty) {
    def folders = instance.getAllItems(AbstractFolder.class)
    def folderProvider = folderExtension.first()
    def domainName
    def store
    def listDomainCredentials
    for (folder in folders) {
        store = folderProvider.getStore(folder)
        println "Processing Store for  " + store.getContext().getUrl()
        listDomainCredentials = new ArrayList<DomainCredentials>();
        for (domain in store.domains) {
            domainName = domain.isGlobal() ? "Global":domain.getName();
            println "   Processing Domain " + domainName
            listDomainCredentials.add(new DomainCredentials(domain, store.getCredentials(domain)));
        }
        println "       Adding all credentials in the Store... "
        domainsFromFolders.put(store.getContext().getUrl(), listDomainCredentials);
    }
}

// The converter ensures that the output XML contains the unencrypted secrets
def converter = new Converter() {
    @Override
    void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        writer.value = Secret.toString(object as Secret)
    }

    @Override
    Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) { null }

    @Override
    boolean canConvert(Class type) { type == Secret.class }
}

def stream = new XStream2()
stream.registerConverter(converter)

// Marshal the list of credentials into XML
def encoded = []

    def xml = Base64.encode(stream.toXML(domainsFromFolders).bytes)
    encoded.add("\"${xml}\"")

println encoded.toString()