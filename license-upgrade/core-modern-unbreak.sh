#!/bin/bash
cbpods=$(kubectl get pods --selector=com.cloudbees.cje.tenant -o jsonpath='{.items[*].metadata.name}' --all-namespaces)
for pod in `echo ${cbpods}`; do  
	printf "\n POD: $pod\n"                                                                                                                                        ✔ 
	kubectl exec -it $pod -- cat /tmp/jenkins/plugins/cloudbees-license/META-INF/MANIFEST.MF    | grep "Plugin-Version"

done
