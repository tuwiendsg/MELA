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
package at.ac.tuwien.dsg.mela.costeval.engines;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.ServiceElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.costeval.control.CostEvalManager;
import static at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngine.log;
import at.ac.tuwien.dsg.mela.costeval.model.CloudServicesSpecification;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.model.LifetimeEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.costeval.utils.conversion.CostJSONConverter;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.cxf.common.i18n.UncheckedException;
import org.hsqldb.server.ServerConstants;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.Assert;

/**
 *
 * @author daniel-tuwien
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"file:src/test/java/spring/test-context.xml"})
public class CostEvalOverTimeTest {

//    @Value("#{persistenceDelegate}")
    private PersistenceDelegate persistenceDelegate;
    private at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceDelegate dataAccessPersistenceDelegate;

    private PersistenceSQLAccess generalAccess;

    public CostEvalOverTimeTest() {
    }

    @BeforeClass
    public static void setUpClass() {

    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        try {

            //run hsql in memory only for testing purposes
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl("jdbc:hsqldb:mem:mela-test-db-2");
            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            //read content of sql schema
            BufferedReader reader = null;
            try {

                reader = new BufferedReader(new FileReader("src/test/resources/create-initial-db-schema.sql"));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(CostEvalOverTimeTest.class.getName()).log(Level.SEVERE, null, ex);
                fail(ex.getMessage());
            }
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    jdbcTemplate.execute(line);
                }
            }

            persistenceDelegate = new PersistenceDelegate();
            persistenceDelegate.setDataSource(dataSource);
            persistenceDelegate.setJdbcTemplate(jdbcTemplate);

            generalAccess = new PersistenceSQLAccess().withDataSource(dataSource).withJdbcTemplate(jdbcTemplate);

            persistenceDelegate.setPersistenceSQLAccess(generalAccess);

        } catch (IOException ex) {
            Logger.getLogger(CostEvalOverTimeTest.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        }

    }

    @After
    public void tearDown() {

    }

    /**
     * Test of applyCompositionRules method, of class CostEvalEngine.
     */
    @Test
    public void testCostEval1() throws Exception {

//        assertFalse( "Please check why in the enriched monitoring snapshot we still have the COST ORIGIN metrics as COST metrics, and thus, why are they USED in RADIAL chart, and especially"
//                + "why they HAVE COST in this radial crap. THEY SHOULD NOOOOOT HAAAAVE", true);
        CostEvalEngine costEvalEngine = new CostEvalEngine();
        DataAggregationEngine aggregationEngine = new DataAggregationEngine();
        costEvalEngine.setInstantMonitoringDataEnrichmentEngine(aggregationEngine);

        CloudProvider provider = new CloudProvider("Amazon");
        provider.setUuid(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"));

        CloudServicesSpecification cloudServicesSpecification = new CloudServicesSpecification();
        cloudServicesSpecification.addCloudProvider(provider);

        CloudOfferedService vm1SmallService = new CloudOfferedService("IaaS", "VM", "m1.small");
        vm1SmallService.withUuid(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"));

        //VM COST
        {
            CostFunction vmCost = new CostFunction(vm1SmallService.getName() + "_cost");

            CostElement periodicCostElement = new CostElement("vmCost", new Metric("instance", "#/h", Metric.MetricType.COST), CostElement.Type.PERIODIC);
            periodicCostElement.addBillingInterval(new MetricValue(1), 1d);
            periodicCostElement.addBillingInterval(new MetricValue(2), 2d);
            periodicCostElement.addBillingInterval(new MetricValue(3), 3d);
            vmCost.addCostElement(periodicCostElement);
            vm1SmallService.addCostFunction(vmCost);

            CostElement usageCostElement = new CostElement("usageCost", new Metric("usage", "#/s", Metric.MetricType.COST), CostElement.Type.USAGE);
            usageCostElement.addBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.5d);
            vmCost.addCostElement(usageCostElement);
        }

        provider.addCloudOfferedService(vm1SmallService);

        CloudOfferedService vm1LargeService = new CloudOfferedService("IaaS", "VM", "m1.large");
        vm1LargeService.withUuid(UUID.fromString("38400000-8cf0-11bd-b23e-000000000001"));

        //VM COST
        {
            CostFunction vmCost = new CostFunction(vm1LargeService.getName() + "_cost");

            CostElement periodicCostElement = new CostElement("vmCost", new Metric("instance", "#/h", Metric.MetricType.COST), CostElement.Type.PERIODIC);
            periodicCostElement.addBillingInterval(new MetricValue(1), 2d);
            periodicCostElement.addBillingInterval(new MetricValue(2), 4d);
            periodicCostElement.addBillingInterval(new MetricValue(3), 6d);
            vmCost.addCostElement(periodicCostElement);
            vm1LargeService.addCostFunction(vmCost);

            CostElement usageCostElement = new CostElement("usageCost", new Metric("usage", "#/s", Metric.MetricType.COST), CostElement.Type.USAGE);
            usageCostElement.addBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 1d);
            vmCost.addCostElement(usageCostElement);
        }
        provider.addCloudOfferedService(vm1LargeService);

        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = new HashMap<UUID, Map<UUID, CloudOfferedService>>();

        Map<UUID, CloudOfferedService> cloudUnits = new HashMap<UUID, CloudOfferedService>();
        cloudProvidersMap.put(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"), cloudUnits);

        for (CloudOfferedService unit : provider.getCloudOfferedServices()) {
            cloudUnits.put(unit.getUuid(), unit);
        }

        MonitoredElement vm = new MonitoredElement("UNIT_INSTANCE").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                .withCloudOfferedService(new UsedCloudOfferedService()
                        .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                        .withCloudProviderName("Amazon")
                        .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-000000000000"))
                        .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                        .withName("m1.small")
                );

        MonitoredElement unit = new MonitoredElement("Unit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                .withContainedElement(vm);

        MonitoredElement topology = new MonitoredElement("Topology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                .withContainedElement(unit);

        MonitoredElement service = new MonitoredElement("Service").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                .withContainedElement(topology);

        //persist service struct
        {
            JAXBContext elementContext = JAXBContext.newInstance(MonitoredElement.class);
            //persist structure
            Marshaller m = elementContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(service, new FileWriter("src/test/resources/serviceStructure_with_services.xml"));
        }

        //make sure all is clean
        persistenceDelegate.removeService(service.getId());

        persistenceDelegate.writeMonitoringSequenceId(service.getId());
        persistenceDelegate.writeConfiguration(service.getId(), new ConfigurationXMLRepresentation().withServiceConfiguration(service).withCompositionRulesConfiguration(new CompositionRulesConfiguration()).withRequirements(new Requirements()));

        ServiceMonitoringSnapshot monitoringSnapshot1 = new ServiceMonitoringSnapshot().withTimestamp("1000");

//        Metric instanceMetric = new Metric("instance", "#", Metric.MetricType.RESOURCE);
        Metric usageMetric = new Metric("usage", "#/s", Metric.MetricType.RESOURCE);

        Metric ELEMENT_COST_METRIC = new Metric("element_cost", "costUnits", Metric.MetricType.COST);
        Metric CHILDREN_COST_METRIC = new Metric("children_cost", "costUnits", Metric.MetricType.COST);

        Metric totalInstanceMetricCost = new Metric("cost_instance", "costUnits", Metric.MetricType.COST);
        Metric totalUsageMetricCost = new Metric("cost_usage", "costUnits", Metric.MetricType.COST);

        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
//            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(0).withFreshness(80d));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(0).withFreshness(50d));

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(elementMonitoringSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot1.addMonitoredData(elementMonitoringSnapshot);
            monitoringSnapshot1.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot1.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot1.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot1 = monitoringSnapshot1.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot1.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot1.getTimestamp(), monitoringSnapshot1, service.getId());

        }
        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot2 = new ServiceMonitoringSnapshot().withTimestamp("2000");
        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
//            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(1).withFreshness(80d));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(1.0).withFreshness(50d));

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(elementMonitoringSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot2.addMonitoredData(elementMonitoringSnapshot);
            monitoringSnapshot2.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot2.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot2.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot2 = monitoringSnapshot2.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot2.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot2.getTimestamp(), monitoringSnapshot2, service.getId());

        }

        LifetimeEnrichedSnapshot serviceUsageSnapshot1 = new LifetimeEnrichedSnapshot()
                .withSnapshot(monitoringSnapshot1)
                .withtLastUpdatedTimestampID(monitoringSnapshot1.getTimestampID());

        //test1
        LifetimeEnrichedSnapshot totalUsageSnapshot_0 = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, null, monitoringSnapshot1);

        LifetimeEnrichedSnapshot totalUsageSnapshot = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot_0.clone(), monitoringSnapshot2);

        //test that the service structure is still ok
        {
            MonitoredElement testServiceStruct = totalUsageSnapshot.getSnapshot().getMonitoredService();

            assertTrue(testServiceStruct.getContainedElements().size() == 1 && testServiceStruct.getContainedElements().contains(topology));
            MonitoredElement testServiceStructTopology = testServiceStruct.getContainedElements().iterator().next();

            assertTrue(testServiceStructTopology.getContainedElements().size() == 1 && testServiceStructTopology.getContainedElements().contains(unit));
            MonitoredElement testServiceUnit = testServiceStructTopology.getContainedElements().iterator().next();

            assertTrue(testServiceUnit.getContainedElements().size() == 1 && testServiceUnit.getContainedElements().contains(vm));

            MonitoredElement testServiceUnitVM = testServiceUnit.getContainedElements().iterator().next();

            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

            assertTrue(testServiceUnitVM.getContainedElements().size() == 1 && testServiceUnitVM.getContainedElements().contains(usedCloudServiceMonitoredElement));

        }

        persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId(), totalUsageSnapshot);

        totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId());

//        assertEquals(new MetricValue(1), totalUsageSnapshot.getSnapshot().getMonitoredData(vm).getMetricValue(instanceMetric));
        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

            assertEquals(new MetricValue(1.0), totalUsageSnapshot.getSnapshot().getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(usageMetric));
        }

        persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId(), totalUsageSnapshot);

        LifetimeEnrichedSnapshot instantCostCleaned1 = costEvalEngine.cleanUnusedServices(totalUsageSnapshot);

        CompositionRulesBlock block1 = costEvalEngine.createCompositionRulesForInstantUsageCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, instantCostCleaned1.getSnapshot().getMonitoredService(), instantCostCleaned1, monitoringSnapshot2.getTimestamp());

        ServiceMonitoringSnapshot instantCost1 = costEvalEngine.applyCompositionRules(block1, costEvalEngine.convertToStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, monitoringSnapshot2));

        generalAccess.writeMonitoringSequenceId(service.getId());
        generalAccess.writeInTimestamp("" + serviceUsageSnapshot1.getLastUpdatedTimestampID(), service, service.getId());
        generalAccess.writeConfiguration(service.getId(), new ConfigurationXMLRepresentation(service, new CompositionRulesConfiguration(),
                new Requirements()));

        persistenceDelegate.persistInstantCostSnapshot(service.getId(), new CostEnrichedSnapshot().withCostCompositionRules(block1).withSnapshot(instantCost1).withLastUpdatedTimestampID(instantCost1.getTimestampID()));

        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot3 = new ServiceMonitoringSnapshot().withTimestamp("3000");
        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
//            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(2).withFreshness(80d));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(2.0).withFreshness(50d));

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(elementMonitoringSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot3.addMonitoredData(elementMonitoringSnapshot);
            monitoringSnapshot3.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot3.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot3.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot3 = monitoringSnapshot3.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot3.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot3.getTimestamp(), monitoringSnapshot3, service.getId());

        }

        LifetimeEnrichedSnapshot totalServiceUsage2 = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot, monitoringSnapshot3);

//        assertEquals(new MetricValue(2), totalServiceUsage2.getSnapshot().getMonitoredData(vm).getMetricValue(instanceMetric));
        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(3.0), totalServiceUsage2.getSnapshot().getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(usageMetric));
        }

        persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId(), totalServiceUsage2);

        totalServiceUsage2 = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId());

        LifetimeEnrichedSnapshot instantCostCleaned2 = costEvalEngine.cleanUnusedServices(totalServiceUsage2);

        generalAccess.writeInTimestamp("" + totalServiceUsage2.getLastUpdatedTimestampID(), service, service.getId());
//        persistenceDelegate.persistTotalCostSnapshot(service.getId(), serviceUsageSnapshot2);

        CompositionRulesBlock block2 = costEvalEngine.createCompositionRulesForInstantUsageCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, instantCostCleaned2.getSnapshot().getMonitoredService(), instantCostCleaned2, monitoringSnapshot3.getTimestamp());

        ServiceMonitoringSnapshot instantCost2 = costEvalEngine.applyCompositionRules(block2, costEvalEngine.convertToStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, monitoringSnapshot3));

        persistenceDelegate.persistInstantCostSnapshot(service.getId(), new CostEnrichedSnapshot().withCostCompositionRules(block2).withSnapshot(instantCost2).withLastUpdatedTimestampID(instantCost2.getTimestampID()));

//        MonitoredElement newVM = new MonitoredElement("UNIT_INSTANCE_2").withLevel(MonitoredElement.MonitoredElementLevel.VM)
//                .withCloudOfferedService(new UsedCloudOfferedService()
//                        .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
//                        .withCloudProviderName("Amazon")
//                        .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
//                        .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-000000000001"))
//                        .withName("m1.small")
//                );

        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot4 = new ServiceMonitoringSnapshot().withTimestamp("37000000");
        {

            unit.getContainedElements().clear();
            unit.withContainedElement(vm);

            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
//            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(10).withFreshness(80d));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(1.0).withFreshness(50d));

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(elementMonitoringSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot4.addMonitoredData(elementMonitoringSnapshot);
            monitoringSnapshot4.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot4.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot4.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot4 = monitoringSnapshot4.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot4.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot4.getTimestamp(), monitoringSnapshot4, service.getId());

        }

        LifetimeEnrichedSnapshot totalServiceUsage3 = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalServiceUsage2, monitoringSnapshot4);

        CompositionRulesBlock totalCostRules = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalServiceUsage3, totalServiceUsage3.getSnapshot().getTimestamp());
        ServiceMonitoringSnapshot totalCostEnrichedSnapshot = costEvalEngine.applyCompositionRules(totalCostRules, totalServiceUsage3.getSnapshot());

        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(30.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalInstanceMetricCost));
            assertEquals(new MetricValue(18499.5), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalUsageMetricCost));
        }
    }

}
