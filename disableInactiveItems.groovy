import com.cloudbees.jenkins.plugins.inactive.ItemsAnalyzer;
import com.cloudbees.jenkins.plugins.inactive.InactiveItemsConfiguration;
import com.cloudbees.jenkins.plugins.inactive.model.InactiveItem;
import java.time.LocalDate;
import java.util.Set;

//Manage Jenkins-> System->Inactive Items Analysis -> Days before Inactivity
int DaysBeforeConsideredInactive = InactiveItemsConfiguration.get().getDaysBeforeConsideredInactive();
Set<InactiveItem> inactiveItems = ItemsAnalyzer.getInactiveItems(Jenkins.instance, DaysBeforeConsideredInactive, LocalDate.now());

inactiveItems.each{ inactiveItem ->
	item = Jenkins.instance.getItemByFullName(inactiveItem.getFullName())
	item.makeDisabled(true)
}
return;