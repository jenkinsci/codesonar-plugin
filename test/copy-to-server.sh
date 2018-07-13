


FILES="
install_dependencies.sh
0-create-credentials.groovy
1-create-seedjob.groovy
CodeSonarHubCredentialsPass
install_codesonar_plugin_under_test.sh
install_plugins.sh
jenkins.credentials
install_and_run_jenkins.sh
plugins.txt
configure_jobs_and_jenkins.sh
mount-volume.sh
configure_jobs_and_jenkins.sh
create-aws-environment-resources.log
install_codesonarhub.sh
configure_and_start_codesonarhub.sh
codesonar-reset-UNACCEPTED_LICENSE.txt
reset_codesonar_installation.sh
stop_codesonarhub.sh
start_codesonarhub.sh
start_services_if_not_running.sh
start_codesonarhub_helperscript.sh
codesonar-4.4p0.20161021-x86_64-pc-linux.tar.gz
"


# resource log needed on server to reference public ip


source create-aws-environment-resources.log

for file in $FILES
do
  scp -i codesonar.pem $file ubuntu@$PublicIp:.
done;
