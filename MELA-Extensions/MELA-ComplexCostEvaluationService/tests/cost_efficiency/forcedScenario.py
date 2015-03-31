#
#########################################
import sys, httplib, uuid, random, time, json, subprocess, StringIO, datetime
from threading import Thread

httplib.HTTPConnection.debuglevel = 0

KeyspaceName = 'm2m'
tablename = 'sensor'

SALSA_URL = '128.130.172.215:8380'

MELA_URL = '10.99.0.35:8180'

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

eventProcessingStartIP = {}
loadBalancerStartIP = {}

STRATEGY_LAST_ADDED="STRATEGY_LAST_ADDED"
STRATEGY_FIRST_ADDED="STRATEGY_FIRST_ADDED"
RANDOM="RANDOM"
STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY="STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY"
STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME="STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME"
 
strategiesList = [STRATEGY_LAST_ADDED, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY] #[STRATEGY_LAST_ADDED, STRATEGY_FIRST_ADDED,STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY]


eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_LAST_ADDED)]="10.99.0.33"
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_LAST_ADDED)]="10.99.0.15"

eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_FIRST_ADDED)]="10.99.0.25"
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_FIRST_ADDED)]="10.99.0.15"

eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME)]="10.99.0.67"
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME)]="10.99.0.15"

eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY)]="10.99.0.48"
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY)]="10.99.0.15"
 
listOfEventProcessingDescriptions = {}
listOfEventProcessingIPs = {}
 
maximumIPs=0
#removed random



def submitMetricCompositionRules(serviceName):
        connection =  httplib.HTTPConnection(MELA_URL)
        description_file = open("./compositionRules.xml", "r")
        body_content =  description_file.read()
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/json, multipart/related'
	}
 
	connection.request('PUT', "/MELA/REST_WS/" + serviceName + "/metricscompositionrules", body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()

def executeRESTCall(restMethod, serviceBaseURL, resourceName):
        print "executeRESTCall connecting to " + serviceBaseURL + '/'+resourceName
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
        print "executeRESTCallWithContent connecting to " + serviceBaseURL + '/'+resourceName
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

def scaleInWithSALSA(serviceName, ip):
        executeRESTCall("POST", SALSA_URL, "salsa-engine/rest/services/"+serviceName + "/vmnodes/"+ip+"/scalein")

def scaleOutWithSALSA(serviceName):
        scaleOutIP = str(executeRESTCall("POST", SALSA_URL, "salsa-engine/rest/services/"+serviceName + "/nodes/EventProcessingUnit/scaleout"))
        print "Scaled Out With " + scaleOutIP
        return scaleOutIP

def createNewMonElementSpecification(ip):
      newElementText='<MonitoredElement level="VM" name="'+ip+'" id="'+ip+'"><UsedCloudOfferedServiceCfg name="1CPU1" uuid="20000000-0000-0000-0000-000000000001"  instanceUUID="'+str(uuid.uuid4())+'" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="CloudStorage" uuid="30000000-0000-0000-0000-000000000002"  instanceUUID="'+str(uuid.uuid4())+'" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"> <QualityProperties/> <ResourceProperties/> </UsedCloudOfferedServiceCfg> </MonitoredElement>'
      return newElementText

def updateMELAServiceDescriptionAfterScaleIn(ip, serviceName):
     #remove element with Ip from list
     #MELA returns only Ip ans I need to call SALSA
     listOfEventProcessingDescriptions[serviceName][:] = [element for element in listOfEventProcessingDescriptions[serviceName] if not ip in element]
     listOfEventProcessingIPs[serviceName].remove(ip)
     for t in listOfEventProcessingDescriptions[serviceName]:
        print t

def updateMELAServiceDescriptionAfterScaleOut(ip, serviceName):
     newElement = createNewMonElementSpecification(ip)
     listOfEventProcessingIPs[serviceName].append(ip)
     listOfEventProcessingDescriptions[serviceName].append(newElement)
     for t in listOfEventProcessingDescriptions[serviceName]:
        print t

def submitUpdatedDescriptionToMELA(serviceName):
     serviceStructure_1='<?xml version="1.0" encoding="UTF-8" standalone="yes"?><MonitoredElement level="SERVICE" name="'+serviceName+'" id="'+serviceName+'"><MonitoredElement level="SERVICE_TOPOLOGY" name="EventProcessingTopology" id="EventProcessingTopology"> <MonitoredElement level="SERVICE_UNIT" name="EventProcessingUnit" id="EventProcessingUnit">'
     completeText = "" + serviceStructure_1;
     for element in listOfEventProcessingDescriptions[serviceName]:
       completeText = completeText + element
     
     serviceStructure_2='<UsedCloudOfferedServiceCfg name="ImageStorage" uuid="40000000-0000-0000-0000-000000000001" instanceUUID="98400000-8cf0-11bd-b23e-000000000001" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg></MonitoredElement><MonitoredElement level="SERVICE_UNIT" name="LoadBalancer" id="LoadBalancer"> <MonitoredElement level="VM" name="'+loadBalancerStartIP[serviceName]+'" id="'+loadBalancerStartIP[serviceName]+'"> <UsedCloudOfferedServiceCfg name="2.0CPU4.0" uuid="20000000-0000-0000-0000-000000000003"  instanceUUID="98400000-8cf0-11bd-b23e-000000000002" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="PublicVLAN" uuid="30000000-0000-0000-0000-000000000001"  instanceUUID="98400000-8cf0-11bd-b23e-000000000003" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="CloudStorage" uuid="30000000-0000-0000-0000-000000000002"  instanceUUID="98400000-8cf0-11bd-b23e-000000000015" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"> <QualityProperties/> <ResourceProperties/></UsedCloudOfferedServiceCfg></MonitoredElement></MonitoredElement></MonitoredElement> </MonitoredElement>'

     completeText = completeText + serviceStructure_2
     print completeText
     executeRESTCallWithContent("PUT", MELA_URL,"MELA/REST_WS/service", completeText)
 
def getScaleInRecommendationFromMELABasedOnCostEffciency(serviceId):
     ipToScaleIn=""
     ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/plain")

     while not ipToScaleIn:
       print "Received no IP to scale in, so waiting and trying again in one minute: cost efficiency"
       time.sleep(60)
       ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/plain")

     print "Received IP to scale " + ipToScaleIn
     return ipToScaleIn

def getScaleInRecommendationFromMELABasedOnLifetime(serviceId):
     ipToScaleIn=""
     ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/lifetime/scalein/EventProcessingUnit/SERVICE_UNIT/plain")

     while not ipToScaleIn:
       print "Received no IP to scale in, so waiting and trying again in one minute: lifetime"
       time.sleep(60)
       ipToScaleIn = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/recommend/lifetime/scalein/EventProcessingUnit/SERVICE_UNIT/plain")

     print "Received IP to scale " + ipToScaleIn
     return ipToScaleIn

def getResponseTimeMetricFromMELA(serviceId):
     metricValue = executeRESTCall("GET",MELA_URL,"MELA/REST_WS/"+serviceId+"/monitoringdata/EventProcessingUnit/SERVICE_UNIT/avgResponseTime/ms")
     print "Received Metric value '" + metricValue + "'"
     if metricValue:
       return float(metricValue)
     else:
       print "Nothing returned as value, so we replace with 0, which does not trigger anything"
       return 0

def getCostEfficiencyForScalingInFromMELA(serviceId,ip):
     efficiency = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/"+ip+"/plain")
     print "Evaluated cost efficiency for scaling in "+ ip + " is '" + efficiency + "'"
     if efficiency:
       return float(efficiency)
     else:
       print "Nothing returned as efficiency, so we replace with -1, which means error"
       return -1

def evaluateAndPersistCostEfficiencyForScalingStrategy(strategy, serviceId):
   global maximumIPs
   if len(listOfEventProcessingIPs[serviceId]) > 1:
     
     if maximumIPs < len(listOfEventProcessingIPs[serviceId]):
         maximumIPs = len(listOfEventProcessingIPs[serviceId])
     ipSStringForMela=""
     
     print "ReturnedIP for " + strategy + " is " + getIPToScaleIn(strategy, serviceId)
     ip = getIPToScaleIn(strategy, serviceId)
     ipSStringForMela = ipSStringForMela + "-" + ip

     for ip in listOfEventProcessingIPs[serviceId]:	
        ipSStringForMela = ipSStringForMela + "-" + ip
     
      #remove first -
     ipSStringForMela = ipSStringForMela[1:]

     efficiency = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/more/EventProcessingUnit/SERVICE_UNIT/"+ipSStringForMela+"/plain")
     #efficiency returned as JSON so need to parse it
     jsonRepresentation = json.loads(efficiency)

     efficiencyCSVLine = "" + str(int(time.time())) + "," + str(datetime.datetime.now()) + "," + str(len(listOfEventProcessingIPs[serviceId]))
      
     for i in range(0, len(jsonRepresentation)):
        report  = jsonRepresentation[i]
        efficiencyCSVLine = efficiencyCSVLine + "," + str(report["ip"]) + "," + str(report["efficiency"])
     
     if maximumIPs > len(listOfEventProcessingIPs[serviceId]):
        for i in range(0, maximumIPs -len(listOfEventProcessingIPs[serviceId])):
           efficiencyCSVLine = efficiencyCSVLine + ", "

     f = open("./efficiency_"+str(strategy)+".csv", "a+")
     f.write(efficiencyCSVLine + "\n")
     f.close()
     #sys.exit("ddd")

def getIPToScaleIn(strategy, serviceId):
   if len(listOfEventProcessingDescriptions[serviceId]) > 1 :
           if strategy == STRATEGY_LAST_ADDED:
              return listOfEventProcessingIPs[serviceId][len(listOfEventProcessingIPs[serviceId])-1]
           elif strategy == STRATEGY_FIRST_ADDED :
              return listOfEventProcessingIPs[serviceId][0]
           elif strategy == RANDOM:
              return listOfEventProcessingIPs[serviceId][random.randint(0,len(listOfEventProcessingIPs[serviceId])-1)]
           elif strategy == STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY:
              return getScaleInRecommendationFromMELABasedOnCostEffciency(serviceId)
           elif strategy == STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME:
              return getScaleInRecommendationFromMELABasedOnLifetime(serviceId)
           else:
              print "Unknown strategy " + strategy
   else:
       print "only one event processing instance remaining, not scaling in"
       return ""
 
def directScaleIn(strategy, serviceId):
    if len(listOfEventProcessingIPs[serviceId]) > 1:
          #print "Evaluated all strategies"
          #evaluateAndPersistCostEfficiencyForScalingAllStrategies()
          #first record decision efficiency of all strategies
          print "Getting IP for strategy " + strategy
          iPToScaleIn = getIPToScaleIn(strategy, serviceId)
          scaleInWithSALSA(serviceId, iPToScaleIn)
          updateMELAServiceDescriptionAfterScaleIn(iPToScaleIn, serviceId)
          submitUpdatedDescriptionToMELA(serviceId)

def directScaleOut(serviceId):
    scaledIP = scaleOutWithSALSA(serviceId)
    if scaledIP:
      updateMELAServiceDescriptionAfterScaleOut(scaledIP, serviceId)
      submitUpdatedDescriptionToMELA(serviceId)
    else:
      print "SALSA returned no IP"

def scaleOutAndInchreaseLoad(proc, serviceId):
    directScaleOut(serviceId)
    if proc:
       proc.terminate()
       proc = subprocess.Popen(["exec python ./load.py " + loadBalancerStartIP[serviceId] + " " + str(len(listOfEventProcessingIPs[serviceId]) * 90)], shell=True)

def scaleInAndInchreaseLoad(proc, serviceId, strategy):
    directScaleIn(strategy, serviceId)
    if proc:
       proc.terminate()
       proc = subprocess.Popen(["exec python ./load.py " + loadBalancerStartIP[serviceId] + " " + str(len(listOfEventProcessingIPs[serviceId]) * 90)], shell=True)

if __name__=='__main__':
   for strategy in  strategiesList:
     serviceName = "EventProcessingTopology_"+ str(strategy)
     listOfEventProcessingIPs[serviceName] = []
     listOfEventProcessingDescriptions[serviceName] = []

   updateMELAServiceDescriptionAfterScaleOut("10.99.0.56", "EventProcessingTopology_"+ str(STRATEGY_LAST_ADDED))
   updateMELAServiceDescriptionAfterScaleOut("10.99.0.65", "EventProcessingTopology_"+ str(STRATEGY_LAST_ADDED))


   #updateMELAServiceDescriptionAfterScaleOut("10.99.0.54", "EventProcessingTopology_"+ str(STRATEGY_FIRST_ADDED))
   #updateMELAServiceDescriptionAfterScaleOut("10.99.0.56", "EventProcessingTopology_"+ str(STRATEGY_FIRST_ADDED))


   #updateMELAServiceDescriptionAfterScaleOut("10.99.0.54", "EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME))
   #updateMELAServiceDescriptionAfterScaleOut("10.99.0.80", "EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME))


   updateMELAServiceDescriptionAfterScaleOut("10.99.0.58", "EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY))
   updateMELAServiceDescriptionAfterScaleOut("10.99.0.54", "EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY))

   for strategy in  strategiesList:
     serviceName = "EventProcessingTopology_"+ str(strategy)
     updateMELAServiceDescriptionAfterScaleOut(eventProcessingStartIP[serviceName], serviceName)
     submitMetricCompositionRules(serviceName)
     submitUpdatedDescriptionToMELA(serviceName)
     #starting control process
     f = open("./efficiency_"+str(strategy)+".csv", "w+")
     f.write("Timestamp, Date, Instances, STRATEGY_LAST_ADDED IP, STRATEGY_LAST_ADDED Efficiency, STRATEGY_FIRST_ADDED IP, STRATEGY_FIRST_ADDED Efficiency, RANDOM IP, RANDOM Efficiency, STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME IP, STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME Efficiency, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY IP, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY Efficiency \n")
     f.close()
     #currently we run no load must be run remote, in cloud
     #proc=False
     proc = subprocess.Popen(["exec python ./load.py " + loadBalancerStartIP[serviceName] + " " + str(len(listOfEventProcessingIPs[serviceName]) * 90)], shell=True)
 
   #add 2 instances

   generatedProcesses = []

   #sleep so mela cost can intitiate the evaluation
   time.sleep(500)
  
   for i in range(0, 20):
 
     #scale in each
     print "Scale in"
     
     #for each strategy evaluate 7 times scaling in at 1 minute efficiency, in ||
     for j in range(0,7):
       for strategy in  strategiesList:
          print "Evaluating after for " + str(j)
          p = Thread(target=evaluateAndPersistCostEfficiencyForScalingStrategy, args=[strategy, serviceName])
          generatedProcesses.append(p)
          p.start()
       for p in generatedProcesses:         
          p.join()
       generatedProcesses = [] 
       print "Sleeping"   
       time.sleep(60)
    
     #for all strategies scale in
     for strategy in  strategiesList:
        serviceName = "EventProcessingTopology_"+ str(strategy)
        p = Thread(target=scaleInAndInchreaseLoad, args=[proc, serviceName, strategy])
        generatedProcesses.append(p)
        p.start()
     for p in generatedProcesses:         
        p.join()
     generatedProcesses = []

     time.sleep(120)
        
     #for all scale out 
     print "Scale out" 
     for strategy in  strategiesList:
         serviceName = "EventProcessingTopology_"+ str(strategy)
         p = Thread(target=scaleOutAndInchreaseLoad, args=[proc, serviceName])
         generatedProcesses.append(p)
         p.start()
         time.sleep(50) #avoid errors in open stack 
     for p in generatedProcesses:         
         p.join()
     generatedProcesses = []   
            
     for j in range(0,7):
       #evaluate and persist
       print "Evaluating after for" + str(j)
       for strategy in  strategiesList:
          serviceName = "EventProcessingTopology_"+ str(strategy)
          p = Thread(target=evaluateAndPersistCostEfficiencyForScalingStrategy, args=[strategy, serviceName])
          generatedProcesses.append(p)
          p.start()
       for p in generatedProcesses:         
          p.join()
       generatedProcesses = []  
       print "Sleeping" 
       time.sleep(60)

