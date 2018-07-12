#!/bin/bash

LOG=jenkins_run.log

echo "Installing deps, downloading Jenkins and starting Jenkins, then installing plugins and configuring jobs etc."
echo "Check $LOG file for progress"

echo "Setting path for codesonar bins we need to use later"
touch .bashrc
echo "PATH=$PATH:/home/ubuntu/data/codesonar-4.4p0/codesonar/bin" >> .bashrc
cat .bashrc | grep "PATH"
echo "sourcing .bashrc to take effect with new path"
source .bashrc
echo "PATH is now. $PATH"


# prepare Ubuntu with apt install package we need
./install_dependencies.sh

echo "deleting old jenkins.war and getting new"
rm -vf jenkins.war
wget http://mirrors.jenkins.io/war-stable/2.121.1/jenkins.war >> $LOG 2>&1


echo "Setting jenkins home"
[[ -d /home/ubuntu/data/jenkins_home ]] || mkdir -v /home/ubuntu/data/jenkins_home
export JENKINS_HOME=/home/ubuntu/data/jenkins_home

echo "installing plugins"
./install_plugins.sh plugins.txt >> $LOG 2>&1

# get server public ip as $PublicIp to configure monitoring
source create-aws-environment-resources.log

echo "Prepare java monitoring, using nohup to keep alive after logout"
echo "grant {permission java.security.AllPermission; };" >> jstatd.all.policy
nohup jstatd -J-Djava.security.policy=jstatd.all.policy -J-Djava.rmi.server.hostname=$PublicIp &

# java option for monitoring:
echo "Starting jenkins, home will be $JENKINS_HOME"
nohup java -Djenkins.install.runSetupWizard=false\
 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false\
 -Dcom.sun.management.jmxremote.authenticate=false\
 -Dcom.sun.management.jmxremote.port=1098\
 -Djava.rmi.server.hostname=$PublicIp -jar jenkins.war >> $LOG 2>&1 &


echo "Installing CodeSonar plugin calling helper script"
./install_codesonar_plugin_under_test.sh >> $LOG 2>&1

echo "creating groovy init files for Jenkins to create jobs and credentials"
./configure_jobs_and_jenkins.sh >> $LOG 2>&1

echo "DONE starting up Jenkins stuff"
ps axu | grep jstatd
ps axu | grep jenkins
echo "You can get to Jenkins on $PublicIp:8080"
echo "You can monitor it with visualvm on $PublicIp:1099 with jstat connection"
echo "and using jxm on $PublicIp:1098"
echo "Jenkins home is in $JENKINS_HOME"
