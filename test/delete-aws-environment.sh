

# Script contructed by figuring out which resources needs to be deleted first due to dependencies.
source create-aws-environment-resources.log

aws ec2 terminate-instances --instance-ids $InstanceId
while state=$(aws ec2 describe-instances --instance-ids $InstanceId --output text --query 'Reservations[*].Instances[*].State.Name'); test "$state" != "terminated"; do
  sleep 1; echo -n '.'
done; echo " $state"
aws ec2 delete-volume --volume-id $VolumeId
aws ec2 delete-network-interface --network-interface-id $NetworkInterfaceId
aws ec2 release-address --allocation-id $AllocationId
aws ec2 delete-security-group --group-id $GroupId
aws ec2 detach-internet-gateway --internet-gateway-id $InternetGatewayId --vpc-id $VpcId
aws ec2 delete-internet-gateway --internet-gateway-id $InternetGatewayId
aws ec2 delete-subnet --subnet-id $SubnetId
aws ec2 delete-vpc --vpc-id $VpcId

mv -vf create-aws-environment-resources.log create-aws-environment-resources.log.last
