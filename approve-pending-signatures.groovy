/*
Small script that can be used to approve all the pending signatures
To be used with extreme caution
*/

import org.jenkinsci.plugins.scriptsecurity.scripts.*
ScriptApproval sa = ScriptApproval.get();

println "Signatures pending Approval..."
for (ScriptApproval.PendingSignature pending : sa.getPendingSignatures()) {
        println "Pending approval : " + pending.signature
}


println "Starting cleanup..."
sa = ScriptApproval.get();



while(sa.getPendingSignatures().size()>0) {
  try{
        item= sa.getPendingSignatures()[0]
        if (!org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist.isBlacklisted(item. signature)) {
             println "[WARNING] Not approving " + item. signature + " because it is blacklisted"
        }
        println item.signature
        sa.approveSignature(item.signature);
        println "Approved : " + item.signature

  }catch (Exception e){
    println e
  }
}

println "Verify that the signatures were approved..."
sa = ScriptApproval.get();
println "Retrieving pending approval signatures..."
for (ScriptApproval.PendingSignature pending : sa.getPendingSignatures()) {
        println "Pending approval : " + pending.signature
}
