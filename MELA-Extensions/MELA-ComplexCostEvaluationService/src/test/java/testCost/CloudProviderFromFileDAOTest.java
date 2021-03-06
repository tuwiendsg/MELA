/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package testCost;

import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.ServiceUnitDAO;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import junit.framework.TestCase;

/**
 *
 * @author daniel-tuwien
 */
public class CloudProviderFromFileDAOTest extends TestCase {

    private DataAccess access;

    public CloudProviderFromFileDAOTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        access = new DataAccess("/tmp/neo4j");
        access.clear();

//        transaction = access.startTransaction();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
//        transaction.success();
//        transaction.finish();
        access.destroy();
    }

    /**
     * Test of matchServiceUnit method, of class RequirementsMatchingEngine.
     */
    public void testEcosystemDescription() throws IOException, JAXBException {

        List<CloudProvider> cloudProviders = new ArrayList<CloudProvider>();

        JAXBContext jAXBContext = JAXBContext.newInstance(CloudProvider.class);
        InputStream fileStream = new FileInputStream(new File("./config/default/Flexiant.xml"));
        CloudProvider specification = (CloudProvider) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        cloudProviders.add(specification);

        CloudProviderDAO.persistCloudProviders(cloudProviders, access.getGraphDatabaseService());

//        ServiceUnitDAO.persistCloudServiceUnits(cloudProviders.get(0).getServiceUnits(), access.getGraphDatabaseService());
        //to get individual data you use DAOS
//        from at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos
        for (CloudProvider cloudProvider : CloudProviderDAO.getAllCloudProviders(access.getGraphDatabaseService())) {
            for (CloudOfferedService unit : ServiceUnitDAO.getCloudServiceUnitsForCloudProviderNode(cloudProvider.getId(), access.getGraphDatabaseService())) {
                System.out.println(unit.getId() + " " + unit.getName());
            }
        }

    }
}
