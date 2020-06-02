#!/bin/bash

VERSION=1591044222

## Script to automatically determine what version of plugin needs to be downloaded
## and installs it
echo "Executing core-traditional-unbreak.sh version $VERSION"

hinit() {
    rm -f /tmp/hashmap.$1
}

hput() {
    echo "$2 $3" >> /tmp/hashmap.$1
}

hget() {
    PLUGIN_URL=$(grep "^$2 " /tmp/hashmap.$1 | awk '{ print $2 };' )
}

# is JENKINS_HOME set?
 if [[ -z "$JENKINS_HOME" ]]; then
    echo "JENKINS_HOME not set, exiting..."
    exit
else
    echo "Using JENKINS_HOME defined as $JENKINS_HOME"
fi

toolsMissing="0"
verify_command() {
  echo "Verifying command [${1}] is installed..."

  if command -v $1 >/dev/null 2>&1; then
    echo "Confirmed command [${1}] present."
  else
    echo "Command [${1}] required but not installed!"
    toolsMissing="1"
  fi
}

echo "Checking to see if required tools are present"

verify_command awk
verify_command grep 
verify_command tr

if [ "$toolsMissing" == "1" ] ; then
    echo "Required tools are missing, please install and re-run."
    exit 1
fi

echo "Checking for wget or curl...."
downloadTool=""
verify_command wget
if [ "$toolsMissing" == "0" ] ; then
    downloadTool="wget -nv --output-document=$JENKINS_HOME/plugins/cloudbees-license.jpi"
else
    toolsMissing="0"
    verify_command curl
    if [ "$toolsMissing" == "0" ] ; then
        downloadTool="curl -sS --output $JENKINS_HOME/plugins/cloudbees-license.jpi"
    fi
fi

if [ "$toolsMissing" == "1" ] ; then
    echo "curl or wget are required, please install one of these and re-run."
    exit 1
fi

#TODO Add checks to ensure that user executing script has filesystem permission to make update
#TODO ensure that downloaded plugin is installed using the right user && group

hinit versions
hput versions "9.33" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.33.1/cloudbees-license.hpi"
hput versions "9.32" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.32.1/cloudbees-license.hpi"
hput versions "9.31" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.31.1/cloudbees-license.hpi"
hput versions "9.30" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.30.1/cloudbees-license.hpi"
hput versions "9.28" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.28.1/cloudbees-license.hpi"
hput versions "9.27" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.27.1/cloudbees-license.hpi"
hput versions "9.26" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.26.1/cloudbees-license.hpi"
hput versions "9.24" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.24.1/cloudbees-license.hpi"
hput versions "9.20" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.20.1/cloudbees-license.hpi"
hput versions "9.18.1" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.18.1.1/cloudbees-license.hpi"
hput versions "9.18" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.18.0.1/cloudbees-license.hpi"
hput versions "9.17" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.17.1/cloudbees-license.hpi"
hput versions "9.13" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.13.1/cloudbees-license.hpi"
hput versions "9.11" "https://jenkins-updates.cloudbees.com/download/plugins/cloudbees-license/9.11.1/cloudbees-license.hpi"

hinit backports
hput backports "9.33.1" "backport"
hput backports "9.32.1" "backport"
hput backports "9.31.1" "backport"
hput backports "9.30.1" "backport"
hput backports "9.28.1" "backport"
hput backports "9.27.1" "backport"
hput backports "9.26.1" "backport"
hput backports "9.24.1" "backport"
hput backports "9.20.1" "backport"
hput backports "9.18.0.1" "backport"
hput backports "9.18.1.1" "backport"
hput backports "9.13.1" "backport"
hput backports "9.11.1" "backport"

hput backports "9.34" "ok"
hput backports "9.34.1" "ok"
hput backports "9.35" "ok"
hput backports "9.35.1" "ok"
hput backports "9.36" "ok"
hput backports "9.36.1" "ok"
hput backports "9.37" "ok"
hput backports "9.38" "ok"
hput backports "9.39" "ok"
hput backports "9.39.1" "ok"
hput backports "9.40" "ok"
hput backports "9.41" "ok"
hput backports "9.42" "ok"

# find the currently installed version of the cloudbees-license plugin
#echo "$JENKINS_HOME/plugins/cloudbees-license/META-INF/MANIFEST.MF"
#strip out any odd control chars!
CURRENT_PLUGIN_VERSION=$(grep Plugin-Version $JENKINS_HOME/plugins/cloudbees-license/META-INF/MANIFEST.MF | awk '{ print $2 };' | tr -d '\000-\031')
echo "CURRENT_PLUGIN_VERSION = $CURRENT_PLUGIN_VERSION"


# Check if the user does not need to upgrade (ie. 9.34 or newer already)
hget backports $CURRENT_PLUGIN_VERSION

if [ "$PLUGIN_URL" == "ok" ] ; then
    echo "Currently installed plugin version $CURRENT_PLUGIN_VERSION already supports the new license.  No upgrade nescecary"
    exit 0
fi

# Check if the  user has already upgraded
hget backports $CURRENT_PLUGIN_VERSION
if [ "$PLUGIN_URL" == "backport" ] ; then
    echo "Currently installed plugin version $CURRENT_PLUGIN_VERSION already supports the new license.  No upgrade nescecary"
    exit 0
fi

# lookup the updated plugin download url
hget versions $CURRENT_PLUGIN_VERSION

 if [[ -z "$PLUGIN_URL" ]]; then
    echo "No updated plugin exists for $CURRENT_PLUGIN_VERSION"
    echo "Please contact support"
    exit 1
fi

# backup the currently installed plugin
echo "Backing up the currently installed license plugin"
mv $JENKINS_HOME/plugins/cloudbees-license.jpi $JENKINS_HOME/plugins/cloudbees-license.bak

# now download the plugin

echo "Downloading updated plugin from $PLUGIN_URL"
$downloadTool $PLUGIN_URL

echo "Plugin updated successfully, please restart your Jenkins instance to complete the installation"
