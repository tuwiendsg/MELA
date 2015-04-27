import urllib, urllib2, sys, httplib

url = "/MELA/REST_WS"
#HOST_IP="128.130.172.191:8180"
newName="EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY_Larger_VMs"
HOST_IP="localhost:8480"


 

if __name__=='__main__':
	connection =  httplib.HTTPConnection(HOST_IP)
        description_file = open("./20hstruct_LAST_ADDED_LARGER_VMS.xml", "r")
        body_content =  description_file.read()
        headers={
	        'Content-Type':'application/xml; charset=utf-8',
                'Accept':'application/json, multipart/related'
	}
 
	connection.request('PUT', url+'/service/emulate/'+newName, body=body_content,headers=headers,)
	result = connection.getresponse()
        print result.read()
 

 

