/*
Author: Félix Belzunce Arcos
Since: January 2021
Description: Encode in Base64 all the credentials of a Jenkins Master at System level. The script is used to update all the credentials at System level, re-encrypting them in another Jenkins Master. 

The script should be executed in the Script Console. It will output an encoded message containing a flattened list of all the System credentials. Copy that encoded message.

The encoded message can be used to update the System credentials in a new Jenkins Master. 
*/

import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.DomainCredentials
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import hudson.util.Secret
import com.cloudbees.plugins.credentials.SecretBytes
import hudson.util.XStream2
import jenkins.model.Jenkins

def instance = Jenkins.get()
def credentials = []

// Copy all domains from the system credentials provider
def systemProvider = instance.getExtensionList(SystemCredentialsProvider.class)
if (!systemProvider.empty) {
    def systemStore = systemProvider.first().getStore()
    def domainName
    for (domain in systemStore.domains) {
        domainName = domain.isGlobal() ? "Global":domain.getName()
        println "Adding credentials from System Domain: " + domainName
        credentials.add(new DomainCredentials(domain, systemStore.getCredentials(domain)))
    }
}

// The converter ensures that the output XML contains the unencrypted secrets
def converter = new Converter() {
    @Override
    void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        switch (object.class) {
            case Secret: writer.value = Secret.toString(object as Secret); break
            case SecretBytes: writer.value = Base64.getEncoder().encodeToString((object as SecretBytes).getPlainData()); break
        }
    }

    @Override
    Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) { null }

    @Override
    boolean canConvert(Class type) { type == Secret.class || type == SecretBytes.class }
}

def stream = new XStream2()
stream.registerConverter(converter)

// Marshal the list of credentials into XML
def encoded = []
def sections = credentials.collate(25)
for (section in sections) {
    encoded.add("\"${Base64.getEncoder().encodeToString(stream.toXML(section).bytes)}\"")
}
println encoded.toString()


// Marshal the list of credentials into XML in a file (parent directories must exist and the user running Jenkins must have sufficient permission in the directory)
// If you encounter the error "String too long" when using the update-credentials-system-level.groovy script, then the following should be used to
// save the encoded data to a file, such as /home/jenkins/system_credentials.txt:
// org.codehaus.groovy.runtime.IOGroovyMethods.withStream(Base64.getEncoder().wrap(new File("/home/jenkins/system_credentials.txt").newOutputStream()), {os -> stream.toXMLUTF8(credentials, os)})