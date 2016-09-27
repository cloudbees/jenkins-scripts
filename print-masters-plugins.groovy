/*
Author: Jean-Philippe Briend + Isaac Cohen
This script prints all the installed plugins and their version for every online Client Master.
It must be launched as a Groovy script from the CJOC server.
There is one parameter "csv" that defines whether the output should be in csv or should be formatted
*/
import com.cloudbees.opscenter.server.model.*
import com.cloudbees.opscenter.server.clusterops.steps.*

// CSV parameter defines whether the output should be a CSV or formatted
def csv=0

def retour = '\n'
def matrix = [:]
def headers = ['Plugins']

// Loop over all online Client Masters
Jenkins.instance.getAllItems(ConnectedMaster.class).eachWithIndex{ it, index ->
  headers.add(it.name)
  if (it.channel) {
    def stream = new ByteArrayOutputStream();
    def listener = new StreamBuildListener(stream);
    // Execute remote Groovy script in the Client Master
    // Result of the execution must be a String
    it.channel.call(new MasterGroovyClusterOpStep.Script("""
        result = ''
        for (plugin in Jenkins.instance.pluginManager.plugins) {
			result = result + /"\${plugin.displayName}"/ + ',' + /"\${plugin.version}"\n/
        }
       return result
    """, listener, "host-script.groovy"))
    retour = retour << "Master ${it.name}:\n${stream.toString().minus('Result: ')}"

    stream.toString().eachLine { line, count ->
      if (line?.trim()) {
        matcher = ( line =~ /"(.*)"*","(.*)"/ )

        if (matcher[0] && matcher[0].size() == 3) {
            if (!matrix[matcher[0][1]]) {
              matrix[matcher[0][1]] = []
            }
          	matrix[matcher[0][1]][index] = matcher[0][2]
        }
      }
    }
  }
}

if (csv==0) {
  printFormatted(headers, matrix)
} else {
  printCSV(headers, matrix)
}


def printCSV(headers, matrix) {

  // Print the headers
  headers.eachWithIndex{ it, index ->
    print "\"${it}\""
    def delimiter =  (index+1 == headers.size()) ? "\n" : ","
    print delimiter
  }

  //Print the plugin versions
  matrix.each{ k, v ->
    print "\"${k}\","
    v.eachWithIndex{ it, index ->
      def value = (it) ? it : ""
      print "\"${value}\""
      def delimiter =  (index+1 == v.size()) ? "\n" : ","
      print delimiter
    }
  }
}

def printFormatted(headers, matrix) {
  // Determine sizes to add padding
  int keylength = 0
  def valuelength = []

  // Start with header sizes
  headers.eachWithIndex { it, index ->
    if (index == 0) {
      keylength = headers[0].size()
    } else {
      valuelength[index-1] = headers[index-1].size()
    }
  }

  // Determine the size of the Key + values
  matrix.each{ k, v ->
    keylength = (k.size() > keylength) ? k.size() : keylength
    v.eachWithIndex{ it, index ->
      (!valuelength[index]) ? 0 : valuelength[index]
      if (it) {
        valuelength[index] = (it.size() > valuelength[index]) ? it.size() : valuelength[index]
      } else {
        //any null values should be set
        v[index] = ""
      }
  }
}

// Print the headers
headers.eachWithIndex{ it, index ->
  def header =  (index == 0) ? it.padRight(keylength) : it.padRight(valuelength[index-1])
  print header
  print " | "
}
print "\n"

//Print the plugin versions
 matrix.each{ k, v ->
   print k.padRight(keylength) + " | "

   v.eachWithIndex{ it, index ->
     print it.padRight(valuelength[index])
     print " | "
   }
   print "\n"
 }
}

return 0
