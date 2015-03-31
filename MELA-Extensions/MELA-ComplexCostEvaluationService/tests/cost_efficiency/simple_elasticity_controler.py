#!/usr/bin/python

import sys, httplib, uuid, random, time
from threading import Thread

httplib.HTTPConnection.debuglevel = 0

KeyspaceName = 'm2m'
tablename = 'sensor'

SALSA_URL = '128.130.172.215:8380'

MELA_URL = '128.130.172.214:8180'

MELA_COST_URL = 'localhost:8480'

#SALSA CALLS
#============================
#SCALE IN
#@POST
#@Path("/services/{serviceId}/vmnodes/{ip}/scalein")
#	
#SCALE OUT
#@POST
#@Path("/services/{serviceId}/vmnodes/{ip}/scaleOutNode")
#@Path("/services/" + serviceId +"/topologies/"+ topologyId + "/nodes/"+ nodeId +"/instance-count/" + quantity)


#MELA CALLS
#========================
# GET METRIC
# @GET
# @Path("/{serviceID}/monitoringdata/{monitoredElementID}/{monitoredElementLevel}/{metricName}/{metricUnit}")
# 
# SUBMIT SERVICE STRUCTURE
# @PUT
# @Path("/service")
# @Consumes("application/xml")
# public void putServiceDescription(MonitoredElement element) 
#
# get cost efficiency if scaling X unit. USed to compare efficiency of scaling actions for different strategies
# /{serviceID}/cost/evaluate/costefficiency/scalein/{monitoredElementID}/{monitoredElementLevel}/{unitInstanceID}/plain
#
#
#


upperResponseTimeLimit=30
lowerResponseTimeLimit=5

eventProcessingStartIP="10.99.0.93"
loadBalancerStartIP="10.99.0.77"

STRATEGY_LAST_ADDED="LAST_ADDED"
STRATEGY_FIRST_ADDED="FIRST_ADDED"
STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY="MELA_COST_RECOMMENDATION_EFFICIENCY"
STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME="MELA_COST_RECOMMENDATION_LIFETIME"
 
listOfEventProcessingDescriptions = []
listOfEventProcessingIPs = []
serviceId= "EventProcessingTopology"
serviceSALSAId= "EventProcessingTopologyCostDaniel"


serviceStructure_1='<?xml version="1.0" encoding="UTF-8" standalone="yes"?><MonitoredElement level="SERVICE" name="'+serviceId+'" id="'+serviceId+'"><MonitoredElement level="SERVICE_TOPOLOGY" name="EventProcessingTopology" id="EventProcessingTopology"> <MonitoredElement level="SERVICE_UNIT" name="EventProcessingUnit" id="EventProcessingUnit"> <MonitoredElement level="VM" name="'+eventProcessingStartIP+'" id="'+eventProcessingStartIP+'"><UsedCloudOfferedServiceCfg name="1CPU1" uuid="20000000-0000-0000-0000-000000000001"  instanceUUID="98400000-8cf0-11bd-b23e-000000000000" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="CloudStorage" uuid="30000000-0000-0000-0000-000000000002"  instanceUUID="98400000-8cf0-11bd-b23e-000000000016" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"> <QualityProperties/> <ResourceProperties/> </UsedCloudOfferedServiceCfg> </MonitoredElement>'

serviceStructure_2='<UsedCloudOfferedServiceCfg name="ImageStorage" uuid="40000000-0000-0000-0000-000000000001" instanceUUID="98400000-8cf0-11bd-b23e-000000000001" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg></MonitoredElement><MonitoredElement level="SERVICE_UNIT" name="LoadBalancer" id="LoadBalancer"> <MonitoredElement level="VM" name="'+loadBalancerStartIP+'" id="'+loadBalancerStartIP+'"> <UsedCloudOfferedServiceCfg name="2.0CPU4.0" uuid="20000000-0000-0000-0000-000000000003"  instanceUUID="98400000-8cf0-11bd-b23e-000000000002" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="PublicVLAN" uuid="30000000-0000-0000-0000-000000000001"  instanceUUID="98400000-8cf0-11bd-b23e-000000000003" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="CloudStorage" uuid="30000000-0000-0000-0000-000000000002"  instanceUUID="98400000-8cf0-11bd-b23e-000000000015" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"> <QualityProperties/> <ResourceProperties/></UsedCloudOfferedServiceCfg></MonitoredElement></MonitoredElement></MonitoredElement> </MonitoredElement>'

def executeRESTCall(restMethod, serviceBaseURL, resourceName):
        print "connecting to " + serviceBaseURL + '/'+resourceName
        headers={
                'Content-Type':'plain/txt; charset=utf-8'
        }
        connection =  httplib.HTTPConnection(serviceBaseURL)
        connection.request(restMethod, '/'+resourceName,headers=headers)
        result = connection.getresponse()
        response  = result.read()
        connection.close()
        print result.status, result.reason
        print response
	return response

def executeRESTCallWithContent(restMethod, serviceBaseURL, resourceName,  content):
        print "connecting to " + serviceBaseURL + '/'+resourceName
        connection =  httplib.HTTPConnection(serviceBaseURL)
       
        headers={
                'Content-Type':'application/xml; charset=utf-8'
        }

        connection.request(restMethod, '/'+resourceName, body=content,headers=headers)
        result = connection.getresponse()
        response  = result.read()
        connection.close()
        print result.status, result.reason
        print response
	return response	
 
def scaleInWithSALSA(ip):
	executeRESTCall("POST", SALSA_URL, "salsa-engine/rest/services/"+serviceSALSAId + "/vmnodes/"+ip+"/scalein")

def scaleOutWithSALSA():
	return str(executeRESTCall("POST", SALSA_URL, "salsa-engine/rest/services/"+serviceSALSAId + "/nodes/EventProcessingUnit/scaleout"))

def createNewMonElementSpecification(ip):
      newElementText='<MonitoredElement level="VM" name="'+ip+'" id="'+ip+'"><UsedCloudOfferedServiceCfg name="1CPU1" uuid="20000000-0000-0000-0000-000000000001"  instanceUUID="'+str(uuid.uuid4())+'" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="CloudStorage" uuid="30000000-0000-0000-0000-000000000002"  instanceUUID="'+str(uuid.uuid4())+'" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"> <QualityProperties/> <ResourceProperties/> </UsedCloudOfferedServiceCfg> </MonitoredElement>'
      return newElementText

def updateMELAServiceDescriptionAfterScaleIn(ip):
     #remove element with Ip from list
     #MELA returns only Ip ans I need to call SALSA
     listOfEventProcessingDescriptions[:] = [element for element in listOfEventProcessingDescriptions if not ip in element]
     listOfEventProcessingIPs.remove(ip)
     for t in listOfEventProcessingDescriptions:
        print t

def updateMELAServiceDescriptionAfterScaleOut(ip):
     newElement = createNewMonElementSpecification(ip)
     listOfEventProcessingIPs.append(ip)
     listOfEventProcessingDescriptions.append(newElement)
     for t in listOfEventProcessingDescriptions:
        print t

def submitUpdatedDescriptionToMELA():
     completeText = "" + serviceStructure_1;
     for element in listOfEventProcessingDescriptions:
       completeText = completeText + element
     completeText = completeText + serviceStructure_2
     print completeText
     executeRESTCallWithContent("PUT", MELA_URL,"MELA/REST_WS/service", completeText)

def getScaleInRecommendationFromMELABasedOnCostEffciency():
     ipToScaleIn=""
     while not ipToScaleIn:
       ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/plain")
       print "Received no IP to scale in, so waiting and trying again in one minute"
       time.sleep(60)
     print "Received IP to scale " + ipToScaleIn
     return ipToScaleIn

def getScaleInRecommendationFromMELABasedOnLifetime():
     ipToScaleIn=""
     while not ipToScaleIn:
       ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/lifetime/scalein/EventProcessingUnit/SERVICE_UNIT/plain")
       print "Received no IP to scale in, so waiting and trying again in one minute"
       time.sleep(60)
     print "Received IP to scale " + ipToScaleIn
     return ipToScaleIn

def getResponseTimeMetricFromMELA():
     metricValue = executeRESTCall("GET",MELA_URL,"MELA/REST_WS/"+serviceId+"/monitoringdata/EventProcessingUnit/SERVICE_UNIT/avgResponseTime/ms")
     print "Received Metric value '" + metricValue + "'"
     if metricValue:
       return float(metricValue)
     else:
       print "Nothing returned as value, so we replace with 0, which does not trigger anything"
       return 0

def getCostEfficiencyForScalingInFromMELA(ip):
     efficiency = executeRESTCall("GET",MELA_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/"+ip+"/plain")
     print "Evaluated cost efficiency for scaling in "+ ip + " is '" + efficiency + "'"
     if efficiency:
       return float(efficiency)
     else:
       print "Nothing returned as efficiency, so we replace with -1, which means error"
       return -1

def getIPToScaleIn(strategy):
   if len(listOfEventProcessingDescriptions) > 1 :
	   if strategy == STRATEGY_LAST_ADDED:
	      return listOfEventProcessingIPs.pop()
	   elif strategy == STRATEGY_FIRST_ADDED :
	      return listOfEventProcessingIPs.pop(0)
	   elif strategy == STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY:
	      return getScaleInRecommendationFromMELABasedOnCostEffciency()
	   elif strategy == STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME:
	      return getScaleInRecommendationFromMELABasedOnLifetime()
           else:
              print "Unknown strategy " + strategy
   else:
       print "only one event processing instance remaining, not scaling in"
       return "" 
   
def persistCostEfficiencyInFile(scaledIP,efficiency,strategy):
        f = open("./"+strategy+"_efficiency.csv", "a+")
        f.write(scaledIP+ "," + efficiency + "\n")
	f.close()

def monitorResponseTimeAndAct(strategy):
    while True:
       metricValue = getResponseTimeMetricFromMELA()
       if metricValue:
	       if metricValue >= upperResponseTimeLimit:
		  print "Scaling OUT"
		  scaledIP = scaleOutWithSALSA()
		  if scaledIP:
		     updateMELAServiceDescriptionAfterScaleOut(scaledIP)
		     submitUpdatedDescriptionToMELA()
		  else:
		     print "SALSA returned no IP"
	       elif metricValue <= lowerResponseTimeLimit:
		   print "Scaling IN"
		   iPToScaleIn = getIPToScaleIn(strategy)
                   efficiencyIfScalingIn = getCostEfficiencyForScalingInFromMELA(iPToScaleIn)
                   persistCostEfficiencyInFile(iPToScaleIn,efficiencyIfScalingIn,strategy)
		   scaleInWithSALSA(iPToScaleIn)
		   updateMELAServiceDescriptionAfterScaleIn(iPToScaleIn)
		   submitUpdatedDescriptionToMELA()	
               else:
                   print "Metric value " + str(metricValue)+ " > " + str(lowerResponseTimeLimit) + " and < "  + str(upperResponseTimeLimit) + ". So doing nothing"
       else:
          print "Metric value is zero, so doing nothing" 
       time.sleep(60)

if __name__=='__main__':
     #starting control process
     strategyInUse = STRATEGY_FIRST_ADDED
     f = open("./"+strategyInUse+"_efficiency.csv", "w+")
     f.write("IP,EFFICIENCY \n")
     f.close()
     updateMELAServiceDescriptionAfterScaleOut("10.99.0.93")
     monitorResponseTimeAndAct(strategyInUse)
 
