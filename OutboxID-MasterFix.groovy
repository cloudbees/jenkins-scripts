/*
Author: Alvaro Lobato
Since: June 2017
Description: This script will fix the outboxSequenceId of a master which had its database
reset or became out of synch.
Parameters: None
Scope: Cloudbees Jenkins Platform
*/

import com.cloudbees.opscenter.context.Messaging;

println Messaging.getInstance().local.outboxSequenceId
Messaging.getInstance().local.outboxSequenceId.set($RETURNED_NUMBER);
println Messaging.getInstance().local.outboxSequenceId