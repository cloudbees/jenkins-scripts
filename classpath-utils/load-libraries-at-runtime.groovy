import java.lang.reflect.Method;
import groovy.sql.Sql
// Grab the dependencies
@Grapes([
    @Grab('mysql:mysql-connector-java:5.1.6')
])
// Get the locations
def grape = groovy.grape.Grape.getInstance()
def r = grape.listDependencies(this.getClass().getClassLoader())
def jarURIs = grape.resolve(r[0])
// Add to systemClassLoader
try {
    jarURIs.each {
        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        println "Adding jar: ${it}"
        method.invoke(classLoader, it.toURL());
    }
} catch (Exception e) {
    throw new RuntimeException("Unexpected exception", e);
}
// Run your code
def sql=Sql.newInstance("JDBC_URL", "USERNAME", "PASSWORD", "com.mysql.jdbc.Driver")
sql.firstRow('SELECT * FROM INFORMATION_SCHEMA.COLUMNS LIMIT 10')



// Script isolation

scriptToRun = '''
...insert the script from above here...
'''
GroovyClassLoader groovyCL = new GroovyClassLoader();
Class groovyShellClazz = groovyCL.loadClass(GroovyShell.class.getName());
Object groovyShellObj = groovyShellClazz.newInstance();
java.lang.reflect.Method evaluateMethod = groovyShellClazz.getMethod("evaluate", String.class);
evaluateMethod.invoke(groovyShellObj, scriptToRun);



// Classpath List

def printClassPath(classLoader) {
    println "$classLoader"
    urls = []
    try {
        urls = classLoader.getURLs()
    } catch (def e) {
        // ignore
    }
    urls.each {url->
        println "- ${url.toString()}"
        this.class.classLoader.addURL(url);
    }
    if (classLoader.parent) {
        printClassPath(classLoader.parent)
    }
}
printClassPath this.class.classLoader


// Classpath List

def printClassPath(classLoader) {
    println "Hi $classLoader"
    urls = []
    try {
        urls = classLoader.getURLs()
    } catch (def e) {
        // ignore
    }
    urls.each {url->
        println "- ${url.toString()}"
        this.class.classLoader.addURL(url);
    }
    if (classLoader.parent) {
      println "Parent: ${classLoader.parent} (parent of $classLoader)"
        printClassPath(classLoader.parent)
    }
}
printClassPath this.class.classLoader