//Purge build queue by name

// find tasks by name which contains string pattern REPLACEME
import hudson.model.*
def q = Jenkins.instance.queue
q.items.findAll { it.task.name.contains('REPLACEME') }.each { println it.task.name }

// purge
import hudson.model.*
def q = Jenkins.instance.queue
q.items.findAll { it.task.name.contains('REPLACEME') }.each { q.cancel(it.task) }
