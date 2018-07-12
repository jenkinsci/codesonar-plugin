#!/bin/bash

JENKINS_HOME=/home/ubuntu/data/jenkins_home
INIT_DIR=$JENKINS_HOME/init.groovy.d
mkdir $INIT_DIR
cp $HOME/0-create-credentials.groovy $HOME/1-create-seedjob.groovy $INIT_DIR/
cp $HOME/jenkins.credentials $HOME/CodeSonarHubCredentialsPass $HOME/data
echo "Restarting Jenkins to make sure plugin is in correct version, in case old was installed"

wget http://localhost:8080/jnlpJars/jenkins-cli.jar

java -jar jenkins-cli.jar -s http://localhost:8080/ restart
