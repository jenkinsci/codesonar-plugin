#!/bin/bash

LOG=jenkins_run.log

echo "Installing deps, downloading Jenkins and starting Jenkins, then installing plugins and configuring jobs etc."
echo "Check $LOG file for progress"

# need java for running jenkins, expect to automate CodeSonar hub start up and unzip for plugins install
# Expect is in universe repo, so need to enable that first.
echo "sudo enabling universe packages and installing apt dependencies"
sudo add-apt-repository "deb http://archive.ubuntu.com/ubuntu $(lsb_release -sc) universe"
sudo apt update
sudo apt install -y openjdk-8-jre-headless openjdk-8-jdk unzip expect maven >> $LOG 2>&1
# maven used to build codesonar plugin, one of the analysis example as jenkins job

# there are needed for compiling linux kernel we run in our test jobs
sudo apt install -y build-essential gcc libncurses5-dev libssl-dev bison flex libelf-dev bc
