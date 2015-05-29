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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
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
public class FlexiantCloudDescriptionGenerationTest {

//    @Value("#{persistenceDelegate}")
    private PersistenceDelegate persistenceDelegate;

    private PersistenceSQLAccess generalAccess;

    private org.hsqldb.Server server;

    static final Logger log = LoggerFactory.getLogger(FlexiantCloudDescriptionGenerationTest.class);

    public FlexiantCloudDescriptionGenerationTest() {
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
        CloudProvider provider = new CloudProvider("Flexiant");
        provider.setUuid(UUID.fromString("10000000-0000-0000-0000-000000000001"));

        CloudServicesSpecification cloudServicesSpecification = new CloudServicesSpecification();
        cloudServicesSpecification.addCloudProvider(provider);

        {
            //storage service
            {

                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("CloudStorage")
                        .withUuid(UUID.fromString("30000000-0000-0000-0000-000000000002"));
                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("diskSizeCost")
                                .withCostMetric(new Metric("diskSize", "GB", Metric.MetricType.RESOURCE)) // needs to be converted from disk_total in Ganglia
                                .withBillingCycle(CostElement.BillingCycle.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //5 units per month => 5 /30 units per day per GB no mather how many GBs no mather how many hours
                                .withBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 5d / 30 / 24))
                        .withCostElement(new CostElement("diskUsageCost")
                                .withCostMetric(new Metric("IODataSize", "GB", Metric.MetricType.RESOURCE)) //todo Write Ganglia Plug-in for IOStat
                                .withType(CostElement.Type.USAGE)
                                .withBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 2d)));
                provider.addCloudOfferedService(unit);

            }

//            RAM	CPU Cores	Units per hour
//            0.5Gb	1               2
//            1 Gb	1               3
//            2 Gb	1               5
//            4 Gb	2               10
//            6 Gb	3               15
//            8 Gb	4               20
            class PricingHelper {

                Double RAM;
                Double CPU;
                Double unitsPerHour;

                public PricingHelper(Double RAM, Double CPU, Double unitsPerHour) {
                    this.RAM = RAM;
                    this.CPU = CPU;
                    this.unitsPerHour = unitsPerHour;
                }

            }

            PricingHelper[] vmCosts = new PricingHelper[]{
                new PricingHelper(0.5d, 1d, 2d), //0.5 GB RAM 1 CPU VM COST
                new PricingHelper(1d, 1d, 3d),
                new PricingHelper(2d, 1d, 5d),
                new PricingHelper(4d, 2d, 10d),
                new PricingHelper(6d, 3d, 15d),
                new PricingHelper(8d, 4d, 20d)

            };

            int index = 0;

            for (PricingHelper helper : vmCosts) {

                CloudOfferedService unit = new CloudOfferedService()
                        .withCategory("IaaS")
                        .withSubcategory("VM")
                        .withName("" + helper.CPU + "CPU" + helper.RAM)
                        .withUuid(UUID.fromString("20000000-0000-0000-0000-00000000000" + index++));

                Resource r = new Resource("CPU");
                r.addProperty(new Metric("VCPU", "#", Metric.MetricType.RESOURCE), new MetricValue(2));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vmCost")
                                .withCostMetric(new Metric("instance", "#", Metric.MetricType.RESOURCE))
                                .withBillingCycle(CostElement.BillingCycle.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //2 units per hour no mather how many hours
                                .withBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), helper.unitsPerHour)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //network cost
            {

                CloudOfferedService unit = new CloudOfferedService("IaaS", "Network", "PublicVLAN")
                        .withUuid(UUID.fromString("30000000-0000-0000-0000-000000000001"));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("vlanCost")
                                .withCostMetric(new Metric("vlan", "#", Metric.MetricType.RESOURCE))
                                .withBillingCycle(CostElement.BillingCycle.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //first VLAN free, then rest 1.37 units per hour
                                .withBillingInterval(new MetricValue(1), 0d)
                                .withBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 1.37)
                        ).withCostElement(new CostElement("publicIPsCost")
                                .withCostMetric(new Metric("publicIP", "#", Metric.MetricType.RESOURCE))
                                .withBillingCycle(CostElement.BillingCycle.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //first VLAN free, then rest 1.37 units per hour
                                .withBillingInterval(new MetricValue(5), 0d)
                                .withBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.137)
                        ).withCostElement(new CostElement("dataTransferCost")
                                .withCostMetric(new Metric("dataTransfer", "GB", Metric.MetricType.RESOURCE))
                                .withType(CostElement.Type.USAGE)
                                //first VLAN free, then rest 1.37 units per hour
                                .withBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 5d)
                        )
                );

                provider.addCloudOfferedService(unit);
            }

            //image storage cost
            {

                CloudOfferedService unit = new CloudOfferedService("IaaS", "Misc", "ImageStorage")
                        .withUuid(UUID.fromString("40000000-0000-0000-0000-000000000001"));

                unit.withCostFunction(new CostFunction(unit.getName())
                        .withCostElement(new CostElement("imageStorageCost")
                                .withCostMetric(new Metric("imageSize", "GB", Metric.MetricType.RESOURCE)) // needs to be converted from disk_total in Ganglia
                                .withBillingCycle(CostElement.BillingCycle.HOUR)
                                .withType(CostElement.Type.PERIODIC)
                                //5 units per month => 5 /30 units per day per GB of stored Image Size no mather how many GBs no mather how many hours
                                .withBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 5 / 30d / 24)
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
                                            .withCloudOfferedService(new UsedCloudOfferedService(provider.getUuid(), "Flexiant", UUID.fromString("20000000-0000-0000-0000-000000000001"), "1CPU1")
                                                    .withInstanceUUID(UUID.randomUUID()))
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
                m.marshal(cloudServicesSpecification.getCloudProviders().iterator().next(), new FileWriter("src/test/resources/FLEXIANT_cloudServicesSpecification.xml"));
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

//
//            CompositionRule computeDiskDataLoadForServiceTopolgy = new CompositionRule()
//                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
//                    .withResultingMetric(new Metric("diskDataLoad", "GB", Metric.MetricType.RESOURCE))
//                    .withOperation(new CompositionOperation()
//                            .withOperationType(CompositionOperationType.SUM)
//                            .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
//                            .withReferenceMetric(new Metric("diskDataLoad", "GB", Metric.MetricType.RESOURCE))
//                    );
//
//            CompositionRule computeDiskDataLoadForService = new CompositionRule()
//                    .withTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
//                    .withResultingMetric(new Metric("diskDataLoad", "GB", Metric.MetricType.RESOURCE))
//                    .withOperation(new CompositionOperation()
//                            .withOperationType(CompositionOperationType.SUM)
//                            .withMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
//                            .withReferenceMetric(new Metric("diskDataLoad", "GB", Metric.MetricType.RESOURCE))
//                    );
//            
//            publicIP
//
            {
                Marshaller m = rulesContext.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                m.marshal(compositionRulesConfiguration, new FileWriter("src/test/resources/compositionRules.xml"));
            }

//            FileBasedGangliaParser basedGangliaParser = new FileBasedGangliaParser(new File("src/test/java/resources/monitoring_snapshot_1.xml"));
//
//            MonitoringData snapshot1 = basedGangliaParser.getMonitoringData();
//
//            DataAccessWithManualStructureManagement access = DataAccessWithManualStructureManagement.createInstance();
//            access.addDataSource(basedGangliaParser);
//            access.setDataFreshnessAnalysisEngine(new DefaultFreshnessAnalysisEngine());
//
//            ServiceMonitoringSnapshot monitoringSnapshot;
//            while (access.getFreshestMonitoredData().isEmpty()) {
//                Thread.sleep(1000);
//            }
//
//            monitoringSnapshot = access.getStructuredMonitoredData(service);
//
//            DataAggregationEngine aggregationEngine = new DataAggregationEngine();
//
//            ServiceMonitoringSnapshot enrichedSnapshot = aggregationEngine.enrichMonitoringData(compositionRulesConfiguration, monitoringSnapshot);
// 
        }
//
//        CloudOfferedService service = new CloudOfferedService()
//                .withUuid(UUID) 
//                .withName("Name")
//                .withCategory("CategoryName")
//                .withSubcategory("SubcategoryName")
////                
//                .withCostFunction(new CostFunction()
//                    .withAppliedIfServiceInstanceUses(List<Unit>)
//                    .withCostElement(new CostElement()
//                            .withCostMetric(new Metric("name", "unit/time", Metric.MetricType))
//                            .withBillingPeriod(CostElement.BillingPeriod)
//                            .withType(CostElement.Type.PERIODIC)
//                            .withCostInterval(new MetricValue(FirstIntervalValue), costUnits)
//                            .withCostInterval(new MetricValue(SecondIntervalValue), costUnits)
//                            .withCostInterval(...
//                    ).withCostElement(...
//                     
//        );

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
            InputStream fileStream = new FileInputStream(new File("src/test/resources/FLEXIANT_cloudServicesSpecification.xml"));
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

            DataAccess access = new DataAccess("/tmp/flexiantTstCloud");
            CloudProviderDAO.persistCloudProviders(providers, access.getGraphDatabaseService());

            List<CloudProvider> retrievedProviders = CloudProviderDAO.getAllCloudProviders(access.getGraphDatabaseService());

            CloudProvider mustHaveCostElementsWithMetricsNotCost = retrievedProviders.iterator().next();
            for (CloudOfferedService cloudOfferedService : mustHaveCostElementsWithMetricsNotCost.getCloudOfferedServices()) {
                for (CostFunction function : cloudOfferedService.getCostFunctions()) {
                    for (CostElement element : function.getCostElements()) {
                        Assert.assertNotEquals("Cost element " + element.getName() + " for metric " + element.getCostMetric().getName() + " is cost", element.getCostMetric().getType(), Metric.MetricType.COST);
                    }

                }
            }

        }

    }
}
