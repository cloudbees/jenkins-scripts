import jenkins.model.Jenkins

import java.time.Instant

// dates should use the ISO8601 format
def startDate = Instant.parse("2012-12-01T19:22:43.000Z")
def endDate = Instant.now()
def jobName = 'changeMe'

println "Will return all the builds for job ${jobName} that ran between ${startDate} and ${endDate}"

def builds = Jenkins.get().getItemByFullName(jobName, hudson.model.Job).builds.byTimestamp(startDate.toEpochMilli(), endDate.toEpochMilli())

return builds