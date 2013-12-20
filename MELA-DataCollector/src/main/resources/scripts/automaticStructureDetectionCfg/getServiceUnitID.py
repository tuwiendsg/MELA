from xml.dom.minidom import parseString
import subprocess
import sys

def temp_handler(name):
 
   #in the case static ServiceUnit ID "CassandraNode" is used
   serviceUnitID = 'ServiceUnitA'
   p = subprocess.Popen('echo ' + serviceUnitID, shell=True,stdout=subprocess.PIPE, stderr=subprocess.PIPE)
   try:
        out, err = p.communicate()
   except:
        return -1
   return out.rstrip();
    
   #in the case the ServiceUnit ID is taken from contextualization or other call for OpenStack contextualization
   #import  sys, httplib3, re 
   #contextualizationURL="http://169.254.169.254/latest/user-data"
   #propertyName = 'serviceUnitID'
   #response, content = httplib2.Http().request(contextualizationURL)
   #if response.status!=200:
   #  print "Error: " + response
   #  sys.exit(1)
   #regexResult = re.search(propertyName + '=.*', content)
   #if regexResult.start() < 0:
   #  print "Error: No serviceUnitID=.* specified on contextualization"
   #  sys.exit(1)
   #configLine=regexResult.group()
   #serviceUnitID= configLine.split("=")[1].strip()
   #return serviceUnitID

def metric_init(params):
    global descriptors

    d1 = {'name': 'serviceUnitID',
        'call_back': temp_handler,
        'time_max': 5,
        'value_type': 'string',
        'units': 'ID',
        'slope': 'zero',
        'format': '%s',
        'description': 'IF of the Service Unit to which this VM belongs',
        'groups': 'serviceInfo'}

    descriptors = [d1]

    return descriptors

def metric_cleanup():
    '''Clean up the metric module.'''
    pass

#This code is for debugging and unit testing
if __name__ == '__main__':
    metric_init({})
    for d in descriptors:
        v = d['call_back'](d['name'])
        print 'value for %s is %s' % (d['name'],  v)

