set -e

echo "Will install local codesonar.hpi file if present in $HOMEDIR or latest from update center"

plugin_dir=/home/ubuntu/data/jenkins_home/plugins


FILE=$HOME/codesonar.hpi     
if [ -f $FILE ]; then
   echo "Found local $FILE, copying to plugin dir."
   cp -fv $FILE ${plugin_dir}/
else
   echo "No local $FILE found, downloading latest"
   curl -sSL -f --output ${plugin_dir}/codesonar.hpi https://updates.jenkins.io/latest/codesonar.hpi
fi

echo "Restarting Jenkins to make sure plugin is in correct version, in case old was installed"

wget http://localhost:8080/jnlpJars/jenkins-cli.jar
java -jar jenkins-cli.jar -s http://localhost:8080/ restart

