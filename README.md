# Handy Groovy Scripts for Jenkins and CloudBees Jenkins Platform

The scripts in this repository can be run in Jenkins script console: Manage Jenkins > Script Console. Most work on any Jenkins Enterprise instance, while some are specific to CloudBees Jenkins Operations Center (CJOC).

## Count CJOC JSON

The Count CJOC JSON script is intended to run on CJOC's script console. It is intended to assess the executor, cloud, and CPU core count attached to the platform. The script script dynamically creates a Cluster Operation and executes the operation across all Client Masters connected to CJOC.


