import socket, time, random
from threading import Thread

spark_master_ip="10.99.0.48"
spark_master_port=9999

sensorFrequency = 1
sensorsCount = 500

def sendDataToSocket(ip, port, data):
   clientsocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
   clientsocket.connect((spark_master_ip, spark_master_port))
   clientsocket.send(data)


def createSensorData(sensorID,destination ):

  #message =   + str(sensorID) + "'to='" + str(destination) + "'>"
  #+ "<fields xmlns='urn:xmpp:iot:sensordata' seqnr='1' done='true'>"+ "<node nodeId='"
  #+str(sensorID) + "'>"+  "<timestamp value='" + str(int(time.time())) +"'>"
  #+    "<numeric name='Temperature' momentary='true' automaticReadout='true' value='" 
  #+ str(random.randint(10,100)) + "' unit='C'/>"+ "</timestamp>"+ "</node>"
  #+  "</fields>"+ "</message>"

  message = ''.join(["<message from='",  str(sensorID), "'to='" , str(destination) , "'>"
  , "<fields xmlns='urn:xmpp:iot:sensordata' seqnr='1' done='true'>", "<node nodeId='"
  ,str(sensorID) , "'>",  "<timestamp value='" , str(int(time.time())) ,"'>"
  ,    "<numeric name='Temperature' momentary='true' automaticReadout='true' value='"
  , str(random.randint(10,100)) , "' unit='C'/>", "</timestamp>", "</node>"
  ,  "</fields>", "</message>"] )
  return message

def createAndSendData(ip, port,sensorID ):
  data = createSensorData(sensorID,ip)
  sendDataToSocket(ip,port,data)

if __name__=='__main__':

   sensors = []

   #generate sensors
   for i in range(0,sensorsCount):
    sensors.append("Sensor_" + str(i))

   while True:
     data = []
     for sensor in sensors:
        data.append(createSensorData(sensor, spark_master_ip))
     sendDataToSocket(spark_master_ip,spark_master_port, "".join(data))
     time.sleep(sensorFrequency)
~                                                                                                                                                                                                            
~                                                                                                                                                                                                            
~                                                                                                                                                                                                            
~                             
