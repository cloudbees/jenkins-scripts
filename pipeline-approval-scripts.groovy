/**
Author: kuisathaverat
Description: approve scripts/signatures pending on "In-process Script Approval" using the parameters method and signature
Parameters: method and signature that you want to approve
**/

import org.jenkinsci.plugins.scriptsecurity.scripts.*
  
def method = "something"
def signature = "something"
  
final ScriptApproval sa = ScriptApproval.get();
for (ScriptApproval.PendingScript pending : sa.getPendingScripts()) {
   if (pending.script.equals(method)) {
       	sa.approveScript(pending.getHash());
     	println "Approved : " + pending.script
      }
}

for (ScriptApproval.PendingSignature pending : sa.getPendingSignatures()) {
   if (pending.equals(signature)) {
       	sa.approveSignature(pending.signature);
     	println "Approved : " + pending.signature
      }
}

  
