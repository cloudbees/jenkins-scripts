@Grapes([
    @Grab('software.amazon.awssdk:secretsmanager:2.17.188'),
    @Grab('software.amazon.awssdk:sts:2.17.188')
])
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.*
import software.amazon.awssdk.services.secretsmanager.model.*
import java.util.List


def listSecrets(SecretsManagerClient secretsClient) {
    try {
        ListSecretsResponse secretsResponse = secretsClient.listSecrets()
        List<SecretListEntry> secrets = secretsResponse.secretList()

        for (SecretListEntry secret: secrets) {
            System.out.println("The secret name is "+secret.name())
            System.out.println("The secret description is "+secret.description())
        }

    } catch (SecretsManagerException e) {
        println e.awsErrorDetails().errorMessage()
        throw e
    }
}

def listSecretVersions(SecretsManagerClient secretsClient, String secretName) {
    try {
        ListSecretVersionIdsRequest listSecretVersionIdsRequest = ListSecretVersionIdsRequest.builder()
            .secretId(secretName)
            .build()

        ListSecretVersionIdsResponse listSecretVersionIdsResponse = secretsClient.listSecretVersionIds(listSecretVersionIdsRequest)
        for (SecretVersionsListEntry version: listSecretVersionIdsResponse.versions()) {
            println "The version response is " + version
            print "    The version value is: "
            getValueByVersionId(secretsClient, secretName, version.versionId)
        }
    } catch (SecretsManagerException e) {
        println e.awsErrorDetails().errorMessage()
        throw e
    }
}

def getValue(SecretsManagerClient secretsClient, String secretName) {
    try {
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
            .secretId(secretName)
            .build()

        GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest)
        String secret = valueResponse.secretString()
        println secret
    } catch (SecretsManagerException e) {
        println e.awsErrorDetails().errorMessage()
        throw e
    }
}

def getValueByStage(SecretsManagerClient secretsClient, String secretName, String versionStage) {
    try {
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
            .secretId(secretName)
            .versionStage(versionStage)
            .build()

        GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest)
        String secret = valueResponse.secretString()
        println secret
    } catch (SecretsManagerException e) {
        println e.awsErrorDetails().errorMessage()
        throw e
    }
}

def getValueByVersionId(SecretsManagerClient secretsClient, String secretName, String versionId) {
    try {
        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
            .secretId(secretName)
            .versionId(versionId)
            .build()

        GetSecretValueResponse valueResponse = secretsClient.getSecretValue(valueRequest)
        String secret = valueResponse.secretString()
        println secret
    } catch (SecretsManagerException e) {
        println e.awsErrorDetails().errorMessage()
        throw e
    }
}

// -------- START
String regionStr = 'us-east-1'
String secretStr = 'sboardwell/test/jenkins/token'
Region region = Region.of(regionStr)
SecretsManagerClient secretsClient = SecretsManagerClient.builder()
        .region(region)
        .build()
try {
    println "------------------------------"
    println "Listing ALL secrets"
    println "------------------------------"
    listSecrets(secretsClient)

    println "------------------------------"
    println "Listing single secret by versionId"
    println "------------------------------"
    listSecretVersions(secretsClient, secretStr)

    println "------------------------------"
    println "Listing single secret"
    println "------------------------------"
    getValue(secretsClient, secretStr)

    println "------------------------------"
    println "Listing single secret by stage - AWSCURRENT"
    println "------------------------------"
    getValueByStage(secretsClient, secretStr, 'AWSCURRENT')

    println "------------------------------"
    println "Listing single secret by stage - AWSPREVIOUS"
    println "------------------------------"
    getValueByStage(secretsClient, secretStr, 'AWSPREVIOUS')

} catch (SecretsManagerException e) {
    println e.awsErrorDetails().errorMessage()
    throw e
} finally {
    secretsClient.close()
}

