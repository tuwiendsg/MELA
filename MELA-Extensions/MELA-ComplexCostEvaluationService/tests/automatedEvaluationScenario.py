# not forced. Actually inchreases event processing instances up to 6, and decreases them by scaling in according to some strategy
#########################################
import sys, httplib, uuid, random, time, json, subprocess, StringIO, datetime
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

eventProcessingStartIP = {}
loadBalancerStartIP = {}

STRATEGY_LAST_ADDED="STRATEGY_LAST_ADDED"
STRATEGY_FIRST_ADDED="STRATEGY_FIRST_ADDED"
RANDOM="RANDOM"
STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY="STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY"
STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME="STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME"
 
strategiesList = [STRATEGY_LAST_ADDED, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY] #[STRATEGY_LAST_ADDED, STRATEGY_FIRST_ADDED,STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME, STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY]


eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_LAST_ADDED)]="10.99.0.25"
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_LAST_ADDED)]="10.99.0.15"

eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_FIRST_ADDED)]="10.99.0."
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_FIRST_ADDED)]="10.99.0.15"

eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME)]="10.99.0.25"
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_LIFETIME)]="10.99.0.15"

eventProcessingStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY)]="10.99.0.33"
loadBalancerStartIP["EventProcessingTopology_"+ str(STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY)]="10.99.0.15"
 
listOfEventProcessingDescriptions = {}
listOfEventProcessingIPs = {}

#this historical list is used to determine if when we scaled out we received same IP again. With this, we change the instance ID to add encounteredIndex
eventProcessingIPIndex = {}

#used to have nice CSV in which if an Ip is encountered AGAIN, I padd it.
#also contains in the name last "_instanceIndex" part
historicalyUsedUnitInstancesNames ={}
 
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

def createNewMonElementSpecification(elementIP, elementID):
      newElementText='<MonitoredElement level="VM" name="'+elementIP+'" id="'+elementID+'"><UsedCloudOfferedServiceCfg name="1CPU1" uuid="20000000-0000-0000-0000-000000000001"  instanceUUID="'+str(uuid.uuid4())+'" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg><UsedCloudOfferedServiceCfg name="CloudStorage" uuid="30000000-0000-0000-0000-000000000002"  instanceUUID="'+str(uuid.uuid4())+'" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"> <QualityProperties/> <ResourceProperties/> </UsedCloudOfferedServiceCfg> </MonitoredElement>'
      return newElementText

def updateMELAServiceDescriptionAfterScaleIn(ip, serviceName):
     #remove element with Ip from list
     #MELA returns only Ip ans I need to call SALSA
     listOfEventProcessingDescriptions[serviceName][:] = [element for element in listOfEventProcessingDescriptions[serviceName] if not ip in element]
     listOfEventProcessingIPs[serviceName].remove(ip)
     for t in listOfEventProcessingDescriptions[serviceName]:
        print t

def updateMELAServiceDescriptionAfterScaleOut(ip, serviceName):
     index = 0
     if ip in eventProcessingIPIndex[serviceName]:
         index = eventProcessingIPIndex[serviceName][ip] + 1
         eventProcessingIPIndex[serviceName][ip] = index
         newElement = createNewMonElementSpecification(ip, ip +"_i"+ str(index))
     else:
         newElement = createNewMonElementSpecification(ip,ip)
     eventProcessingIPIndex[serviceName][ip] = index
     historicalyUsedUnitInstancesNames[serviceName].append(ip)
     listOfEventProcessingIPs[serviceName].append(ip)
     listOfEventProcessingDescriptions[serviceName].append(newElement)
     for t in listOfEventProcessingDescriptions[serviceName]:
        print t

def submitUpdatedDescriptionToMELA(serviceName):
     serviceStructure_1='<?xml version="1.0" encoding="UTF-8" standalone="yes"?><MonitoredElement level="SERVICE" name="'+serviceName+'" id="'+serviceName+'"><MonitoredElement level="SERVICE_TOPOLOGY" name="EventProcessingTopology" id="EventProcessingTopology"> <MonitoredElement level="SERVICE_UNIT" name="EventProcessingUnit" id="EventProcessingUnit">'
     completeText = "" + serviceStructure_1;
     for element in listOfEventProcessingDescriptions[serviceName]:
       completeText = completeText + element
     
     serviceStructure_2='<UsedCloudOfferedServiceCfg name="ImageStorage" uuid="40000000-0000-0000-0000-000000000001" instanceUUID="98400000-8cf0-11bd-b23e-000000000001" cloudProviderName="Flexiant" cloudProviderID="10000000-0000-0000-0000-000000000001"><QualityProperties/><ResourceProperties/></UsedCloudOfferedServiceCfg></MonitoredElement></MonitoredElement> </MonitoredElement>'

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

def getMetricFromMELA(serviceID, elementID, elementLevel, metricName, metricUnit):
     metricValue = executeRESTCall("GET",MELA_URL,"MELA/REST_WS/"+serviceID+"/monitoringdata/"+elementID+"/"+elementLevel+"/" + metricName + "/" + metricUnit)
     print "Received Metric value '" + metricValue + "'"
     if metricValue:
       value = -1 
       try:
          value = float(metricValue)
       except:
          print "Got instead of metric: " + str(value)
       return value
     else:
       print "Nothing returned as value, so we replace with 0, which does not trigger anything"
       return 0


def getCostEfficiencyForScalingInFromMELA(serviceId,ip):
     efficiency = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/"+ip+"/plain")
     print "Evaluated cost efficiency for scaling in "+ ip + " is '" + efficiency + "'"
     if efficiency:
       value = -1 
       try:
          value = float(efficiency)
       except:
          print "Got instead of efficiency: " + str(value)
       return value
     else:
       print "Nothing returned as efficiency, so we replace with -1, which means error"
       return -1

def evaluateAndPersistCostEfficiencyForScalingStrategy(strategy, serviceId):
   global maximumIPs
   if len(listOfEventProcessingIPs[serviceId]) > 0:
     
     if maximumIPs < len(listOfEventProcessingIPs[serviceId]):
         maximumIPs = len(listOfEventProcessingIPs[serviceId])
     ipSStringForMela=""
     
     #print "ReturnedIP for " + strategy + " is " + getIPToScaleIn(strategy, serviceId)
     #ip = getIPToScaleIn(strategy, serviceId)
     #ipSStringForMela = ipSStringForMela + "-" + ip
     
     for ip in listOfEventProcessingIPs[serviceId]:
        ipName = ip
        if ip in eventProcessingIPIndex[serviceId]:
           if eventProcessingIPIndex[serviceId][ip] > 0:
             ipName = ip  +"_i"+ str(eventProcessingIPIndex[serviceId][ip])
        ipSStringForMela = ipSStringForMela + "-" + ipName
      #remove first -
     ipSStringForMela = ipSStringForMela[1:]

     efficiency = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/more/EventProcessingUnit/SERVICE_UNIT/"+ipSStringForMela+"/plain")
     #efficiency returned as JSON so need to parse it
     jsonRepresentation = json.loads(efficiency)

     efficiencyCSVLine = "" + str(int(time.time())) + "," + str(datetime.datetime.now()) + "," + str(len(listOfEventProcessingIPs[serviceId]))
     reportMap={}  
     #map of returned report on IP and info
     for i in range(0, len(jsonRepresentation)):
        report = jsonRepresentation[i]       
        diskUsage = getMetricFromMELA(serviceId,report["ip"],"VM","IODataSize","GB") 
        #convert disk usage to current life
        diskUsage = diskUsage - int(diskUsage)
        report["diskUsage"] = diskUsage
        reportMap[str(report["ip"])] = report

   
     for instanceName in historicalyUsedUnitInstancesNames[serviceId]:
	ipToSearchFor = instanceName
        if "_" in ipToSearchFor:
          ipToSearchFor = ipToSearchFor[:ipToSearchFor.index("_")]    
        if ipToSearchFor in reportMap:  
           report = reportMap[ipToSearchFor]
           efficiencyCSVLine = efficiencyCSVLine + "," + str(report["ip"]) + "," + str(report["efficiency"]) + "," + str(report["lifetime"]) + ","+ str(report[diskUsage])
        else:
            efficiencyCSVLine = efficiencyCSVLine + ", , , , "

     f = open("./efficiency_"+str(strategy)+".csv", "a+")
     f.write(efficiencyCSVLine + "\n")
     f.close()

def getIPToScaleIn(strategy, serviceId):
 
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
 
 
def directScaleIn(strategy, serviceId):

  #print "Evaluated all strategies"
  #evaluateAndPersistCostEfficiencyForScalingAllStrategies()
  #first record decision efficiency of all strategies
  iPToScaleIn = getIPToScaleIn(strategy, serviceId)
  #log what we are doing in separate log
  efficiency = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceId+"/cost/evaluate/costefficiency/scalein/EventProcessingUnit/SERVICE_UNIT/"+iPToScaleIn+"/plain") 
  waste = 1 - float(efficiency)
  cSVLine = str(iPToScaleIn) + "," + str(efficiency) + "," + str(waste)
  f = open("./efficiency_action_"+str(strategy)+".csv", "a+")
  f.write(cSVLine + "\n")
  f.close()
  
  
  #check if the ipToScaleIn still here. 
  #a but can appear IF we have 2 waiting scale ins, in parralel as deamon, and both get same recomendation
  while not iPToScaleIn in listOfEventProcessingIPs[serviceId]: 
     time.sleep(10) 
     iPToScaleIn = getIPToScaleIn(strategy, serviceId)
  print "Getting IP for strategy " + strategy + " " + str(iPToScaleIn)
  if "_" in iPToScaleIn:
     iPToScaleIn = iPToScaleIn[:iPToScaleIn.index("_")]
  scaleInWithSALSA(serviceId, iPToScaleIn)
  updateMELAServiceDescriptionAfterScaleIn(iPToScaleIn, serviceId)
  submitUpdatedDescriptionToMELA("EventProcessingTopology_"+ str(strategy)) 

def directScaleOut(serviceId):
    scaledIP = scaleOutWithSALSA(serviceId)
    if scaledIP:
      updateMELAServiceDescriptionAfterScaleOut(scaledIP, serviceId)
    else:
      print "SALSA returned no IP"

def scaleOutAndInchreaseLoad(proc, serviceId):
    directScaleOut(serviceId)
    #if proc:
    #   proc.terminate()
    #   proc = subprocess.Popen(["exec python ./load.py " + loadBalancerStartIP[serviceId] + " " + str(len(listOfEventProcessingIPs[serviceId]) * 90)], shell=True)

def scaleInAndInchreaseLoad(proc, serviceId, strategy):
    directScaleIn(strategy, serviceId)
    #if proc:
    #   proc.terminate()
    #   proc = subprocess.Popen(["exec python ./load.py " + loadBalancerStartIP[serviceId] + " " + str(len(listOfEventProcessingIPs[serviceId]) * 90)], shell=True)

def executeScenarioForStrategy(strategy):

   serviceName = "EventProcessingTopology_"+ str(strategy)
   listOfEventProcessingIPs[serviceName] = []
   listOfEventProcessingDescriptions[serviceName] = []
   eventProcessingIPIndex[serviceName] = {} #is dictionary as it holds encounter rate for each IP
   historicalyUsedUnitInstancesNames[serviceName] = []
   submitMetricCompositionRules(serviceName)
   f = open("./efficiency_"+str(strategy)+".csv", "w+")
   f.write("Timestamp, Date, Instances \n")
   f.close()

   f = open("./efficiency_action_"+str(strategy)+".csv", "w+")
   f.write("Timestamp, Date, Instances \n")
   f.close()
 
   proc = subprocess.Popen(["exec python ./load.py " + loadBalancerStartIP[serviceName] + " " + str(5 * 90)], shell=True)

   for k in range (0,5):
     #for all scale out 
       print "Scale out"
       scaleOutAndInchreaseLoad(proc, "EventProcessingTopology_"+ str(strategy))
       print "Scaled out " + serviceName
   submitUpdatedDescriptionToMELA("EventProcessingTopology_"+ str(strategy))
   print "Sleep 90 seconds after scale out " + serviceName
   time.sleep(90)
   print "For 45 minutes, evaluate efficiency " + serviceName
   for j in range(0,45):
     evaluateAndPersistCostEfficiencyForScalingStrategy(strategy, "EventProcessingTopology_"+ str(strategy))
     time.sleep(60)
   
   #x times we scale in 2, scale out 2, and repeat
   for repetitions in range(0,10):
           print "For repetition " + str(repetitions) + " " + serviceName
	   for k in range (0,2):
	     #for all strategies scale ini     
		#scaleInAndInchreaseLoad(proc, "EventProcessingTopology_"+ str(strategy), strategy)
		#submitUpdatedDescriptionToMELA("EventProcessingTopology_"+ str(strategy))  
	        print "Start thread to scale in " + serviceName
		p = Thread(target=scaleInAndInchreaseLoad, args=[proc, "EventProcessingTopology_"+ str(strategy), strategy])
		p.setDaemon(True)
		p.start() 
		time.sleep(90)
		for j in range(0,45):
		 evaluateAndPersistCostEfficiencyForScalingStrategy(strategy, "EventProcessingTopology_"+ str(strategy))
		 time.sleep(60)
	   for k in range (0,2): 
	     #for all scale out 
	       print "Scale out " + serviceName  
	       scaleOutAndInchreaseLoad(proc, "EventProcessingTopology_"+ str(strategy)) 
	       submitUpdatedDescriptionToMELA("EventProcessingTopology_"+ str(strategy))
	       for j in range(0,45):
		 evaluateAndPersistCostEfficiencyForScalingStrategy(strategy, "EventProcessingTopology_"+ str(strategy))
		 time.sleep(60)
 

if __name__=='__main__':

   generatedProcesses = []

   for strategy in  strategiesList:
     p = Thread(target=executeScenarioForStrategy, args=["" + strategy])
     generatedProcesses.append(p)
     p.start()
    
   for p in generatedProcesses:         
     p.join()
    
