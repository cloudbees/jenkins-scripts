import java.util.Hashtable
import javax.naming.Context
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.*
import javax.naming.ldap.*
import jenkins.model.Jenkins;
import nectar.plugins.rbac.strategy.*;
import hudson.security.*;
import nectar.plugins.rbac.groups.*;
import nectar.plugins.rbac.roles.*;


try {
        String ldapAdServer = "ldap://192.0.2.36:389"
        String ldapSearchBase = "dc=example,dc=com"

        String ldapUsername = "CN=tesla,CN=Users,DC=example,DC=com"
        String ldapPassword = "Password12"

  		String searchFilter = "(& (cn=*) (objectclass=group))"


        Hashtable<String, Object> env = new Hashtable<String, Object>()
        env.put(Context.SECURITY_AUTHENTICATION, "simple")
        if(ldapUsername != null) {
            env.put(Context.SECURITY_PRINCIPAL, ldapUsername)
        }
        if(ldapPassword != null) {
            env.put(Context.SECURITY_CREDENTIALS, ldapPassword)
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
        env.put(Context.PROVIDER_URL, ldapAdServer)

  		DirContext ctx = new InitialDirContext(env);

  		SearchControls searchControls = new SearchControls()
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE)

        NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls)

//RBAC integration
        RoleMatrixAuthorizationStrategyImpl strategy = RoleMatrixAuthorizationStrategyImpl.getInstance()
        RoleMatrixAuthorizationConfig config = RoleMatrixAuthorizationPlugin.getConfig()
        GroupContainer container = GroupContainerLocator.locate(Jenkins.getInstance())

        List<Group> groups = config.getGroups();
        Set<String> groupNames = new HashSet<String>()
        groups.each{ g -> groupNames.add(g.name) }

        SearchResult searchResult = null
        List<String> ldapGroups = new ArrayList<String>()
  		results.each{ result ->
        	String name = result.getAttributes().get('name')
            if(!groupNames.contains(name)){
                println 'Group to Add ' + name
                Group group = new Group(container, name)
                container.addGroup(group)
            } else {
                println 'Group exists ' + name
                groupNames.remove(name)
            }
        }

        println 'Groups not in LDAP '
        groupNames.each{
            println '\t' + it
        }
} catch (NamingException e) {
    println("Problem getting attribute:" + e.getMessage())
}
