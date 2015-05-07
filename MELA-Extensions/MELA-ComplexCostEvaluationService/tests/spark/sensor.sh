#!/bin/bash

dataInterval=1
sensorsNr=10000
while true;
     do
     for i in `seq 1 $sensorsNr`;
      do
            python ./sensor.py &
      done
     sleep $dataInterval
done

~                                                                                                                                                                                                            
~                                                                                                                                                                                                            
~                                                                                                                                                                                                            
~                                                                                                                                                                                                            
~                                                                                                                                                                                                            
~                                 
