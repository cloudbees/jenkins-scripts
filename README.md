# Handy Groovy Scripts for Jenkins and CloudBees Jenkins Platform

The scripts in this repository can be run in Jenkins script console: Manage Jenkins > Script Console. Most work on any Jenkins Enterprise instance, while some are specific to CloudBees Jenkins Operations Center (CJOC).

## Count CJOC JSON

The Count CJOC JSON script runs on CJOC's script console. The script captures the executor, cloud, and CPU core count attached to the platform. The script script dynamically creates a Cluster Operation and executes the operation across all Client Masters connected to CJOC.

**Running the script**

1. Log into CloudBees Jenkins Operations Center (CJOC)

![Login into CJOC](images/1-login.png)

2. Click 'Manage Jenkins' on the left-hand panel, followed by the 'Script Console' link
![Manage Jenkins](images/2-manage-jenkins.png)

3. Copy + Paste [the script](https://github.com/cloudbees/jenkins-scripts/blob/master/count-cjoc-json.groovy) into the console window and click the Run button
![Script Console](images/3-script-console.png)

4. Copy + Paste the output into a file and send it to the CloudBees team
![Result](images/4-result.png)
