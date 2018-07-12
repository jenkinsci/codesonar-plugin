#!/bin/bash


# for experimenting
#InstanceType=t2.micro
# for running real test with the plugin
InstanceType=r4.4xlarge

# name of a file to log created resources into, so we can destroy later
RESFILE=create-aws-environment-resources.log
# Value to tags AWS EC2 resources with:
NAME="CodeSonar-Plugin-test-infra"



# Helper methods for the script

# clean log
echo "" > $RESFILE

tagResources() {
  RES_ID=$1
  aws ec2 create-tags --resources $RES_ID --tags "Key=Name,Value=$NAME"
}

logAndTag() {
  log $1 $2
  tagResources $2
}

log() {
  RES_NAME=$1
  RES_ID=$2
  KV="$RES_NAME=$RES_ID"
  echo $KV
  echo $KV >> $RESFILE
}

# Creating AWS EC2 stuff

# We will completely isolate the instance on VPC, subnet etc.
# and open for traffice for our fixed IP to our locations

VpcId=$(aws ec2 create-vpc --cidr-block 10.0.0.0/16 --output text --query 'Vpc.VpcId')
logAndTag "VpcId" $VpcId
aws ec2 modify-vpc-attribute --vpc-id $VpcId --enable-dns-hostnames "{\"Value\":true}"

SubnetId=$(aws ec2 create-subnet --vpc-id $VpcId --cidr-block 10.0.1.0/24 --output text --query 'Subnet.SubnetId')
logAndTag "SubnetId" $SubnetId
aws ec2 modify-subnet-attribute --subnet-id $SubnetId --map-public-ip-on-launch


InternetGatewayId=$(aws ec2 create-internet-gateway --output text --query 'InternetGateway.InternetGatewayId')
logAndTag "InternetGatewayId" $InternetGatewayId
aws ec2 attach-internet-gateway --internet-gateway-id $InternetGatewayId --vpc-id $VpcId

RouteTableId=$(aws ec2 describe-route-tables --filters Name=vpc-id,Values=$VpcId --output text --query 'RouteTables[].RouteTableId')

aws ec2 create-route --route-table-id $RouteTableId --destination-cidr-block 0.0.0.0/0 --gateway-id $InternetGatewayId
# only returns true

GroupId=$(aws ec2 create-security-group --group-name codesonar --vpc-id $VpcId --description "Security group for $NAME" --output text --query 'GroupId')
logAndTag "GroupId" $GroupId

# CPh Office
aws ec2 authorize-security-group-ingress --group-id $GroupId --ip-permissions '[{"IpProtocol": "tcp", "FromPort":0, "ToPort":65535,  "IpRanges": [{"CidrIp": "5.103.134.138/32"}]}]'
# Allerød Office
aws ec2 authorize-security-group-ingress --group-id $GroupId --ip-permissions '[{"IpProtocol": "tcp", "FromPort":0, "ToPort":65535,  "IpRanges": [{"CidrIp": "5.57.50.214/32"}]}]'
# Bue's IP
aws ec2 authorize-security-group-ingress --group-id $GroupId --ip-permissions '[{"IpProtocol": "tcp", "FromPort":0, "ToPort":65535,  "IpRanges": [{"CidrIp": "213.32.247.26/32"}]}]'


NetworkInterfaceId=$(aws ec2 create-network-interface --subnet-id $SubnetId --description "Network interface for $NAME" --groups $GroupId --output text --query 'NetworkInterface.NetworkInterfaceId')
logAndTag "NetworkInterfaceId" $NetworkInterfaceId

InstanceId=$(aws ec2 run-instances --image-id ami-58d7e821 --security-group-ids $GroupId --subnet-id $SubnetId --count 1 --instance-type $InstanceType --key-name codesonar --output text --query 'Instances[0].InstanceId')
logAndTag "InstanceId" $InstanceId
while state=$(aws ec2 describe-instances --instance-ids $InstanceId --output text --query 'Reservations[*].Instances[*].State.Name'); test "$state" = "pending"; do
  sleep 1; echo -n '.'
done; echo " $state"
aws ec2 create-tags --resource $InstanceId  --tags "Key=Status,Value=Test"
aws ec2 create-tags --resource $InstanceId  --tags "Key=Maintainier,Value=bue@praqma.net"

AvailabilityZone=$(aws ec2 describe-instances --instance-ids $InstanceId --output text --query 'Reservations[*].Instances[*].Placement.AvailabilityZone')
log "AvailabilityZone" $AvailabilityZone

aws ec2 attach-network-interface --network-interface-id $NetworkInterfaceId --instance-id $InstanceId --device-index 1

AllocationId=$(aws ec2 allocate-address --domain vpc --output text --query 'AllocationId')
logAndTag "AllocationId" $AllocationId

AssociationId=$(aws ec2 associate-address --network-interface-id $NetworkInterfaceId --allocation-id $AllocationId --output text --query 'AssociationId')

VolumeId=$(aws ec2 create-volume --size 1000 --region eu-west-1 --availability-zone $AvailabilityZone --tag-specifications 'ResourceType=volume,Tags=[{Key=purpose,Value=CodeSonar-Plugin-test-infra}]' --output text --query 'VolumeId')
logAndTag "VolumeId" $VolumeId
while state=$(aws ec2 describe-volumes --volume-ids $VolumeId --output text --query 'Volumes[*].State'); test "$state" != "available"; do
  sleep 1; echo -n '.'
done; echo " $state"

aws ec2 attach-volume --volume-id $VolumeId --instance-id $InstanceId --device /dev/sdf

PublicIp=$(aws ec2 describe-instances --instance-ids $InstanceId --output text --query 'Reservations[*].Instances[*].PublicIpAddress')
log "PublicIp" $PublicIp

echo "Done creating AWS setup"
echo "Connect using ssh -i codesonar.pem ubuntu@$PublicIp"
