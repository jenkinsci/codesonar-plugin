# Extend EC2 instance disk howto


Should there be a need for more disk-space on one of the machines, it's easy to extend the disk space as we use LVM.

Basically create AWS EC2 volume, attach to instance and inside instance add to LVM and extend logical disk. It can all be done live - nothing needs to shut down.


With your current setup and used resource log file, run these command to create the AWS parts:

    source create-aws-environment-resources.log

    VolumeId2=$(aws ec2 create-volume --size 1000 --region eu-west-1 --availability-zone $AvailabilityZone --tag-specifications 'ResourceType=volume,Tags=[{Key=purpose,Value=CodeSonar-Plugin-test-infra}]' --output text --query 'VolumeId')

    echo "VolumneId2=$VolumeId2" >> create-aws-environment-resources.log

Wait for it to come up, check: https://eu-west-1.console.aws.amazon.com/ec2/v2/home?region=eu-west-1#Volumes:sort=desc:createTime

Then attach to instance:

    aws ec2 attach-volume --volume-id $VolumeId2 --instance-id $InstanceId --device /dev/sdg


**Remember to manually delete the volume when tearing down the machine again**.


Now ssh into the machine and to the LVM magic.

Find disk:

    $ lsblk
    NAME        MAJ:MIN RM  SIZE RO TYPE MOUNTPOINT
    xvda        202:0    0    8G  0 disk
    └─xvda1     202:1    0    8G  0 part /
    xvdf        202:80   0 1000G  0 disk
    └─data-data 252:0    0 1000G  0 lvm  /home/ubuntu/data
    xvdg        202:96   0 1000G  0 disk


Prepare disk for lvm:

    $ sudo pvcreate /dev/xvdg
      Physical volume "/dev/xvdg" successfully created


We now our current volume group is called `data` to extend it:


    $ sudo vgextend data /dev/xvdg
      Volume group "data" successfully extended

Now add it logical volume:


    $ sudo lvm lvextend -l +100%FREE /dev/data/data
      Size of logical volume data/data changed from 1000.00 GiB (255999 extents) to 1.95 TiB (511998 extents).
      Logical volume data successfully resized.

Now there is 2GB instead of 1GB:


      $ df -h
      Filesystem             Size  Used Avail Use% Mounted on
      udev                    60G     0   60G   0% /dev
      tmpfs                   12G  8.6M   12G   1% /run
      /dev/xvda1             7.7G  2.4G  5.4G  30% /
      tmpfs                   60G     0   60G   0% /dev/shm
      tmpfs                  5.0M     0  5.0M   0% /run/lock
      tmpfs                   60G     0   60G   0% /sys/fs/cgroup
      tmpfs                   12G     0   12G   0% /run/user/1000
      /dev/mapper/data-data  2.0T  685G  1.2T  37% /home/ubuntu/data


Good reference:

* https://www.cyberciti.biz/faq/howto-add-disk-to-lvm-volume-on-linux-to-increase-size-of-pool
