/**
Create a Managed Master on PSE using code
**/
import com.cloudbees.jce.masterprovisioning.mesos.MesosMasterProvisioning
import com.cloudbees.opscenter.server.model.ManagedMaster 
import com.cloudbees.jce.masterprovisioning.mesos.HealthCheckConfiguration
import com.cloudbees.opscenter.server.properties.ConnectedMasterOwnerProperty
import com.cloudbees.opscenter.server.properties.ConnectedMasterLicenseServerProperty;

ManagedMaster instance = Jenkins.getInstance().createProject(ManagedMaster.class,'TestFromCode')
MesosMasterProvisioning mesos = new MesosMasterProvisioning()
mesos.setDomain('testfromcode')
mesos.setDisk(1)
mesos.setMemory(512)
mesos.setRatio(0.25)
mesos.setCpus(0.2)
mesos.setSystemProperties('''com.cloudbees.tiger.plugins.palace.PalaceCloud.maxInProvisioning=25
hudson.model.DirectoryBrowserSupport.CSP=""''')
HealthCheckConfiguration hc = new HealthCheckConfiguration()
hc.setGracePeriodSeconds(600)
hc.setIntervalSeconds(60)
mesos.setHealthCheckConfiguration(hc)
mesos.setImage('CloudBees Jenkins Enterprise 2.19.4.2')
instance.setConfiguration(mesos)
instance.setDescription('TestFromCode Description')
instance.setDisplayName('Test From Code Display Name')
instance.getProperties().add(new ConnectedMasterOwnerProperty('owners', Integer.MAX_VALUE))
instance.getProperties().replace(new ConnectedMasterLicenseServerProperty(new ConnectedMasterLicenseServerProperty.FloatingExecutorsStrategy()));
instance.save()
instance.provisionAndStartAction()
