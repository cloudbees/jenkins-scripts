// This outputs the user token data as a csv
// USER,TOKEN_LAST_USED,IS_LEGACY,TOKEN_NAME

import org.acegisecurity.*
import jenkins.security.*
import java.util.Date
println "USER,TOKEN_LAST_USED,IS_LEGACY,TOKEN_NAME"
User.getAll().each{ u ->
  def tProp = u.getProperty(jenkins.security.ApiTokenProperty)
  // https://javadoc.jenkins.io/jenkins/security/ApiTokenProperty.TokenInfoAndStats.html
  tProp.tokenList.each { println  "${u}, ${it.lastUseDate?.format("yyyy-MM-dd HH:mm:ss")}, ${it.isLegacy}, ${it.name.replaceAll(',','_COMMA_')} " }
}
null