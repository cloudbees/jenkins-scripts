/*** BEGIN META {
 "name" : "Copy/Move/Promote Diagnosis",
 "comment" : "Troubleshooting script to diagnose Move/Copy/Promote issue in a particular directory.",
 "parameters" : [ ],
 "core": "1.642",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

//The test directory ($JENKINS_HOME or the directory of a job under $JENKINS_HOME)
def directory = "/var/jenkins/"

//Create a `groovyTestDiectory/jobs` directory
File file = new File ("${directory}/groovyTestDiectory/jobs")

//Check we can create directories under the `directory`
println "can we create the final groovyTestDirectory/test directory? "
boolean created = file.mkdirs();

//The final directory cannot be created
if (!created) {

    //Does it already exists
    if (file.exists()) {
        println "File already exists"
        return
    }

    //Check that we can create directory
    println "Can we create dirs? " + file.mkdir()

    File canonFile = null;
    try {
        canonFile = file.getCanonicalFile();
        println "Canonical file is " + canonFile
        println "Canonical file created successfully"
    } catch (IOException e) {
        println "Error while creating canonical file"
    }

    File parent = canonFile.getParentFile();

    println "Parent is not null " + (parent != null)
    println "Parent can be created or already exists " + (parent.mkdirs() || parent.exists())
    println "CanonFile already exists is " + canonFile.exists()
    println "I can create canonFile is " + canonFile.mkdir()
    println "Last check is " + (parent != null && (parent.mkdirs() || parent.exists()) && canonFile.mkdir());

    //Remove the a `groovyTestDirectory`
    if(file.parentFile.deleteDir()) {
        println "Test directory deleted";
    }

} else {
    //Remove the a `groovyTestDirectory`
    if(file.parentFile.deleteDir()) {
        println "Test directory deleted";
    }
}