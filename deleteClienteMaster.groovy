/**
@Author kuisathaverat
@Description example of how to delete a Client Master from groovy
**/

Jenkins.getInstance().getAllItems()
	.findAll{it instanceof com.cloudbees.opscenter.server.model.ClientMaster}
	.each{cm ->
      if(cm.name == 'NAME_OF_THE_CLIENT_MASTER'){
  		println "Deleting Client Master " + cm.class
        cm.delete()
      }
	}
