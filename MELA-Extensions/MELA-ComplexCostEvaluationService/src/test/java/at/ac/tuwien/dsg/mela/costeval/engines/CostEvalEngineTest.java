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
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import static at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngine.log;
import at.ac.tuwien.dsg.mela.costeval.model.CloudServicesSpecification;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.persistence.PersistenceDelegate;
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
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Properties;
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

    private org.hsqldb.Server server;

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
            server = new org.hsqldb.Server();
            server.setLogWriter(null);
            server.setRestartOnShutdown(false);
            server.setNoSystemExit(true);
            server.setPort(9001);
            server.setDatabasePath(0, "/tmp/mela");
            server.setDatabaseName(0, "mela");

            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl("jdbc:hsqldb:hsql://localhost:9001/mela;hsqldb.cache_rows=100;hsqldb.log_data=false");
            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");

            log.debug("HSQL Database path: " + server.getDatabasePath(0, true));
            log.info("Starting HSQL Server database '" + server.getDatabaseName(0, true) + "' listening on port: "
                    + server.getPort());
            server.start();
            // server.start() is synchronous; so we should expect online status from server.
            Assert.isTrue(server.getState() == ServerConstants.SERVER_STATE_ONLINE,
                    "HSQLDB could not be started. Maybe another instance is already running on " + server.getAddress()
                    + ":" + server.getPort() + " ?");
            log.info("Started HSQL Server");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            //read content of sql schema
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader("src/test/java/resources/schema.sql"));
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
        } catch (IOException ex) {
            Logger.getLogger(CostEvalEngineTest.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        }

    }

    @After
    public void tearDown() {

        server.shutdown();
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

                CostElement periodicCostElement = new CostElement("vmCost", new Metric("instance", "#/s", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                periodicCostElement.addCostInterval(new MetricValue(1), 1d);
                periodicCostElement.addCostInterval(new MetricValue(2), 2d);
                periodicCostElement.addCostInterval(new MetricValue(3), 3d);
                vmCost.addCostElement(periodicCostElement);
                unit.addCostFunction(vmCost);

                CostElement usageCostElement = new CostElement("usageCost", new Metric("usage", "#", Metric.MetricType.COST), CostElement.Type.USAGE);
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

        MonitoredElement vm = new MonitoredElement("VM").withLevel(MonitoredElement.MonitoredElementLevel.VM)
                .withCloudOfferedService(new UsedCloudOfferedService()
                        .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                        .withCloudProviderName("Amazon")
                        .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                        .withName("m1.small")
                );

        MonitoredElement service = new MonitoredElement("Service").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                .withContainedElement(new MonitoredElement("Topology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                        .withContainedElement(new MonitoredElement("Unit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                                .withContainedElement(vm)
                        )
                );

        ServiceMonitoringSnapshot monitoringSnapshot1 = new ServiceMonitoringSnapshot().withTimestamp("0");

        Metric instanceMetric = new Metric("instance", "#/s", Metric.MetricType.RESOURCE);
        Metric usageMetric = new Metric("usage", "#", Metric.MetricType.RESOURCE);

        Metric instanceMetricCost = new Metric("cost_instance_for_m1.small", "costUnits/s", Metric.MetricType.RESOURCE);
        Metric usageMetricCost = new Metric("cost_usage", "costUnits", Metric.MetricType.RESOURCE);

        Metric totalInstanceMetricCost = new Metric("total_cost_instance_for_m1.small", "costUnits", Metric.MetricType.RESOURCE);
        Metric totalUsageMetricCost = new Metric("total_cost_usage", "costUnits", Metric.MetricType.RESOURCE);

        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(0));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(0));
            monitoringSnapshot1.addMonitoredData(elementMonitoringSnapshot);

        }
        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot2 = new ServiceMonitoringSnapshot().withTimestamp("1000");
        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(1));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(1));
            monitoringSnapshot2.addMonitoredData(elementMonitoringSnapshot);

        }

        CostEnrichedSnapshot serviceUsageSnapshot1 = new CostEnrichedSnapshot()
                .withSnapshot(monitoringSnapshot1)
                .withtLastUpdatedTimestampID(monitoringSnapshot1.getTimestampID());

        //test1
        ServiceMonitoringSnapshot updatedTotalUsageSoFar1 = instance.updateTotalUsageSoFar(cloudProvidersMap, serviceUsageSnapshot1, monitoringSnapshot2);

        CostEnrichedSnapshot totalUsageSnapshot1 = new CostEnrichedSnapshot()
                .withSnapshot(updatedTotalUsageSoFar1)
                .withtLastUpdatedTimestampID(updatedTotalUsageSoFar1.getTimestampID());

        CompositionRulesBlock block1 = instance.createCompositionRulesForInstantUsageCost(cloudProvidersMap, service, totalUsageSnapshot1, monitoringSnapshot2.getTimestamp());

        ServiceMonitoringSnapshot cost1 = instance.applyCompositionRules(block1, monitoringSnapshot1);

        assertEquals(new MetricValue(1.0), cost1.getMonitoredData(vm).getMetricValue(instanceMetricCost));
        assertEquals(new MetricValue(0.5), cost1.getMonitoredData(vm).getMetricValue(usageMetricCost));

        persistenceDelegate.persistTotalUsageSnapshot(service.getId(), serviceUsageSnapshot1);
        persistenceDelegate.persistInstantCostSnapshot(service.getId(), new CostEnrichedSnapshot().withCostCompositionRules(block1).withSnapshot(updatedTotalUsageSoFar1).withLastUpdatedTimestampID(cost1.getTimestampID()));

        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot3 = new ServiceMonitoringSnapshot().withTimestamp("2000");
        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(2));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(2));

            monitoringSnapshot3.addMonitoredData(elementMonitoringSnapshot);

        }

        totalUsageSnapshot1 = persistenceDelegate.extractTotalUsageSnapshot(service.getId());

        ServiceMonitoringSnapshot updatedTotalUsageSoFar2 = instance.updateTotalUsageSoFar(cloudProvidersMap, totalUsageSnapshot1, monitoringSnapshot3);

        CostEnrichedSnapshot serviceUsageSnapshot2 = new CostEnrichedSnapshot()
                .withSnapshot(updatedTotalUsageSoFar2)
                .withtLastUpdatedTimestampID(updatedTotalUsageSoFar2.getTimestampID());

        persistenceDelegate.persistTotalCostSnapshot(service.getId(), serviceUsageSnapshot2);

        CompositionRulesBlock block2 = instance.createCompositionRulesForInstantUsageCost(cloudProvidersMap, service, serviceUsageSnapshot2, monitoringSnapshot3.getTimestamp());

        ServiceMonitoringSnapshot cost2 = instance.applyCompositionRules(block2, monitoringSnapshot1);

        assertEquals(new MetricValue(2.0), cost2.getMonitoredData(vm).getMetricValue(instanceMetricCost));
        assertEquals(new MetricValue(1.5), cost2.getMonitoredData(vm).getMetricValue(usageMetricCost));

        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot4 = new ServiceMonitoringSnapshot().withTimestamp("3000");
        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(10));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(10));

            monitoringSnapshot4.addMonitoredData(elementMonitoringSnapshot);

            monitoringSnapshot4.addMonitoredData(new MonitoredElementMonitoringSnapshot(service));

        }

        CostEnrichedSnapshot totalCostSnapshot = new CostEnrichedSnapshot()
                .withSnapshot(monitoringSnapshot4)
                .withtLastUpdatedTimestampID(monitoringSnapshot4.getTimestampID());

        CompositionRulesBlock totalCostRules = instance.createCompositionRulesForTotalCost(cloudProvidersMap, totalCostSnapshot, monitoringSnapshot4.getTimestamp());
        ServiceMonitoringSnapshot totalCostEnrichedSnapshot = instance.applyCompositionRules(totalCostRules, monitoringSnapshot4);

        assertEquals(new MetricValue(6.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalInstanceMetricCost));
        assertEquals(new MetricValue(5.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalUsageMetricCost));

        persistenceDelegate.persistTotalCostSnapshot(service.getId(), totalCostSnapshot);

        totalCostSnapshot = persistenceDelegate.extractTotalCostSnapshot(service.getId());
        totalCostEnrichedSnapshot = totalCostSnapshot.getSnapshot();

        assertEquals(new MetricValue(6.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalInstanceMetricCost));
        assertEquals(new MetricValue(5.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalUsageMetricCost));

    }

}
