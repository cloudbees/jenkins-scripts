#!/bin/bash

#############################################################################################
# Common code

_bold=""
_underline=""
_standout=""
_normal=""
_black=""
_red=""
_green=""
_yellow=""
_blue=""
_magenta=""
_cyan=""
_white=""

# check if stdout is a terminal...
if [ -t 1 ]; then

    # see if it supports colors...
    _ncolors=$(tput colors)

    if test -n "$_ncolors" && test $_ncolors -ge 8; then
        _bold="$(tput bold)"
        _underline="$(tput smul)"
        _standout="$(tput smso)"
        _normal="$(tput sgr0)"
        _black="$(tput setaf 0)"
        _red="$(tput setaf 1)"
        _green="$(tput setaf 2)"
        _yellow="$(tput setaf 3)"
        _blue="$(tput setaf 4)"
        _magenta="$(tput setaf 5)"
        _cyan="$(tput setaf 6)"
        _white="$(tput setaf 7)"
    fi
fi

_horizontal_line="================================================================================"

# Display DEBUG message
echo_debug() {
  if [ "${VERBOSE:-false}" == "true" ]; then
    echo -e "${_cyan}$*${_normal}"
  fi
}

# Display INFO message
echo_info() {
  echo -e "${_green}$*${_normal}"
}

# Display WARN message
echo_warn() {
  echo -e "${_yellow}$*${_normal}"
}

# Display ERROR message
echo_error() {
  echo -e "${_bold}${_red}$*${_normal}"
}

# Display a title
echo_title(){
  echo_info "${_horizontal_line}"
  echo_info "== $1"
  echo_info "${_horizontal_line}"
}

# Display usage help
print_help ()
{
    cat <<EOM
Unable to find required arguments.  Please rerun the script as follows:
$(basename $0) JENKINS_CLI JENKINS_URL AUTH_FILE [UPGRADE] [LICENSE]
    JENKINS_CLI:          jenkins-cli.jar file path
    JENKINS_URL:          Operations Center URL
    AUTH_FILE:            Authentication file contatining userid:token
  Optional parameters
    UPGRADE:              "UPGRADE" string is required to upgrade cloudbees-license pugin.
    LICENSE:              "NEW" string is required to look for new license if cloudbees-license plugin is valid.
  Environment variables
    VERBOSE:              Additional debug output is exposed. Default false.
    LICENSE_SCRIPTS_DIR:  Path to groovy scripts. Default current folder.
Example: VERBOSE=true LICENSE_SCRIPTS_DIR=/home/user/license-upgrade-scripts ./$(basename $0) /home/user/jenkins/jenkins-cli.jar https://core.example.com/cjoc/ /home/user/.cli-auth UPGRADE NEW
EOM
}

# Remove [ ]
prepare_status() {
  local string="${1}"
  local size=${#string}
  size=$((size-2))
  string=${string:1:$size}

  echo "$string"
}


print_status() {
  local instance="${1}"
  local plugin_installed="${2}"
  local plugin_version="${3}"
  local plugin_upgrade="${4}"
  local license_upgrade="${5}"

  local summary="[${instance}] cloudbees-license plugin"

  if [[ ${plugin_installed} == "1" ]]; then
    summary=" ${summary} ${plugin_version} is installed."

    if [[ ${plugin_upgrade} == "1" ]]; then
      summary="${summary} The plugin must be updated in this instance before installing the new license." 
      echo_warn "${summary}"
    elif [[ ${plugin_upgrade} == "0" ]] && [[ ${license_upgrade} == "1" ]]; then
      summary="${summary} This instance is ready to install the new license." 
      echo_warn "${summary}"
    elif [[ ${plugin_upgrade} == "0" ]] && [[ ${license_upgrade} == "0" ]]; then
      summary="${summary} This instance is up to date and running the new license." 
      echo_info "${summary}"
    else
      summary="${summary} This instance is in an inconsistent state."
      echo_error "${summary}"
    fi  

  else
      summary="${summary} is not installed. The plugin must be installed in this instance before updating the license."
      echo_error "${summary}"
  fi

}

# check if the file exists
validate_file_exists() {
  local file="${1}"
  local is_error="${2:-false}"

  if [ ! -f "${file}" ]; then
    local message="File ${file} does not exist."
    if ${is_error}; then
      (>&2 echo_error "${message}")
      exit 1
    else
      echo_warn "${message}"
    fi
  fi
}


#############################################################################################
# Main code

echo_title "Running verify-cluster bash script..."
echo_warn "Please run this command with skipMasters and onlyStatus flags enabled in groovy scripts."
echo_warn "verify-system-readiness.groovy, upgrade-license-plugin.groovy, and apply-license.groovy are required."

if [ -z "$LICENSE_SCRIPTS_DIR" ]; then
    LICENSE_SCRIPTS_DIR="$(pwd)"
fi

if [ $# -eq 3 ]
then
  jenkins_cli=$1
  jenkins_url=$2
  jenkins_auth=$3
  upgrade_license_plugin="NO"
  apply_license="NO"
  validate_file_exists "${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy" true
elif [ $# -eq 4 ]
then
  jenkins_cli=$1
  jenkins_url=$2
  jenkins_auth=$3
  upgrade_license_plugin=$4
  apply_license="NO"
  validate_file_exists "${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy" true
  validate_file_exists "${LICENSE_SCRIPTS_DIR}/upgrade-license-plugin.groovy" true
elif [ $# -eq 5 ]
then
  jenkins_cli=$1
  jenkins_url=$2
  jenkins_auth=$3
  upgrade_license_plugin=$4
  apply_license=$5
  validate_file_exists "${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy" true
  validate_file_exists "${LICENSE_SCRIPTS_DIR}/upgrade-license-plugin.groovy" true
  validate_file_exists "${LICENSE_SCRIPTS_DIR}/apply-license.groovy" true
else
  print_help $0
  exit 1
fi

echo_debug "  Execution parameters:"
echo_debug "    JENKINS_CLI:          ${jenkins_cli}"
echo_debug "    JENKINS_URL:          ${jenkins_url}"
echo_debug "    AUTH_FILE:            ${jenkins_auth}"
echo_debug "  Optional parameters:"
echo_debug "    UPGRADE:              ${upgrade_license_plugin}"
echo_debug "    LICENSE:              ${apply_license}"
echo_debug "  Environment variables:"
echo_debug "    VERBOSE:              ${VERBOSE}"
echo_debug "    LICENSE_SCRIPTS_DIR:  ${LICENSE_SCRIPTS_DIR}"
echo " "


echo_debug "Executing java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy"
oc_status=$(java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy)
echo_debug "[operations-center] ${oc_status}"

oc_status=$(prepare_status "${oc_status}")
IFS=', ' read -r -a oc_status_arr <<< "$oc_status"

## _statusKey[0] = "Is cloudbees-license plugin installed?"
## _statusKey[1] = "Is BeeKeeper enabled?"
## _statusKey[2] = "Is Custom Update Center configured?"
## _statusKey[3] = "Is your instance compatible with Incremental Upgrades?"
## _statusKey[4] = "Is cloudbees-license plugin version compatible with new Certificate?"
## _statusKey[5] = "Is the license managed by this instance?"
## _statusKey[6] = "Is the license signed by a known certificate?"
## _statusKey[7] = "Plugin version"
## _statusKey[8] = "License expiration date"
## _statusKey[9] = "Root CA expiration date"
## _statusKey[10] = "Is the expiration date of license before than the Root CA?"
## _statusKey[11] = "Is the Root CA about to expire?"
## _statusKey[12] = "Plugin requires update?"
## _statusKey[13] = "License requires update?"
## _statusKey[14] = "Error"
## _statusKey[15] = "Product Version"
## _statusKey[16] = "Using wildcard license?"

print_status "operations-center" "${oc_status_arr[0]}" "${oc_status_arr[7]}" "${oc_status_arr[12]}" "${oc_status_arr[13]}"
echo " "

echo_debug "Executing java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} list-masters"
masters=$(java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} list-masters)
online_masters=$(echo $masters | jq -cr '.data.masters[] | select(.status == "ONLINE")')
masters_name=$(echo $online_masters | jq -r '.fullName')
masters_url=$(echo $online_masters | jq -r '.url')
m_u_arr=($masters_url)
m_n_arr=($masters_name)

plugins=0

for index in "${!m_u_arr[@]}"; do 
  echo_debug "Executing java -jar ${jenkins_cli} -s ${m_u_arr[index]} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy"
  master_status=$(java -jar ${jenkins_cli} -s ${m_u_arr[index]} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/verify-system-readiness.groovy)
  echo_debug "[${m_n_arr[index]}] ${master_status}"

  master_status=$(prepare_status "${master_status}")
  IFS=', ' read -r -a master_status_arr <<< "$master_status"

  print_status "${m_n_arr[index]}" "${master_status_arr[0]}" "${master_status_arr[7]}" "${master_status_arr[12]}" "${master_status_arr[13]}"

  if [[ ${master_status_arr[0]} != "1" ]] || [[ ${master_status_arr[12]} == "1" ]]; then
    ((plugins++))
    if [[ ${upgrade_license_plugin} == "UPGRADE" ]]; then
      echo_info "Asking for upgrading cloudbees-license plugin..."
      echo_debug "Executing java -jar ${jenkins_cli} -s ${m_u_arr[index]} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/upgrade-license-plugin.groovy"
      upgrade_license_plugin_output=$(java -jar ${jenkins_cli} -s ${m_u_arr[index]} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/upgrade-license-plugin.groovy)
      echo_info "UPGRADE LICENSE PLUGIN OUTPUT ${upgrade_license_plugin_output}"
    fi
  fi
done;

if [[ ${apply_license} == "NEW" ]] && [[ ${plugins} -eq 0 ]]; then
  echo_info "Asking for new license..."
  echo_debug "Executing java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/apply-license.groovy"
  apply_license_output=$(java -jar ${jenkins_cli} -s ${jenkins_url} -auth @${jenkins_auth} groovy = < ${LICENSE_SCRIPTS_DIR}/apply-license.groovy)
  echo_info "LICENSE OUTPUT ${apply_license_output}"
fi


