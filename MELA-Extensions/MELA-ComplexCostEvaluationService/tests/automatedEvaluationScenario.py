#!/usr/bin/python

#########################################
#
#
# Scenario:
#  1. Clean application, no inside behavior issues. Predictable performance
#  2. Actions are independent. 
#  3. Load is uniformly distributed => no load skew. Thus, any scaled IN event processing has same efect.
#
#
#########################################
import sys, httplib, uuid, random, time, json, subprocess, StringIO, datetime
from threading import Thread

httplib.HTTPConnection.debuglevel = 0

KeyspaceName = 'm2m'
tablename = 'sensor'

SALSA_URL = 'localhost:8380'

MELA_URL = 'localhost:8180'

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

eventProcessingStartIP="10.99.0.15"
loadBalancerStartIP="10.99.0.9"

STRATEGY_LAST_ADDED="LAST_ADDED"
STRATEGY_FIRST_ADDED="FIRST_ADDED"
RANDOM="RANDOM"
STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY="MELA_COST_RECOMMENDATION_EFFICIENCY"
STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME="MELA_COST_RECOMMENDATION_LIFETIME"
 
listOfEventProcessingDescriptions = []
listOfEventProcessingIPs = []
serviceId= "EventProcessingTopology"
serviceSALSAId= "EventProcessingTopologyCostDaniel"

strategiesList = [STRATEGY_LAST_ADDED,STRATEGY_FIRST_ADDED,RANDOM,STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY]

serviceStructure_1='<?xml version="1.0" encoding="UTF-8" standalone="yes"?><MonitoredElement level="SERVICE" name="'+serviceId+'" id="'+serviceId+'"><MonitoredElement level="SERVICE_TOPOLOGY" name="EventProcessingTopology" id="EventProcessingTopology"> <MonitoredElement level="SERVICE_UNIT" name="EventProcessingUnit" id="EventProcessingUnit">'

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
     ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/plain")

     while not ipToScaleIn:
       print "Received no IP to scale in, so waiting and trying again in one minute: cost efficiency"
       time.sleep(60)
       ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/plain")
       
     print "Received IP to scale " + ipToScaleIn
     return ipToScaleIn

def getScaleInRecommendationFromMELABasedOnLifetime():
     ipToScaleIn=""
     ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/lifetime/scalein/EventProcessingUnit/SERVICE_UNIT/plain")
       
     while not ipToScaleIn:
       print "Received no IP to scale in, so waiting and trying again in one minute: lifetime"
       time.sleep(60)
       ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/lifetime/scalein/EventProcessingUnit/SERVICE_UNIT/plain")
       
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
     efficiency = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/"+ip+"/plain")
     print "Evaluated cost efficiency for scaling in "+ ip + " is '" + efficiency + "'"
     if efficiency:
       return float(efficiency)
     else:
       print "Nothing returned as efficiency, so we replace with -1, which means error"
       return -1

def evaluateAndPersistCostEfficiencyForScalingAllStrategies():
   if len(listOfEventProcessingIPs) > 1:   
     scalingStrategiesIPs = {} 
     for strategy in strategiesList:     
        print "ReturnedIP for " + strategy + " is " + getIPToScaleIn(strategy) 
     	scalingStrategiesIPs[strategy] = getIPToScaleIn(strategy)
      
     ipSStringForMela=""
     for k, v in scalingStrategiesIPs.iteritems():
        ipSStringForMela = ipSStringForMela + "-" + v

     #remove first -
     ipSStringForMela = ipSStringForMela[1:]

     efficiency = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/more/EventProcessingUnit/SERVICE_UNIT/"+ipSStringForMela+"/plain")
     #efficiency returned as JSON so need to parse it
     jsonRepresentation = json.loads(efficiency)
      
     efficiencyCSVLine = "" + str(int(time.time())) + "," + str(datetime.datetime.now());
      
     for i in range(0, len(jsonRepresentation) - 1):
        report  = jsonRepresentation[i]
        efficiencyCSVLine = efficiencyCSVLine + "," + str(report["ip"]) + "," + str(report["efficiency"])
        	
     f = open("./efficiency.csv", "a+")
     f.write(efficiencyCSVLine + "\n")
     f.close()
     #sys.exit("ddd")

	
    
def getIPToScaleIn(strategy):
   if len(listOfEventProcessingDescriptions) > 1 :
	   if strategy == STRATEGY_LAST_ADDED:
	      return listOfEventProcessingIPs[len(listOfEventProcessingIPs)-1]
	   elif strategy == STRATEGY_FIRST_ADDED :
	      return listOfEventProcessingIPs[0]
	   elif strategy == RANDOM:
	      return listOfEventProcessingIPs[random.randint(0,len(listOfEventProcessingIPs)-1)]
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
                   #first record decision efficiency of all strategies
                   evaluateAndPersistCostEfficiencyForScalingAllStrategies()
		   iPToScaleIn = getIPToScaleIn(strategy)
		   scaleInWithSALSA(iPToScaleIn)
		   updateMELAServiceDescriptionAfterScaleIn(iPToScaleIn)
		   submitUpdatedDescriptionToMELA()	
               else:
                   print "Metric value " + str(metricValue)+ " > " + str(lowerResponseTimeLimit) + " and < "  + str(upperResponseTimeLimit) + ". So doing nothing"
       else:
          print "Metric value is zero, so doing nothing" 
       time.sleep(60)

def monitorResponseTimeAndOnlyScaleIn(strategy):
    while len(listOfEventProcessingIPs) > 1:
       metricValue = getResponseTimeMetricFromMELA()
       if metricValue:
	       if metricValue >= upperResponseTimeLimit:
		  print "It seems we should scale out, so we do"
		  scaledIP = scaleOutWithSALSA()
		  if scaledIP:
		     updateMELAServiceDescriptionAfterScaleOut(scaledIP)
		     submitUpdatedDescriptionToMELA()
		  else:
		     print "SALSA returned no IP"
	       elif metricValue <= lowerResponseTimeLimit:
		   print "Scaling IN"
                   #first record decision efficiency of all strategies
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


def directScaleIn(strategy):
    if len(listOfEventProcessingIPs) > 1:
          print "Evaluated all strategies" 
          evaluateAndPersistCostEfficiencyForScalingAllStrategies()
	  #first record decision efficiency of all strategies
          print "Getting IP for strategy " + strategy
	  iPToScaleIn = getIPToScaleIn(strategy)
	  scaleInWithSALSA(iPToScaleIn)
	  updateMELAServiceDescriptionAfterScaleIn(iPToScaleIn)
	  submitUpdatedDescriptionToMELA()	
     
def directScaleOut():
    scaledIP = scaleOutWithSALSA()
    if scaledIP:
      updateMELAServiceDescriptionAfterScaleOut(scaledIP)
      submitUpdatedDescriptionToMELA()
    else:
      print "SALSA returned no IP"	

def scaleOutAndInchreaseLoad(proc):
    directScaleOut()
    if proc:
       subprocess.call(["sudo -S kill", "-9", "%d" % proc.pid])
       proc = subprocess.Popen(["python ./load.py " + loadBalancerStartIP + " " + str(len(listOfEventProcessingIPs) * 90)], shell=True)

def scaleInAndInchreaseLoad(proc):
    directScaleIn(STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY)
    if proc:
       subprocess.call(["sudo -S kill", "-9", "%d" % proc.pid])
       proc = subprocess.Popen(["python ./load.py " + loadBalancerStartIP + " " + str(len(listOfEventProcessingIPs) * 90)], shell=True)

def prrint():
    print "i"
    time.sleep(10)

if __name__=='__main__':
     updateMELAServiceDescriptionAfterScaleOut(eventProcessingStartIP)
     updateMELAServiceDescriptionAfterScaleOut("10.99.0.25")
     updateMELAServiceDescriptionAfterScaleOut("10.99.0.33")
     updateMELAServiceDescriptionAfterScaleOut("10.99.0.40")
     updateMELAServiceDescriptionAfterScaleOut("10.99.0.44")
     updateMELAServiceDescriptionAfterScaleOut("10.99.0.48")

     submitUpdatedDescriptionToMELA()
     #starting control process
     f = open("./efficiency.csv", "w+")
     f.write("Timestamp, Date, STRATEGY_LAST_ADDED IP, STRATEGY_LAST_ADDED Efficiency, STRATEGY_FIRST_ADDED IP, STRATEGY_FIRST_ADDED Efficiency, RANDOM IP, RANDOM Efficiency, STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME IP, STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME Efficiency, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY IP, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY Efficiency \n")
     f.close()
     #currently we run no load must be run remote, in cloud
     #proc=False
     proc = subprocess.Popen(["python ./load.py " + loadBalancerStartIP + " " + str(len(listOfEventProcessingIPs) * 10)], shell=True)
    
     #more stable if I deploy 5 at once manually
     #scaleOutProcesses = []
     #for i in range(0,5):
     #  p = Thread(target=scaleOutAndInchreaseLoad(proc))
     #  p.daemon = True  
     #  scaleOutProcesses.append(p)
     #  p.start()
     #for p in scaleOutProcesses:
     #  p.join()
     time.sleep(3600) #wait so all VMS have been running for one hour hour

     #100 times, scale in, wait 10 minutes, scale out, wait 10 minutes, to see how cost fragmentation happends
     for i in range(0, 100):
        print "Repetition " + str(i)
        print "Scaling IN"
        scaleInAndInchreaseLoad(proc)
        print "Scaled IN. Sleeping 600 seconds"
	time.sleep(1800) #sleep 10 minutes
        print "Scaling OUT"
        scaleOutAndInchreaseLoad(proc)
        print "Scaled OUT. Sleeping 600 seconds"
        time.sleep(1800) #although scale out takes around 10 minutes, still leave it another 10 minutes to run registered in MELA
     print "Evaluation finished" 
