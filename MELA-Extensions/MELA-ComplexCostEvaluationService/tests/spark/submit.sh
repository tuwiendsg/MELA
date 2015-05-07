#!/bin/bash

python ./submitMetricCompositionRules.py "SparkCluster_1" 
python ./submitMetricCompositionRules.py "SparkCluster_2"

python ./submitServiceDescription.py "1.xml" 
python ./submitServiceDescription.py "2.xml"



