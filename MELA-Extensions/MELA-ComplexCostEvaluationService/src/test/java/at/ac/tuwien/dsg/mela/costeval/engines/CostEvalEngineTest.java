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
import at.ac.tuwien.dsg.mela.costeval.model.UnusedCostUnitsReport;
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
public class CostEvalEngineTest {

//    @Value("#{persistenceDelegate}")
    private PersistenceDelegate persistenceDelegate;
    private at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceDelegate dataAccessPersistenceDelegate;

    private PersistenceSQLAccess generalAccess;

    public CostEvalEngineTest() {
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
            dataSource.setUrl("jdbc:hsqldb:mem:mela-test-db");
            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            //read content of sql schema
            BufferedReader reader = null;
            try {

                reader = new BufferedReader(new FileReader("src/test/resources/create-initial-db-schema.sql"));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(CostEvalEngineTest.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(CostEvalEngineTest.class.getName()).log(Level.SEVERE, null, ex);
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
    public void testCostEval() throws Exception {

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

            CostElement periodicCostElement = new CostElement("vmCost", new Metric("instance", "#/s", Metric.MetricType.COST), CostElement.Type.PERIODIC);
            periodicCostElement.addBillingInterval(new MetricValue(1), 1d);
            periodicCostElement.addBillingInterval(new MetricValue(2), 2d);
            periodicCostElement.addBillingInterval(new MetricValue(3), 3d);
            periodicCostElement.addBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 4d);
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

            CostElement periodicCostElement = new CostElement("vmCost", new Metric("instance", "#/s", Metric.MetricType.COST), CostElement.Type.PERIODIC);
            periodicCostElement.addBillingInterval(new MetricValue(1), 2d);
            periodicCostElement.addBillingInterval(new MetricValue(2), 4d);
            periodicCostElement.addBillingInterval(new MetricValue(3), 6d);
            periodicCostElement.addBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 12d);
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

        Metric instanceMetricCost = new Metric("cost_instance", "costUnits/s", Metric.MetricType.COST);
        Metric usageMetricCost = new Metric("cost_usage", "costUnits", Metric.MetricType.COST);

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

        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

            assertEquals(new MetricValue(1.0), instantCost1.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(instanceMetricCost));
            assertEquals(new MetricValue(0.5), instantCost1.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(usageMetricCost));

        }

        assertEquals(new MetricValue(1.5), instantCost1.getMonitoredData(service).getMetricValue(ELEMENT_COST_METRIC));
        assertEquals(new MetricValue(1.5), instantCost1.getMonitoredData(service).getMetricValue(CHILDREN_COST_METRIC));

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

        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(2.0), instantCost2.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(instanceMetricCost));
            assertEquals(new MetricValue(1.0), instantCost2.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(usageMetricCost));
        }

        MonitoredElement newVM = new MonitoredElement("UNIT_INSTANCE_2").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                .withCloudOfferedService(new UsedCloudOfferedService()
                        .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                        .withCloudProviderName("Amazon")
                        .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                        .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-000000000001"))
                        .withName("m1.small")
                );

        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot4 = new ServiceMonitoringSnapshot().withTimestamp("4000");
        {

            unit.getContainedElements().clear();
            unit.withContainedElement(newVM);

            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(newVM);
//            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(10).withFreshness(80d));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(100.0).withFreshness(50d));

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
        persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId(), totalServiceUsage3);

        totalServiceUsage3 = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId());

//        assertEquals(new MetricValue(3), totalServiceUsage3.getSnapshot().getMonitoredData(vm).getMetricValue(instanceMetric));
//        assertEquals(new MetricValue(1), totalServiceUsage3.getSnapshot().getMonitoredData(newVM).getMetricValue(instanceMetric));
        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(3.0), totalServiceUsage3.getSnapshot().getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(usageMetric));
        }

        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(newVM.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(newVM.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(100.0), totalServiceUsage3.getSnapshot().getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(usageMetric));

        }

        CompositionRulesBlock totalCostRules = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalServiceUsage3, totalServiceUsage3.getSnapshot().getTimestamp());
        ServiceMonitoringSnapshot totalCostEnrichedSnapshot = costEvalEngine.applyCompositionRules(totalCostRules, totalServiceUsage3.getSnapshot());

        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(6.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalInstanceMetricCost));
            assertEquals(new MetricValue(1.5), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalUsageMetricCost));
        }

        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(newVM.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(newVM.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(1.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalInstanceMetricCost));
            assertEquals(new MetricValue(50.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalUsageMetricCost));
        }

        generalAccess.writeInTimestamp("" + totalServiceUsage3.getLastUpdatedTimestampID(), service, service.getId());
        persistenceDelegate.setPersistenceSQLAccess(generalAccess);
        persistenceDelegate.persistTotalCostSnapshot(service.getId(),
                new CostEnrichedSnapshot()
                .withCostCompositionRules(totalCostRules)
                .withSnapshot(totalCostEnrichedSnapshot).
                withLastUpdatedTimestampID(totalCostEnrichedSnapshot.getTimestampID()));

        totalCostEnrichedSnapshot = persistenceDelegate.extractTotalCostSnapshot(service.getId()).getSnapshot();
        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(vm.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(vm.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(6.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalInstanceMetricCost));
            assertEquals(new MetricValue(1.5), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalUsageMetricCost));
        }

        {
            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                    .withId(newVM.getCloudOfferedServices().iterator().next().getInstanceUUID().toString())
                    .withName(newVM.getCloudOfferedServices().iterator().next().getName())
                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
            assertEquals(new MetricValue(1.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalInstanceMetricCost));
            assertEquals(new MetricValue(50.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(totalUsageMetricCost));
        }

        CostEvalManager manager = new CostEvalManager();
        manager.setPersistenceDelegate(persistenceDelegate);

        ElasticitySpace space = manager.updateAndGetInstantCostElasticitySpace(service.getId());
        List<MetricValue> metricValues = space.getMonitoredDataForService(vm).get(ELEMENT_COST_METRIC);

        assertEquals(new MetricValue(1.5), metricValues.get(0));
        assertEquals(new MetricValue(3.0), metricValues.get(1));

        ServiceElasticityPathway elasticityPathway = manager.updateAndGetInstantCostElasticityPathway(service.getId());
        assertFalse(elasticityPathway.getPathway().isEmpty());

        log.info("Situations for VM " + elasticityPathway.getPathway(vm).getSituationGroups().size());

        CostJSONConverter converter = new CostJSONConverter();

//        totalCostEnrichedSnapshot.getMonitoredData().remove(MonitoredElement.MonitoredElementLevel.VM);
        log.info("");
        log.info("Radial : totalCostEnrichedSnapshot");
        log.info("");
        log.info(converter.toJSONForRadialPieChart(totalCostEnrichedSnapshot));
        log.info("");
        log.info("Radial : instantCost2");
        log.info("");
        log.info(converter.toJSONForRadialPieChart(instantCost2));
        log.info("Tree view : totalCostEnrichedSnapshot");
        log.info("");
        log.info(converter.convertMonitoringSnapshotAndCompositionRules(totalCostEnrichedSnapshot, totalCostRules));
        log.info("");
        log.info("ElasticitySpace : totalCostEnrichedSnapshot");
        log.info("");
        log.info(converter.convertElasticitySpace(space, service));

        //test what happends when we emulate another pricing scheme
        {
            MonitoredElement newVM1 = new MonitoredElement("UNIT_INSTANCE").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    .withCloudOfferedService(new UsedCloudOfferedService()
                            .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                            .withCloudProviderName("Amazon")
                            .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-000000000002"))
                            .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000001"))
                            .withName("m1.large")
                    );

            MonitoredElement newVM2 = new MonitoredElement("UNIT_INSTANCE_2").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    .withCloudOfferedService(new UsedCloudOfferedService()
                            .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                            .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-000000000003"))
                            .withCloudProviderName("Amazon")
                            .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000001"))
                            .withName("m1.large")
                    );

            MonitoredElement newUnit = new MonitoredElement("Unit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                    .withContainedElement(newVM1)
                    .withContainedElement(newVM2);

            MonitoredElement newTopology = new MonitoredElement("Topology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                    .withContainedElement(newUnit);

            MonitoredElement newService = new MonitoredElement("Service").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                    .withContainedElement(newTopology);

            Map<MonitoredElement, List<UsedCloudOfferedService>> usedServicesMap = new HashMap<>();
            for (MonitoredElement element : newService) {
                usedServicesMap.put(element, element.getCloudOfferedServices());
            }
            String newname = "Service_emulated";
            //get allready monitored service
            ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service.getId());
            if (cfg == null) {
                throw new UncheckedException(new Throwable("Service with ID " + service.getId() + " not found in mon data"));
            }

            CompositionRulesConfiguration compositionRulesConfiguration = cfg.getCompositionRulesConfiguration();

            //for each Structured Monitoring Information Stored, we need to extract it, update used services, aggregate it, then apply cost rules
            MonitoredElement prevService = cfg.getServiceConfiguration();

            //if space is not null, update it with new data
            List<ServiceMonitoringSnapshot> dataFromTimestamp = persistenceDelegate.extractStructuredMonitoringData(prevService.getId());

            //extract usage so far only once, and just add to it.
            LifetimeEnrichedSnapshot usageSoFar = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(newname);

            //as the above method retrieves in steps of 1000 the data to avoids killing the HSQL
            while (!dataFromTimestamp.isEmpty()) {
                //gets data after the supplied timestamp

                int nextTimestamp = dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID();
                for (ServiceMonitoringSnapshot snapshot : dataFromTimestamp) {

                    MonitoredElement snapshotServicfCFG = snapshot.getMonitoredService();
                    MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = snapshot.getMonitoredData(snapshotServicfCFG);
                    //change used cloud services
                    for (MonitoredElement element : snapshotServicfCFG) {
                        if (usedServicesMap.containsKey(element)) {
                            element.setCloudOfferedServices(usedServicesMap.get(element));
                        } else {
                            //maybe in this proposed structure the element does not use services anymore
                            element.getCloudOfferedServices().clear();
                        }
                    }
                    //clear prev service data
                    //the next steps solve an issue with previousely computed hash code, as If I change the ID of the service, the old hashcode is invalid, 
                    //and while I can get the Key from the map, if can;t use it to access the map values
                    snapshot.getMonitoredData().remove(MonitoredElement.MonitoredElementLevel.SERVICE);
                    snapshotServicfCFG.setId(newname);
                    elementMonitoringSnapshot.withMonitoredElement(snapshotServicfCFG);
                    snapshot.addMonitoredData(elementMonitoringSnapshot);

                    //persist new added struct
                    persistenceDelegate.writeMonitoringSequenceId(newname);
                    persistenceDelegate.writeConfiguration(newname, new ConfigurationXMLRepresentation().withServiceConfiguration(snapshotServicfCFG).withCompositionRulesConfiguration(cfg.getCompositionRulesConfiguration()).withRequirements(cfg.getRequirements()));

                    //aggregate struct data
                    ServiceMonitoringSnapshot aggregated = aggregationEngine.enrichMonitoringData(compositionRulesConfiguration, snapshot);
                    //update total usage

                    usageSoFar = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, usageSoFar, aggregated);

                    //persist instant cost
                    LifetimeEnrichedSnapshot cleanedCostSnapshot = costEvalEngine.cleanUnusedServices(usageSoFar);

                    //compute composition rules to create instant cost based on total usage so far
                    CompositionRulesBlock block = costEvalEngine.createCompositionRulesForInstantUsageCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, cleanedCostSnapshot.getSnapshot().getMonitoredService(), cleanedCostSnapshot, aggregated.getTimestamp());
                    ServiceMonitoringSnapshot enrichedSnapshot = costEvalEngine.applyCompositionRules(block, costEvalEngine.convertToStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, aggregated));

                    //persist instant cost
                    persistenceDelegate.persistInstantCostSnapshot(newname, new CostEnrichedSnapshot().withCostCompositionRules(block)
                            .withLastUpdatedTimestampID(enrichedSnapshot.getTimestampID()).withSnapshot(enrichedSnapshot));

                    //create rules for metrics for total cost based on usage so far
                    CompositionRulesBlock totalCostBlock = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, usageSoFar, aggregated.getTimestamp());
                    ServiceMonitoringSnapshot snapshotWithTotalCost = costEvalEngine.applyCompositionRules(totalCostBlock, usageSoFar.getSnapshot());

                    //persist mon snapshot enriched with total cost 
                    persistenceDelegate.persistTotalCostSnapshot(newname, new CostEnrichedSnapshot().withCostCompositionRules(totalCostBlock)
                            .withLastUpdatedTimestampID(snapshotWithTotalCost.getTimestampID()).withSnapshot(snapshotWithTotalCost));

                }
                //continue

                dataFromTimestamp = persistenceDelegate.extractStructuredMonitoringData(nextTimestamp, prevService.getId());

            }

            CostEnrichedSnapshot totalCostForNew = persistenceDelegate.extractTotalCostSnapshot(newname);

            Metric totalInstanceMetricCostNew = new Metric("cost_instance", "costUnits", Metric.MetricType.COST);
            Metric totalUsageMetricCostNew = new Metric("cost_usage", "costUnits", Metric.MetricType.COST);

            {

                MonitoredElement vmUsedCloudServiceMonitoredElement = new MonitoredElement()
                        .withId(UUID.fromString("98400000-8cf0-11bd-b23e-000000000002").toString())
                        .withName("m1.large")
                        .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                MonitoredElement newVMUsedCloudServiceMonitoredElement = new MonitoredElement()
                        .withId(UUID.fromString("98400000-8cf0-11bd-b23e-000000000003").toString())
                        .withName("m1.large")
                        .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                assertEquals(new MetricValue(2.0), totalCostForNew.getSnapshot().getMonitoredData(newVMUsedCloudServiceMonitoredElement).getMetricValue(totalInstanceMetricCostNew));
                assertEquals(new MetricValue(12.0), totalCostForNew.getSnapshot().getMonitoredData(vmUsedCloudServiceMonitoredElement).getMetricValue(totalInstanceMetricCostNew));
                assertEquals(new MetricValue(3.0), totalCostForNew.getSnapshot().getMonitoredData(vmUsedCloudServiceMonitoredElement).getMetricValue(totalUsageMetricCostNew));
                assertEquals(new MetricValue(100.0), totalCostForNew.getSnapshot().getMonitoredData(newVMUsedCloudServiceMonitoredElement).getMetricValue(totalUsageMetricCostNew));
            }
        }
        {
            List<UnusedCostUnitsReport> report = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsageSnapshot, "4500", unit);
            for (UnusedCostUnitsReport costUnitsReport : report) {
                //test cost efficiency analysis
                //using 2 cost units from what was billed (4)
                assertEquals(new Double(2.0), costUnitsReport.getTotalCostUsedFromWhatWasBilled());
                //cost efficiency is 50% (1/2) as we are using only 2 cost units from 4,.
                assertEquals(new Double(0.5), costUnitsReport.getCostEfficiency());
                log.debug("Wasted cost for instance {} = {} with cost efficienty {}", new Object[]{costUnitsReport.getUnitInstance().getName(), costUnitsReport.getTotalCostUsedFromWhatWasBilled(), costUnitsReport.getCostEfficiency()});
            }
        }
    }

}
