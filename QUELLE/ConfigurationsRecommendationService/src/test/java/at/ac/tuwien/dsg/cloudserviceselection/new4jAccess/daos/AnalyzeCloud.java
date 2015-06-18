/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.cloudserviceselection.new4jAccess.daos;

import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.CloudProviderAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import junit.framework.TestCase;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Value;

/**
 *
 * @author daniel-tuwien
 */
public class AnalyzeCloud extends TestCase {

    @Value("${dataAccess}")
    private DataAccess access;

    public AnalyzeCloud(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        access = new DataAccess("/tmp/neo4j");
        access.clear();

     

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        access.clear();
       
        access.getGraphDatabaseService().shutdown();
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
            CloudProvider provider = (CloudProvider) unmarshaller.unmarshal(new File("./config/default/amazonDescription.xml"));
            cloudProviders.add(provider);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        CloudProviderDAO.persistCloudProviders(cloudProviders, access.getGraphDatabaseService());

        int cfgs = new CloudProviderAnalysis().getNrOfPossibleConfigurations(cloudProviders.get(0), "IaaS", "VM");
        int services = new CloudProviderAnalysis().getNrOfServices(cloudProviders.get(0), "IaaS", "VM");
        System.out.println("Services " + services + " with Possible CFGs  "  + cfgs);

    }

}
