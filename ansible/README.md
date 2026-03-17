# create-jenkins-agent README.md

## Description

Create a jenkins agent through automation (Ansible)

## Commands

Sample commands

* Prompt for sudo password

`ansible-playbook create-jenkins-agent.yml -i myhost, -K --extra-vars "jenkins_url=$IP:$PORT"`

* Or store password in ~/.ansible.hosts

`ansible-playbook create-jenkins-agent.yml -i ~/.ansible.hosts --extra-vars "jenkins_url=$IP:$PORT"`

* pass variables from command line

`ansible-playbook create-jenkins-agent.yml -i ~/.ansible.hosts --extra-vars "jenkins_url=$IP:$PORT node_name=local-agent num_executors=1 user=jenkins remote_root=/home/jenkins"`

* run only tags

`ansible-playbook create-jenkins-agent.yml -i ~/.ansible.hosts --extra-vars "jenkins_url=$IP:$PORT node_name=local-agent num_executors=1 user=jenkins remote_root=/home/jenkins" --tags capture-secret,debug`

## Helpers

* `~/.ansible.cfg`

```
[defaults]
inventory               = ~/.ansible.hosts
deprecation_warnings    = False
command_warnings        = False
```

* `~/.ansible.hosts`

```
[all]
myhost ansible_sudo_pass='mypassword'
```
