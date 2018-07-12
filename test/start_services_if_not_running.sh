#!/bin/bash

echo "Checking if jstat, jenkins is running and else starts them"
echo "CodeSonar Hub you can start, if stopped, using start_codesonar_hub.sh"


# get server public ip as $PublicIp to configure monitoring
source create-aws-environment-resources.log

LOG=jenkins_run.log


# Check if "prg" is running
# -x flag only match processes whose name (or command line if -f is
# specified) exactly match the pattern. 

#if pgrep -x "pgr" > /dev/null
#then
#    echo "Running"
#else
#    echo "Stopped"
#fi


if pgrep -x "jstatd" > /dev/null
then
	echo "jstatd running"
else
	echo "jstatd not running - starting ..."
	nohup jstatd -J-Djava.security.policy=jstatd.all.policy -J-Djava.rmi.server.hostname=$PublicIp &
fi


if pgrep -f "java -Djenkins.install.runSetupWizard=false -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=1098 -Djava.rmi.server.hostname=$PublicIp -jar jenkins.war" > /dev/null
then
	echo "jenkins running"
else
	echo "jenkins not running - starting ..."
	export JENKINS_HOME=/home/ubuntu/data/jenkins_home
	nohup java -Djenkins.install.runSetupWizard=false\
	 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false\
	 -Dcom.sun.management.jmxremote.authenticate=false\
	 -Dcom.sun.management.jmxremote.port=1098\
	 -Djava.rmi.server.hostname=$PublicIp -jar jenkins.war >> $LOG 2>&1 &

fi
