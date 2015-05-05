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
public class CostTotalEvalTest {

//    @Value("#{persistenceDelegate}")
    private PersistenceDelegate persistenceDelegate;
    private at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceDelegate dataAccessPersistenceDelegate;

    private PersistenceSQLAccess generalAccess;

    public CostTotalEvalTest() {
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
            dataSource.setUrl("jdbc:hsqldb:mem:mela-test-total-db");
            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            //read content of sql schema
            BufferedReader reader = null;
            try {

                reader = new BufferedReader(new FileReader("src/test/resources/create-initial-db-schema.sql"));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(CostTotalEvalTest.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(CostTotalEvalTest.class.getName()).log(Level.SEVERE, null, ex);
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
    public void testCostEvalTotal() throws Exception {

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
            periodicCostElement.addBillingInterval(new MetricValue(Double.POSITIVE_INFINITY), 1d);
            vmCost.addCostElement(periodicCostElement);
            vm1SmallService.addCostFunction(vmCost);

        }

        provider.addCloudOfferedService(vm1SmallService);

        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = new HashMap<UUID, Map<UUID, CloudOfferedService>>();

        Map<UUID, CloudOfferedService> cloudUnits = new HashMap<UUID, CloudOfferedService>();
        cloudProvidersMap.put(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"), cloudUnits);

        for (CloudOfferedService unit : provider.getCloudOfferedServices()) {
            cloudUnits.put(unit.getUuid(), unit);
        }

        int instanceIndex = 0;

        MonitoredElement vm = new MonitoredElement("10.0.0.1").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                .withCloudOfferedService(new UsedCloudOfferedService()
                        .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                        .withCloudProviderName("Amazon")
                        .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + instanceIndex++))
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

        persistenceDelegate.writeMonitoringSequenceId(service.getId());
        persistenceDelegate.writeConfiguration(service.getId(), new ConfigurationXMLRepresentation().withServiceConfiguration(service).withCompositionRulesConfiguration(new CompositionRulesConfiguration()).withRequirements(new Requirements()));

        Metric ELEMENT_COST_METRIC = new Metric("element_cost", "costUnits", Metric.MetricType.COST);

        ServiceMonitoringSnapshot monitoringSnapshot1 = new ServiceMonitoringSnapshot().withTimestamp("1000");
        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);

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

        //test1
        LifetimeEnrichedSnapshot totalUsageSnapshot = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, null, monitoringSnapshot1);
        {
            CompositionRulesBlock totalCostRules = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot, totalUsageSnapshot.getSnapshot().getTimestamp());
            ServiceMonitoringSnapshot totalCostEnrichedSnapshot = costEvalEngine.applyCompositionRules(totalCostRules, totalUsageSnapshot.getSnapshot());

            {
                assertEquals(new MetricValue(1.0), totalCostEnrichedSnapshot.getMonitoredData(service).getMetricValue(ELEMENT_COST_METRIC));
            }
        }

        ServiceMonitoringSnapshot monitoringSnapshot2 = new ServiceMonitoringSnapshot().withTimestamp("2000");
        {
            MonitoredElement vm2 = new MonitoredElement("10.0.0.2").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    .withCloudOfferedService(new UsedCloudOfferedService()
                            .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                            .withCloudProviderName("Amazon")
                            .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + instanceIndex++))
                            .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                            .withName("m1.small")
                    );

            unit.getContainedElements().clear();
            unit.withContainedElement(vm);
            unit.withContainedElement(vm2);

            MonitoredElementMonitoringSnapshot vm1MonSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            MonitoredElementMonitoringSnapshot vm2MonSnapshot = new MonitoredElementMonitoringSnapshot(vm2);

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(vm1MonSnapshot);
            unitMonSnapshpot.addChild(vm2MonSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot2.addMonitoredData(vm2MonSnapshot);
            monitoringSnapshot2.addMonitoredData(vm1MonSnapshot);
            monitoringSnapshot2.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot2.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot2.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot2 = monitoringSnapshot2.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot2.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot2.getTimestamp(), monitoringSnapshot2, service.getId());

        }

        //test2
        LifetimeEnrichedSnapshot totalUsageSnapshot1 = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot, monitoringSnapshot2);
        {
            CompositionRulesBlock totalCostRules = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot1, totalUsageSnapshot1.getSnapshot().getTimestamp());
            ServiceMonitoringSnapshot totalCostEnrichedSnapshot = costEvalEngine.applyCompositionRules(totalCostRules, totalUsageSnapshot1.getSnapshot());

            {
                assertEquals(new MetricValue(2.0), totalCostEnrichedSnapshot.getMonitoredData(service).getMetricValue(ELEMENT_COST_METRIC));
            }
        }

        //scaling in instance 2
        ServiceMonitoringSnapshot monitoringSnapshot3 = new ServiceMonitoringSnapshot().withTimestamp("3000");
        {

            unit.getContainedElements().clear();
            unit.withContainedElement(vm);

            MonitoredElementMonitoringSnapshot vm1MonSnapshot = new MonitoredElementMonitoringSnapshot(vm);

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(vm1MonSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot3.addMonitoredData(vm1MonSnapshot);
            monitoringSnapshot3.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot3.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot3.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot3 = monitoringSnapshot3.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot3.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot3.getTimestamp(), monitoringSnapshot3, service.getId());

        }

        //test3
        LifetimeEnrichedSnapshot totalUsageSnapshot_2 = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot1, monitoringSnapshot3);
        {
            CompositionRulesBlock totalCostRules = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot_2, totalUsageSnapshot_2.getSnapshot().getTimestamp());
            ServiceMonitoringSnapshot totalCostEnrichedSnapshot = costEvalEngine.applyCompositionRules(totalCostRules, totalUsageSnapshot_2.getSnapshot());

            {
                assertEquals(new MetricValue(2.0), totalCostEnrichedSnapshot.getMonitoredData(service).getMetricValue(ELEMENT_COST_METRIC));
            }
        }

        //scaling out with instance with same IP
        ServiceMonitoringSnapshot monitoringSnapshot4 = new ServiceMonitoringSnapshot().withTimestamp("4000");
        {

            MonitoredElement vm2 = new MonitoredElement("10.0.0.2(1)").withName("10.0.0.2").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    .withCloudOfferedService(new UsedCloudOfferedService()
                            .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                            .withCloudProviderName("Amazon")
                            .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + instanceIndex++))
                            .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                            .withName("m1.small")
                    );

            unit.getContainedElements().clear();
            unit.withContainedElement(vm);
            unit.withContainedElement(vm2);

            MonitoredElementMonitoringSnapshot vm1MonSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            MonitoredElementMonitoringSnapshot vm2MonSnapshot = new MonitoredElementMonitoringSnapshot(vm2);

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(vm1MonSnapshot);
            unitMonSnapshpot.addChild(vm2MonSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot4.addMonitoredData(vm1MonSnapshot);
            monitoringSnapshot4.addMonitoredData(vm2MonSnapshot);
            monitoringSnapshot4.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot4.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot4.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot4 = monitoringSnapshot4.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot4.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot4.getTimestamp(), monitoringSnapshot4, service.getId());

        }

        //test4
        LifetimeEnrichedSnapshot totalUsageSnapshot3 = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot_2, monitoringSnapshot4);
        {
            CompositionRulesBlock totalCostRules = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot3, totalUsageSnapshot3.getSnapshot().getTimestamp());
            ServiceMonitoringSnapshot totalCostEnrichedSnapshot = costEvalEngine.applyCompositionRules(totalCostRules, totalUsageSnapshot3.getSnapshot());

            {
                assertEquals(new MetricValue(3.0), totalCostEnrichedSnapshot.getMonitoredData(service).getMetricValue(ELEMENT_COST_METRIC));

            }
        }
        //scaling in instance 2
        ServiceMonitoringSnapshot monitoringSnapshot5 = new ServiceMonitoringSnapshot().withTimestamp("3700000");
        {

            MonitoredElement vm2 = new MonitoredElement("10.0.0.2(1)").withName("10.0.0.2").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                    .withCloudOfferedService(new UsedCloudOfferedService()
                            .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                            .withCloudProviderName("Amazon")
                            .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + instanceIndex))
                            .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                            .withName("m1.small")
                    );

            unit.getContainedElements().clear();
            unit.withContainedElement(vm);
            unit.withContainedElement(vm2);

            MonitoredElementMonitoringSnapshot vm1MonSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            MonitoredElementMonitoringSnapshot vm2MonSnapshot = new MonitoredElementMonitoringSnapshot(vm2);

            MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
            unitMonSnapshpot.addChild(vm1MonSnapshot);
            unitMonSnapshpot.addChild(vm2MonSnapshot);

            MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
            topologyMonSnapshpot.addChild(unitMonSnapshpot);

            MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
            serviceMonSnapshpot.addChild(topologyMonSnapshpot);

            monitoringSnapshot5.addMonitoredData(vm1MonSnapshot);
            monitoringSnapshot5.addMonitoredData(vm2MonSnapshot);
            monitoringSnapshot5.addMonitoredData(unitMonSnapshpot);
            monitoringSnapshot5.addMonitoredData(topologyMonSnapshpot);
            monitoringSnapshot5.addMonitoredData(serviceMonSnapshpot);

            monitoringSnapshot5 = monitoringSnapshot5.clone();

            generalAccess.writeInTimestamp(monitoringSnapshot5.getTimestamp(), service, service.getId());
            generalAccess.writeStructuredMonitoringData(monitoringSnapshot5.getTimestamp(), monitoringSnapshot5, service.getId());

        }

        //test4
        LifetimeEnrichedSnapshot totalUsageSnapshot4 = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot3, monitoringSnapshot5);
        {
            CompositionRulesBlock totalCostRules = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsageSnapshot4, totalUsageSnapshot4.getSnapshot().getTimestamp());
            ServiceMonitoringSnapshot totalCostEnrichedSnapshot = costEvalEngine.applyCompositionRules(totalCostRules, totalUsageSnapshot4.getSnapshot());

            {
                assertEquals(new MetricValue(5.0), totalCostEnrichedSnapshot.getMonitoredData(service).getMetricValue(ELEMENT_COST_METRIC));
                log.debug(CostJSONConverter.convertMonitoringSnapshot(totalCostEnrichedSnapshot));
            }
            {//cost of last added VM
                MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                        .withId(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + --instanceIndex).toString())
                        .withName("m1.small")
                        .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                assertEquals(new MetricValue(2.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(ELEMENT_COST_METRIC));
            }
            {
                //cost of scaled IN VM. must differ than of last added
                MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                        .withId(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + --instanceIndex).toString())
                        .withName("m1.small")
                        .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                assertEquals(new MetricValue(1.0), totalCostEnrichedSnapshot.getMonitoredData(usedCloudServiceMonitoredElement).getMetricValue(ELEMENT_COST_METRIC));
            }
        }

    }

}
