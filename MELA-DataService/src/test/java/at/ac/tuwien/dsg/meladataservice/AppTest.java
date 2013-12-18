package at.ac.tuwien.dsg.meladataservice;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import at.ac.tuwien.dsg.mela.dataservice.config.dataSourcesManagement.DataSourceConfig;
import at.ac.tuwien.dsg.mela.dataservice.config.dataSourcesManagement.DataSourceConfigs;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
        try {
        	DataSourceConfigs list = new DataSourceConfigs(); 
        	
        	javax.xml.bind.JAXBContext jAXBContext = javax.xml.bind.JAXBContext.newInstance(list.getClass());
			
			{
				DataSourceConfig config = new DataSourceConfig();
				config.setType("Ganglia");
				config.setName("GangliaDataSource");
				config.setClassName("DDD");
				
				ArrayList<String> properties=  new ArrayList<String>();
				properties.add("poolingInterval=10000");
				properties.add("ganglia.port=8649");
				properties.add("ganglia.ip=localhostr");
				
				config.setProperties(properties);
				list.addConfig(config);
			}
			
			{
				DataSourceConfig config = new DataSourceConfig();
				config.setType("Ganglia");
				config.setName("GangliaDataSource2");
				config.setClassName("DDD");
				
				ArrayList<String> properties=  new ArrayList<String>();
				properties.add("poolingInterval=10000");
				properties.add("ganglia.port=8649");
				properties.add("ganglia.ip=localhostr");
				
				config.setProperties(properties);
				list.addConfig(config);
			}
			try {
				javax.xml.bind.Marshaller m = jAXBContext.createMarshaller();
				m.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true);
				m.marshal(list, new FileWriter("dataSources.conf"));
								
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (javax.xml.bind.JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
    }
    
   
}
