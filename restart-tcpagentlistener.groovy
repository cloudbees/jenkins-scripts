import hudson.model.*

//This script will get the TcpSlaveAgentListener thread and
//* If it exists, it shuts it down and set it to null
//* After that, it restarts the thread using Java reflection
 
def jenkins = jenkins.model.Jenkins.get()
if(jenkins.tcpSlaveAgentListener != null) {
    jenkins.tcpSlaveAgentListener.shutdown()
    jenkins.tcpSlaveAgentListener=null
}

Class<?> innerClazz = jenkins.model.Jenkins.class
java.lang.reflect.Method privateMethod = innerClazz.getDeclaredMethod("launchTcpSlaveAgentListener");
privateMethod.setAccessible(true);
privateMethod.invoke(jenkins);