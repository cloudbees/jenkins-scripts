﻿/*
Author: Carlos Rodriguez Lopez
Since: September 2017
Description: It ensures all the parameters are meet in the instance. If they are met, it creates for the node pTemplate a pGroup (membership: pUsers, rol: pRol)
Parameters: pGroup, pTemplate, pRol, pPermission
Scope: Cloudbees Jenkins Enterprise
*/

import nectar.plugins.rbac.groups.*
import nectar.plugins.rbac.roles.*
import nectar.plugins.rbac.groups.Group.RoleAssignment
import com.cloudbees.hudson.plugins.modeling.Model

// PARAMETERS (To be checked on B - Checking Requisites)
def pGroup = "groupX"
List<String> pUsers = Arrays.asList("developer1", "developer2", "developer3") // users' id (from the Jenkins's own database)
def pTemplate = "TemplateFolder/testTemplate" // If the template is in the root just add template name. If it is located under a folder structure specify this by "/"
def pRol = "job-read" // Rol to associated to pGroup
def pPermission = "hudson.model.Item.Read" // Permission included into pRol

// VARIABLES
Map groupsContainers = new LinkedHashMap()
GroupContainer gc, gc2update
Group g2create //Output
Map groupIterator
def nodeCounter = 0 // Root Level
def nodeString = "Jenkins /" //
def pTemplateNodeArray = pTemplate.split("/")
def processingFlag = true
def rolFoundFlag = false
def permissionFoundFlag = false
def jenkins = Jenkins.instance
def roles = new Roles(jenkins)

//-------------------------------------------
// A - Getting definition of your instance
//-------------------------------------------

// A.1.- Populating groupsContainers with RBAC nodes definition

// Add the root container: Level 0
groupsContainers.put(jenkins.displayName, GroupContainerLocator.locate(jenkins))
// Add all the items that are be containers: Level > 0
for (i in jenkins.allItems) {
    if (GroupContainerLocator.isGroupContainer(i.getClass())) {
        gc = GroupContainerLocator.locate(i);
        if (gc != null) groupsContainers.put(jenkins.displayName + "/" + i.fullDisplayName, gc);
    }
}

// A.2.- Template Location Node as String according to (1)

pTemplateNodeArray.each { node ->
    if (nodeCounter < 1){//Root Level
        nodeString = nodeString + "${node}"
    }else{
        nodeString = nodeString + " » ${node}"
    }
    nodeCounter++
}
//println "[DEBUG]: Template Location Node String: " + nodeString

//-------------------------------------------
// B - Checking Requisites
//-------------------------------------------


// B.1- Job Template (pTemplate) should be existing

if (jenkins.getItemByFullName(pTemplate)==null){
    println "[ERROR]: Parameter pTemplate: `$pTemplate` does not exits in this instance"
    processingFlag = false
} else {
    if (jenkins.getItemByFullName(pTemplate) instanceof Model==false){
        println "[ERROR]: Parameter pTemplate: `$pTemplate` is not a Job Template type"
        if (processingFlag) processingFlag = false
    } else {
        //println "[DEBUG]: Job Template `$pTemplate` has been found"
    }
}

// 4.- ROLE_JOB_READ should be existing at contains Job:Read permission

for (rol in roles.getRoles()){
    //println "[DEBUG]: rol.id:" + rol.id
    if((rol.id).equals(pRol)==true){
        rolFoundFlag=true
        for (permis in rol.getPermissionProxyIds()){
            if(permis.equals(pPermission)==true){
                permissionFoundFlag = true
                break;
            }
        }
    }
}
if (!rolFoundFlag){
    println "[ERROR]: Parameter pRol: `$pRol` does not exist in this instance"
    if (processingFlag) processingFlag = false
} else {
    if (!permissionFoundFlag){
        println "[ERROR]: Parameter pPermission: `$pPermission` is not included into the Permission List of pRol: `$pRol`"
        if (processingFlag) processingFlag = false
    }
}

// 5.- There should not be an existing Group (pGroup) at Template (pTemplate) level

groupIterator = groupsContainers
for (gc2 in groupIterator) {
    // nodeString has been build following same structure that the key of the Map groupsContainers
    if ((gc2.key).equals(nodeString)){
        // Group Container where changes should be applied. Node of pTemplate
        gc2update = gc2.value
        for (g in gc2.value.groups) {
            if ((g.name).equals(pGroup)){
                println "[ERROR]: There is an exiting group '$pGroup'in the '$pTemplate' of this instance"
                if (processingFlag) processingFlag = false
            }
        }
    }
}

// 6.- All users in pUsers should be existing in Jenkins

for (u in pUsers) {
    //println "[DEBUG]: user -" + u
    //Checking if the user exist on Jenkins own's database
    if(hudson.model.User.get(u,false,null)==null){
        println "[ERROR]: User '" + u + "' is not included into Jenkins Own's database of this instance"
        if (processingFlag) processingFlag = false
    }
}

//----------------------
//Processing
//----------------------

if (processingFlag) {
    println "[DEBUG]: Strating to process it..."
    //Creating group
    g2create = new Group (gc2update, pGroup)
    //Adding members
    g2create.setMembers(pUsers)
    def List<RoleAssignment> assignments = new ArrayList<RoleAssignment>()
    def rolAssigmment = new RoleAssignment(pRol)
    assignments.add(rolAssigmment)
    //Adding roles Group#setRoleAssignments(List<RoleAssignment> roleAssignments)
    g2create.setRoleAssignments(assignments)
    g2create.save()
    gc2update.addGroup(g2create)
    jenkins.save()
    println "[INFO]: '$pGroup' has been created succesfully for '$pTemplate'"
} else {
    println "[ERROR]: Process aborted because parameters and/or requeriments do not meet the desired criteria"
}