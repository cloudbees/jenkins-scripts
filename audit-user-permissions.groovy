/*
    Copyright (c) 2015-2018 Sam Gleske - https://github.com/samrocketman/jenkins-script-console-scripts
    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to
    use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
    the Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
/**
  This Jenkins script runs through accounts in Jenkins and highlights the
  permissions of each user.  This script can be run from a Jenkins job via
  "Execute system Groovy script" or the script console.
  This script is useful for identifying users who might have more access than
  they need (web search "principle of least privilege").  This can also help
  identify users who should be deleted.
  Notes:
  - This script only reads user privileges and does not do anything destructive
    (i.e. it will not delete users).
  - This audit list only contains users which exist in Jenkins.
  - Accounts from security realm groups will have a Jenkins user created only when
    they log in the first time.  Security realm users who have not logged in
    but would still get permissions are not counted here.
  - Jenkins automatically creates users from SCM authors in order to track
    their work across jobs and repositories.  Therefore, it's possible accounts
    will exist in Jenkins which are not available in the security realm.  These
    are not a concern.
  - Groovy bindings `out` and `build` are available in Jenkins jobs running via
    the "Execute system Groovy script" build step.  It serves as a reliable
    means of detecting whether or not this script is run from the script
    console or a job.

   Please note that this script may report users who are not able to login
   due to restrictions imposed by the configured security realm. Only the 
   authorization configuration is consulted.


 */
import hudson.model.Item
import hudson.model.User
import hudson.security.Permission
import hudson.security.PermissionGroup
import hudson.security.ACL
import hudson.tasks.Mailer
import jenkins.model.Jenkins
import org.acegisecurity.Authentication
import org.acegisecurity.userdetails.UsernameNotFoundException

if(!binding.hasVariable('writeToFile')) {
    writeToFile = false
}
if(!binding.hasVariable('includeCoreAccounts')) {
    includeCoreAccounts = true
}
if(writeToFile in String) {
    writeToFile = ('true' == writeToFile.toLowerCase())
}
if(includeCoreAccounts in String) {
    includeCoreAccounts = ('true' == includeCoreAccounts.toLowerCase())
}
if(!(writeToFile instanceof Boolean)) {
    throw new Exception('ERROR: writeToFile must be a boolean.')
}
if(!(includeCoreAccounts instanceof Boolean)) {
    throw new Exception('ERROR: includeCoreAccounts must be a boolean.')
}

/**
  This class simplifies getting details about a user and their access in a CSV
  format.  Not only will it get permissions but it will smartly populate notes
  about a user based on known facts and issues.
 */
class UserCSV {
    private String id
    private String email
    private String fullName
    private Authentication impersonate
    private boolean coreAccount = false
    private boolean builtinAccount = false
    private static ACL acl = Jenkins.instance.authorizationStrategy.rootACL
    private static List<Permission> permissions = PermissionGroup.all*.permissions.flatten().findAll { Permission p ->
        !(this.displayPermission(p) in ['Overall:ConfigureUpdateCenter', 'Overall:RunScripts', 'Overall:UploadPlugins']) &&
        !this.displayPermission(p).startsWith('N/A')
    }
    private static Permission admin = permissions.find { it.name == 'Administer' }
    private List<String> notes = []
    private List<String> authorities = []
    private String global_permissions = 'No permissions'
    private String item_permissions = 'No permissions'
    /**
      A new instance determines a user's access to Jenkins.
     */
    def UserCSV(User u) {
        this.id = u.id ?: ''
        this.email = this.getEmail(u)
        this.fullName = u.fullName ?: ''
        this.authorities = u.authorities
        switch(id) {
            case 'admin':
                this.notes << 'This built-in account is the first account created in the Jenkins 2.0 setup wizard when you set up Jenkins the first time.'
                this.notes << 'This account should be deleted because it is a default Jenkins account.'
                break
            case 'anonymous':
                this.notes << 'This core pseudo account is for users who are not authenticated with Jenkins a.k.a. all anonymous users.'
                break
            case 'authenticated':
                this.notes << 'This core pseudo account is for any user who has authenticated with Jenkins a.k.a. all logged in users.'
                break
        }
        this.coreAccount = (id in ['anonymous', 'authenticated']) as Boolean
        if(coreAccount) {
            this.notes << 'This account cannot be deleted.'
        } else {
            try {
                //for performance reasons, only try to impersonate a user once when this object is first instantiated
                this.impersonate = u.impersonate()
            } catch(UsernameNotFoundException e) {
                if(this.id != 'admin') {
                    this.notes << 'This user does not exist in the security realm.'
                    this.notes << 'It is an automatically created account from Jenkins tracking source code manager (SCM) authors.'
                    this.notes << 'This is normal Jenkins behavior and can be ignored.'
                    this.notes << 'It is safe to delete this account, but it will be recreated when the user shows up as an SCM author again.'
                }
                builtinAccount = true
            }
        }
        this.global_permissions = this.getGlobalPermissions()
        if(supportsItemPermissions()) {
            this.item_permissions = getItemPermissions()
        }
        if(this.global_permissions == 'No permissions' && this.item_permissions == 'No permissions' && !builtinAccount && !coreAccount) {
            this.notes << 'This user exists in the security realm and their account was created upon first login; however, they were not granted any permissions.'
            this.notes << 'This account should be deleted.'
        }
        if((this.global_permissions + this.item_permissions).contains('Job:Configure')) {
            this.notes << 'This user can elevate permissions of other users within jobs they can configure.'
            if(Jenkins.instance.numExecutors > 0) {
                this.notes << "This user can trivially elevate themselves to ${this.displayPermission(admin)} because jobs can be configured to run on the master."
            }
        }
    }
    private static Boolean supportsItemPermissions() {
        Jenkins.instance.authorizationStrategy.class.simpleName == 'ProjectMatrixAuthorizationStrategy'
    }
    private static String displayPermission(Permission p) {
        "${p.group.title}:${p.name}".toString()
    }
    private String getEmail(User u) {
        if(u.getProperty(Mailer.UserProperty)) {
            (u.getProperty(Mailer.UserProperty).address ?: '').toString()
        } else {
            ''
        }
    }
    private String getItemPermissions() {
        if(coreAccount || builtinAccount || this.global_permissions == 'Overall:Administer') {
            this.item_permissions
        } else {
            Set discovered_permissions = []
            //find additional permissions granted in items e.g. folders, jobs, etc
            Jenkins.instance.getAllItems(Item.class).findAll {
                it.properties.find { it.class.simpleName == 'AuthorizationMatrixProperty' }
            }.each { Item item ->
                item.properties.find {
                    it.class.simpleName == 'AuthorizationMatrixProperty'
                }.with { item_auth ->
                    item_auth.getGrantedPermissions().keySet().findAll { Permission p ->
                        !(displayPermission(p) in discovered_permissions)
                    }.each { Permission p ->
                        if(item_auth.acl.hasPermission(this.impersonate, p)) {
                            discovered_permissions << displayPermission(p)
                        }
                    }
                }
            }
            discovered_permissions.sort().join(',') ?: this.item_permissions
        }
    }
    private String getGlobalPermissions() {
        if(coreAccount) {
            this.notes << 'Permissions cannot be determined on core accounts because they cannot be used as a normal user.'
            'No permissions'
        } else if(builtinAccount) {
            'No permissions'
        } else if(this.acl.hasPermission(this.impersonate, admin)) {
            this.notes << 'Admins can affect any infrastructure Jenkins integrates with including decrypting configured credentials.'
            displayPermission(admin)
        } else {
            permissions.findAll { Permission p ->
                p != admin && this.acl.hasPermission(this.impersonate, p)
            }.collect {
                displayPermission(it)
            }.join(',') ?: this.global_permissions
        }
    }
    public static String getCSVHeader() {
        List<String> csvList = [
            'User',
            '"Full Name"',
            'Email',
            '"Notes"',
            '"Global Permissions"',
            '"Authorities/Groups"'
        ]
        if(supportsItemPermissions()) {
            csvList << '"Additional permissions granted within folders or jobs"'
        }
        csvList.join(', ')
    }
    public String getCSV() {
        List<String> csvList = [
            this.id,
            "\"${this.fullName}\"",
            this.email,
            "\"${this.notes.join('  ')}\"",
            "\"${this.global_permissions}\"",
            "\"${this.authorities.join(' ')}\""
        ]
        if(supportsItemPermissions()) {
            csvList << "\"${this.item_permissions}\""
        }
        csvList.join(', ')
    }
}

/**
  Print out the result to the script console or log a line to the job console
  output.  It also optionally writes the line to a writer (used in other
  functions to write to a file).
 */
void writeLine(Writer writer = null, String line) {
    if(writer) {
        writer.write line + '\n'
    }
    if(binding.hasVariable('out')) {
        out.println line
    } else {
        println line
    }
}

/**
  optionalWriter is a Groovy binding which provides flexibility in executing a
  closure while writing output to a file, script console output, or the output
  to a Jenkins job.
 */
optionalWriter = { String file = null, Closure c ->
    if(file) {
        new File(file).withWriter('UTF-8') { writer ->
            c(writer)
        }
    } else {
        c(null as Writer)
    }
}

String separator = '-' * 80
writeLine 'Start of CSV'
writeLine separator
String outputFile
if(binding.hasVariable('out') && binding.hasVariable('build') && writeToFile) {
    outputFile = "${build.workspace}/audit-user-permissions.csv"
}
optionalWriter(outputFile) { writer ->
    writeLine(writer, UserCSV.getCSVHeader())
    User.all.each { User u ->
        if(includeCoreAccounts || !(u.id in ['anonymous', 'authenticated'])) {
            writeLine(writer, new UserCSV(u).getCSV())
        }
    }
}
writeLine separator