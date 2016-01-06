//Purge build queue by name

import hudson.model.*
def q = Jenkins.instance.queue
q.items.findAll { it.task.name.startsWith(‘REPLACEME') }.each { q.cancel(it.task) }