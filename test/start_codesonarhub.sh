#!/bin/bash

set +x

echo "creating virtual network cards with fixed mac before starting cshub"

# find network name, some times eth0 or ens3

REAL_NIC=$(ip addr show | awk '/inet.*brd/{print $NF}')
sudo ip link add link $REAL_NIC address 0a:f3:ce:99:59:2c eth0.1 type macvlan
sudo ip link add link $REAL_NIC address 0a:91:13:70:a3:7e eth0.2 type macvlan
sudo ifconfig eth0.1 up
sudo ifconfig eth0.2 up

echo "running start hub helper script"

./start_codesonarhub_helperscript.sh
