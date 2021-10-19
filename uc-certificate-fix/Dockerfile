FROM cloudbees/cloudbees-core-mm:2.303.2.6

RUN mkdir -p /var/jenkins_home/init.groovy.d
COPY ./ucCertRemediation.groovy /var/jenkins_home/init.groovy.d/

RUN touch /var/jenkins_home/init.groovy.d/DO_NOT_RUN_UC_REMEDIATION && \
    chown jenkins /var/jenkins_home/init.groovy.d/DO_NOT_RUN_UC_REMEDIATION && \
    chmod 755 /var/jenkins_home/init.groovy.d/DO_NOT_RUN_UC_REMEDIATION
