# CLI Disable/Enable Scripts
This folder contains scripts to disable and enable the Command Line Interface (CLI) access over HTTP/Websocket and SSH without having to perform a restart every time that we want to reenable. 

* `cli-toggle-disable.groovy` disables the CLI while storing all needed parameters and objects in memory in the jenkins instance. This way the CLI can be enabled if it's needed temporarily.
* `cli-toggle-disable-permanent.groovy` writes a copy of the previous script to the post-init folder of the jenkins, so that it is always run on start-up and CLI is disabled but ready te reenabled if needed.
* `cli-toggle-enable.groovy` enables the CLI again restoring the previously saved configuration. This should only be done to perform a specific action and then run the disabling script again.

These can be easily run as cluster operations in the same way that it's possible to do with [Disable Jenkins CLI across all controllers](https://docs.cloudbees.com/docs/cloudbees-ci-kb/latest/operations-center/disable-jenkins-cli-across-all-controllers "Disable Jenkins CLI across all controllers") but using tthe scripts defined here instead.


Please note that these are just a hacky workaround and not intended for long-term usage. If you are impacted by [CloudBees Security Advisory 2024-01-24](https://www.cloudbees.com/security-advisories/cloudbees-security-advisory-2024-01-24 "CloudBees Security Advisory 2024-01-24") you should upgrade your system as soon as possible.

