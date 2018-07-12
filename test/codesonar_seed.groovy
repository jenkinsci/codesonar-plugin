job("build_and_analyze_linux_kernel_gitsha") {
  parameters {
    stringParam("HUB", "127.0.0.1:7340", "CodeSonar Hub <IP>:<PORT>, no http/https")
    stringParam("PROJNAME", "linuxkernel","The project name in CodeSonar Hub")
  }

  // Using the same git SHA every time to get same results
  scm {
    git("https://github.com/torvalds/linux.git","f3afe530d644488a074291da04a69a296ab63046")
  }

  steps {
    shell('''export PATH=$PATH:/home/ubuntu/data/codesonar-4.4p0/codesonar/bin
make clean
echo "copying configuration file for the kernel to build"
cp $HOME/config .config
codesonar analyze $PROJNAME -foreground $HUB make -j$(nproc --all)''')
  }

  publishers {
    codesonar {
      protocol('http')
      hubAddress('${HUB}')
      projectName('${PROJNAME}')
      credentialId('codesonarhub')
     }
  }
}

job("build_and_analyze_codesonar_plugin_branch-master") {
parameters {
  stringParam("HUB", "127.0.0.1:7340", "CodeSonar Hub <IP>:<PORT>, no http/https")
  stringParam("PROJNAME", "codesonarplugin","The project name in CodeSonar Hub")
  }
  scm {
    git("https://github.com/Praqma/codesonar-plugin","master")
  }
  steps {
    shell('''mvn clean compile''')
    shell('''export PATH=$PATH:/home/ubuntu/data/codesonar-4.4p0/codesonar/bin
codesonar analyze $PROJNAME -foreground $HUB cs-java-scan target/classes/org/jenkinsci/plugins/codesonar/ -sourcepath src/main/java/ -disable-findbugs''')
  }
  publishers {
    codesonar {
      protocol('http')
      hubAddress('${HUB}')
      projectName('${PROJNAME}')
      credentialId('codesonarhub')
     }
  }
}
