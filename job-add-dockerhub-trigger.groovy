import org.jenkinsci.plugins.dockerhub.notification.DockerHubTrigger
import org.jenkinsci.plugins.dockerhub.notification.opt.impl.TriggerOnSpecifiedImageNames
import org.jenkinsci.plugins.dockerhub.notification.opt.TriggerOption

List<String> imageNameOptions = [ 'kmadel/mobile-deposit-api']*.toString()
TriggerOption triggerOption = new TriggerOnSpecifiedImageNames(imageNameOptions);
def triggerOptions = [triggerOption]
DockerHubTrigger dockerHubTrigger = new DockerHubTrigger(triggerOptions);

def job = Jenkins.instance.getItemByFullName("beedemo-mobile/mobile-deposit-ui/master")
dockerHubTrigger.start(job, true);
job.addTrigger(dockerHubTrigger);
job.save();
