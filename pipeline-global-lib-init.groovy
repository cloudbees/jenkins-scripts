/*
Author: Kurt Madel
This script allows pulling an external Git repo into a Jenkins instance to be used as 
the Jenkins Pipeline Global lib repository. 
It may be run as a cluster operation from CloudBees Jenkins Operations Center to keep 
the Pipeline Global Libs in sync across multiple connected masters. 
*/

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.StoredConfig;
import org.jenkinsci.plugins.workflow.cps.global.UserDefinedGlobalVariableList;

def upstreamRepoUrl = "https://github.com/beedemo-sa/workflowLibs"
Jenkins j = Jenkins.getInstance();
println(j.getRootPath())
File workflowLibGitDir = new File(j.getRootPath().toString(), "workflow-libs/.git")
println(workflowLibGitDir)
File workflowLibDir = new File(j.getRootPath().toString(), "workflow-libs")
println "git init".execute(null, workflowLibDir).text
println "git status".execute(null, workflowLibDir).text
Repository localRepo = new FileRepositoryBuilder()
    .setGitDir(workflowLibGitDir)
    .build();
Git git = new Git(localRepo);
StoredConfig config = git.getRepository().getConfig();
config.setString("remote", "upstream", "url", upstreamRepoUrl);
config.save();

println "git remote -v".execute(null, workflowLibDir).text
println "git pull upstream master".execute(null, workflowLibDir).text
println "ls -la".execute(null, workflowLibDir).text

//Get Pipeline Global Library Jenkins Extension that rebuilds global library on Git Push
List extensions = ExtensionList.lookup(UserDefinedGlobalVariableList.class);
extensions.get(0).rebuild() //may want to add a check here to make sure extensions isn't null
