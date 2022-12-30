/*
Author: Félix Belzunce Arcos
Since: January 2021
Description: Decode from export-credentials-root-level.groovy script, all the credentials of a Jenkins Master at System level. Paste the encoded message output from the export-credentials-root-level.groovy script as the value in the encoded variable in this script and execute it in the Script Console on the destination Jenkins. All the credentials and domains at root level from the source Jenkins will now be updated.
*/

import com.cloudbees.plugins.credentials.SystemCredentialsProvider
import com.cloudbees.plugins.credentials.domains.DomainCredentials
import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.trilead.ssh2.crypto.Base64
import hudson.util.XStream2
import com.cloudbees.plugins.credentials.SecretBytes
import jenkins.model.Jenkins
import java.nio.charset.StandardCharsets

// Paste the encoded message from the script on the source Jenkins
def encoded = []
// If you encounter the error "String too long. The given string is X Unicode code units long, but only a maximum of 65535 is allowed." when running this script,
// save the encoded data to a file, such as /home/jenkins/credentials.txt, omitting the starting [" and ending "], and un-comment the following line:
// encoded = [new File("/home/jenkins/credentials.txt").text]
if (!encoded) {
    return
}

// This converter ensure that the output XML contains plain-text for secretBytes (FileCredentials)
def converterSecretBytes = new Converter() {
    @Override
    void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        writer.value = Base64.encode(new String(object.getPlainData(), StandardCharsets.UTF_8).bytes);
    }

    @Override
    Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) { 
        return SecretBytes.fromBytes(new String(Base64.decode(reader.getValue().toCharArray())).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    boolean canConvert(Class type) { type == SecretBytes.class }
}

// The message is decoded and unmarshaled
for (slice in encoded) {
    def decoded = new String(Base64.decode(slice.chars))
    def stream = new XStream2()
    stream.registerConverter(converterSecretBytes)
    def list = stream.fromXML(decoded) as List<DomainCredentials>
    // Put all the domains from the list into system credentials
    def store = Jenkins.get().getExtensionList(SystemCredentialsProvider.class).first().getStore()
    def domainName
    for (domain in list) {
        domainName = domain.getDomain().isGlobal() ? "Global":domain.getDomain().getName()
        println "Updating domain: " + domainName
        for (credential in domain.credentials) {
            println "   Updating credential: ${credential.id}"
            if (! store.updateCredentials(domain.getDomain(), credential, credential) ){
                if (! store.addCredentials(domain.getDomain(), credential) ){
                    println "ERROR: Unable to add credential ${credential.id}"
                }
            }
        }
    }
}
