#!/bin/bash

echo "This scripts clean the codesonar configuration so you can run configure expect script once more."
echo "remember there is a stop_codesonarhub.sh script also to stop it first"
sleep 5

rm -rfv /home/ubuntu/.csurf/codesonar /home/ubuntu/data/cshub
cp -fv codesonar-reset-UNACCEPTED_LICENSE.txt /home/ubuntu/data/codesonar-4.4p0/UNACCEPTED_LICENSE.txt

echo "now run configure_and_start_codesonarhub.sh script again"
