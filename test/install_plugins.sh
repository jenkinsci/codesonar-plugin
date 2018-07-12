#!/bin/bash

#Generate plugins.txt with this groovy snippet in the Jenkins script console on a running instance with the plugins you like:

# plugins = [:]
# jenkins.model.Jenkins.instance.getPluginManager().getPlugins().each {plugins << ["${it.getShortName()}":"${it.getVersion()}"]}
# plugins.sort().each() { println "${it.key},${it.value}"}


# This script is partly copied and edited from the following scripts and resources.
# It was adjusted to be independent from the officiel Jenkins docker setup, that relies on several other Bash script to be present.
# https://stackoverflow.com/questions/14950408/how-to-install-a-plugin-in-jenkins-manually
# https://github.com/jenkinsci/docker/blob/3ee97cb10aa317701252171d598f70fcfed7bba1/plugins.sh
# https://github.com/jenkinsci/docker/blob/3ee97cb10aa317701252171d598f70fcfed7bba1/install-plugins.sh
# https://gist.github.com/micw/e80d739c6099078ce0f3
# https://github.com/Praqma/JenkinsAsCodeReference/blob/master/dockerizeit/master/scripts/get_plugins.groovy
# https://stackoverflow.com/questions/9815273/how-to-get-a-list-of-installed-jenkins-plugins-with-name-and-version-pair

# Script is hard-coded to work with our CodeSonar plugin testing, e.g. hard-coded paths for Jenkins home etc.



set -e

if [ $# -eq 0 ]; then
  echo "USAGE: $0 plugins.txt"
  exit 1
fi

plugin_dir=/home/ubuntu/data/jenkins_home/plugins
file_owner=ubuntu.ubuntu

mkdir -p $plugin_dir

installPlugin() {
  echo "installPlugin($@)"
  NAME=$1
  VER=$2
  FILENAME=$NAME-$VER
  echo "FILENAME=$FILENAME"
  if [ -f ${plugin_dir}/${FILENAME}.hpi -o -f ${plugin_dir}/${FILENAME}.jpi ]; then
    echo "Skipped: $FILENAME (already installed)"
    return 0
  else
    echo "Installing plugin $NAME in $VER"
    echo curl -sSL -f -o  https://updates.jenkins.io/download/plugins/${NAME}/${VER}/${NAME}.hpi
    curl -sSL -f --output ${plugin_dir}/${FILENAME}.hpi  https://updates.jenkins.io/download/plugins/${NAME}/${VER}/${NAME}.hpi
    return 0
  fi
}

installDeps() {
  if [ -f ${plugin_dir}/${1}.hpi -o -f ${plugin_dir}/${1}.jpi ]; then
    if [ "$2" == "1" ]; then
      return 1
    fi
    echo "Skipped: $1 (already installed)"
    return 0
  else
    echo "Installing dependency: $1"
    curl -L --silent --output ${plugin_dir}/${1}.hpi  https://updates.jenkins-ci.org/latest/${1}.hpi
    return 0
  fi
}


for plugin in `cat $1`
do
    NAME=$(echo $plugin | cut -f1 -d,)
    VER=$(echo $plugin | cut -f2 -d,)

    echo "installing $NAME in version $VER"
    installPlugin $NAME $VER
done

changed=1
maxloops=100

while [ "$changed"  == "1" ]; do
  echo "Check for missing dependecies ..."
  if  [ $maxloops -lt 1 ] ; then
    echo "Max loop count reached - probably a bug in this script: $0"
    exit 1
  fi
  ((maxloops--))
  changed=0
  for f in ${plugin_dir}/*.hpi ; do
    # without optionals
    #deps=$( unzip -p ${f} META-INF/MANIFEST.MF | tr -d '\r' | sed -e ':a;N;$!ba;s/\n //g' | grep -e "^Plugin-Dependencies: " | awk '{ print $2 }' | tr ',' '\n' | grep -v "resolution:=optional" | awk -F ':' '{ print $1 }' | tr '\n' ' ' )
    # with optionals
    deps=$(unzip -p ${f} META-INF/MANIFEST.MF | tr -d '\r' | sed -e ':a;N;$!ba;s/\n //g' | grep -e "^Plugin-Dependencies: " | awk '{ print $2 }' | tr ',' '\n' | awk -F ':' '{ print $1 }' | tr '\n' ' ' )
    for plugin in $deps; do
      installDeps "$plugin" 1 && changed=1
    done
  done
done

echo "fixing permissions"

chown ${file_owner} ${plugin_dir} -R

echo "all done"
