/**
Author: Emilio Escobar, kuisathaverat
Since: February 2017
Description: Create a new ClientMaster and connect it to CJOC
Parameters: name of the Master and url of CJOC
Scope: Cloudbees Jenkins Operations Center
**/
import com.cloudbees.opscenter.server.model.ClientMaster;
import com.cloudbees.opscenter.server.properties.ConnectedMasterLicenseServerProperty;
import com.cloudbees.opscenter.server.security.SecurityEnforcer;
import com.cloudbees.opscenter.server.security.TrustedEquivalentRAMF;
import jenkins.model.Jenkins
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.net.HttpURLConnection
import java.io.DataOutputStream
import jenkins.model.*

Jenkins jenkins = Jenkins.instance

/*
//uncoment this if you want to use it from command line
if (args.length != 2 ) {
  println "addMaster.groovy name url"
}
args.each{println it}
def name = args[0]
String url = args[1] //"http://cje.example.com:8282"
*/

//coment this if you want to use it from command line
def name = "jenkins-"
String url = "http://cje.example.com:8282"

int id = Jenkins.instance.getAllItems(ClientMaster.class).size() + 1
String grantId = id + '-' + name //"jenkins-$id"

println("ID $id");
println("GRANTID $grantId");
println("URL $url");
println("NAME $name");

if (jenkins.getItem(name) == null) {
    ClientMaster cm = jenkins.createProject(ClientMaster.class, name);
    cm.setId(id)
    cm.setGrantId(grantId);
    cm.getProperties().replace(new ConnectedMasterLicenseServerProperty(new ConnectedMasterLicenseServerProperty.FloatingExecutorsStrategy()));
    cm.getProperties().replace(new SecurityEnforcer.OptOutProperty(false, true, new TrustedEquivalentRAMF()));
    cm.save();

    println("SAVE performed");

    def details = cm.getBase64Config(true);

    String registrar = url + "/descriptorByName/com.cloudbees.opscenter.client.plugin.OperationsCenterRegistrar/pushRegistration?details="+URLEncoder.encode(details, "UTF-8")

    println("Registrar: $registrar");

    URL master_registrar = new URL(registrar)

    def connection = master_registrar.openConnection();

    println("Response Headers");
    println("================");
    for (def e in connection.getHeaderFields()) {
         println("${e.key}: ${e.value}");
    }
    println("");
    println("Response status: HTTP/${connection.responseCode}");
    println("");
    println("Response");
    println("========");
    println(connection.inputStream.text)

    String push = url + "/descriptorByName/com.cloudbees.opscenter.client.plugin.OperationsCenterRegistrar/pushRegistrationConfirm"
    String urlParameters = "details="+URLEncoder.encode(details, "UTF-8")+"&Submit=Join Operations Center"

    byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
    int    postDataLength = postData.length;

    URL    master_push            = new URL( push );
    HttpURLConnection conn= (HttpURLConnection) master_push.openConnection();
    conn.setDoOutput( true );
    conn.setInstanceFollowRedirects( true );
    conn.setRequestMethod( "POST" );
    conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
    conn.setRequestProperty( "charset", "utf-8");
    conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
    conn.setUseCaches( false );
    DataOutputStream wr = new DataOutputStream( conn.getOutputStream())
    wr.write( postData );

   println("Response Headers");
    println("================");
    for (def e in conn.getHeaderFields()) {
         println("${e.key}: ${e.value}");
    }
    println("");
    println("Response status: HTTP/${conn.responseCode}");
    println("");
    println("Response");
    println("========");
    println(conn.inputStream.text)
}
