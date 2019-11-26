/*
* Basic script to set a controlled agent from an existing agent
* by setting properly the SecurityGrantsFolderProperty
*/

import hudson.model.*
import jenkins.model.*
import hudson.slaves.*
import com.cloudbees.hudson.plugins.folder.*
import com.cloudbees.jenkins.plugins.foldersplus.*


String nodeName = "${nodeName}"
String folderName = "${folderName}"
Slave node = Jenkins.instance.getNode(nodeName)
node.getNodeProperties().add(new SecurityTokensNodeProperty(false));
SecurityToken token = SecurityToken.newInstance();
node.getNodeProperties().get(SecurityTokensNodeProperty.class).addSecurityToken(token);
node.save()
SecurityGrant request = SecurityGrant.newInstance();
SecurityGrant grant = token.issue(request);

Folder folder= Jenkins.instance.createProject(Folder.class, folderName)
folder.getProperties().replace(new SecurityGrantsFolderProperty(Collections.<SecurityGrant>emptyList()));
folder.getProperties().get(SecurityGrantsFolderProperty.class).addSecurityGrant(grant);
folder.save()
return "Node has been set successfully."
