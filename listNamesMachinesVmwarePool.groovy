import hudson.plugins.vmware.*
import hudson.plugins.vmware.staticpool.*
import hudson.plugins.vmware.folder.*
  
  Root.get().getMachineCenters().each{ mc ->
    println 'Machine Center name : ' + mc.name
    mc.getPools().each{ mp ->
      println 'Machine Pool : ' + mp.name
      if(mp instanceof StaticMachinePool){
        ((StaticMachinePool)mp).getMachines().each{ m ->
          println 'Machine Name : ' + m.name
        }
      } else if (mp instanceof FolderMachinePool){
		((FolderMachinePool)mp).getMachines().each{ m ->
          println 'Machine Name : ' + m.name
        }
      } else {
        println 'Machine Pool type unknown'
      }
    }
  }