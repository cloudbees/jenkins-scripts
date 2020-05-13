#!/bin/bash

echo "Please run this command with skipMasters and onlyStatus flags enabled in groovy scripts."

print_help ()
{
    cat <<EOM
Unable to find required arguments.  Please rerun the script as follows:
$(basename $0) JENKINS_CLI JENKINS_URL AUTH_FILE
    JENKINS_CLI:  jenkins-cli.jar file path
    JENKINS_URL:  JENKINS_HOST URL
    AUTH_FILE:    Authentication file contatining userid:token
    Run $(basename $0) --help to print help.
EOM
}

if [ $# -eq 3 ]
then
  jenkins_cli=$1
  jenkins_url=$2
  jenkins_auth=$3
else
  print_help $0
  exit 1
fi

if [ -z "$LICENSE_SCRIPTS_DIR" ]; then
    LICENSE_SCRIPTS_DIR="$(pwd)"
    #echo $(date) "Output dir $LICENSE_SCRIPTS_DIR"
fi

oc_status=$(java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy)
echo "[operations-center] ${oc_status}"


masters=$(java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} list-masters)
online_masters=$(echo $masters | jq -cr '.data.masters[] | select(.status == "ONLINE")')
masters_name=$(echo $online_masters | jq -r '.fullName')
masters_url=$(echo $online_masters | jq -r '.url')
m_u_arr=($masters_url)
m_n_arr=($masters_name)
for index in "${!m_u_arr[@]}"; do 
  master_status=$(java -jar ${jenkins_cli} -s ${m_u_arr[index]} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy)
  echo "[${m_n_arr[index]}] ${master_status}"
done;