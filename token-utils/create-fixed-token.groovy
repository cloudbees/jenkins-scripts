import hudson.model.User
import jenkins.security.ApiTokenProperty
// generate token (this would be pre-defined)
def r = new Random()
def result = (0..<32).collect { r.nextInt(16) }
                    .collect { Integer.toString(it, 16).toLowerCase() }
                    .join();
String tok = "11" + result
println tok
// assign to user
String userName = "bob"
String tokenName = "test-token"
User user = User.get(userName)
user.addProperty(new ApiTokenProperty());
user.getProperty(ApiTokenProperty.class).addFixedNewToken(tokenName, tok);
// test (can be deleted - just for test purposes)
String jenkinsUrl = "http://..."
def sout = new StringBuilder(), serr = new StringBuilder()
def proc = "curl -s -w '%{http_code}\n' -u ${userName}:${tok}
${jenkinsUrl}/cjoc/api/json".execute()
proc.consumeProcessOutput(sout, serr)
proc.waitForOrKill(1000)
println "out> $sout\nerr> $serr"