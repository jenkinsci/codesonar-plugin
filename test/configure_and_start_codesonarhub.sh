#!/usr/bin/expect -f

# timeout have to be long in case startup waits for ports
# timeout doesn't matter if everything goes smooth as the output
# is detected and we continue.
set timeout 240

spawn codesonar config
expect "Do you accept the terms of this license agreement"
send "y\r"
expect "What would you like to do"
send "2\r"
expect "What kind of hub would you like to create"
send "1\r"
expect "What interface should this hub use"
send "1\r"
expect "Specify a port"
send "7340\r"
expect "What directory should be used to store hub data"
send "/home/ubuntu/data/cshub\r"
expect "Would you like to use this as the default hub for all analyses"
send "y\r"
expect "What level of analysis parallelism should be used on this installation"
send "1\r"
expect "Should analyses performed on this installation utilize the hub cluster's analysis cloud"
send "1\r"
expect "Should this installation contribute to the selected hub cluster's analysis cloud"
send "1\r"
expect "The analysis cloud is made up of smaller groups called Launch Daemon Groups."
send "/\r"
expect "A background process is required to perform analyses."
send "Administrator\r"
expect "Provide a new password for the hub's Administrator account"
send "praqma\r"
expect "Please retype the password to verify."
send "praqma\r"
expect "A -hubuser was specified in without an '-auth' option. Assuming '-auth password'."
send "praqma\r"
expect "The license setup page can be viewed at"
send "\r"
expect "What would you like to do?"
send "15\r"
expect eof
