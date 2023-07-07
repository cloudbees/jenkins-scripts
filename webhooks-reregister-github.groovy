/**
This script is used to re-register webhooks for all GitHub repositories in Jenkins.
It first enables webhook management for all GitHub servers, then re-registers webhooks
for multibranch projects not in a Github organization, then disables webhook management
and re-registers webhooks for all Github organization folders.

It is done this way to avoid re-registering webhooks in all repositories AND at the Org level of a github org.
This is because the GitHub plugin will automatically register webhooks for all repositories in an org
when a webhook is registered at the org level, if the webhook management option is enabled.
*/

import jenkins.branch.MultiBranchProject
import jenkins.branch.OrganizationFolder
import jenkins.util.io.FileBoolean
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource
import org.jenkinsci.plugins.github_branch_source.GitHubOrgWebHook
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator
import org.jenkinsci.plugins.github.config.GitHubPluginConfig
import org.jenkinsci.plugins.github.config.GitHubServerConfig

def githubConfig = Jenkins.instance.getDescriptorByType(GitHubPluginConfig.class)
def serverConfigs = githubConfig.getConfigs()

//Enable webhook management for all Github servers
serverConfigs.each { serverConfig ->
    serverConfig.setManageHooks(true)
    githubConfig.save()
    println("Enabled Manage Webhooks for GitHub server: "+serverConfig.getName())
}

//Recreate webhooks for all Multibranch jobs that aren't in an OrganizationFolder
Jenkins.instance.allItems(MultiBranchProject.class).each { project ->
  if (!(project.getParent() instanceof OrganizationFolder)) {
    print("MultiBranchProject: ${project.fullName}")
    project.save()
    project.fireSCMSourceAfterSave(project.getSCMSources())
    println ("  Recreated Hook")
  }
}

//Disable webhook management for all Github servers
serverConfigs.each { serverConfig ->
    serverConfig.setManageHooks(false)
    githubConfig.save()
    println("Disabled Manage Webhooks for GitHub server: "+serverConfig.getName())
}

//Recreate webhooks for all OrganizationFolders
Jenkins.instance.allItems(OrganizationFolder.class).each { project ->
  print("OrganizationFolder: ${project.fullName}")
    project.getSCMNavigators().each {
      if ( it instanceof GitHubSCMNavigator ) {
        FileBoolean orghook = new FileBoolean(new File (Jenkins.get().getRootDir(), "github-webhooks/GitHubOrgHook." + it.repoOwner))
        orghook.off()
        it.afterSave(project)
        print ("  Recreated Hook")
      }

    }
    println()
}
