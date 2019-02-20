//https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/model/OverallLoadStatistics.java


import hudson.model.OverallLoadStatistics;

OverallLoadStatistics overallLoadStatistics = new OverallLoadStatistics()

String sep = "|"
String summaryHeader = "Total_Executors${sep}Idle_Executors${sep}Queue_Length${sep}"
String summary = "${overallLoadStatistics.computeTotalExecutors()}${sep}\
${overallLoadStatistics.computeIdleExecutors()}${sep}${overallLoadStatistics.computeQueueLength()}"


println(summaryHeader)
println(summary)

