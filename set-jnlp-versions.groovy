import jenkins.model.*

// Available protocols are: JNLP-connect, JNLP2-connect, JNLP4-connect, Ping
Set enabledProtocols = [
        "Ping",
        "JNLP4-connect"
]

Jenkins.instance.setAgentProtocols(enabledProtocols)
