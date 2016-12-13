/*** BEGIN META {
 "name" : "Dump all the Move/Copy/Promote history logs in a file",
 "comment" : "Disable all the buildable projects inside a Folder",
 "parameters" : [ 'folderName' ],
 "core": "1.609",
 "authors" : [
 { name : "Allan Burdajewicz" }
 ]
 } END META**/

import com.cloudbees.opscenter.replication.ItemReplicationHistory

//Get all the move/copy/promote tasks that were unsuccessful
def records = ItemReplicationHistory.instance().records
        .findAll{ it -> it.result.toString() != 'SUCCESS'}
        .collect();

//Print the records
println records;

OutputStream os = new FileOutputStream("/tmp/move-error-logs.txt");
try {
    records.each {
        // Write the log of this record to the file
        it.writeWholeLogTo(os)
    }

} catch (Exception ex) {
    ex.printStackTrace();
} finally {
    os.close();
}

return;