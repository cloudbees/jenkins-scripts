import com.cloudbees.jenkins.plugins.foldersplus.*;

def all = [ Jenkins.instance ]
all.addAll(Jenkins.instance.nodes)
all.each {
  def c = it.toComputer()
  println "[$it.nodeName] - $it.assignedLabels - $it.nodeProperties - $c.offline"
  SecurityTokensNodeProperty prop = it.nodeProperties.get(SecurityTokensNodeProperty.class)
  if (prop != null){
    println(prop.getSecurityTokens())
    prop.getSecurityTokens().each{ token->
      token.getFolders().each{folder->
        println(folder.getFullDisplayName());
      }
    }
  }
}