/*
 *  Copyright 2015 Technische Universitat Wien (TUW), Distributed Systems Group E184
 * 
 *  This work was partially supported by the European Commission in terms of the 
 *  CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package at.ac.tuwien.dsg.mela.costeval.pricingscheme;

import at.ac.tuwien.dsg.mela.costeval.engines.*;
import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperationType;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.costeval.model.CloudServicesSpecification;
import at.ac.tuwien.dsg.mela.costeval.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel-tuwien
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"file:src/test/java/spring/test-context.xml"})
public class AmazonFlexiantEquivalentCloudDescriptionGenerationTest {

//    @Value("#{persistenceDelegate}")
    private PersistenceDelegate persistenceDelegate;

    private PersistenceSQLAccess generalAccess;

    private org.hsqldb.Server server;

    static final Logger log = LoggerFactory.getLogger(AmazonFlexiantEquivalentCloudDescriptionGenerationTest.class);

    public AmazonFlexiantEquivalentCloudDescriptionGenerationTest() {
    }

    @BeforeClass
    public static void setUpClass() {

    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
//        try {
//            server = new org.hsqldb.Server();
//            server.setLogWriter(null);
//            server.setRestartOnShutdown(false);
//            server.setNoSystemExit(true);
//            server.setPort(9001);
//
//            if (System.getProperty("os.name").contains("Windows")) {
//                server.setDatabasePath(0, "C:\\Windows\\Temp\\mela_test_cost");
//            } else {
//                server.setDatabasePath(0, "/tmp/test/mela_cost");
//            }
//
//            server.setDatabaseName(0, "mela");
//
//            DriverManagerDataSource dataSource = new DriverManagerDataSource();
//            dataSource.setUrl("jdbc:hsqldb:hsql://localhost:9001/mela;hsqldb.cache_rows=100;hsqldb.log_data=false");
//            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
//            dataSource.setUsername("sa");
//            dataSource.setPassword("");
//
//            log.debug("HSQL Database path: " + server.getDatabasePath(0, true));
//            log.info("Starting HSQL Server database '" + server.getDatabaseName(0, true) + "' listening on port: "
//                    + server.getPort());
//            server.start();
//            // server.start() is synchronous; so we should expect online status from server.
//            Assert.isTrue(server.getState() == ServerConstants.SERVER_STATE_ONLINE,
//                    "HSQLDB could not be started. Maybe another instance is already running on " + server.getAddress()
//                    + ":" + server.getPort() + " ?");
//            log.info("Started HSQL Server");
//
//            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//
//            //read content of sql schema
//            BufferedReader reader = null;
//            try {
//
//                reader = new BufferedReader(new FileReader("src/test/java/resources/create-initial-db-schema.sql"));
//            } catch (FileNotFoundException ex) {
//                Logger.getLogger(FlexiantCloudDescription.class.getName()).log(Level.SEVERE, null, ex);
//                fail(ex.getMessage());
//            }
//            String line = "";
//            while ((line = reader.readLine()) != null) {
//                if (!line.isEmpty()) {
//                    jdbcTemplate.execute(line);
//                }
//            }
//
//            persistenceDelegate = new PersistenceDelegate();
//            persistenceDelegate.setDataSource(dataSource);
//            persistenceDelegate.setJdbcTemplate(jdbcTemplate);
//
//            generalAccess = new PersistenceSQLAccess().withDataSource(dataSource).withJdbcTemplate(jdbcTemplate);
//
//        } catch (IOException ex) {
//            Logger.getLogger(FlexiantCloudDescription.class.getName()).log(Level.SEVERE, null, ex);
//            fail(ex.getMessage());
//        }

    }

    @After
    public void tearDown() {

//        server.shutdown();
    }

    /**
     * Test of applyCompositionRules method, of class CostEvalEngine.
     */
    @Test
    public void testDescribeFlexiantServicesAndPrice() throws Exception {

        CostEvalEngine instance = new CostEvalEngine();
        DataAggregationEngine aggregationEngine = new DataAggregationEngine();
        instance.setInstantMonitoringDataEnrichmentEngine(aggregationEngine);

        // to make it easy, I will mark with:
        // 10000000-0000-0000-0000 cloud providers
        // 20000000-0000-0000-0000 VMs
        // 30000000-0000-0000-0000 Network
        // 40000000-0000-0000-0000 Image Storage
        CloudProvider provider = new CloudProvider("Amazon");
        provider.setUuid(UUID.fromString("10000000-0000-0000-0000-000000000002"));

        CloudServicesSpecification cloudServicesSpecification = new CloudServicesSpecification();
        cloudServicesSpecification.addCloudProvider(provider);

        {

//            Amazon EBS General Purpose (SSD) volumes
//            $0.10 per GB-month of provisioned storage
//            Amazon EBS Provisioned IOPS (SSD) volumes
//            $0.125 per GB-month of provisioned storage
//            $0.065 per provisioned IOPS-month
//            Amazon EBS Magnetic volumes
//            $0.05 per GB-month of provisioned storage
//            $0.05 per 1 million I/O requests
//            Amazon EBS Snapshots to Amazon S3
//            $0.095 per GB-month of data stored
            //EBSGeneralPurpose storage service
            {

                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("Storage")
                        .withName("EBSGeneralPurpose")
                        .withUuid(UUID.fromString("30000000-0000-0000-0000-000000000002"));
                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("diskSizeCost")
                                .withCostMetric(new Metric("diskSize", "GB", Metric.MetricType.RESOURCE)) // needs to be converted from disk_total in Ganglia
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //5 units per month => 5 /30 units per day per GB no mather how many GBs no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.1 / 30 / 24))
                        .withCostElement(new CostElement("diskUsageCost")
                                .withCostMetric(new Metric("IODataSize", "GB", Metric.MetricType.RESOURCE)) //todo Write Ganglia Plug-in for IOStat
                                .withType(CostElement.Type.USAGE)
                                //5 units per month => 5 /30 units per day per GB no mather how many GBs no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 2d)));
                provider.addCloudOfferedService(unit);

            }

            int index = 0;

            //m1.small
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("m1.small")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.044)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //t2.micro
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("t2.micro")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.013)
                        )
                ) //                        .withCostFunction(new CostFunction(unit.getName() + "EBSOptimized")
                        //                        .withAppliedIfServiceInstanceUses(new CloudOfferedService()
                        //                                .withCategory("IaaS")
                        //                                .withSubcategory("Storage")
                        //                                .withName("EBSGeneralPurpose")
                        //                                .withUuid(UUID.fromString("30000000-0000-0000-0000-000000000002"))
                        //                        )
                        //                        .withCostElement(new CostElement("EBSOptimizedCost")
                        //                                .withCostMetric(new Metric("ebs_optimized", "#", Metric.MetricType.RESOURCE))
                        //                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                        //                                .withType(CostElement.Type.PERIODIC)
                        //                                //2 units per hour no mather how many hours
                        //                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.044)
                        //                        )
                        //                )
                        ;

                provider.addCloudOfferedService(unit);
            }

            //t1.micro
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("t1.micro")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.026)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //m1.large
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("m1.large")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.175)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //t2.medium
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("m1.large")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.052)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //c3.large
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("c3.large")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.105)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //m1.xlarge
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("m1.xlarge")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.350)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //m3.xlarge
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("m3.xlarge")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.280)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //c4.xlarge
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("m3.xlarge")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.232)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //c3.xlarge
            {
                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("m3.xlarge")
                        .withUuid(UUID.fromString("21000000-0000-0000-0000-00000000000" + index++));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.210)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //            
            //First 1 GB / month	$0.00 per GB
            //Up to 10 TB / month	$0.09 per GB
            //Next 40 TB / month	$0.085 per GB
            //Next 100 TB / month	$0.07 per GB
            //Next 350 TB / month	$0.05 per GB
            //        
//        
            //network cost
            {
                CloudOfferedService unit = new CloudOfferedService("IaaS", "Network", "ElasticIP")
                        .withUuid(UUID.fromString("31000000-0000-0000-0000-000000000001"));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("publicIPsCost")
                                .withCostMetric(new Metric("publicIP", "#", Metric.MetricType.RESOURCE))
                                .withBillingPeriod(CostElement.BillingPeriod.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //first public IP free, then rest 0.005 $ per hour
                                .withCostInterval(new MetricValue(1.0), 0.0)
                                .withCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.005)
                        ).withCostElement(new CostElement("dataTransferCost")
                                .withCostMetric(new Metric("dataTransfer", "GB", Metric.MetricType.RESOURCE))
                                .withType(CostElement.Type.USAGE)
                                //first GB of da transfer free
                                .withCostInterval(new MetricValue(1.0), 0.0)
                                .withCostInterval(new MetricValue(10.0 * 1024), 0.09)
                                .withCostInterval(new MetricValue(40.0 * 1024), 0.085)
                                .withCostInterval(new MetricValue(100.0 * 1024), 0.07)
                                .withCostInterval(new MetricValue(350.0 * 1024), 0.05)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            MonitoredElement service = new MonitoredElement("Service").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                    .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("30000000-0000-0000-0000-000000000001"), "PublicVLAN"))
                    //local processing topology
                    //                    .withContainedElement(new MonitoredElement("LocalProcessingTopology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                    //                            .withContainedElement(new MonitoredElement("LocalProcessingUnit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    //                                    .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("40000000-0000-0000-0000-000000000001"), "ImageStorage"))
                    //                                    .withContainedElement(new MonitoredElement("10.99.0.40").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    //                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000001"), "1CPU1"))
                    //                                    )
                    //                                    .withContainedElement(new MonitoredElement("10.99.0.23").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    //                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000001"), "1CPU1"))
                    //                                    )
                    //                            )
                    //                            .withContainedElement(new MonitoredElement("LocalProcessingQueueUnit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    //                                    .withContainedElement(new MonitoredElement("10.99.0.20").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    //                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000002"), "1CPU2"))
                    //                                    )
                    //                            )
                    //                    )
                    //eventProcessingTopology
                    .withContainedElement(new MonitoredElement("EventProcessingTopology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                            .withContainedElement(new MonitoredElement("EventProcessingUnit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                                    .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("40000000-0000-0000-0000-000000000001"), "ImageStorage"))
                                    .withContainedElement(new MonitoredElement("10.99.0.25").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000001"), "1CPU1"))
                                    )
                            //                                    .withContainedElement(new MonitoredElement("10.99.0.18").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                            //                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000001"), "1CPU1"))
                            //                                    )
                            )
                            .withContainedElement(new MonitoredElement("LoadBalancer").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                                    .withContainedElement(new MonitoredElement("10.99.0.56").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000003"), "2CPU3"))
                                    )
                            )
                    ) //                    //dataEndTopology
                    //                    .withContainedElement(new MonitoredElement("DataEndTopology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                    //                            .withContainedElement(new MonitoredElement("DataNode").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    //                                    .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("40000000-0000-0000-0000-000000000001"), "ImageStorage"))
                    //                                    .withContainedElement(new MonitoredElement("10.99.0.19").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    //                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000001"), "1CPU1"))
                    //                                    )
                    //                                    .withContainedElement(new MonitoredElement("10.99.0.65").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    //                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000001"), "1CPU1"))
                    //                                    )
                    //                            )
                    //                            .withContainedElement(new MonitoredElement("DataController").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    //                                    .withContainedElement(new MonitoredElement("10.99.0.16").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    //                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000003"), "2CPU3"))
                    //                                    )
                    //                            )
                    //                    )
                    ;

            {
                JAXBContext elementContext = JAXBContext.newInstance(MonitoredElement.class);
                //persist structure
                Marshaller m = elementContext.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                m.marshal(service, new FileWriter("src/test/resources/serviceStructure.xml"));
            }

            {
                JAXBContext elementContext = JAXBContext.newInstance(CloudProvider.class);
                //persist structure
                Marshaller m = elementContext.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                m.marshal(cloudServicesSpecification.getCloudProviders().iterator().next(), new FileWriter("src/test/resources/AMAZON_FLEXIANT_cloudServicesSpecification.xml"));
            }

            JAXBContext rulesContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);

            CompositionRulesConfiguration compositionRulesConfiguration = (CompositionRulesConfiguration) rulesContext.createUnmarshaller()
                    .unmarshal(new File("src/test/resources/compositionRules.xml"));

            compositionRulesConfiguration.setTargetServiceID(service.getId());

            ArrayList<CompositionRule> rules = compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules();

            //some things are static and need to be injected
            CompositionRule localProcessingUnitImageSizeRule = new CompositionRule()
                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    .withTargetMonitoredElementID("LocalProcessingUnit")
                    .withResultingMetric(new Metric("imageSize", "GB", Metric.MetricType.RESOURCE))
                    .withOperation(new CompositionOperation()
                            .withOperationType(CompositionOperationType.SET_VALUE)
                            .withValue("20.0")
                    );
            if (!rules.contains(localProcessingUnitImageSizeRule)) {
                rules.add(localProcessingUnitImageSizeRule);
            }

            CompositionRule eventProcessingUnitAndDataNodeImageSizeRule = new CompositionRule()
                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    .withTargetMonitoredElementID("EventProcessingUnit")
                    .withTargetMonitoredElementID("DataNode")
                    .withResultingMetric(new Metric("imageSize", "GB", Metric.MetricType.RESOURCE))
                    .withOperation(new CompositionOperation()
                            .withOperationType(CompositionOperationType.SET_VALUE)
                            .withValue("40.0")
                    );
            if (!rules.contains(eventProcessingUnitAndDataNodeImageSizeRule)) {
                rules.add(eventProcessingUnitAndDataNodeImageSizeRule);
            }
            CompositionRule computeIPUsedForService = new CompositionRule()
                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                    .withResultingMetric(new Metric("publicIP", "#", Metric.MetricType.RESOURCE))
                    .withOperation(new CompositionOperation()
                            .withOperationType(CompositionOperationType.SUM)
                            .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM)
                            .withReferenceMetric(new Metric("publicIP", "#", Metric.MetricType.RESOURCE))
                    );

            if (!rules.contains(computeIPUsedForService)) {
                rules.add(computeIPUsedForService);
            }

            //data transfer rule
            CompositionRule computePublicIPDataTransferForServiceUnit = new CompositionRule()
                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    .withResultingMetric(new Metric("dataTransfer", "GB", Metric.MetricType.RESOURCE))
                    .withOperation(new CompositionOperation()
                            .withOperationType(CompositionOperationType.SUM)
                            .withSubOperation(new CompositionOperation()
                                    .withOperationType(CompositionOperationType.DIV)
                                    .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM)
                                    .withReferenceMetric(new Metric("dataIn", "byte", Metric.MetricType.RESOURCE))
                                    .withValue("" + ((Double) Math.pow(1024, 3)).toString())
                            )
                            .withSubOperation(new CompositionOperation()
                                    .withOperationType(CompositionOperationType.DIV)
                                    .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM)
                                    .withReferenceMetric(new Metric("dataOut", "byte", Metric.MetricType.RESOURCE))
                                    .withValue("" + ((Double) Math.pow(1024, 3)).toString())
                            )
                    );

            if (!rules.contains(computePublicIPDataTransferForServiceUnit)) {
                rules.add(computePublicIPDataTransferForServiceUnit);
            }

            CompositionRule computePublicIPDataTransferForServiceTopology = new CompositionRule()
                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                    .withResultingMetric(new Metric("dataTransfer", "GB", Metric.MetricType.RESOURCE))
                    .withOperation(new CompositionOperation()
                            .withOperationType(CompositionOperationType.SUM)
                            .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                            .withReferenceMetric(new Metric("dataTransfer", "GB", Metric.MetricType.RESOURCE))
                    );

            if (!rules.contains(computePublicIPDataTransferForServiceTopology)) {
                rules.add(computePublicIPDataTransferForServiceTopology);
            }
            CompositionRule computePublicIPDataTransferForService = new CompositionRule()
                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                    .withResultingMetric(new Metric("dataTransfer", "GB", Metric.MetricType.RESOURCE))
                    .withOperation(new CompositionOperation()
                            .withOperationType(CompositionOperationType.SUM)
                            .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                            .withReferenceMetric(new Metric("dataTransfer", "GB", Metric.MetricType.RESOURCE))
                    );

            if (!rules.contains(computePublicIPDataTransferForService)) {
                rules.add(computePublicIPDataTransferForService);
            }

            //data transfer rule
            CompositionRule computeDiskDataLoadForServiceUnit = new CompositionRule()
                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    .withResultingMetric(new Metric("IODataSize", "GB", Metric.MetricType.RESOURCE))
                    .withOperation(new CompositionOperation()
                            .withOperationType(CompositionOperationType.SUM)
                            .withSubOperation(new CompositionOperation()
                                    .withOperationType(CompositionOperationType.DIV)
                                    .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM)
                                    .withReferenceMetric(new Metric("diskDataLoad", "kbyte", Metric.MetricType.RESOURCE))
                                    .withValue("" + ((Double) Math.pow(1024, 2)).toString())
                            )
                    );

            if (!rules.contains(computeDiskDataLoadForServiceUnit)) {
                rules.add(computeDiskDataLoadForServiceUnit);
            }

        }

        //test we can read what was generated
        {

            for (CloudOfferedService cloudOfferedService : provider.getCloudOfferedServices()) {
                for (CostFunction function : cloudOfferedService.getCostFunctions()) {
                    for (CostElement element : function.getCostElements()) {
                        Assert.assertNotEquals("Cost element " + element.getName() + " for metric " + element.getCostMetric().getName() + " is cost", element.getCostMetric().getType(), Metric.MetricType.COST);
                    }

                }
            }

            JAXBContext jAXBContext = JAXBContext.newInstance(CloudProvider.class);
            InputStream fileStream = new FileInputStream(new File("src/test/resources/AMAZON_FLEXIANT_cloudServicesSpecification.xml"));
            CloudProvider fromText = (CloudProvider) jAXBContext.createUnmarshaller().unmarshal(fileStream);
            TestCase.assertNotNull(fromText);

            List<CloudProvider> providers = new ArrayList<>();
            providers.add(fromText);

            for (CloudOfferedService cloudOfferedService : fromText.getCloudOfferedServices()) {
                for (CostFunction function : cloudOfferedService.getCostFunctions()) {
                    for (CostElement element : function.getCostElements()) {
                        Assert.assertNotEquals("Cost element " + element.getName() + " for metric " + element.getCostMetric().getName() + " is cost", element.getCostMetric().getType(), Metric.MetricType.COST);
                    }

                }
            }

        }

    }
}
