for (j in Jenkins.instance.getExtensionList(jenkins.metrics.api.MetricProvider.class)) {
  for (m in j.getMetricSet()) {
    for (i in m.metrics) {
    println i.getKey()
    }
  }
}