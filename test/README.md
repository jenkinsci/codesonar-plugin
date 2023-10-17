# Manual test of the Jenkins CodeSonar Plugin

This folder contain everything related to our manual testing of the CodeSonar Plugin, using some AWS instance and very large projects like the Linux kernel.

**When?** Once a while when we do larger changes in CodeSonar Plugin we will validate the plugin manually on a huge code base, as well as with many CodeSonar warnings to find issues related to large data set, memory usage etc.


## Prerequisites

* **AWS CLI** - you need to install the AWS CLI
* **Run all commands from the root of this repository!**
* **A CodeSonar installation file for linux 64 bit**

## Create AWS infrastructure

We need a huge machine to the large project on, so we use a big EC2 instance in AWS we create for the purpose of running test temporarily.

First configure you AWS CLI with a user that can create the necessary objects (EC2 instances, VPC, network interfaces etc.).
You need an ID and key.

This is a one-off for the rest of the process.

(**Praqma employees can find relevant credentials for the codesonar use and the pem-file on our test resource folder mentioned above**)

    $ aws configure
    AWS Access Key ID [None]: AKIAJLKV5PC57Z6LHQLA
    AWS Secret Access Key [None]: *********************************************
    Default region name [None]: eu-west-1
    Default output format [None]:

Then create a key-pair to attach to the EC2 instance we will create and to use to connect to it when deployed. Should it exist, delete in AWS EC2 first, as that is probably left-overs from last time.
Also ensure secure permissions.

    aws ec2 create-key-pair --key-name codesonar --query 'KeyMaterial' --output text > codesonar.pem
    chmod 0600 codesonar.pem

Then run our AWS EC2 create environment script:

    ./create-aws-environment.sh

(The reverse action, is if you like to destroy it to `run delete_aws_environment.sh`).

Check the `create-aws-environment-resources.log` for resources created and source it to use it in your terminal:

    source create-aws-environment-resources.log

(in this repository, is an example version `create-aws-environment-resources.log.example`)

After this you can now ssh to machine, if you like. Follow next section to configure the instance to run Jenkins and CodeSonar application.

## Configure EC2 instance with Jenkins and CodeSonar

Start copying the needed scripts to the server, so you avoid typing all commands manually:

    ./copy-to-server.sh

Now login on the ec2 to run the install and configure commands.

**From here on - run all commands on the server!**


Then go to the server run these two commands:

    source create-aws-environment-resources.log
    ssh -i codesonar.pem ubuntu@$PublicIp

and run (possible in a screen session is you know `screen`)

    sudo ./mount-volume.sh
    ./install_and_run_jenkins.sh

*Output from the script can be found the in log file* `jenkins_run.log`.
You can tail it in another sessions our use screen-program to have several other sessions open.

Now for the CodeSonar setup, run the following still on the server:

    ./install_codesonarhub.sh
    ./configure_and_start_codesonarhub.sh

Visit $PublicIp:7340/lines.html and:
* click `Configure license`
* Login with `Administrator` and password (as configured in the `configure_and_start_codesonarhub.sh` script)
* use email-address `codesonar@praqma.net`
* copy paste in the license information and click `Save`. Get the license, if you're a Praqma employees on our [private GoogleDrive folder `Jenkins CodeSonar Plugin testing resources`](https://drive.google.com/drive/folders/1qcFvmDRblKZl4W8R3jGYqgPfYhbwjRG0).

CodeSonar hub should now be usable (thanks to Linux virtual network cards with fixed MACs)


## Run test jobs

With Jenkins up and running and a CodeSonar Hub you can start running the the test jobs, that are created by the initial seed-job the Jenkins have already.

* Default parameters in the jobs are all set to work for the complete setup.
* You can see if CodeSonar Hub uses many works automatically on the Analysis Cloud page in CodeSonar Hub: http://$PublicIp:7340/manage_launchd.html?filter=3&prj_filter=11
* The projects are automatically created on CodeSonar hub - you don't have to do anything there.

### Job: build_and_analyze_codesonar_plugin_branch-master

**Expected result:** Should be successful and and finish first build in about 6-7 minutes (Maven downloads the world to start with ...) and less than 2 minutes in second run.

*Purpose of the job is to have a simple analysis to run that hows the setup and plugin works.*

### Job: build_and_analyze_linux_kernel_gitsha

Analysis of the Linux kernel job.

*Purpose of the job is to stress the Jenkins and the plugin in respect to memory usage, as an analysis of the Linux kernel generate many thousand warnings and consist of millions of line of code. The plugin save many data from warnings, and earlier had problems handling such big projects*.

The Linux kernel job is using a specific git sha of the kernel, so we get the same result each time, and it build based on the `config`-file we have in this repository so we also knows it builds the same things each time.

If you haven't done Linux kernel building before, you can read here:  https://www.makeuseof.com/tag/compile-linux-kernel/


**Expected result:**

* successful
* runs about 3 hours
* when building the linux kernel, all cpus in the instance is utilized nicely, also when doing the analysis in CodeSonar
* Lines With Code: 1,987,177
* About 13K warnings

![Result from CodeSonar Plugin and the CodeSonar analysis of the linux kernel build](build_and_analyze_linux_kernel_gitsha.png)
In the image the failing jobs were configuration error that are not fixed with recent job dsl.


### Job: build_and_analyze_linux_kernel_gitsha_preset-misra_inc

Analysis of the Linux kernel job, but with preset configuration for analysis checking for MISRA compliance.

The Linux kernel job is using a specific git sha of the kernel, so we get the same result each time, and it build based on the `config`-file we have in this repository so we also knows it builds the same things each time.

*Purpose of the job is to see what the limits are and if it can succeed at all.*.

**Expected results:**

* FAILED - **Job stopped, used 2 TB disk space**. We didn't want to take it further. CodeSonar generated 2 TB project data during the analysis, while Jenkins took about 16 GB for the console log of the job.
* Jeg analysis was running for 23 hours, and the status in CodeSonar was `Collecting Constants`, showing expected time as 1 day and 22 hours (and was still increasing at this time).

* CodeSonar plugin - at no point the Jenkins java process (monitored with Visualvm) used more a few GBs of memory, since the plugin never really got to do anything because basic analysis wasn't done yet. Post-build action wasn't reached.



### Job: wgen_generate_warnings

*Purpose is to auto generate a (large) amount of warnings, to stress test the plugin and setup. We use wgen.py, a script supplied by CodeSonar developers that can generate a number of warnings per line of code*.

The fibonacci.c program has 26 lines of code.

The warnings will contain much lesser data than real life data, as the messages are simpler and generated, but we can generate more warnings than a real project would if we like.

Number of warnings is input to the script, used as a plugin in CodeSonar.

Our earlier execution used the following inputs:

* build 1: 1 warning per line (26 lines)
* build 2: 2 warnings per line
* build 3: 10 warnings per line
* build 4: 100 warnings per line
* build 5: 1000 warnings per line
* build 6: 4000 warnings per line
* build 7: 10K warnings per line
* build 8: 30K warnings per line
* build 9: 50K warnings per line

**Expected result:**

* The above should all run successfully
* Execution times should not differ much.
  * build 9:	8 hr 53 min
  * build 8:	4 hr 44 min
  * build 7:	1 hr 48 min
  * build 6:	37 min
  * build 5:	8 min 51 sec
  * build 4:	1 min 8 sec
  * build 3:	29 sec
  * build 2:	25 sec
  * build 1:	34 sec

![wgen generate warnings job for the 9 above builds showing CodeSonar Plugin graphs](wgen_generate_warnings.png)

![wgen generate warnigns build time trends for the 9 above builds](wgen_generate_warnings Build time trend.png)


# Java monitoring

The scripted installation ensures several things are in place to do some Java monitoring:

* jstatd is running
* all java processes are started as single processes, not wrapped in containers and as other users
* jmx configuration java parameters are added to Jenkins java command, so jxm which allows more detailed monitoring is also available.
* the jenkins process runs a java - jar jenknins.war (with lots of parameters) so it can be monitored easily


Monitoring setup was created based on the following references, where also alternatives solutions can be found in some of them:

* https://blog.rapid7.com/2012/12/31/guide-to-monitoring-jvm-memory-usage-draft/#S3
* http://www.catify.com/2012/09/26/remote-monitoring-with-visualvm/
* https://stackoverflow.com/questions/14341485/visualvm-find-all-references-to-a-class/34120420#34120420
* https://stackoverflow.com/a/30070070
* https://www.jamasoftware.com/blog/monitoring-java-applications/


## Recommended monitoring

Install VisualVM on your local machine (Ubuntu users `sudo apt install visualvm`) and connect to the monitoring easily as follows:

* Add `Remote` using the $PublicIp, and then shortly after jstatd data shows up automatically for all the Java processes running.
* To the remove (right click) add jmx connection, and use port `1098` as configured in our installation and configuration scripts.

# Tips and tricks

## Running out of space?

Extend the disk, see [extend disk howto](extend-disk-howto.md)

## Debugging

From command line on the server you should be able to run the same commands as the Jenkins jobs, if you want a faster and more interactive method of debugging than trying to run jobs all the time.
Just clone the repository, call the same builds commands etc. *Maybe replace project name though!*

## You don't need to spawn CodeSonar workers


It's done automatically by the CodeSonar hub and the configuration we created.

Earlier there was issues with running Jenkins under another user, and permissions of file running CodeSonar Hub on another user.
We run it all under that same user, and as single processes so it all works automatically.


## You can run as many setups as you like

If you take care of the `create-aws-environment-resources.log` file, you can run as many AWS setups you like in parallel.
Notice each time you run `create-aws-environment`-script the resource log file is overwritten, and last one saved as `.last`.

## Cannot find device "eth0"

Problems with our creation of virtual nics - check `ifconfig` to see if commands matches interface name.

We experienced problems with `eth0` sometimes called `ens3` in AWS EC2 instances, maybe because of the instance types.

```
+ sudo ip link add link eth0 address 0a:f3:ce:99:59:2c eth0.1 type macvlan
Cannot find device "eth0"
+ sudo ip link add link eth0 address 0a:91:13:70:a3:7e eth0.2 type macvlan
Cannot find device "eth0"
+ sudo ifconfig eth0.1 up
eth0.1: ERROR while getting interface flags: No such device
+ sudo ifconfig eth0.2 up
eth0.2: ERROR while getting interface flags: No such device
```

Update commands in CodeSonar scripts in this repo to match real interface name.

* `install_codesonarhub.sh`
* `start_codesonarhub.sh`




## CodeSonar licenses

## "Breaking" the lock to specific MACs in the CodeSonar license

The CodeSonar license request you get and send is locked to a MAC address, and so is the signed request you get back.

This doesn't all play well with modern ways of working doing infrastructure as code and redeploying machines in AWS EC2, so we're circumventing it by creating virtual networks cards with fixed MAC addresses in Linux.

AWS EC2 doesn't allow to do it on the primary: https://forums.aws.amazon.com/thread.jspa?threadID=236739

Networking in Linux references:

  * https://unix.stackexchange.com/questions/21841/make-some-virtual-mac-address
  * https://ubuntuforums.org/showthread.php?t=1923806

## Our saved license

The original request we made, and the correspoding MACs at that time is shown below. You will these in scripts.

```
{
    "ip addresses" : [
        "10.0.1.136"
    ],
    "lock file" : "/tmp/codesonar_license.lock",
    "mac addresses" : [
        "0a:f3:ce:99:59:2c",
        "0a:91:13:70:a3:7e"
    ],
    "machine" : "ip-10-0-1-136",
    "name" : "CodeSonar",
    "user" : "ubuntu",
    "version" : 404
}
```

A valid license file for the above request, if you're a Praqma employees on our [private GoogleDrive folder `Jenkins CodeSonar Plugin testing resources`](https://drive.google.com/drive/folders/1qcFvmDRblKZl4W8R3jGYqgPfYhbwjRG0).


## Where is a file used?

There is a lot of scripts in the repository, that are tied tightly together. It's a proof-of-concept and should be transformed to Terraform, packer etc. later.

If in doubt where a script, path, directory etc. are used, just use grep from root of repo:

    grep -i -H -r "word" .

e.g.

    $ grep -i -H -r "plugins.txt" .
    Binary file ./.git/index matches
    ./install_and_run_jenkins.sh:./install_plugins.sh plugins.txt >> $LOG 2>&1
    ./copy-to-server.sh:plugins.txt
    ./install_plugins.sh:#Generate plugins.txt with this groovy snippet in the Jenkins script console on a running instance with the plugins you like:
    ./install_plugins.sh:  echo "USAGE: $0 plugins.txt"


## After rebooting EC2 instance

Start jstatd, Jenkins and CodeSonar again:

    ./start_services_if_not_running.sh
    ./start_codesonarhub.sh

## Import site error

If you run one of the expect script that starts `codesonar config` and you get something like import site error, it probably related to Python. We don't know why and when, but reboots solved problems earlier.
Remember to then start Jenkins and CodeSonar again.

    ./start_services_if_not_running.sh
    ./start_codesonarhub.sh


# Improvements

* Use packer
* Use Terraform
* Configure all common variable like Jenkins home, data dir, etc. in one common place across script, configuration etc.
* Make a java monitoring setup the trends all relevant data over the complete build time
