for (item in Jenkins.instance.allItems) {
  if (item.class.canonicalName == "com.cloudbees.hudson.plugins.folder.Folder") {
    println "folder      [" + item.fullName + "]"

    item.properties.each { p -> 
      if(p.class.canonicalName == "com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty") {

        p.getGrantedPermissions().each { gp -> 
          println "            " + gp
        }
      }
    } 
    println ""
  } else {
    print "item        [" + item.fullName + "]" 

    try {
      def p = item.getProperty(hudson.security.AuthorizationMatrixProperty.class)

      if (p != null) {
        println ""
        p.getGrantedPermissions().each { gp -> 
          println "            " + gp
        }
      }

    }catch (err) {
      println " folder -> " + item.class.canonicalName 
      item.properties.each { p -> 
        if(p.class.canonicalName == "com.cloudbees.hudson.plugins.folder.properties.AuthorizationMatrixProperty") {

          p.getGrantedPermissions().each { gp -> 
            println "            " + gp
          }
        }
      }
    }

    println ""
  }
}