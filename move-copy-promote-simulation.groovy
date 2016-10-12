/*** BEGIN META {
 "name" : "Move/Copy/Promote Simulation",
 "comment" : "Simulate a Move/Copy/Promote operation for troubleshooting purpose. The script create two folders
 (names starting with `testF0`) that can be deleted after the test",
 "parameters" : [ ],
 "core": "1.642",
 "authors" : [
 { name : "Allan Burdajewicz, Emilio Escobar Reyero" }
 ]
 } END META**/

import com.cloudbees.hudson.plugins.folder.Folder
import com.cloudbees.opscenter.context.remote.RemotePath;
import com.cloudbees.opscenter.replication.*

def now = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
def j = Jenkins.instance
def r = new java.security.SecureRandom()

def f1 = j.createProject(Folder.class, "testF01-$now-" + r.nextInt(10000))
def f2 = j.createProject(Folder.class, "testF02-$now-" + r.nextInt(10000))
def job = f1.createProject(FreeStyleProject.class, "TestJob-$now-" + r.nextInt(10000))

def session = new ReplicationSession(job).withRemotePath(new RemotePath(f2)).withFileSet(ItemReplicationFileSet.newInstance(job, ReplicationMode.MOVE))
def result = ItemReplicationTask.scheduleMoveTask(session);

println "Created:" + result.isCreated()