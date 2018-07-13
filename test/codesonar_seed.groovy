job("build_and_analyze_linux_kernel_gitsha") {
  parameters {
    stringParam("HUB", "127.0.0.1:7340", "CodeSonar Hub <IP>:<PORT>, no http/https")
    stringParam("PROJNAME", "${JOB_NAME}","The project name in CodeSonar Hub. Defaults to name of Jenkins job")
  }

  // Using the same git SHA every time to get same results
  scm {
    git("https://github.com/torvalds/linux.git","f3afe530d644488a074291da04a69a296ab63046")
  }

  steps {
    shell('''export PATH=$PATH:/home/ubuntu/data/codesonar-4.4p0/codesonar/bin
make clean
echo "using configuration file config for this build"
cp -vf test/config .config
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
  stringParam("PROJNAME", "${JOB_NAME}","The project name in CodeSonar Hub. Defaults to name of Jenkins job")
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

job("wgen_generate_warnings") {
  parameters {
    stringParam("HUB", "127.0.0.1:7340", "CodeSonar Hub <IP>:<PORT>, no http/https")
    stringParam("PROJNAME", "${JOB_NAME}","The project name in CodeSonar Hub. Defaults to name of Jenkins job")
    stringParam("WARNPRLINES", "2","Number of warnings to generate pr. line of code in fibonacci.c using the wgen.py generator plugin for CodeSonar")
  }

  // Using the same git SHA every time to get same results
  scm {
    git("https://github.com/Praqma/codesonar-plugin","master")
  }

  // notice \ are escaped to become \\
  steps {
    shell('''export PATH=$PATH:/home/ubuntu/data/codesonar-4.4p0/codesonar/bin
cp -vf test/wgen-codesonar-template.conf test/wgen.py test/fibonacci.c .
sed -e 's/\\(WARNINGS_PER_LINE = \\).\\([\\d+*]\\)*/\\1'"$WARNPRLINES"'/' -i wgen.py
cat wgen-codesonar-template.conf wgen.py fibonacci.c
codesonar analyze $PROJNAME -foreground -conf-file wgen-codesonar-template.conf $HUB g++ fibonacci.c -o fibonacci''')
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
