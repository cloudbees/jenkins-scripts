import nectar.plugins.rbac.groups.*

def job = Jenkins.instance.items.each{it instanceof Job}.find{it.name == 'CopyG'}

if(job instanceof Job){
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