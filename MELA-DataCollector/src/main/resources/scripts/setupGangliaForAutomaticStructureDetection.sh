#!/bin/bash
GANGLIA_CONFIG_DIR=/etc/ganglia
GANGLIA_MODULES_CONFIG_DIR=/etc/ganglia/conf.d
GANGLIA_MODULES_CONFIG_FILE=/etc/ganglia/conf.d/modpython.conf
GANGLIA_PLUGINS_DIR=

MELA_GANGLIA_CONFIG_FILES_LOCATION=./automaticStructureDetectionCfg

EXTRACT_GANGLIA_PLUGINS_DIR_CMD="cat $GANGLIA_MODULES_CONFIG_FILE | grep params | grep -o \"/.*\""

#check if ganglia config dir exists
if [ ! -d "$GANGLIA_CONFIG_DIR" ]; then
     echo "Ganglia configuration folder " + GANGLIA_CONFIG_DIR + " was not found. Please install Ganglia or point to correct configuration directory";
     exit;
fi

#check if ganglia dir where the configuration files for new ganglia plug-ins exists, and if not, create it and copy the modpython.conf
if [ ! -d "$GANGLIA_CONFIG_DIR" ]; then
     su -S mkdir 
fi
   
if [! -f "$GANGLIA_MODULES_CONFIG_FILE" ]
 #copy file which activates Python modules
 sudo -S cp $MELA_GANGLIA_CONFIG_FILES_LOCATION/modpython.conf $GANGLIA_MODULES_CONFIG_FILE       
fi

#extract path where Ganglia Python plug-ins need to be copied 
GANGLIA_PLUG_INS_DIR = $EXTRACT_GANGLIA_PLUGINS_DIR_CMD;

#copy Ganglia plug-ins config files
sudo -S cp $MELA_GANGLIA_CONFIG_FILES_LOCATION/getServiceIDPlugIn.conf $GANGLIA_CONFIG_DIR/getServiceIDPlugIn.conf

#copy Ganglia plug-ins

sudo -S cp $MELA_GANGLIA_CONFIG_FILES_LOCATION/getServiceUnitID.py $GANGLIA_PLUG_INS_DIR/getServiceUnitID.py

#restart ganglia such that new plug-ins are activated
sudo -S service ganglia-monitor restart
