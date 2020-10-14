import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait
import org.jenkinsci.plugins.github_branch_source.*
import org.jenkinsci.plugins.workflow.libs.*
import hudson.scm.SCM
import hudson.plugins.git.*

/*
 * Creates a Global Shared Library
 * @param: globalLibraryName - The name of your global shared library
 * @param: repoOwner - The owner of your git repository
 * @param: repository - The name of the repository where your library is stored
 * @param: repositoryUrl - The URL of the repository where your library is stored
 * @param: implicit - Load implicitly (or not) to allow pipeline to immediately use classes and/or variables from the libraries
 * @param: credentialsId - The ID of your git credentials in Jenkins credentials database
 * @param: defaultVersion - The default branch for your library in git
 */

def globalLibraryName = "my-global-library"
def repoOwner = "repository-owner"
def repository = "global-shared-library"
def repositoryUrl = "https://<path-to-the-shared-library-repository>"
def implicit = false
def credentialsId = "my-git-credentials-id"
def defaultVersion = "master"

def jenkins = Jenkins.getInstance()
def globalLibraryDescriptor = jenkins.getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")

GitHubSCMSource gitHubSCMSource = new GitHubSCMSource(repoOwner, repository, repositoryUrl, implicit)
gitHubSCMSource.credentialsId = credentialsId

BranchDiscoveryTrait branchDiscoveryTrait = new BranchDiscoveryTrait(3)
List<BranchDiscoveryTrait> branchDiscoveryTraits = new ArrayList<BranchDiscoveryTrait>();
branchDiscoveryTraits.add(new BranchDiscoveryTrait(3))
gitHubSCMSource.setTraits(branchDiscoveryTraits)

SCMSourceRetriever retriever = new SCMSourceRetriever(gitHubSCMSource)

LibraryConfiguration libraryConfiguration = new LibraryConfiguration(globalLibraryName, retriever)
libraryConfiguration.setDefaultVersion(defaultVersion)
libraryConfiguration.setImplicit(false)
globalLibraryDescriptor.get().setLibraries([libraryConfiguration])
