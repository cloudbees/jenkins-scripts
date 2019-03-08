/**
@Author kuisathaverat
list and mark all real users: those that have the LastGrantedAuthoritiesProperty because they've logged in at least once.
**/
import org.acegisecurity.*
import jenkins.security.*
import java.util.Date

User.getAll().each{ u ->
  def prop = u.getProperty(LastGrantedAuthoritiesProperty)
  def realUser = false
  def creationDate = null
  if (prop) {
    realUser=true
    creationDate = new Date(prop.timestamp).toString()
  }

  def lastLogin = new Date()
  if(u.getDescription() != null && u.getDescription().startsWith('###') && realUser){
    def timestamp = (u.getDescription() =~ /###(.+)###/)[ 0 ][ 1 ]
    try{
        lastLogin = new Date(java.lang.Long.valueOf(timestamp))
        println u.getId() + ":" + u.getDisplayName() + ':' + realUser + ':' + u.getDescription() + ':creationDate=' + creationDate + ":lastLogin=" + lastLogin
    } catch( Exception e){
        println "ERROR:" + u.getId() + ":" + u.getDisplayName() + ':' + realUser + ':' + u.getDescription() + ':timestamp=' + timestamp + ":lastLogin=" + lastLogin
    }
  } else if (realUser){
        u.setDescription('###' + lastLogin.getTime() + '###' + (u.getDescription() != null ? u.getDescription() : '' ) )
        println u.getId() + ':' + u.getDisplayName() + ':' + realUser + ':' + u?.getDescription()
  } else if (realUser==false){
        u.setDescription('Is not a regular user')
        println u.getId() + ':' + u.getDisplayName() + ':' + realUser + ':' + u?.getDescription()
  }
}
