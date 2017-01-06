/*** BEGIN META {
 "name" : "Dump Copy/Move/Promote Error History",
 "comment" : "Dump all the history logs of failed Move/Copy/Promote operations in a file",
 "parameters" : [ ],
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