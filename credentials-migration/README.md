# Jenkins Credentials Migration

The following scripts are created to migrate the credentials at Root (System) and Folder level from one Jenkins Master to another one.

In Jenkins, `$JENKINS_HOME/secrets/master.key` is the secret used to encrypt all the credentials stored on disk. The new instances created will have their own `$JENKINS_HOME/secrets/master.key`, so it is necessary to export/import the credentials to re-encrypt them in the new Jenkins master.

## Migrate the System credentials

1. Download the `export-credentials-system-level.groovy` script on [GitHub](https://github.com/cloudbees/jenkins-scripts/tree/master/credentials-migration/export-credentials-system-level.groovy).
2. Run `export-credentials-system-level.groovy` in the Script Console on the source instance. It will output an encoded message containing a flattened list of all system credentials. Copy that encoded message.

The encoded message will be used later on to export the credentials in the new Jenkins Master.

3. Download the `update-credentials-system-level.groovy` on [GitHub](https://github.com/cloudbees/jenkins-scripts/tree/master/credentials-migration/update-credentials-system-level.groovy).

Paste the encoded message output from the `export-credentials-system-level.groovy` script as the value in the encoded variable in the `update-credentials-system-level.groovy` script and execute it in the Script Console on the destination Jenkins. All the System credentials from the source Jenkins will now be updated to the system store of the destination Jenkins. 

## Migrate the Folder credential

1. Download the `export-credentials-folder-level.groovy` script on [GitHub](https://github.com/cloudbees/jenkins-scripts/tree/master/credentials-migration/export-credentials-folder-level.groovy).
2. Run `export-credentials-folder-level.groovy` in the Script Console on the source instance. It will output an encoded message containing a flattened list of all system credentials. Copy that encoded message.

The encoded message will be used later on to update the credentials in the new Jenkins Master.

3. Download the `update-credentials-folder-level.groovy` on [GitHub](https://github.com/cloudbees/jenkins-scripts/tree/master/credentials-migration/update-credentials-folder-level.groovy).

Paste the encoded message output from the `export-credentials-folder-level.groovy` script as the value in the encoded variable in the `update-credentials-folder-level.groovy` script and execute it in the Script Console on the destination Jenkins. All the folder credentials from the source Jenkins will now be updated to each folder store of the destination Jenkins.

These scripts were created taking as an example [cloudbees-ci/cje-to-ci-migration-examples](https://github.com/cloudbees/cloudbees-examples/tree/master/cloudbees-ci/cje-to-ci-migration-examples/CredentialsMigration)