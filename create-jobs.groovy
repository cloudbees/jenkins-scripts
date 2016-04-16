/*** BEGIN META {
 "name" : "Create Items",
 "comment" : "Different ways of creating items (Jobs, Folders, ...)",
 "parameters" : [ ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.FreeStyleProject
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob

/**
 * Create Item from class.
 */

//Create a project at the root
Jenkins.instance.createProject(FreeStyleProject.class, "FreestyleRoot");

//Create a Folder at the root
Jenkins.instance.createProject(Folder.class, "FolderA");

//Create a project inside a Folder
Jenkins.instance.getItem("FolderA").createProject(FreeStyleProject.class, "FreestyleA1");

//Create a project inside a Folder
Jenkins.instance.getItem("FolderA").createProject(Folder.class, "FolderAA");

//Create a project inside a nested Folder
Jenkins.instance.getItemByFullName("FolderA/FolderAA").createProject(FreeStyleProject.class, "FreestyleAA1");

//Create a pipeline job
Jenkins.instance.getItemByFullName("FolderA/FolderAA").createProject(WorkflowJob.class, "PipelineAA1");

/**
 * Create from XML.
 */

//Define the XML of a Folder
def folderXml = "" +
        "<com.cloudbees.hudson.plugins.folder.Folder>" +
            "<actions/>" +
            "<description/>" +
            "<properties/>" +
            "<views/>" +
            "<viewsTabBar/>" +
            "<healthMetrics/>" +
            "<icon/>" +
        "</com.cloudbees.hudson.plugins.folder.Folder>"
"";

//Define the XML of a Freestyle Job
def jobXml = "" +
        "<project>" +
            "<keepDependencies>false</keepDependencies>" +
            "<properties/>" +
            "<scm/>" +
            "<canRoam>true</canRoam>" +
            "<disabled>false</disabled>" +
            "<blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>" +
            "<blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>" +
            "<triggers/>" +
            "<concurrentBuild>false</concurrentBuild>" +
            "<builders/>" +
            "<publishers/>" +
            "<buildWrappers/>" +
        "</project>" +
        "";

//Create the Job at the root from XML
Jenkins.instance.createProjectFromXML("myRootJobFromXML", new ByteArrayInputStream(jobXml.getBytes()));
//Create the Folder at the root from XML
Jenkins.instance.createProjectFromXML("myRootFolderFromXML", new ByteArrayInputStream(folderXml.getBytes()));

/**
 * Copy (only configuration, not build history and stuffs) to create a bunch of similar items.
 */

//Copy a job at the root
Jenkins.instance.copy(Jenkins.instance.getItem('FreestyleRoot'), "CopyOfFreestyleRoot");

//Copy a job under a Folder
Jenkins.instance.getItem("FolderA").copy(Jenkins.instance.getItem('FreestyleRoot'), "CopyOfFreestyleRoot");

//Copy a Folder under a Folder
Jenkins.instance.getItem("FolderA").copy(Jenkins.instance.getItemByFullName('FolderA/FolderAA'), "CopyOfFolderAA");