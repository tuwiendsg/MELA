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
public class CostEfficiencyTest {

    public CostEfficiencyTest() {
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

            CostElement periodicCostElement = new CostElement("vmCost", new Metric("instance", "#/m/10", Metric.MetricType.COST), CostElement.Type.PERIODIC);
            periodicCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 2d);
            vmCost.addCostElement(periodicCostElement);
            vm1SmallService.addCostFunction(vmCost);

            CostElement usageCostElement = new CostElement("usageCost", new Metric("usage", "#/m/10", Metric.MetricType.COST), CostElement.Type.USAGE);
            usageCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 2d);
            vmCost.addCostElement(usageCostElement);
        }

        provider.addCloudOfferedService(vm1SmallService);

        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = new HashMap<UUID, Map<UUID, CloudOfferedService>>();

        Map<UUID, CloudOfferedService> cloudUnits = new HashMap<UUID, CloudOfferedService>();
        cloudProvidersMap.put(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"), cloudUnits);

        for (CloudOfferedService unit : provider.getCloudOfferedServices()) {
            cloudUnits.put(unit.getUuid(), unit);
        }

        MonitoredElement unit = new MonitoredElement("Unit").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);

        int unitInstanceIndex = 0;

        {
            for (int i = 0; i < 3; i++) {
                MonitoredElement vm = new MonitoredElement("VM" + unitInstanceIndex).withLevel(MonitoredElement.MonitoredElementLevel.VM)
                        .withCloudOfferedService(new UsedCloudOfferedService()
                                .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                                .withCloudProviderName("Amazon")
                                .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + unitInstanceIndex))
                                .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                                .withName("m1.small")
                        );

                unit.withContainedElement(vm);
                unitInstanceIndex++;
            }

        }

        MonitoredElement topology = new MonitoredElement("Topology").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                .withContainedElement(unit);

        MonitoredElement service = new MonitoredElement("Service").withLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                .withContainedElement(topology);

        Metric usageMetric = new Metric("usage", "#/s", Metric.MetricType.RESOURCE);

        int snapshotsCount = 10;

        LifetimeEnrichedSnapshot totalUsage = null;

        List<ServiceMonitoringSnapshot> snapshots = new ArrayList<>();

        for (int i = 0; i < 4; i++) {

            ServiceMonitoringSnapshot monitoringSnapshot = new ServiceMonitoringSnapshot().withTimestamp("" + (i * 1800000/60));
            {

                MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
                for (MonitoredElement element : unit.getContainedElements()) {
                    MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(element);
                    elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue((i > 0) ? 0.5 : 0.0).withFreshness(50d));
                    unitMonSnapshpot.addChild(elementMonitoringSnapshot);
                    monitoringSnapshot.addMonitoredData(elementMonitoringSnapshot);
                }

                MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
                topologyMonSnapshpot.addChild(unitMonSnapshpot);

                MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
                serviceMonSnapshpot.addChild(topologyMonSnapshpot);

                monitoringSnapshot.addMonitoredData(unitMonSnapshpot);
                monitoringSnapshot.addMonitoredData(topologyMonSnapshpot);
                monitoringSnapshot.addMonitoredData(serviceMonSnapshpot);

                monitoringSnapshot = monitoringSnapshot.clone();

                snapshots.add(monitoringSnapshot);

                totalUsage = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsage, monitoringSnapshot);
                {
                    List<UnusedCostUnitsReport> report = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsage, "" + (i * 1800000/60), unit);
                    for (UnusedCostUnitsReport costUnitsReport : report) {
                        log.info("cost efficiency for {} is {}, used is {} from {} ", new Object[]{costUnitsReport.getUnitInstance().getId(), costUnitsReport.getCostEfficiency(),
                            costUnitsReport.getTotalCostUsedFromWhatWasBilled(), costUnitsReport.getTotalCostBilled()});
                    }
                }

                {
                    List<UnusedCostUnitsReport> report = costEvalEngine.computeLifetimeInBillingPeriods(cloudProvidersMap, totalUsage, "" + (i * 1800000/60), unit);
                    for (UnusedCostUnitsReport costUnitsReport : report) {
                        log.info("Lifetime efficiency for {} is {}, used is {} from {}", new Object[]{costUnitsReport.getUnitInstance().getId(), costUnitsReport.getCostEfficiency(),
                            costUnitsReport.getTotalCostUsedFromWhatWasBilled(), costUnitsReport.getTotalCostBilled()});
                    }
                }

            }

        }

        for (int i = 4; i < 10; i++) {

            ServiceMonitoringSnapshot monitoringSnapshot = new ServiceMonitoringSnapshot().withTimestamp("" + (i * 1800000/60));
            {

                List<UnusedCostUnitsReport> report = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsage, "" + (i * 1800000/60), unit);
                for (UnusedCostUnitsReport costUnitsReport : report) {
                    log.info("efficiency for {} is {}, used is {} ", new Object[]{costUnitsReport.getUnitInstance().getId(), costUnitsReport.getCostEfficiency(),
                        costUnitsReport.getTotalCostUsedFromWhatWasBilled()});
                }

                //remove best
                UnusedCostUnitsReport best = report.get(0);
                unit.getContainedElements().remove(best.getUnitInstance());
                //add a new Unit
                unitInstanceIndex++;
                {
                    MonitoredElement vm = new MonitoredElement("VM" + unitInstanceIndex).withLevel(MonitoredElement.MonitoredElementLevel.VM)
                            .withCloudOfferedService(new UsedCloudOfferedService()
                                    .withCloudProviderID(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"))
                                    .withCloudProviderName("Amazon")
                                    .withInstanceUUID(UUID.fromString("98400000-8cf0-11bd-b23e-00000000000" + unitInstanceIndex))
                                    .withId(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"))
                                    .withName("m1.small")
                            );

                    unit.withContainedElement(vm);
                }

                MonitoredElementMonitoringSnapshot unitMonSnapshpot = new MonitoredElementMonitoringSnapshot(unit);
                for (MonitoredElement element : unit.getContainedElements()) {
                    MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(element);
                    elementMonitoringSnapshot.getMonitoredData().put(usageMetric, new MetricValue(0.5).withFreshness(50d));
                    unitMonSnapshpot.addChild(elementMonitoringSnapshot);
                    monitoringSnapshot.addMonitoredData(elementMonitoringSnapshot);
                }

                MonitoredElementMonitoringSnapshot topologyMonSnapshpot = new MonitoredElementMonitoringSnapshot(topology);
                topologyMonSnapshpot.addChild(unitMonSnapshpot);

                MonitoredElementMonitoringSnapshot serviceMonSnapshpot = new MonitoredElementMonitoringSnapshot(service);
                serviceMonSnapshpot.addChild(topologyMonSnapshpot);

                monitoringSnapshot.addMonitoredData(unitMonSnapshpot);
                monitoringSnapshot.addMonitoredData(topologyMonSnapshpot);
                monitoringSnapshot.addMonitoredData(serviceMonSnapshpot);

                monitoringSnapshot = monitoringSnapshot.clone();

                snapshots.add(monitoringSnapshot);

                totalUsage = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, totalUsage, monitoringSnapshot);

            }

        }

    }

}
