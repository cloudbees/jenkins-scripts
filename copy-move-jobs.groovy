/*** BEGIN META {
 "name" : "Copy Move Jobs",
 "comment" : "Different methods to Copy/Move jobs.",
 "parameters" : [ ],
 "core": "1.642",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.FreeStyleProject
import hudson.model.Items
import jenkins.model.Jenkins

//Create a FolderA and FolderB at the root
Jenkins.instance.createProject(Folder.class, "FolderA");
Jenkins.instance.createProject(Folder.class, "FolderB");

//Create a project at the root
Jenkins.instance.createProject(FreeStyleProject.class, "FreestyleRoot");

/**
 * Copy
 */

//Copy a job at the root
Jenkins.instance.copy(Jenkins.instance.getItem('FreestyleRoot'), "CopyOfFreestyleRoot");

//Copy a job under a Folder
Jenkins.instance.getItem("FolderA").copy(Jenkins.instance.getItem('FreestyleRoot'), "jobA");
//Copy a folder under a Folder
Jenkins.instance.getItem("FolderB").copy(Jenkins.instance.getItem('FolderA'), "CopyOfFolderB");

/**
 * Move
 */
//Move a job to the root
Items.move(Jenkins.instance.getItemByFullName('FolderA/jobA'), Jenkins.instance);

//Move a job under a Folder
Items.move(Jenkins.instance.getItem('FreestyleRoot'), Jenkins.instance.getItem("FolderB"));

//Move a Folder
Items.move(Jenkins.instance.getItem("FolderA"), Jenkins.instance.getItem("FolderB"));