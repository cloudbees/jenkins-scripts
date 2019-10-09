import com.cloudbees.jenkins.plugins.nodesplus.OwnerNodeProperty

String nodeOwner  = "node-owner"
boolean onOnline = true // Send email when connected
boolean onOffline = true // Send email when disconnected
boolean onLaunchFailure = true // Send email on every launch failure
boolean onTemporaryOfflineApplied = true // Send email when temporary off-line mark applied
boolean onTemporaryOfflineRemoved = true // Send email when temporary off-line mark removed
boolean onFirstLaunchFailure = true // Send email on first launch failure

jenkins.model.Jenkins.instance.nodes.each { agent ->
	agent.getNodeProperties().replace(new OwnerNodeProperty(nodeOwner, onOnline, onOffline,
			onLaunchFailure, onTemporaryOfflineApplied, onTemporaryOfflineRemoved, onFirstLaunchFailure));
	agent.save()
}