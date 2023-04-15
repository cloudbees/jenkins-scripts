import hudson.model.User
def count = 0
User.getAll().each { user ->
   println user
   count++
}
println "Total Users :" +count