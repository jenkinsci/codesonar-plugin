#!/bin/bash

set -x

FILE=codesonar-4.4p0.20161021-x86_64-pc-linux.tar.gz
# copy to make script able to rerun
cp -v $HOME/$FILE $HOME/data
tar xvzf $HOME/data/$FILE -C $HOME/data
cd /usr/bin
sudo ln -fs $HOME/data/codesonar-4.4p0/codesonar/bin/codesonar codesonar

echo "creating interfaces needed for codesonar"
REAL_NIC=$(ip addr show | awk '/inet.*brd/{print $NF}')
sudo ip link add link $REAL_NIC address 0a:f3:ce:99:59:2c eth0.1 type macvlan
sudo ip link add link $REAL_NIC address 0a:91:13:70:a3:7e eth0.2 type macvlan
sudo ifconfig eth0.1 up
sudo ifconfig eth0.2 up
