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
import com.trilead.ssh2.crypto.Base64
import hudson.util.Secret
import com.cloudbees.plugins.credentials.SecretBytes
import hudson.util.XStream2
import jenkins.model.Jenkins
import java.nio.charset.StandardCharsets

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
        writer.value = Secret.toString(object as Secret)
    }

    @Override
    Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) { null }

    @Override
    boolean canConvert(Class type) { type == Secret.class }
}
// This converter ensure that the output XML contains base64 encoded for secretBytes (to handle FileCredentials)
def converterSecretBytes = new Converter() {
    @Override
    void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        writer.value = Base64.encode(new String(object.getPlainData(), StandardCharsets.UTF_8).bytes).toString();
    }

    @Override
    Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) { 
        return SecretBytes.fromBytes(new String(Base64.decode(reader.getValue().toCharArray())).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    boolean canConvert(Class type) { type == SecretBytes.class }
}

def stream = new XStream2()
stream.registerConverter(converter)
stream.registerConverter(converterSecretBytes)

// Marshal the list of credentials into XML
//println stream.toXML(credentials); return ""; //For debug purpose
def encoded = []
def sections = credentials.collate(25)
for (section in sections) {
    def xml = Base64.encode(stream.toXML(section).bytes)
    encoded.add("\"${xml}\"")
}

println encoded.toString()