import urllib, urllib2, sys, httplib

url = "/MELA/REST_WS"
HOST_IP="109.231.126.217:8180"
#HOST_IP="localhost:8180"
 
 

if __name__=='__main__':
	connection =  httplib.HTTPConnection(HOST_IP)
        description_file = open("./costTest.xml", "r")
        body_content =  description_file.read()
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/json, multipart/related'
	}
 
	connection.request('PUT', url+'/service', body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()
 

 

