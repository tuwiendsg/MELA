///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package at.ac.tuwien.dsg.cloudserviceselection.new4jAccess.daos;
//
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.ServiceUnit;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.DataAccess;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.Neo4jDataAccess;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.daos.CloudProviderDAO;
//import at.ac.tuwien.dsg.cloudofferedservices.util.TransactionManager;
//import at.ac.tuwien.dsg.cloudserviceselection.engines.CloudServiceElasticityAnalysisEngine;
//import at.ac.tuwien.dsg.cloudserviceselection.util.writers.AnalysisResultCSVWriter;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.Unmarshaller;
//import junit.framework.TestCase;
//import org.neo4j.graphdb.Transaction;
//import org.springframework.beans.factory.annotation.Value;
//
///**
// *
// * @author daniel-tuwien
// */
//public class CloudProviderDAOTest extends TestCase {
//
//    @Value("${dataAccess}")
//    private DataAccess access;
//    private Transaction transaction;
//
//    public CloudProviderDAOTest(String testName) {
//        super(testName);
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        access = new DataAccess("/tmp/neo4j");
//        access.clear();
//
//        transaction = access.startTransaction();
//
//        super.setUp();
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        super.tearDown();
//        access.clear();
//        transaction.success();
//        transaction.finish();
//        access.getGraphDatabaseService().shutdown();
//    }
//
//    /**
//     * Grand test
//     */
//    public void testPersistserviceUnit() {
//
//        List<at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider> cloudProviders = new ArrayList<at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider>();
////        
//
//        //
//        // ==========================================================================================
//        // amazon cloud description
//        try {
//            JAXBContext context = JAXBContext.newInstance(at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider.class);
//            Unmarshaller unmarshaller = context.createUnmarshaller();
//            at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider provider = (at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider) unmarshaller.unmarshal(new File("./experiments/amazonDescription.xml"));
//            cloudProviders.add(provider);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        CloudProvider cloudProvider = cloudProviders.get(0);
//
//        cloudProvider.setServiceUnits(cloudProvider.getServiceUnits().subList(4, 5));
//
//        CloudProviderDAO.persistCloudProviders(cloudProviders, access.getGraphDatabaseService());
//
//        try {
//            access.writeGraphAsGraphVis("./experiments/scenario1/AmazonExample_partial.dot");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//    }
//
//}
