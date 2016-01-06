#!/bin/sh
[ -z "$1" ] && echo "Must supply IP of host"
rsync -avz -e "ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null" --progress out/artifacts/DynamoTest_jar ec2-user@$1:/home/ec2-user
