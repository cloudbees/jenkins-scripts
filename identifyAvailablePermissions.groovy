/*
Description: Can be run from the Script Console to identify all permissions available in your environment including their description
*/

import hudson.security.*

Permission.getAll().each {
    println 'ID: ' + it.getId() + ' Name: ' + it.name + ' Description: ' + (it.description ?: 'None')
}
return