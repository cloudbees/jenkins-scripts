FROM cloudbees/cloudbees-core-mm:2.303.2.6

USER jenkins

RUN mkdir -p /var/jenkins_home/init.groovy.d
COPY --chown=jenkins:jenkins ./ucCertRemediation.groovy /var/jenkins_home/init.groovy.d/

RUN touch /var/jenkins_home/DO_NOT_RUN_UC_REMEDIATION && \
    chown jenkins /var/jenkins_home/DO_NOT_RUN_UC_REMEDIATION && \
    chmod 755 /var/jenkins_home/DO_NOT_RUN_UC_REMEDIATION 
