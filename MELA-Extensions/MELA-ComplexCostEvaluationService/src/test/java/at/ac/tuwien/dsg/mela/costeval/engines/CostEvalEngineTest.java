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
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.costeval.control.CostEvalManager;
import at.ac.tuwien.dsg.mela.costeval.model.CloudServicesSpecification;
import at.ac.tuwien.dsg.mela.costeval.model.ServiceUsageSnapshot;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
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

/**
 *
 * @author daniel-tuwien
 */
public class CostEvalEngineTest {

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
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of applyCompositionRules method, of class CostEvalEngine.
     */
    @Test
    public void testApplyCompositionRules() {
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

        ServiceUsageSnapshot serviceUsageSnapshot1 = new ServiceUsageSnapshot()
                .withTotalUsageSoFar(monitoringSnapshot1)
                .withtLastUpdatedTimestampID(monitoringSnapshot1.getTimestampID());

        //test1
        ServiceMonitoringSnapshot updatedTotalUsageSoFar1 = instance.updateTotalUsageSoFar(cloudProvidersMap, serviceUsageSnapshot1, monitoringSnapshot2);

        ServiceUsageSnapshot totalUsageSnapshot1 = new ServiceUsageSnapshot()
                .withTotalUsageSoFar(updatedTotalUsageSoFar1)
                .withtLastUpdatedTimestampID(updatedTotalUsageSoFar1.getTimestampID());

        CompositionRulesBlock block1 = instance.createCompositionRulesForInstantUsageCost(cloudProvidersMap, service, totalUsageSnapshot1, monitoringSnapshot2.getTimestamp());

        ServiceMonitoringSnapshot cost1 = instance.applyCompositionRules(block1, monitoringSnapshot1);

        assertEquals(new MetricValue(1.0), cost1.getMonitoredData(vm).getMetricValue(instanceMetricCost));
        assertEquals(new MetricValue(0.5), cost1.getMonitoredData(vm).getMetricValue(usageMetricCost));

        //add another monitoring snapshot
        ServiceMonitoringSnapshot monitoringSnapshot3 = new ServiceMonitoringSnapshot().withTimestamp("2000");
        {
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vm);
            elementMonitoringSnapshot.getMonitoredData().put(instanceMetric, new MetricValue(2));
            elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(2));

            monitoringSnapshot3.addMonitoredData(elementMonitoringSnapshot);

        }

        ServiceMonitoringSnapshot updatedTotalUsageSoFar2 = instance.updateTotalUsageSoFar(cloudProvidersMap, totalUsageSnapshot1, monitoringSnapshot3);

        ServiceUsageSnapshot serviceUsageSnapshot2 = new ServiceUsageSnapshot()
                .withTotalUsageSoFar(updatedTotalUsageSoFar2)
                .withtLastUpdatedTimestampID(updatedTotalUsageSoFar2.getTimestampID());

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

        ServiceUsageSnapshot totalCostSnapshot = new ServiceUsageSnapshot()
                .withTotalUsageSoFar(monitoringSnapshot4)
                .withtLastUpdatedTimestampID(monitoringSnapshot4.getTimestampID());

        CompositionRulesBlock totalCostRules = instance.createCompositionRulesForTotalCost(cloudProvidersMap, totalCostSnapshot, monitoringSnapshot4.getTimestamp());
        ServiceMonitoringSnapshot totalCostEnrichedSnapshot = instance.applyCompositionRules(totalCostRules, monitoringSnapshot4);

        assertEquals(new MetricValue(6.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalInstanceMetricCost));
        assertEquals(new MetricValue(5.0), totalCostEnrichedSnapshot.getMonitoredData(vm).getMetricValue(totalUsageMetricCost));

    }

}
