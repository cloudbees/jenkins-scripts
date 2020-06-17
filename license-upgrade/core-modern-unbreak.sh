#!/bin/bash
VERSION=1591273737
## Script to automatically determine what version of plugin needs to be downloaded
## and installs it

## Uncomment these lines if you get errors about invalid ssl certificates
#WGET_OPTIONS="--no-check-certificate"
#CURL_OPTIONS="-k"

echo "Executing core-modern-unbreak.sh version $VERSION"

#TODO: Verify that this will always be the location
PLUGIN_ROOT=/var/jenkins_home/plugins
SELECTOR="com.cloudbees.cje.tenant"

hinit() {
    rm -f /tmp/hashmap.$1
}

hput() {
    echo "$2 $3" >> /tmp/hashmap.$1
}

hget() {
    PLUGIN_URL=$(grep "^$2 " /tmp/hashmap.$1 | awk '{ print $2 };' )
}

# is PLUGIN_ROOT set?
 if [[ -z "$PLUGIN_ROOT" ]]; then
    echo "PLUGIN_ROOT not set, exiting..."
    exit
else
    echo "Using PLUGIN_ROOT defined as $PLUGIN_ROOT"
fi

toolsMissing="0"
verify_command() {
  echo "Verifying command [${1}] is installed..."

  if command -v $1 >/dev/null 2>&1; then
    echo "Confirmed command [${1}] present."
  else
    echo "Command [${1}] ${2}"
    toolsMissing="1"
  fi
}

echo "Checking to see if required tools are present"
echo ""

verify_command awk "awk is required, please install it and re-run this script"
verify_command grep "grep is required, please install it and re-run this script"
verify_command tr "tr is required, please install it and re-run this script"
verify_command kubectl "kubectl is required, please install it and re-run this script"

if [ "$toolsMissing" == "1" ] ; then
    echo "Required tools are missing, please install and re-run."
    exit 1
fi

echo "Checking for wget or curl...."
downloadTool=""
verify_command wget "wget is not installed, checking for curl"
if [ "$toolsMissing" == "0" ] ; then
    downloadTool="wget -nv $WGET_OPTIONS --output-document=./cloudbees-license.jpi"
else
    toolsMissing="0"
    verify_command curl "curl is not installed"
    if [ "$toolsMissing" == "0" ] ; then
        downloadTool="curl -sS $CURL_OPTIONS --output ./cloudbees-license.jpi"
    fi
fi

if [ "$toolsMissing" == "1" ] ; then
    echo "curl or wget are required, please install one of these and re-run."
    exit 1
fi

echo ""

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

# get all pods running Cloudbees Jenkins
#cbpods=$(kubectl get pods --selector=$SELECTOR -o jsonpath='{.items[*].metadata.name}' --all-namespaces)
cbpods=$(kubectl get pods --selector=com.cloudbees.cje.tenant -o jsonpath='{range .items[*]}{.metadata.name}{"|"}{.metadata.namespace}{" "}{end}' --all-namespaces)
echo "Checking all pods..."

#echo $cbpods

# loop over pods
for pod in `echo ${cbpods}`; do  
	echo "-----------------"
	set -- `echo $pod | tr '|' ' '`
    POD_NAME=$1
    POD_NAMESPACE=$2

	echo "pod name $POD_NAME"
	echo "pod ns $POD_NAMESPACE"

	echo "Checking pod $POD_NAME"

	# find the currently installed version of the cloudbees-license plugin
	# strip out any odd control chars!
	CURRENT_PLUGIN_VERSION=$(kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- cat /tmp/jenkins/plugins/cloudbees-license/META-INF/MANIFEST.MF    | grep "Plugin-Version" | awk '{ print $2 };' | tr -d '\000-\031')
	echo "Current plugin version is [$CURRENT_PLUGIN_VERSION]"
	
	# Check if the user does not need to upgrade (ie. 9.34 or newer already)
	hget backports "$CURRENT_PLUGIN_VERSION"

	if [ "$PLUGIN_URL" == "ok" ] ; then
		echo "Currently installed plugin version $CURRENT_PLUGIN_VERSION already supports the new license.  No upgrade necessary"
		continue
	fi

	# Check if the  user has already upgraded
	hget backports "$CURRENT_PLUGIN_VERSION"
	if [ "$PLUGIN_URL" == "backport" ] ; then
		echo "Currently installed plugin version $CURRENT_PLUGIN_VERSION already supports the new license.  No upgrade necessary"
		continue
	fi

	# lookup the updated plugin download url
	hget versions "$CURRENT_PLUGIN_VERSION"

	if [[ -z "$PLUGIN_URL" ]]; then
		echo "No updated plugin exists for $CURRENT_PLUGIN_VERSION"
		echo "Please contact support@cloudbees.com"
		continue
	fi

	# try to determine the current fileowner
    file_meta=($(kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- ls -ld "$PLUGIN_ROOT/cloudbees-license.jpi"))
    JENKINS_USER="${file_meta[2]}"
	echo "Using JENKINS_USER defined as $JENKINS_USER"

    # try to determine the current filegroup
    file_meta=($(kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- ls -ld "$PLUGIN_ROOT/cloudbees-license.jpi"))
    JENKINS_GROUP="${file_meta[3]}"
	echo "Using JENKINS_GROUP defined as $JENKINS_GROUP"

	# backup the currently installed plugin
	echo "Backing up the currently installed license plugin"
	kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- mv $PLUGIN_ROOT/cloudbees-license.jpi $PLUGIN_ROOT/cloudbees-license.bak
	kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- chown $JENKINS_USER $PLUGIN_ROOT/cloudbees-license.bak
	kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- chgrp $JENKINS_GROUP $PLUGIN_ROOT/cloudbees-license.bak

	# now download the plugin

	echo "Downloading updated plugin from $PLUGIN_URL"
	$downloadTool $PLUGIN_URL

	echo "Copying updated plugin to pod"
	kubectl cp ./cloudbees-license.jpi --namespace $POD_NAMESPACE $POD_NAME:/var/jenkins_home/plugins/cloudbees-license.jpi
	kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- chown $JENKINS_USER $PLUGIN_ROOT/cloudbees-license.jpi
	kubectl exec -it $POD_NAME --namespace $POD_NAMESPACE -- chgrp $JENKINS_GROUP $PLUGIN_ROOT/cloudbees-license.jpi

	echo "Restarting pod $pod"
	kubectl delete pod $POD_NAME --namespace $POD_NAMESPACE
done
echo "-----------------"