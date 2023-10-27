import com.cloudbees.jenkins.plugins.todeclarative.converter.maven.MavenJobToDeclarativeConverter
import hudson.ExtensionList
import hudson.maven.MavenModuleSet
import io.jenkins.plugins.todeclarative.converter.api.ConverterRequest
import io.jenkins.plugins.todeclarative.converter.api.ConverterResult
import jenkins.model.Jenkins
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTPipelineDef
import com.cloudbees.hudson.plugins.folder.Folder
import org.apache.commons.cli.Option

import java.util.regex.Pattern

/**
 * Groovy script to do a bulk migration of maven-job type jobs to pipeline declarative job
 *
 * The script can migrate all jobs of a Jenkins instance (default)
 * The script can migrate jobs of a folder using the folder name (using variable folderName)
 * The script can migrate jobs filtering on Job name Wildcard (using variable jobWildcard)
 * The script can migrate jobs of a folder using the folder name and filter job of this folder using wildcard (using both variables folderName and jobWildcard)
 */

//Define variable to filter jobs
//To filter jobs from the folder with name folderName
def folderName = ""
//To filter jobs using this wildcard
def jobWildcard = ""

//Use converter class from cloudbees-maven-migration-assistant plugin
def mavenJobToDeclarativeConverter = ExtensionList.lookupSingleton(MavenJobToDeclarativeConverter.class);
def converterResult = new ConverterResult().modelASTPipelineDef(new ModelASTPipelineDef(null))

def folder = Jenkins.get()
if (!folderName.isEmpty()) {
    folder = Jenkins.get().getItemByFullName(folderName, Folder.class)
    if (!folder) {
        println("Could not find a folder named ${folderName}")
        return false
    }
}
Pattern pattern = null
if (!jobWildcard.isEmpty()) {
    if (jobWildcard instanceof Pattern) {
        pattern = jobWildcard
    } else {
        pattern = Pattern.compile(jobWildcard)  //Will Throw exception if not valid
    }
}
print("Migrating all maven jobs in ${folder.fullName} ")
pattern != null ? println("matching ${pattern}") : println("")
int count = 0
folder.getAllItems(MavenModuleSet.class).each { job ->
    if ((pattern != null && job.name ==~ pattern) || pattern == null) {
        println("Migrating ${job.fullName}")
        count++
        mavenJobToDeclarativeConverter.convert(new ConverterRequest().job(job).createNewProject(true), converterResult, job)
        //println(converterResult.getWarnings())
    }
}
println("Migration complete of ${count} jobs")