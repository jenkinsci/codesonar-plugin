#!/usr/bin/expect -f


set timeout 240

spawn codesonar config
expect "What would you like to do"
send "5\r"
expect "What hub would you like to stop"
send "1\r"
expect "Press ENTER to continue"
send "\r"
expect "What would you like to do?"
send "15\r"
expect eof
