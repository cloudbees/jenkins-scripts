/*
Description: Can be run from the Script Console to identify available job parameters in your environment as well as what plugin/class they are provided by
*/

ParameterDefinition.all().each {
    println 'Name: ' + it.getDisplayName() + ' ' + (it.getPlugin() ?: 'Plugin:core') + ' Class: ' + it.getJsonSafeClassName()
}
return