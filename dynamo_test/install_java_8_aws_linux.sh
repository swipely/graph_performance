#!/bin/sh
[ -z "$1" ] && echo "Must supply IP of host"
ssh -t ec2-user@$1 "sudo yum install java-1.8.0 && sudo yum remove java-1.7.0-openjdk"
