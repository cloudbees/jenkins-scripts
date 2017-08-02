/**
Author: kuisathaverat
Description: List all EC2 instances owner by the Account configured on the EC2 Cloud configurationn, it helps to diagnose EC2 Plugin issues.
see also https://github.com/jenkinsci/jenkins-scripts/pull/98 for list only EC2 templates instances.
**/

import com.amazonaws.services.ec2.model.InstanceStateName

Jenkins.instance.clouds.findAll{it -> it instanceof hudson.plugins.ec2.AmazonEC2Cloud}
    .each{ c -> 
      println c.getCloudName() + ' - ' + c.getRegion() + ' - CAP:' + c.instanceCap 
        int running = 0
        int terminated = 0
        int shuttingdown = 0
        c.connect()?.describeInstances()?.getReservations().each{ r->
            r?.getInstances().each{ i ->
                InstanceStateName stateName = InstanceStateName.fromValue(i.getState().getName());
                if (stateName != InstanceStateName.Terminated && stateName != InstanceStateName.ShuttingDown) {
                    running++
                } else if (stateName == InstanceStateName.Terminated) {
                  terminated++
                } else if (stateName == InstanceStateName.ShuttingDown) {
                    shuttingdown++
                }
                println "\t\tExisting instance found: " + i.getInstanceId() + " AMI: " + i.getImageId() + ' - State:' + stateName + ' - Description' + i.getTags()
            }
        }
    println "\tTotal Intances Running:" + running
    println "\tTotal Intances Terminated:" + terminated
    println "\tTotal Intances ShuttingDown:" + shuttingdown
    }
return 
