import urllib, urllib2, sys, httplib

url = "/MELA/REST_WS"
#HOST_IP="128.130.172.191:8180"
HOST_IP="128.130.172.230:8180"
filename="./serviceDescription.xml"

 

if __name__=='__main__':
        args = sys.argv;
        if (len(args) > 1): 
            filename = str(args[1])
	connection =  httplib.HTTPConnection(HOST_IP)
        description_file = open(filename, "r")
        body_content =  description_file.read()
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/json, multipart/related'
	}
 
	connection.request('PUT', url+'/service', body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()
 

 

