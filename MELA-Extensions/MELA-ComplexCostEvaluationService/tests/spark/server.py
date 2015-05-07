import socket, random
import time
 

sensorFrequency = 1
sensorsCount = 1000

def createSensorData(sensorID ):

  #message =   + str(sensorID) + "'to='" + str(destination) + "'>"
  #+ "<fields xmlns='urn:xmpp:iot:sensordata' seqnr='1' done='true'>"+ "<node nodeId='"
  #+str(sensorID) + "'>"+  "<timestamp value='" + str(int(time.time())) +"'>"
  #+    "<numeric name='Temperature' momentary='true' automaticReadout='true' value='" 
  #+ str(random.randint(10,100)) + "' unit='C'/>"+ "</timestamp>"+ "</node>"
  #+  "</fields>"+ "</message>"

  message = ''.join(["<message from='",  str(sensorID), "'to='" , str("spark") , "'>"
  , "<fields xmlns='urn:xmpp:iot:sensordata' seqnr='1' done='true'>", "<node nodeId='"
  ,str(sensorID) , "'>",  "<timestamp value='" , str(int(time.time())) ,"'>"
  ,    "<numeric name='Temperature' momentary='true' automaticReadout='true' value='"
  , str(random.randint(10,100)) , "' unit='C'/>", "</timestamp>", "</node>"
  ,  "</fields>", "</message>"] )
  return message


if __name__=='__main__':

# create a socket object
  serversocket = socket.socket(
                socket.AF_INET, socket.SOCK_STREAM)

# get local machine name
  host = "localhost"

  port = 9999

# bind to the port
  serversocket.bind((host, port))

# queue up to 5 requests
  serversocket.listen(5)
  sensors = []
  #generate sensors
  for i in range(0,sensorsCount):
      sensors.append("Sensor_" + str(i))

  while True:
    # establish a connection
    clientsocket,addr = serversocket.accept()
    data = []
    for sensor in sensors:
        data.append(createSensorData(sensor))

    print("Got a connection from %s" % str(addr))
    currentTime = time.ctime(time.time()) + "\r\n"
    clientsocket.send("".join(data))
    clientsocket.close()
                                                                                                                                                                                                            
