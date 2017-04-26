/**
Author: kuisathaverat
Description: list pending approvals, approve scripts/signatures pending on "In-process Script Approval" using the parameters method and signature, and add a signature tho the list.
Parameters: method and signature that you want to approve
**/

import org.jenkinsci.plugins.scriptsecurity.scripts.*
  
def method = "something"
def signature = "something"

ScriptApproval sa = ScriptApproval.get();

//list pending approvals
for (ScriptApproval.PendingScript pending : sa.getPendingScripts()) {
        println "Pending Approved : " + pending.script
}

for (ScriptApproval.PendingSignature pending : sa.getPendingSignatures()) {
        println "Pending Approved : " + pending.signature
}  

// approve scripts
for (ScriptApproval.PendingScript pending : sa.getPendingScripts()) {
   if (pending.script.equals(method)) {
       	sa.approveScript(pending.getHash());
     	println "Approved : " + pending.script
      }
}

// approbve signatures
for (ScriptApproval.PendingSignature pending : sa.getPendingSignatures()) {
   if (pending.equals(signature)) {
       	sa.approveSignature(pending.signature);
     	println "Approved : " + pending.signature
      }
}

//Add a signature to the list
signature = "staticMethod org.codehaus.groovy.runtime.DefaultGroovyMethods getText java.net.URL"

ScriptApproval.PendingSignature s = new ScriptApproval.PendingSignature(signature, false, ApprovalContext.create())
sa.getPendingSignatures().add(s)
