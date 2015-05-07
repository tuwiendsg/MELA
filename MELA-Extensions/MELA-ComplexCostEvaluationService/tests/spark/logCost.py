# not forced. Actually inchreases event processing instances up to 6, and decreases them by scaling in according to some strategy
#########################################
import sys, httplib, uuid, random, time, json, subprocess, StringIO, datetime, threading, signal
from threading import Thread

httplib.HTTPConnection.debuglevel = 0


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

eventProcessingStartIP = {}
loadBalancerStartIP = {}

 
 
strategiesList = ["SparkCluster_1","SparkCluster_2"]
 
startTimestamp = {}

 
def executeRESTCall(restMethod, serviceBaseURL, resourceName):
        #print "executeRESTCall connecting to " + serviceBaseURL + '/'+resourceName
        headers={
                'Content-Type':'plain/txt; charset=utf-8'
        }
        connection =  httplib.HTTPConnection(serviceBaseURL)
        connection.request(restMethod, '/'+resourceName,headers=headers)
        result = connection.getresponse()
        response  = result.read()
        connection.close()
        #print result.status, result.reason
        #print response
        return response

def getTotalCostFromMELACost(serviceID):
     metricValue = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceID+"/cost/total")
     #print "Received total cost value '" + metricValue + "'"
     if metricValue:
       value = -1 
       try:
          value = float(metricValue)
       except:
          print "Got instead of cost: " + str(value)
       return value
     else:
       print "Nothing returned as value, so we replace with 0, which does not trigger anything"
       return 0


def getInstantCostFromMELACost(serviceID):
     metricValue = executeRESTCall("GET",MELA_COST_URL,"MELA/REST_WS/"+serviceID+"/cost/instant")
     #print "Received total cost value '" + metricValue + "'"
     if metricValue:
       value = -1 
       try:
          value = float(metricValue)
       except:
          print "Got instead of cost: " + str(value)
       return value
     else:
       print "Nothing returned as value, so we replace with 0, which does not trigger anything"
       return 0
 
def evaluateAndPersistCostEfficiencyForScalingStrategy(serviceId):
     totalServiceCost = getTotalCostFromMELACost(serviceId)
     instantServiceCost = getInstantCostFromMELACost(serviceId)

     currentTime = time.time() - startTimestamp[serviceId]
     humanReadableTime  = datetime.datetime.fromtimestamp(currentTime)
     efficiencyCSVLine = "" + str(int(time.time()))  + "," + str(humanReadableTime.hour) + ":" + str(humanReadableTime.minute) + ":" + str(humanReadableTime.second) + "," + str(instantServiceCost) + "," + str(totalServiceCost)
     
     f = open("./efficiency_" + str(serviceId) + ".csv", "a+")
     f.write(efficiencyCSVLine + "\n")
     f.close()
 

if __name__=='__main__':
 
   for strategy in  strategiesList:
       startTimestamp[strategy] = time.time()
       f = open("./efficiency_"+str(strategy)+".csv", "w+")
       f.write("Timestamp, Date, Instant Cost " +str(strategy)+ ", Total Cost " +str(strategy)+ "\n")
       f.close()
 
   while True:
         threads = []
         for strategy in  strategiesList:
             p = Thread(target=evaluateAndPersistCostEfficiencyForScalingStrategy, args=[strategy])
             p.setDaemon(True)
             p.start()
             threads.append(p)
         for t in threads:
              t.join() 
         time.sleep(60)
 

