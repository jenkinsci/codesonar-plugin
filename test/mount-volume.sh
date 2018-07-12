pvcreate /dev/xvdf
vgcreate data /dev/xvdf
lvcreate --name data -l 100%FREE data
mkfs.ext4 /dev/data/data
mkdir /home/ubuntu/data
mount /dev/data/data /home/ubuntu/data
chown -R ubuntu:ubuntu /home/ubuntu/data
/bin/bash -c 'echo "/dev/data/data    /home/ubuntu/data    ext4    defaults    0    1" >> /etc/fstab'
cat /etc/fstab
df -h
