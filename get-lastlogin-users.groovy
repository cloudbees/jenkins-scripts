/**
@Author kuisathaverat
list all real users and shows the lastLogin datetime, those that have LastGrantedAuthoritiesProperty because them logen one time.
**/
import org.acegisecurity.*
import jenkins.security.*
import java.util.Date

User.getAll().each{ u ->
  def prop = u.getProperty(LastGrantedAuthoritiesProperty)
  def realUser = false
  def lastLogin = null
  if (prop) {
    realUser=true
    lastLogin = new Date(prop.timestamp).toString()
  }

  if (realUser){
    println u.getId() + ':' + u.getDisplayName() + ':Jenkins-User:' + u?.getDescription() + ':lastLogin=' + lastLogin
  } else if (realUser==false){
    println u.getId() + ':' + u.getDisplayName() + ':No-Jenkins-User:' + u?.getDescription()
  }
} 
