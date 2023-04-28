tokensync() {
user="admin"
admintok="xxxxxxx" # must be `11` followed by 32 hex chars
tok=11$(hexdump -n 16 -v -e '16/1 "%02X" "\n"' /dev/random | tr '[:upper:]' '[:lower:]')
curl -u admin:$admintok -d newTokenName=synched -d newTokenPlainValue=$tok
$CIURL/cjoc/user/$user/descriptorByName/jenkins.security.ApiTokenProperty/addFixedToken
} # token
