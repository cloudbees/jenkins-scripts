/*
The script checks the users who have not logged in for a certain number of days and deletes them if the DELETE_USERS
flag is set to true. This is to prune users that get created automatically on SAML login, but are never deleted.

The script defines two constants:
 - DAYS_SINCE_LOGIN is the number of days since a user has last logged in that determines if their account is deleted.
 - DELETE_USERS is a boolean flag indicating whether or not to actually delete the users.

The script  loops through all the user accounts and checks if the user has a valid SAML login.
If a user has a valid login, it checks the last login timestamp to see if it is older than the last valid date. 
If it is, it adds the user ID to a list of deleted users
 */

import jenkins.security.*

long DAYS_SINCE_LOGIN = 90
boolean DELETE_USERS = false


long lastValidTstamp = System.currentTimeMillis() - (DAYS_SINCE_LOGIN * 86400 * 1000)
def lastValidDate = new Date(lastValidTstamp)
def deletedUsers = new LinkedList<String>()
println "Checking users that didn't login since: ${lastValidDate}; DELETE_USERS=${DELETE_USERS}"
User.getAll().each{ u ->
    def prop = u.getProperty(LastGrantedAuthoritiesProperty)
    def propSamlLoginDetails = u.getProperty(org.jenkinsci.plugins.saml.user.LoginDetailsProperty)
    def realUser = false
    def timestampLGA = null

    if (prop) {
        realUser=true
        timestampLGA = new Date(prop.timestamp).toString()
    }

    if (realUser){
        def samltstamp = propSamlLoginDetails.getLastLoginTimestamp()
        if (samltstamp != 0) {
            println "${u.getId()}:${u.getDisplayName()}:Jenkins-User:SAMLLastLogin=${propSamlLoginDetails.getLastLoginDate()}"
            if ( samltstamp < lastValidTstamp ) {
                print "Last login older than last valid login."
                deletedUsers.add(u.getId())
                if (DELETE_USERS) {
                    u.delete()
                    println "User ${u.getId()} deleted"
                } else {
                    println "User ${u.getId()} would be deleted"
                }
            }
        } else {
            println "${u.getId()}:${u.getDisplayName()}:Jenkins-User:SAMLLastLogin=${samltstamp}"
        }
    } else {
        println "${u.getId()}:${u.getDisplayName()}:No-Jenkins-User"
    }
}
println "Deleted users: ${deletedUsers}"

