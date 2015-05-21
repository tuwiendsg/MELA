#!/bin/bash

python ./submitMetricCompositionRules.py "SparkCluster_0" 
python ./submitMetricCompositionRules.py "SparkCluster_idle" 
python ./submitMetricCompositionRules.py "SparkCluster_1000"
python ./submitMetricCompositionRules.py "SparkCluster_5000"
python ./submitMetricCompositionRules.py "SparkCluster_10000"
python ./submitMetricCompositionRules.py "SparkCluster_50000"

python ./submitServiceDescription.py "0.xml" 
python ./submitServiceDescription.py "idle.xml" 
python ./submitServiceDescription.py "1.xml"
python ./submitServiceDescription.py "2.xml"
python ./submitServiceDescription.py "3.xml"
python ./submitServiceDescription.py "4.xml"



