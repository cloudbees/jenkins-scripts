/**
@Author kuisathaverat
list and mark all real users, those that have LastGrantedAuthoritiesProperty because them logen one time.
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

  def date = new Date()
  if(u.getDescription() != null && u.getDescription().startsWith("###") && realUser){
    def timestamp = (u.getDescription() =~ /###(.+)###/)[ 0 ][ 1 ]
    try{
    date = new Date(java.lang.Long.valueOf(timestamp))
    println u.getId() + ":" + u.getDisplayName() + ':' + realUser + ':' + u.getDescription() + ':creationDate=' + date + ":lastLogin=" + lastLogin
    } catch( Exception e){
 println "ERROR:" + u.getId() + ":" + u.getDisplayName() + ':' + realUser + ':' + u.getDescription() + ':creationDate=' + timestamp + ":lastLogin=" + lastLogin
}
  } else if (realUser){
    u.setDescription('###' + date.getTime() + '###' + (u.getDescription() != null ? u.getDescription() : '' ) )
    println u.getId() + ':' + u.getDisplayName() + ':' + realUser + ':' + u?.getDescription() + ':creationDate=' + date
  } else if (realUser==false){
    u.setDescription('Is not a regular user')
    println u.getId() + ':' + u.getDisplayName() + ':' + realUser + ':' + u?.getDescription() + ':creationDate=' + date
  }
} 
