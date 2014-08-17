import urllib, urllib2, sys, httplib

url = "/MELA/REST_WS"
HOST_IP="128.130.172.214:8080"
#HOST_IP="localhost:8080"


 

if __name__=='__main__':
	connection =  httplib.HTTPConnection(HOST_IP)
        description_file = open("./requirements.xml", "r")
        body_content =  description_file.read()
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/xml, multipart/related'
	}
 
	connection.request('PUT', url+'/servicerequirements', body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()
 

 

