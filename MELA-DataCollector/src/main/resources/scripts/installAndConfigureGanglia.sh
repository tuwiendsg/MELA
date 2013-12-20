#!/bin/bash
GANGLIA_CONFIG_FILE=/etc/ganglia/gmond.conf
CONSIDER_HOST_DEAD_INTERVAL=30 #seconds

sudo -S apt-get install ganglia-monitor gmetad

 
#configure Ganglia to remove hosts which do not respond for 30 seconds. 
sudo -S sed -i.back 's/host_dmax = [0-9]* /host_dmax = $CONSIDER_HOST_DEAD_INTERVAL /' $GANGLIA_CONFIG_FILE
sudo -S sed -i.back 's/cleanup_threshold = [0-9]* /cleanup_threshold = $CONSIDER_HOST_DEAD_INTERVAL /' $GANGLIA_CONFIG_FILE

#also configure Ganglia for automatic detection
echo "Configure Ganglia for automatic detectionM?"

select REPLY in "Y" "N" ; do

case $REPLY in
	'Y' )
         sudo -S ./setupGangliaForAutomaticStructureDetection.sh
         break;;
        'N')
 	 break;;
esac
done  



