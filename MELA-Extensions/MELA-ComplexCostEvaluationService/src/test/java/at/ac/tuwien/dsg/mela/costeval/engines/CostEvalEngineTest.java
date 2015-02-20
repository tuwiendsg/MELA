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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
    public void testApplyCompositionRules() throws Exception {

        CostEvalEngine instance = new CostEvalEngine();
        DataAggregationEngine aggregationEngine = new DataAggregationEngine();
        instance.setInstantMonitoringDataEnrichmentEngine(aggregationEngine);

        CloudProvider provider = new CloudProvider("Amazon");
        provider.setUuid(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"));

        CloudServicesSpecification cloudServicesSpecification = new CloudServicesSpecification();
        cloudServicesSpecification.addCloudProvider(provider);

        {
            ServiceUnit unit = new ServiceUnit("IaaS", "VM", "m1.small");
            unit.withUuid(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"));

            //VM COST
            {
                CostFunction vmCost = new CostFunction(unit.getName() + "_cost");

                CostElement periodicCostElement = new CostElement("vmCost", new Metric("instance", "#", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                periodicCostElement.addCostInterval(new MetricValue(1), 1d);
                periodicCostElement.addCostInterval(new MetricValue(2), 2d);
                periodicCostElement.addCostInterval(new MetricValue(3), 3d);
                vmCost.addCostElement(periodicCostElement);
                unit.addCostFunction(vmCost);

                CostElement usageCostElement = new CostElement("usageCost", new Metric("usage", "#/s", Metric.MetricType.COST), CostElement.Type.USAGE);
                usageCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.5d);
                vmCost.addCostElement(usageCostElement);
            }

            provider.addServiceUnit(unit);

        }

        Map<UUID, Map<UUID, ServiceUnit>> cloudProvidersMap = new HashMap<UUID, Map<UUID, ServiceUnit>>();

        Map<UUID, ServiceUnit> cloudUnits = new HashMap<UUID, ServiceUnit>();
        cloudProvidersMap.put(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"), cloudUnits);

        for (ServiceUnit unit : provider.getServiceUnits()) {
            cloudUnits.put(unit.getUuid(), unit);
        }

        MonitoredElement vm = new MonitoredElement("UNIT_INSTANCE").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                .withCloudOfferedService(new UsedCloudOfferedService()
                        .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                        .withCloudProviderName("Amazon")
                        .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                        .withName("m1.small")
                );

        MonitoredElement unit = new MonitoredElement("Unit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                .withContainedElement(vm);

        MonitoredElement topology = new MonitoredElement("Topology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                .withContainedElement(unit);

        MonitoredElement service = new MonitoredElement("Service").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                .withContainedElement(topology);

        //make sure all is clean
        persistenceDelegate.removeService(service.getId());

        ServiceMonitoringSnapshot monitoringSnapshot1 = new ServiceMonitoringSnapshot().withTimestamp("1000");

//        Metric instanceMetric = new Metric("instance", "#", Metric.MetricType.RESOURCE);
        Metric usageMetric = new Metric("usage", "#/s", Metric.MetricType.RESOURCE);

        Metric ELEMENT_COST_METRIC = new Metric("element_cost", "costUnits", Metric.MetricType.COST);
        Metric CHILDREN_COST_METRIC = new Metric("children_cost", "costUnits", Metric.MetricType.COST);

        Metric instanceMetricCost = new Metric("cost_instance_for_m1.small", "costUnits/s", Metric.MetricType.COST);
        Metric usageMetricCost = new Metric("cost_usage", "costUnits", Metric.MetricType.COST);

        Metric totalInstanceMetricCost = new Metric("cost_instance_for_m1.small", "costUnits", Metric.MetricType.COST);
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

        }

        LifetimeEnrichedSnapshot serviceUsageSnapshot1 = new LifetimeEnrichedSnapshot()
                .withSnapshot(monitoringSnapshot1)
                .withtLastUpdatedTimestampID(monitoringSnapshot1.getTimestampID());

        //test1
        LifetimeEnrichedSnapshot totalUsageSnapshot = instance.updateTotalUsageSoFarWithCompleteStructure(cloudProvidersMap, serviceUsageSnapshot1, monitoringSnapshot2);

//        assertEquals(new MetricValue(1), totalUsageSnapshot.getSnapshot().getMonitoredData(vm).getMetricValue(instanceMetric));
        assertEquals(new MetricValue(1.0), totalUsageSnapshot.getSnapshot().getMonitoredData(vm).getMetricValue(usageMetric));

        persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId(), totalUsageSnapshot);

        LifetimeEnrichedSnapshot instantCostCleaned1 = instance.cleanUnusedServices(totalUsageSnapshot);

        CompositionRulesBlock block1 = instance.createCompositionRulesForInstantUsageCost(cloudProvidersMap, service, instantCostCleaned1, monitoringSnapshot2.getTimestamp());

        ServiceMonitoringSnapshot instantCost1 = instance.applyCompositionRules(block1, monitoringSnapshot2);

        assertEquals(new MetricValue(1.0), instantCost1.getMonitoredData(vm).getMetricValue(instanceMetricCost));
        assertEquals(new MetricValue(0.5), instantCost1.getMonitoredData(vm).getMetricValue(usageMetricCost));
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

        }

        LifetimeEnrichedSnapshot totalServiceUsage2 = instance.updateTotalUsageSoFarWithCompleteStructure(cloudProvidersMap, totalUsageSnapshot, monitoringSnapshot3);

//        assertEquals(new MetricValue(2), totalServiceUsage2.getSnapshot().getMonitoredData(vm).getMetricValue(instanceMetric));
        assertEquals(new MetricValue(3.0), totalServiceUsage2.getSnapshot().getMonitoredData(vm).getMetricValue(usageMetric));

        persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(service.getId(), totalServiceUsage2);

        LifetimeEnrichedSnapshot instantCostCleaned2 = instance.cleanUnusedServices(totalServiceUsage2);

        generalAccess.writeInTimestamp("" + totalServiceUsage2.getLastUpdatedTimestampID(), service, service.getId());
//        persistenceDelegate.persistTotalCostSnapshot(service.getId(), serviceUsageSnapshot2);

        CompositionRulesBlock block2 = instance.createCompositionRulesForInstantUsageCost(cloudProvidersMap, service, instantCostCleaned2, monitoringSnapshot3.getTimestamp());

        ServiceMonitoringSnapshot instantCost2 = instance.applyCompositionRules(block2, monitoringSnapshot3);

        persistenceDelegate.persistInstantCostSnapshot(service.getId(), new CostEnrichedSnapshot().withCostCompositionRules(block2).withSnapshot(instantCost2).withLastUpdatedTimestampID(instantCost2.getTimestampID()));

        assertEquals(new MetricValue(2.0), instantCost2.getMonitoredData(vm).getMetricValue(instanceMetricCost));
        assertEquals(new MetricValue(1.0), instantCost2.getMonitoredData(vm).getMetricValue(usageMetricCost));

        MonitoredElement newVM = new MonitoredElement("UNIT_INSTANCE_2").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                .withCloudOfferedService(new UsedCloudOfferedService()
                        .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                        .withCloudProviderName("Amazon")
                        .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
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

        }

        LifetimeEnrichedSnapshot totalServiceUsage3 = instance.updateTotalUsageSoFarWithCompleteStructure(cloudProvidersMap, totalServiceUsage2, monitoringSnapshot4);

//        assertEquals(new MetricValue(3), totalServiceUsage3.getSnapshot().getMonitoredData(vm).getMetricValue(instanceMetric));
//        assertEquals(new MetricValue(1), totalServiceUsage3.getSnapshot().getMonitoredData(newVM).getMetricValue(instanceMetric));
        assertEquals(new MetricValue(3.0), totalServiceUsage3.getSnapshot().getMonitoredData(vm).getMetricValue(usageMetric));
        assertEquals(new MetricValue(100.0), totalServiceUsage3.getSnapshot().getMonitoredData(newVM).getMetricValue(usageMetric));

        CompositionRulesBlock totalCostRules = instance.createCompositionRulesForTotalCost(cloudProvidersMap, totalServiceUsage3, totalServiceUsage3.getSnapshot().getTimestamp());
        ServiceMonitoringSnapshot totalCostEnrichedSnapshot = instance.applyCompositionRules(totalCostRules, totalServiceUsage3.getSnapshot());

        assertEquals(new MetricValue(0.0), totalCostEnrichedSnapshot.getMonitoredData(newVM).getMetricValue(totalInstanceMetricCost));
        assertEquals(new MetricValue(6.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalInstanceMetricCost));
        assertEquals(new MetricValue(1.5), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalUsageMetricCost));
        assertEquals(new MetricValue(50.0), totalCostEnrichedSnapshot.getMonitoredData(newVM).getMetricValue(totalUsageMetricCost));

        generalAccess.writeInTimestamp("" + totalServiceUsage3.getLastUpdatedTimestampID(), service, service.getId());
        persistenceDelegate.setPersistenceSQLAccess(generalAccess);
        persistenceDelegate.persistTotalCostSnapshot(service.getId(),
                new CostEnrichedSnapshot()
                .withCostCompositionRules(totalCostRules)
                .withSnapshot(totalCostEnrichedSnapshot).
                withLastUpdatedTimestampID(totalCostEnrichedSnapshot.getTimestampID()));

        totalCostEnrichedSnapshot = persistenceDelegate.extractTotalCostSnapshot(service.getId()).getSnapshot();

        assertEquals(new MetricValue(0.0), totalCostEnrichedSnapshot.getMonitoredData(newVM).getMetricValue(totalInstanceMetricCost));
        assertEquals(new MetricValue(6.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalInstanceMetricCost));
        assertEquals(new MetricValue(1.5), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalUsageMetricCost));
        assertEquals(new MetricValue(50.0), totalCostEnrichedSnapshot.getMonitoredData(newVM).getMetricValue(totalUsageMetricCost));

        CostEvalManager manager = new CostEvalManager();
        manager.setPersistenceDelegate(persistenceDelegate);

        ElasticitySpace space = manager.updateAndGetInstantCostElasticitySpace(service.getId());
        List<MetricValue> metricValues = space.getMonitoredDataForService(vm).get(instanceMetricCost);

        assertEquals(new MetricValue(1.0), metricValues.get(0));
        assertEquals(new MetricValue(2.0), metricValues.get(1));

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
        log.info(converter.convertMonitoringSnapshotAndCompositionRules(totalCostEnrichedSnapshot,totalCostRules));
        log.info("");
        log.info("ElasticitySpace : totalCostEnrichedSnapshot");
        log.info("");
        log.info(converter.convertElasticitySpace(space, service));
    }

}
