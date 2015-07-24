import urllib, urllib2, sys, httplib


url = "/MELA/REST_WS/Service"
#HOST_IP="128.130.172.214:8180"
HOST_IP="109.231.126.217:8180"
#HOST_IP="localhost:8180"


if __name__=='__main__':
	connection =  httplib.HTTPConnection(HOST_IP)
        description_file = open("./compositionRules.xml", "r")
        body_content =  description_file.read()
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/json, multipart/related'
	}
 
	connection.request('PUT', url+'/metricscompositionrules', body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()
 

 

