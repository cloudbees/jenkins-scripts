count = jenkins.model.Jenkins.instance.items.size();

println "TOP LEVEL ITEMS IN THE CONTROLLER: ${count}"
println "------------------"

findItems(jenkins.model.Jenkins.instance.items.findAll(), "top-level");

def findItems(items, parentName) {
  for (item in items) {
    println "${parentName}/${item.name}"
    if (item instanceof com.cloudbees.hudson.plugins.folder.Folder) {
      println "items in ${parentName}/${item.name}: ${item.getItems().size()}";
      count += item.getItems().size();
      findItems(item.getItems(), parentName + "/" + item.name);
    }
  }
}


println "------------------"
println "TOTAL ITEMS IN THE CONTROLLER: ${count}"