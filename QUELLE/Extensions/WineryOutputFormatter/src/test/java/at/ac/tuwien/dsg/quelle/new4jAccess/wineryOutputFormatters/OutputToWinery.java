/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.new4jAccess.wineryOutputFormatters;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.wineryOutputFormatters.CloudServicesToWinery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;

/**
 *
 * @author daniel-tuwien
 */
public class OutputToWinery extends TestCase {


    public OutputToWinery(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    /**
     * Test of matchServiceUnit method, of class RequirementsMatchingEngine.
     */
    public void testEcosystemDescription() throws IOException {

        List<CloudProvider> cloudProviders = new ArrayList<CloudProvider>();

        //
        // ==========================================================================================
        // amazon cloud description
        try {
            JAXBContext context = JAXBContext.newInstance(CloudProvider.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            CloudProvider provider = (CloudProvider) unmarshaller.unmarshal(new File("./experiments/amazonDescription.xml"));
            cloudProviders.add(provider);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        CloudServicesToWinery cloudServicesToOpenTosca = new CloudServicesToWinery();

        //I need to write for all services new types (except VM) , for which I can just instantiate the existing type
        cloudServicesToOpenTosca.createWineryNodesFromCloudServices(cloudProviders.get(0), "./OpenToscaOutput/nodeTypes");

    }

}
