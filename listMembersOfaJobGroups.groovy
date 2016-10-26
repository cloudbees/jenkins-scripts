import nectar.plugins.rbac.groups.*
def groupName = 'CopyG'

def job = Jenkins.instance.items.getAllItems(Job.class).each {it ->
  if (it.name == groupName) {
    println 'Job : ' + job.name

    GroupContainer container = GroupContainerLocator.locate(job);
      if(container != null){
      container.getGroups().each{
        it.getGroupMembership().each{ println 'GroupMember : ' + it.name }
        it.getMembers().each{ println 'Member : ' + it }
      }
    }
    job.getProperties().each{println it}
  }
}
