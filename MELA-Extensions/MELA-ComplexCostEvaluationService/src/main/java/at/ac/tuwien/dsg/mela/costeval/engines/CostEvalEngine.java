/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.mela.costeval.engines;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperationType;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Relationship;
import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import at.ac.tuwien.dsg.mela.costeval.model.ServiceUsageSnapshot;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.neo4j.helpers.collection.Iterables.cache;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class CostEvalEngine {

    @Autowired
    private DataAggregationEngine instantMonitoringDataEnrichmentEngine;

    static final Logger log = LoggerFactory.getLogger(CostEvalEngine.class);

    public ServiceMonitoringSnapshot applyCompositionRules(CompositionRulesBlock block, ServiceMonitoringSnapshot monitoringSnapshot) {
        return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(
                new CompositionRulesConfiguration().withMetricCompositionRules(block), monitoringSnapshot);
    }

    public void setInstantMonitoringDataEnrichmentEngine(DataAggregationEngine instantMonitoringDataEnrichmentEngine) {
        this.instantMonitoringDataEnrichmentEngine = instantMonitoringDataEnrichmentEngine;
    }

    /**
     * Enriches supplied monitoring snapshots with cost for each VM, so it works
     * DIRECTLY on the monData
     *
     * @param cloudOfferedServices contains pricing scheme
     * @param monData contains mon data and mon structure monData the huge chunk
     * of code below just enriches monitoring snapshots with cost at VM level.
     * Does NOT sum up all cost, but creates the base for anyone to take the
     * list of snapshots, and SUMM UP cost
     * @return a ServiceMonitoringSnapshot containing complete cost aggregated
     * over time
     */
    public ServiceMonitoringSnapshot getTotalCost(List<CloudProvider> cloudProviders, List<ServiceMonitoringSnapshot> monData) {

        //TODO: continue from here and UPDATE the compute total cost method
        Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices = cloudProvidersToMap(cloudProviders);

        //udpates monData in place
        final CompositionRulesConfiguration compositionRulesConfiguration = createAndApplyCompositionRulesForComputingCost(cloudOfferedServices, monData);

        //SUM UP all COST metrics captured in Historical Composition Rules
        //ServiceMonitoringSnapshot complete = instantMonitoringDataEnrichmentEngine.aggregateMonitoringDataOverTime(compositionRulesConfiguration, monData);
        //split rules per level
        final Map<MonitoredElement.MonitoredElementLevel, List<CompositionRule>> rules = new HashMap<MonitoredElement.MonitoredElementLevel, List<CompositionRule>>();

        rules.put(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT, new ArrayList<CompositionRule>());
        rules.put(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY, new ArrayList<CompositionRule>());
        rules.put(MonitoredElement.MonitoredElementLevel.SERVICE, new ArrayList<CompositionRule>());

        for (CompositionRule rule : compositionRulesConfiguration.getHistoricMetricCompositionRules().getCompositionRules()) {
            rules.get(rule.getTargetMonitoredElementLevel()).add(rule);
        }

        final Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, ConcurrentHashMap<Metric, List<MetricValue>>>> valuesToAggregate;
        valuesToAggregate = new ConcurrentHashMap<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, ConcurrentHashMap<Metric, List<MetricValue>>>>();
        valuesToAggregate.put(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT, new ConcurrentHashMap<MonitoredElement, ConcurrentHashMap<Metric, List<MetricValue>>>());
        valuesToAggregate.put(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY, new ConcurrentHashMap<MonitoredElement, ConcurrentHashMap<Metric, List<MetricValue>>>());
        valuesToAggregate.put(MonitoredElement.MonitoredElementLevel.SERVICE, new ConcurrentHashMap<MonitoredElement, ConcurrentHashMap<Metric, List<MetricValue>>>());

        //as enrichMonSnapshotsWithCost enriches only VM level
        //go trough mon snapshots, extract mon data according to the rules
        //multithreaded collects data from history
        List<Thread> threads = new ArrayList<Thread>();

        final Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> metricFilters = new HashMap<MonitoredElement.MonitoredElementLevel, List<MetricFilter>>();

        //clear out data not belonging to cost rules
        for (CompositionRule compositionRule : compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules()) {
            // go trough each CompositionOperation and extract the source
            // metrics

            List<CompositionOperation> queue = new ArrayList<CompositionOperation>();
            queue.add(compositionRule.getOperation());

            while (!queue.isEmpty()) {
                CompositionOperation operation = queue.remove(0);
                queue.addAll(operation.getSubOperations());
                Metric targetMetric = operation.getTargetMetric();
                // metric can be null if a composition rule artificially creates
                // a metric using SET_VALUE
                if (targetMetric != null) {
                    MetricFilter metricFilter = new MetricFilter();
                    metricFilter.setId(targetMetric.getName() + "_Filter");
                    metricFilter.setLevel(operation.getMetricSourceMonitoredElementLevel());
                    Collection<Metric> metrics = new ArrayList<Metric>();
                    metrics.add(new Metric(targetMetric.getName()));
                    metricFilter.setMetrics(metrics);

                    if (metricFilters.containsKey(metricFilter.getLevel())) {
                        List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
                        if (!list.contains(metricFilter)) {
                            list.add(metricFilter);
                        }
                    } else {
                        List<MetricFilter> list = new ArrayList<MetricFilter>();
                        list.add(metricFilter);
                        metricFilters.put(metricFilter.getLevel(), list);
                    }

                }
            }
        }

        for (final ServiceMonitoringSnapshot ms : monData) {

//            Thread t = new Thread() {
//
//                @Override
//                public void run() {
            log.info("Snapshot " + ms.getTimestampID());

            //remove from snapshot all metrics which are not used in aggregation
//                    ms.applyMetricFilters(metricFilters);
            //compute cost for each service unit, topology and service for each snapshot
            ServiceMonitoringSnapshot monitoringSnapshot = instantMonitoringDataEnrichmentEngine.enrichMonitoringData(
                    compositionRulesConfiguration, ms);

            for (MonitoredElement.MonitoredElementLevel level : monitoringSnapshot.getMonitoredData().keySet()) {

                Map<MonitoredElement, ConcurrentHashMap<Metric, List<MetricValue>>> dataToAggregateOnLevel = valuesToAggregate.get(level);

                //we do not process VM.
                if (level.equals(MonitoredElement.MonitoredElementLevel.VM)) {
                    continue;
                }

                Map<MonitoredElement, MonitoredElementMonitoringSnapshot> levelData = monitoringSnapshot.getMonitoredData(level);
                for (MonitoredElement element : levelData.keySet()) {
                    //avoid destroyng source elements
                    element = element.clone();

                    MonitoredElementMonitoringSnapshot elementSnapshot = levelData.get(element);

                    ConcurrentHashMap<Metric, List<MetricValue>> elementMetrics = null;
                    if (dataToAggregateOnLevel.containsKey(element)) {
                        elementMetrics = dataToAggregateOnLevel.get(element);
                    } else {
                        //remove all VM children
                        Iterator<MonitoredElement> it = element.getContainedElements().iterator();
                        while (it.hasNext()) {
                            if (it.next().getLevel().equals(MonitoredElement.MonitoredElementLevel.VM)) {
                                it.remove();
                            }

                        }
                        elementMetrics = new ConcurrentHashMap<Metric, List<MetricValue>>();

                        dataToAggregateOnLevel.put(element, elementMetrics);
                    }
                    //take from each rule the target metric and SUM it 
                    for (CompositionRule rule : rules.get(level)) {
                        if (elementMetrics.containsKey(rule.getResultingMetric())) {
                            List<MetricValue> values = elementMetrics.get(rule.getResultingMetric());
                            if (elementSnapshot.containsMetric(rule.getResultingMetric())) {
                                values.add(elementSnapshot.getMetricValue(rule.getResultingMetric()));
                            }
                        } else {
                            List<MetricValue> values = Collections.synchronizedList(new ArrayList<MetricValue>());
                            elementMetrics.put(rule.getResultingMetric(), values);
                        }

                    }

                }

            }

//                }
//            };
//
//            threads.add(t);
//            t.setDaemon(true);
//            t.start();
            //limit the nr of threads to 1000, to avoid overflooding the CPU and actually reducing execution time
            if (threads.size() > 1000) {
                Iterator<Thread> it = threads.iterator();
                while (it.hasNext()) {
                    try {
                        it.next().join();
                    } catch (InterruptedException ex) {
                        log.warn(ex.getMessage(), ex);
                    }
                    it.remove();
                }
            }

        }

//         //take from each rule the target metric and SUM it 
//                    for (CompositionRule rule : rules.get(level)) {
//                        MetricValue summedSoFar = null;
//                        if (elementMetrics.containsKey(rule.getResultingMetric())) {
//                            summedSoFar = elementMetrics.get(rule.getResultingMetric());
//                            elementMetrics.getMonitoredData().put(rule.getResultingMetric(), summedSoFar);
//                        } else {
//                            summedSoFar = new MetricValue(0.0d);
//                        }
//
//                        if (elementSnapshot.getMonitoredData().containsKey(rule.getResultingMetric())) {
//                            summedSoFar.sum(elementSnapshot.getMonitoredData().get(rule.getResultingMetric()));
//                        }
//                    }
//
//                }
//    
//    
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                log.warn(ex.getMessage(), ex);
            }
        }

        final Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> summedValues;
        summedValues = new EnumMap<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>>(MonitoredElement.MonitoredElementLevel.class);
        summedValues.put(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT, new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>());
        summedValues.put(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY, new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>());
        summedValues.put(MonitoredElement.MonitoredElementLevel.SERVICE, new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>());

        for (MonitoredElement.MonitoredElementLevel level : valuesToAggregate.keySet()) {

            for (MonitoredElement element : valuesToAggregate.get(level).keySet()) {
                ConcurrentHashMap<Metric, List<MetricValue>> metricsToSumUp = valuesToAggregate.get(level).get(element);
                MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(element);

                summedValues.get(level).put(element, elementMonitoringSnapshot);

                for (Metric m : metricsToSumUp.keySet()) {

                    List<MetricValue> values = metricsToSumUp.get(m);

                    Iterator<MetricValue> iterator = values.iterator();
                    if (iterator.hasNext()) {
                        MetricValue sum = iterator.next();
                        while (iterator.hasNext()) {
                            sum.sum(iterator.next());
                        }
                        elementMonitoringSnapshot.putMetric(m, sum);
                    }

                }

            }
        }

        //all PERIODIC metrics must be averaged out over billing period.
        //For example, Cost of 0.1 monitored every second for 1 hour and summed should be divided to 3600
        //check amount of time in millis between two measurement units
        Long previousTimestamp = Long.parseLong(monData.get(0).getTimestamp());
        Long last = Long.parseLong(monData.get(monData.size() - 1).getTimestamp());

        //convert to seconds
        Long timeIntervalInMillis = (last - previousTimestamp) / 1000;

        for (MonitoredElement.MonitoredElementLevel level : summedValues.keySet()) {

            for (MonitoredElement element : summedValues.get(level).keySet()) {

                MonitoredElementMonitoringSnapshot monitoringSnapshot = summedValues.get(level).get(element);

                for (CompositionRule rule : rules.get(level)) {
                    //if rule is periodic, i.e.. has /s /m /h
                    if (rule.getResultingMetric().getMeasurementUnit().contains("/")) {

                        String timePeriod = rule.getResultingMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                        if (monitoringSnapshot.containsMetric(rule.getResultingMetric())) {

                            long periodsBetweenPrevAndCurrentTimestamp = 0;

                            //must standardise these somehow
                            if (timePeriod.equals("s")) {
                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis;
                            } else if (timePeriod.equals("m")) {
                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 60;
                            } else if (timePeriod.equals("h")) {
                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 3600;
                            } else if (timePeriod.equals("d")) {
                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 86400;
                            }

                            MetricValue value = monitoringSnapshot.getMetricValue(rule.getResultingMetric());
                            if (periodsBetweenPrevAndCurrentTimestamp > 0) {
                                value.divide(periodsBetweenPrevAndCurrentTimestamp);
                            }
                        }
                    }

                }
            }

        }

        ServiceMonitoringSnapshot monitoringSnapshot = new ServiceMonitoringSnapshot();

        monitoringSnapshot.setMonitoredData(summedValues);

        //try to compute total cost etc
        monitoringSnapshot = instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, monitoringSnapshot);

        return monitoringSnapshot;

    }

    /**
     * Enriches supplied monitoring snapshots with cost for each VM, so it works
     * DIRECTLY on the monData
     *
     * @param cloudOfferedServices contains pricing scheme
     * @param monData contains mon data and mon structure monData the huge chunk
     * of code below just enriches monitoring snapshots with cost at VM level.
     * Does NOT sum up all cost, but creates the base for anyone to take the
     * list of snapshots, and SUMM UP cost
     * @return a ServiceMonitoringSnapshot containing complete cost aggregated
     * over time
     */
    public ServiceMonitoringSnapshot getLastMonSnapshotEnrichedWithCost(List<CloudProvider> cloudProviders, List<ServiceMonitoringSnapshot> monData) {

//        map them as to make it fast to search what service is applicable
        //udpates monData in place
        CompositionRulesConfiguration cfg = createAndApplyCompositionRulesForComputingCost(cloudProvidersToMap(cloudProviders), monData);

        return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(cfg, monData.get(monData.size() - 1));

    }

    public Map<UUID, Map<UUID, ServiceUnit>> cloudProvidersToMap(List<CloudProvider> cloudProviders) {

        Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices = new HashMap<UUID, Map<UUID, ServiceUnit>>();

        for (CloudProvider cloudProvider : cloudProviders) {
            Map<UUID, ServiceUnit> cloudUnits = new HashMap<UUID, ServiceUnit>();

            cloudOfferedServices.put(cloudProvider.getUuid(), cloudUnits);

            for (ServiceUnit unit : cloudProvider.getServiceUnits()) {
                cloudUnits.put(unit.getUuid(), unit);
            }

        }

        return cloudOfferedServices;
    }

    public SnapshotEnrichmentReport enrichMonSnapshotWithInstantUsageCost(List<CloudProvider> cloudOfferedServices, ServiceMonitoringSnapshot monitoringSnapshot, ServiceUsageSnapshot totalUsageSoFar, final String currentTimesnapshot) {

        Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServicesMap = cloudProvidersToMap(cloudOfferedServices);

        //updates monData in place
        CompositionRulesBlock compositionRulesConfiguration = createCompositionRulesForInstantUsageCost(cloudOfferedServicesMap, monitoringSnapshot.getMonitoredService(), totalUsageSoFar, currentTimesnapshot);

//        Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> metricFilters = new HashMap<>();
        // set metric filters on data access
//        for (CompositionRule compositionRule : compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules()) {
//            // go trough each CompositionOperation and extract the source
//            // metrics
//
//            List<CompositionOperation> queue = new ArrayList<CompositionOperation>();
//            queue.add(compositionRule.getOperation());
//
//            while (!queue.isEmpty()) {
//                CompositionOperation operation = queue.remove(0);
//                queue.addAll(operation.getSubOperations());
//                Metric targetMetric = operation.getTargetMetric();
//                // metric can be null if a composition rule artificially creates
//                // a metric using SET_VALUE
//                if (targetMetric != null) {
//                    MetricFilter metricFilter = new MetricFilter();
//                    metricFilter.setId(targetMetric.getName() + "_Filter");
//                    metricFilter.setLevel(operation.getMetricSourceMonitoredElementLevel());
//                    Collection<Metric> metrics = new ArrayList<Metric>();
//                    metrics.add(new Metric(targetMetric.getName()));
//                    metricFilter.setMetrics(metrics);
//
//                    if (metricFilters.containsKey(metricFilter.getLevel())) {
//                        List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
//                        if (!list.contains(metricFilter)) {
//                            list.add(metricFilter);
//                        }
//                    } else {
//                        List<MetricFilter> list = new ArrayList<MetricFilter>();
//                        list.add(metricFilter);
//                        metricFilters.put(metricFilter.getLevel(), list);
//                    }
//                }
//            }
//        }
//        monitoringSnapshot.applyMetricFilters(metricFilters);
        SnapshotEnrichmentReport enrichmentReport = new SnapshotEnrichmentReport(instantMonitoringDataEnrichmentEngine.enrichMonitoringData(new CompositionRulesConfiguration().withMetricCompositionRules(compositionRulesConfiguration), monitoringSnapshot), compositionRulesConfiguration);
        return enrichmentReport;

    }

    /**
     * Enriches supplied monitoring snapshots with cost for each
     * ConfiguredServiceUnit, so it works DIRECTLY on the monData
     *
     * IT JUST ADDS cost for each UsedCloudOfferedServiceCfg, so an additional
     * aggregation is needed to build unit, topology service
     *
     * @param cloudOfferedServices contains pricing scheme
     * @param monData contains mon data and mon structure monData the huge chunk
     * of code below just enriches monitoring snapshots with cost at VM level.
     * Does NOT sum up all cost, but creates the base for anyone to take the
     * list of snapshots, and SUMM UP cost
     * @return a ServiceMonitoringSnapshot containing complete cost aggregated
     * over time
     */
    public CompositionRulesConfiguration createAndApplyCompositionRulesForComputingCost(Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices, List<ServiceMonitoringSnapshot> monData) {

        List<Metric> createdMetrics = new ArrayList<Metric>();

        //work on VM level now. So for example, one VM can be IN_CONJUNCTION with another VM. Or one VM could have 3 cloudOfferedServices : IaaS VM, PaaS OS + MaaS Monitoring
        //
        //so, we have 2 types of cost, PERIODIC, and PER USAGE
        //usually per USAGE is payed according to some interval, such as free first GB, rest 0.12, etc
        //thus, for each USAGE cost metric, we compute for each snapshot its usage so far, and insert in the snapshot the instant cost rate
        Map<MonitoredElement, Map<Metric, MetricValue>> usageSoFar = new ConcurrentHashMap<MonitoredElement, Map<Metric, MetricValue>>();

        //holds timestamp in which each mon element appears in the service
        Map<MonitoredElement, Long> vmsInstantiationTimes = new ConcurrentHashMap<MonitoredElement, Long>();

        List<MonitoredElement.MonitoredElementLevel> levelsInOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.VM);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);

        //compute cost for each service unit, topology and service for each snapshot
        CompositionRulesBlock costCompositionRules = new CompositionRulesBlock();
        CompositionRulesBlock historicalCostCompositionRules = new CompositionRulesBlock();

        for (int i = 0; i < monData.size(); i++) {
            ServiceMonitoringSnapshot monitoringSnapshot = monData.get(i);

            for (MonitoredElement.MonitoredElementLevel level : levelsInOrder) {

                Map<MonitoredElement, MonitoredElementMonitoringSnapshot> vmsData = monitoringSnapshot.getMonitoredData(level);

                if (vmsData == null) {
                    log.error("No monitoring data for service" + monitoringSnapshot.getMonitoredService() + " at level " + level.toString() + " timestamp " + monitoringSnapshot.getTimestampID());
                    continue;
                }

                for (MonitoredElement monitoredElement : vmsData.keySet()) {

                    Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

                    for (UsedCloudOfferedService service : monitoredElement.getCloudOfferedServices()) {

                        //maybe some do not quality to be apply as the service does not fulfill application requirements
                        List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(service);

                        //if just appeared, add monitored element VM in the instatiationTimes
                        if (!vmsInstantiationTimes.containsKey(monitoredElement)) {
                            vmsInstantiationTimes.put(monitoredElement, Long.parseLong(monitoringSnapshot.getTimestamp()));
                            usageSoFar.put(monitoredElement, new ConcurrentHashMap<Metric, MetricValue>());
                        }

                        Map<Metric, MetricValue> vmUsageSoFar = usageSoFar.get(monitoredElement);

                        //start with USAGE type of cost, easier to apply. 
                        for (CostFunction cf : costFunctionsToApply) {
                            for (CostElement element : cf.getCostElements()) {
                                if (element.getType().equals(CostElement.Type.USAGE)) {
                                    //if we just added the VM, so we do not have any historical usage so far
                                    MonitoredElementMonitoringSnapshot vmMonSnapshot = vmsData.get(monitoredElement);

                                    MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());
                                    if (value != null) {

                                        if (!vmUsageSoFar.containsKey(element.getCostMetric())) {
                                            vmUsageSoFar.put(element.getCostMetric(), value.clone());
                                            //enrich Monitoring Snapshot with COST 
                                            MetricValue costValue = value.clone();
                                            costValue.multiply(element.getCostForCostMetricValue(value));

                                            Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

                                            if (!createdMetrics.contains(cost)) {
                                                createdMetrics.add(cost);
                                            }

                                            vmMonSnapshot.putMetric(cost, costValue);

                                        } else {
                                            //else we need to sum up usage
                                            MetricValue usageSoFarForMetric = vmUsageSoFar.get(element.getCostMetric());

                                            //we should compute estimated usage over time not captured by monitoring points:
                                            //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
                                            //depending on the measurement unit of the measured metric
                                            //I.E. a pkts_out/s will be multiplied with 60
                                            //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
                                            //this works as we assume we target only metrics which change in time using PER USAGE cost functions
                                            String timePeriod = "s";

                                            if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                                timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                            }

                                            //check amount of time in millis between two measurement units
                                            Long previousTimestamp = Long.parseLong(monData.get(i - 1).getTimestamp());
                                            Long currentTimestamp = Long.parseLong(monitoringSnapshot.getTimestamp());

                                            //convert to seconds
                                            Long timeIntervalInMillis = (currentTimestamp - previousTimestamp) / 1000;

                                            long periodsBetweenPrevAndCurrentTimestamp = 0;

                                            //must standardise these somehow
                                            if (timePeriod.equals("s")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis;
                                            } else if (timePeriod.equals("m")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 60;
                                            } else if (timePeriod.equals("h")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 3600;
                                            } else if (timePeriod.equals("d")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 86400;
                                            }
                                            if (periodsBetweenPrevAndCurrentTimestamp <= 1) {
                                                usageSoFarForMetric.sum(value);
                                                //enrich Monitoring Snapshot with COST 
                                                MetricValue costValue = usageSoFarForMetric.clone();
                                                costValue.multiply(element.getCostForCostMetricValue(value));
                                                Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

                                                if (!createdMetrics.contains(cost)) {
                                                    createdMetrics.add(cost);
                                                }

                                                vmMonSnapshot.putMetric(cost, costValue);
                                            } else {
                                                //if more than one period between recordings, need to compute usage for non-monitored periods
                                                //we compute average for intermediary
                                                MonitoredElementMonitoringSnapshot prevVMData = monData.get(i - 1).getMonitoredData(monitoredElement);

                                                MetricValue prevValue = prevVMData.getMetricValue(element.getCostMetric());
                                                MetricValue average = prevValue.clone();
                                                prevValue.sum(value);
                                                average.divide(2);
                                                //add average for intermediary monitoring points
                                                average.multiply((int) periodsBetweenPrevAndCurrentTimestamp);
                                                //add monitored points
                                                average.sum(prevValue);
                                                average.sum(value);
                                                usageSoFarForMetric.setValue(average.getValue());
                                                MetricValue costValue = usageSoFarForMetric.clone();
                                                costValue.multiply(element.getCostForCostMetricValue(value));
                                                Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

                                                if (!createdMetrics.contains(cost)) {
                                                    createdMetrics.add(cost);
                                                }

                                                vmMonSnapshot.putMetric(cost, costValue);
                                            }

                                        }
                                    } else {
                                        log.error("Cost metric {} was not found on VM {}", element.getCostMetric().getName(), monitoredElement.getId());
                                    }
                                } else if (element.getType().equals(CostElement.Type.PERIODIC)) {
                                    MonitoredElementMonitoringSnapshot vmMonSnapshot = vmsData.get(monitoredElement);

                                    MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());
                                    if (value != null) {

                                        //we should compute estimated usage over time not captured by monitoring points:
                                        //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
                                        //depending on the measurement unit of the measured metric
                                        //I.E. a pkts_out/s will be multiplied with 60
                                        //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
                                        //this works as we assume we target only metrics which change in time using PER USAGE cost functions
                                        String timePeriod = "s";

                                        if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                            timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                        }

                                        //check amount of time in millis between two measurement units
                                        Long instantiationTimestamp = vmsInstantiationTimes.get(monitoredElement);
                                        Long currentTimestamp = Long.parseLong(monitoringSnapshot.getTimestamp());

                                        //convert to seconds
                                        Long timeIntervalInMillis = (currentTimestamp - instantiationTimestamp) / 1000;

                                        long costPeriodsFromCreation = 0;

                                        //must standardise these somehow
                                        if (timePeriod.equals("s")) {
                                            costPeriodsFromCreation = timeIntervalInMillis;
                                        } else if (timePeriod.equals("m")) {
                                            costPeriodsFromCreation = timeIntervalInMillis / 60;
                                        } else if (timePeriod.equals("h")) {
                                            costPeriodsFromCreation = timeIntervalInMillis / 3600;
                                        } else if (timePeriod.equals("d")) {
                                            costPeriodsFromCreation = timeIntervalInMillis / 86400;
                                        }

                                        MetricValue totalCostFromCreation = new MetricValue(costPeriodsFromCreation * element.getCostForCostMetricValue(new MetricValue(costPeriodsFromCreation)));

                                        Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST);

                                        if (!createdMetrics.contains(cost)) {
                                            createdMetrics.add(cost);
                                        }

                                        vmMonSnapshot.putMetric(cost, totalCostFromCreation);

                                    }
                                } else {
                                    log.error("Cost metric {} was not found on VM {}", element.getCostMetric().getName(), monitoredElement.getId());
                                }
                            }
                        }
                    }

                }

                for (Metric created : createdMetrics) {
                    //service unit rules
                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM)) {

                        //instant snapshot composition rule 
                        {
                            CompositionRule compositionRule = new CompositionRule();
                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM);
                            compositionOperation.setTargetMetric(created);
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            compositionRule.setOperation(compositionOperation);

                            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                                costCompositionRules.addCompositionRule(compositionRule);
                            }
                        }

                        //historical composition rule
                        {
                            CompositionRule compositionRule = new CompositionRule();
                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                            compositionOperation.setTargetMetric(created);
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            compositionRule.setOperation(compositionOperation);

                            if (!historicalCostCompositionRules.getCompositionRules().contains(compositionRule)) {
                                historicalCostCompositionRules.addCompositionRule(compositionRule);
                            }
                        }

                    }
                    //service topology rules
                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM) || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                        //instant snapshot composition rule 
                        {
                            CompositionRule compositionRule = new CompositionRule();
                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                            compositionOperation.setTargetMetric(created);
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            compositionRule.setOperation(compositionOperation);

                            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                                costCompositionRules.addCompositionRule(compositionRule);
                            }
                        }
                        //historical composition rule
                        {
                            CompositionRule compositionRule = new CompositionRule();
                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                            compositionOperation.setTargetMetric(created);
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            compositionRule.setOperation(compositionOperation);

                            if (!historicalCostCompositionRules.getCompositionRules().contains(compositionRule)) {
                                historicalCostCompositionRules.addCompositionRule(compositionRule);
                            }

                        }

                    }
                    //service rules
                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM) || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                            || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)) {

                        //instant snapshot composition rule 
                        {
                            CompositionRule compositionRule = new CompositionRule();
                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                            compositionOperation.setTargetMetric(created);
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            compositionRule.setOperation(compositionOperation);

                            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                                costCompositionRules.addCompositionRule(compositionRule);
                            }
                        }
                        //historical composition rule
                        {

                            CompositionRule compositionRule = new CompositionRule();
                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
                            compositionOperation.setTargetMetric(created);
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            compositionRule.setOperation(compositionOperation);

                            if (!historicalCostCompositionRules.getCompositionRules().contains(compositionRule)) {
                                historicalCostCompositionRules.addCompositionRule(compositionRule);
                            }

                        }

                    }

                }
            }
        }

        //rules creating total cost
        //service unit rules
        {
            CompositionRule compositionRule = new CompositionRule();
            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));

            CompositionOperation compositionOperation = new CompositionOperation();
            compositionOperation.setOperationType(CompositionOperationType.SUM);
            compositionRule.setOperation(compositionOperation);

            for (Metric m : createdMetrics) {

                CompositionOperation subOperation = new CompositionOperation();
                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM);
                subOperation.setOperationType(CompositionOperationType.SUM);
                subOperation.setTargetMetric(m);

                compositionOperation.addCompositionOperation(subOperation);
            }

            for (Metric m : createdMetrics) {

                CompositionOperation subOperation = new CompositionOperation();
                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                subOperation.setOperationType(CompositionOperationType.KEEP);
                subOperation.setTargetMetric(m);

                compositionOperation.addCompositionOperation(subOperation);
            }

            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                costCompositionRules.addCompositionRule(compositionRule);
            }

        }

        //service topology rules
        {
            CompositionRule compositionRule = new CompositionRule();
            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));

            CompositionOperation compositionOperation = new CompositionOperation();
            compositionOperation.setOperationType(CompositionOperationType.SUM);
            compositionRule.setOperation(compositionOperation);

            {

                CompositionOperation subOperation = new CompositionOperation();
                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                subOperation.setOperationType(CompositionOperationType.SUM);
                subOperation.setTargetMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));

                compositionOperation.addCompositionOperation(subOperation);
            }
            for (Metric m : createdMetrics) {

                CompositionOperation subOperation = new CompositionOperation();
                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                subOperation.setOperationType(CompositionOperationType.KEEP);
                subOperation.setTargetMetric(m);

                compositionOperation.addCompositionOperation(subOperation);
            }

            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                costCompositionRules.addCompositionRule(compositionRule);
            }

        }

        //service rules
        {
            CompositionRule compositionRule = new CompositionRule();
            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));

            CompositionOperation compositionOperation = new CompositionOperation();
            compositionOperation.setOperationType(CompositionOperationType.SUM);
            compositionRule.setOperation(compositionOperation);

            {

                CompositionOperation subOperation = new CompositionOperation();
                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                subOperation.setOperationType(CompositionOperationType.SUM);
                subOperation.setTargetMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));

                compositionOperation.addCompositionOperation(subOperation);
            }
            for (Metric m : createdMetrics) {

                CompositionOperation subOperation = new CompositionOperation();
                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
                subOperation.setOperationType(CompositionOperationType.KEEP);
                subOperation.setTargetMetric(m);

                compositionOperation.addCompositionOperation(subOperation);
            }

            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                costCompositionRules.addCompositionRule(compositionRule);
            }

        }

//        {
//            CompositionRule compositionRule = new CompositionRule();
//            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//            CompositionOperation compositionOperation = new CompositionOperation();
//            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//            compositionOperation.setTargetMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//            compositionOperation.setOperationType(CompositionOperationType.SUM);
//            compositionRule.setOperation(compositionOperation);
//
//            historicalCostCompositionRules.addCompositionRule(compositionRule);
//        }
//
//        {
//            CompositionRule compositionRule = new CompositionRule();
//            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//            CompositionOperation compositionOperation = new CompositionOperation();
//            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//            compositionOperation.setTargetMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//            compositionOperation.setOperationType(CompositionOperationType.SUM);
//            compositionRule.setOperation(compositionOperation);
//
//            historicalCostCompositionRules.addCompositionRule(compositionRule);
//        }
//
//        {
//            CompositionRule compositionRule = new CompositionRule();
//            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//            CompositionOperation compositionOperation = new CompositionOperation();
//            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//            compositionOperation.setTargetMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//            compositionOperation.setOperationType(CompositionOperationType.SUM);
//            compositionRule.setOperation(compositionOperation);
//
//            historicalCostCompositionRules.addCompositionRule(compositionRule);
//        }
        CompositionRulesConfiguration compositionRulesConfiguration = new CompositionRulesConfiguration();

        compositionRulesConfiguration.setHistoricDataAggregationRules(historicalCostCompositionRules);

        compositionRulesConfiguration.setMetricCompositionRules(costCompositionRules);

        return compositionRulesConfiguration;

    }

    /**
     * So, I want to compute PERIODIC COST for the CURRENT INSTANT service
     * structure So, to monitor how much am I paying NOW for PERIODIC COST, and
     * for cost PER USAGE For this, I cache things, to avoid computing all the
     * history of usage
     *
     * @param cloudOfferedServices
     * @param monData new monitoring data from the cached period
     * @param previouselyDeterminedUsage cached service usage so far
     * @return
     */
    public ServiceUsageSnapshot enrichMonSnapshotWithInstantPeriodicCost(Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices, List<ServiceMonitoringSnapshot> monData, ServiceUsageSnapshot previouselyDeterminedUsage) {

        if (monData == null || monData.isEmpty()) {
            return new ServiceUsageSnapshot();
        }

        //for computing instant PERIODIC COST for this instant (with these currently used cloud services), i need to consider only the current used cloud services
//        ServiceMonitoringSnapshot currentMonitoringSnapshot = monData.remove(monData.size() - 1);
        ServiceMonitoringSnapshot cachedMonitoringSnapshot = null;

        //if we have not cached anything before
        if (previouselyDeterminedUsage == null || previouselyDeterminedUsage.getTotalUsageSoFar().getMonitoredData().isEmpty()) {
            if (!monData.isEmpty()) {
                cachedMonitoringSnapshot = monData.remove(0);
                previouselyDeterminedUsage = new ServiceUsageSnapshot()
                        .withTotalUsageSoFar(cachedMonitoringSnapshot)
                        .withtLastUpdatedTimestampID(cachedMonitoringSnapshot.getTimestampID());
            } else {
                return previouselyDeterminedUsage;
            }
        } else {
            cachedMonitoringSnapshot = previouselyDeterminedUsage.getTotalUsageSoFar();
        }

        List<Metric> createdMetrics = new ArrayList<>();

        //work on VM level now. So for example, one VM can be IN_CONJUNCTION with another VM. Or one VM could have 3 cloudOfferedServices : IaaS VM, PaaS OS + MaaS Monitoring
        //
        //so, we have 2 types of cost, PERIODIC, and PER USAGE
        //usually per USAGE is payed according to some interval, such as free first GB, rest 0.12, etc
        //thus, for each USAGE cost metric, we compute for each snapshot its usage so far, and insert in the snapshot the instant cost rate
//        Map<MonitoredElement, Map<Metric, MetricValue>> usageSoFar = new ConcurrentHashMap<MonitoredElement, Map<Metric, MetricValue>>();
        //fill this up with old data from old mon snapshot
        //holds timestamp in which each mon element appears in the service
        //        Map<MonitoredElement, Long> cloudOfferedServiceInstantiationTimes = new ConcurrentHashMap<MonitoredElement, Long>();
        List<MonitoredElement.MonitoredElementLevel> levelsInOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.VM);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);
        //compute cost for each service unit, topology and service for each snapshot
        CompositionRulesBlock costCompositionRules = new CompositionRulesBlock();
//        CompositionRulesBlock historicalCostCompositionRules = new CompositionRulesBlock();

//        //add cached monitoring data in the usage so far
//        for (MonitoredElement.MonitoredElementLevel level : levelsInOrder) {
//            for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> monitoredLevelCachedUsageData : cachedMonitoringSnapshot.getMonitoredData(level).entrySet()) {
//                usageSoFar.put(monitoredLevelCachedUsageData.getKey(), monitoredLevelCachedUsageData.getValue().getMonitoredData());
//            }
//
//        }
        for (int i = 0; i < monData.size(); i++) {
            ServiceMonitoringSnapshot monitoringSnapshot = monData.get(i);

            for (MonitoredElement.MonitoredElementLevel level : levelsInOrder) {

                Map<MonitoredElement, MonitoredElementMonitoringSnapshot> vmsData = monitoringSnapshot.getMonitoredData(level);

                if (vmsData == null) {
                    log.error("No monitoring data for service" + monitoringSnapshot.getMonitoredService() + " at level " + level.toString() + " timestamp " + monitoringSnapshot.getTimestampID());
                    continue;
                }

                for (MonitoredElement monitoredElement : vmsData.keySet()) {

                    //if just appeared, add monitored element VM in the instatiationTimes
                    //update used cloud offered services, i.e., add newly added ones, and remove deleted ones
                    List<UsedCloudOfferedService> monitoredElementUsedServices = monitoredElement.getCloudOfferedServices();

                    //remove deallocated services
                    Iterator<UsedCloudOfferedService> it = previouselyDeterminedUsage.getServicesLifetime(monitoredElement).keySet().iterator();
                    while (it.hasNext()) {
                        UsedCloudOfferedService cloudOfferedService = it.next();
                        //if used service not used anymore, remove it from the map
                        if (!monitoredElementUsedServices.contains(cloudOfferedService)) {
                            it.remove();
                        }

                    }

                    //add newly allocated services
                    for (UsedCloudOfferedService ucos : monitoredElementUsedServices) {
                        if (!previouselyDeterminedUsage.getServicesLifetime(monitoredElement).containsKey(ucos)) {
                            previouselyDeterminedUsage.withInstantiationTimes(monitoredElement, ucos, Long.parseLong(monitoringSnapshot.getTimestamp()));
                        }
                    }

                    Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

                    for (UsedCloudOfferedService usedCloudService : monitoredElement.getCloudOfferedServices()) {
                        {

                            //from the cost functions, we extract those that should be applied.
                            //maybe some do not quality to be apply as the service does not fulfill application requirements
                            List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(usedCloudService);

                            //apply cost functions
                            Map<Metric, MetricValue> vmUsageSoFar = previouselyDeterminedUsage.getTotalUsageSoFar().getMonitoredData(monitoredElement).getMonitoredData();

                            //start with USAGE type of cost, easier to apply. 
                            for (CostFunction cf : costFunctionsToApply) {
                                for (CostElement element : cf.getCostElements()) {

                                    if (element.getType().equals(CostElement.Type.USAGE)) {
                                        //if we just added the VM, so we do not have any historical usage so far
                                        MonitoredElementMonitoringSnapshot vmMonSnapshot = vmsData.get(monitoredElement);

                                        MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());
                                        if (value != null) {

                                            if (!vmUsageSoFar.containsKey(element.getCostMetric())) {
                                                vmUsageSoFar.put(element.getCostMetric(), value.clone());
                                                //enrich Monitoring Snapshot with COST 
                                                MetricValue costValue = value.clone();
                                                costValue.multiply(element.getCostForCostMetricValue(value));

                                                Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

                                                if (!createdMetrics.contains(cost)) {
                                                    createdMetrics.add(cost);
                                                }

                                                vmMonSnapshot.putMetric(cost, costValue);

                                            } else {
                                                //else we need to sum up usage
                                                MetricValue usageSoFarForMetric = vmUsageSoFar.get(element.getCostMetric());

                                                //we should compute estimated usage over time not captured by monitoring points:
                                                //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
                                                //depending on the measurement unit of the measured metric
                                                //I.E. a pkts_out/s will be multiplied with 60
                                                //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
                                                //this works as we assume we target only metrics which change in time using PER USAGE cost functions
                                                String timePeriod = "s";

                                                if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                                    timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                                }

                                                //check amount of time in millis between two measurement units
                                                Long previousTimestamp = Long.parseLong(monitoringSnapshot.getTimestamp());;
                                                Long currentTimestamp = Long.parseLong(monitoringSnapshot.getTimestamp());
                                                Long timeIntervalInMillis = 0l;
                                                if (i > 0) {
                                                    previousTimestamp = Long.parseLong(monData.get(i - 1).getTimestamp());
                                                    timeIntervalInMillis = (currentTimestamp - previousTimestamp) / 1000;
                                                }

                                                //convert to seconds
                                                Long periodsBetweenPrevAndCurrentTimestamp = 0l;

                                                //must standardise these somehow
                                                if (timePeriod.equals("s")) {
                                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis;
                                                } else if (timePeriod.equals("m")) {
                                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 60;
                                                } else if (timePeriod.equals("h")) {
                                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 3600;
                                                } else if (timePeriod.equals("d")) {
                                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 86400;
                                                }

                                                //if metric does not have period, than its a metric which ACCUMULATES, I.E., show summed up hsitorical usage by itself
                                                if (periodsBetweenPrevAndCurrentTimestamp <= 1) {
                                                    usageSoFarForMetric.sum(value);
                                                    //enrich Monitoring Snapshot with COST 
                                                    MetricValue costValue = usageSoFarForMetric.clone();
                                                    costValue.multiply(element.getCostForCostMetricValue(value));
                                                    Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

                                                    if (!createdMetrics.contains(cost)) {
                                                        createdMetrics.add(cost);
                                                    }

                                                    vmMonSnapshot.putMetric(cost, costValue);
                                                } else {
                                                    //if more than one period between recordings, need to compute usage for non-monitored periods
                                                    //we compute average for intermediary

                                                    MetricValue prevValue = vmUsageSoFar.get(element.getCostMetric());
                                                    MetricValue average = prevValue.clone();
                                                    prevValue.sum(value);
                                                    average.divide(2);
                                                    //add average for intermediary monitoring points
                                                    //TODO: add option for adding other plug-ins for filling up missing data, and computing accuracy of estimation
                                                    average.multiply(periodsBetweenPrevAndCurrentTimestamp.intValue());
                                                    //add monitored points
                                                    average.sum(prevValue);
                                                    average.sum(value);
                                                    usageSoFarForMetric.setValue(average.getValue());
                                                    MetricValue costValue = usageSoFarForMetric.clone();
                                                    costValue.multiply(element.getCostForCostMetricValue(value));
                                                    Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

                                                    if (!createdMetrics.contains(cost)) {
                                                        createdMetrics.add(cost);
                                                    }

                                                    vmMonSnapshot.putMetric(cost, costValue);
                                                }

                                            }
                                        } else {
                                            log.error("Cost metric {} was not found on VM {}", element.getCostMetric().getName(), monitoredElement.getId());
                                        }

                                        //if cost is per usage, it is computed by instant monitoring cost.
//                                        continue;
                                    } else if (element.getType().equals(CostElement.Type.PERIODIC)) {
                                        MonitoredElementMonitoringSnapshot vmMonSnapshot = vmsData.get(monitoredElement);

                                        MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());
                                        if (value != null) {

                                            //we should compute estimated usage over time not captured by monitoring points:
                                            //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
                                            //depending on the measurement unit of the measured metric
                                            //I.E. a pkts_out/s will be multiplied with 60
                                            //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
                                            //this works as we assume we target only metrics which change in time using PER USAGE cost functions
                                            String timePeriod = "s";

                                            if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                                timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                            }

//                                            
                                            //check amount of time in millis between two measurement units
                                            Long instantiationTimestamp = previouselyDeterminedUsage.getServicesLifetime(monitoredElement).get(usedCloudService);
                                            Long currentTimestamp = Long.parseLong(monitoringSnapshot.getTimestamp());

                                            //convert to seconds
                                            Long timeIntervalInMillis = (currentTimestamp - instantiationTimestamp) / 1000;

                                            long costPeriodsFromCreation = 0;

                                            //must standardise these somehow
                                            if (timePeriod.equals("s")) {
                                                costPeriodsFromCreation = timeIntervalInMillis;
                                            } else if (timePeriod.equals("m")) {
                                                costPeriodsFromCreation = timeIntervalInMillis / 60;
                                            } else if (timePeriod.equals("h")) {
                                                costPeriodsFromCreation = timeIntervalInMillis / 3600;
                                            } else if (timePeriod.equals("d")) {
                                                costPeriodsFromCreation = timeIntervalInMillis / 86400;
                                            }

                                            MetricValue totalCostFromCreation = new MetricValue(costPeriodsFromCreation * element.getCostForCostMetricValue(new MetricValue(costPeriodsFromCreation)));
                                            MetricValue totalUsageFromCreation = new MetricValue(costPeriodsFromCreation);

                                            Metric cost = new Metric("cost_" + element.getCostMetric().getName() + "_for_" + usedCloudService.getName(), "costUnits/" + timePeriod, Metric.MetricType.COST);
                                            Metric lifetime = new Metric("usage_time" + usedCloudService.getName(), timePeriod, Metric.MetricType.RESOURCE);

                                            if (!createdMetrics.contains(cost)) {
                                                createdMetrics.add(cost);
                                            }
                                            if (!createdMetrics.contains(lifetime)) {
                                                createdMetrics.add(lifetime);
                                            }

                                            vmMonSnapshot.putMetric(cost, totalCostFromCreation);
                                            vmMonSnapshot.putMetric(lifetime, totalUsageFromCreation);

                                            CompositionRule compositionRule = new CompositionRule();
                                            compositionRule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                                            compositionRule.setResultingMetric(cost);
                                            CompositionOperation compositionOperation = new CompositionOperation();
                                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM);
                                            compositionOperation.setTargetMetric(lifetime);
                                            compositionOperation.setOperationType(CompositionOperationType.MUL);
                                            compositionOperation.setValue(element.getCostForCostMetricValue(new MetricValue(costPeriodsFromCreation)).toString());
                                            compositionRule.setOperation(compositionOperation);

                                            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                                                costCompositionRules.addCompositionRule(compositionRule);
                                            }
                                        }
//                                     
                                    } else {
                                        log.error("Cost metric {} was not found on VM {}", element.getCostMetric().getName(), monitoredElement.getId());
                                    }
                                }
                            }
                        }

                    }
                }

//                for (Metric created : createdMetrics) {
//                    //service unit rules
//                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM)) {
//
//                        //instant snapshot composition rule 
//                        {
//                            CompositionRule compositionRule = new CompositionRule();
//                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
//                            CompositionOperation compositionOperation = new CompositionOperation();
//                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM);
//                            compositionOperation.setTargetMetric(created);
//                            compositionOperation.setOperationType(CompositionOperationType.SUM);
//                            compositionRule.setOperation(compositionOperation);
//
//                            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                costCompositionRules.addCompositionRule(compositionRule);
//                            }
//                        }
//
//                        //historical composition rule
//                        {
//                            CompositionRule compositionRule = new CompositionRule();
//                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
//                            CompositionOperation compositionOperation = new CompositionOperation();
//                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                            compositionOperation.setTargetMetric(created);
//                            compositionOperation.setOperationType(CompositionOperationType.SUM);
//                            compositionRule.setOperation(compositionOperation);
//
//                            if (!historicalCostCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                historicalCostCompositionRules.addCompositionRule(compositionRule);
//                            }
//                        }
//
//                    }
//                    //service topology rules
//                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM) || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
//                        //instant snapshot composition rule 
//                        {
//                            CompositionRule compositionRule = new CompositionRule();
//                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
//                            CompositionOperation compositionOperation = new CompositionOperation();
//                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                            compositionOperation.setTargetMetric(created);
//                            compositionOperation.setOperationType(CompositionOperationType.SUM);
//                            compositionRule.setOperation(compositionOperation);
//
//                            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                costCompositionRules.addCompositionRule(compositionRule);
//                            }
//                        }
//                        //historical composition rule
//                        {
//                            CompositionRule compositionRule = new CompositionRule();
//                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
//                            CompositionOperation compositionOperation = new CompositionOperation();
//                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                            compositionOperation.setTargetMetric(created);
//                            compositionOperation.setOperationType(CompositionOperationType.SUM);
//                            compositionRule.setOperation(compositionOperation);
//
//                            if (!historicalCostCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                historicalCostCompositionRules.addCompositionRule(compositionRule);
//                            }
//
//                        }
//
//                    }
//                    //service rules
//                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM) || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
//                            || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)) {
//
//                        //instant snapshot composition rule 
//                        {
//                            CompositionRule compositionRule = new CompositionRule();
//                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
//                            CompositionOperation compositionOperation = new CompositionOperation();
//                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                            compositionOperation.setTargetMetric(created);
//                            compositionOperation.setOperationType(CompositionOperationType.SUM);
//                            compositionRule.setOperation(compositionOperation);
//
//                            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                costCompositionRules.addCompositionRule(compositionRule);
//                            }
//                        }
//                        //historical composition rule
//                        {
//
//                            CompositionRule compositionRule = new CompositionRule();
//                            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//                            compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
//                            CompositionOperation compositionOperation = new CompositionOperation();
//                            compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//                            compositionOperation.setTargetMetric(created);
//                            compositionOperation.setOperationType(CompositionOperationType.SUM);
//                            compositionRule.setOperation(compositionOperation);
//
//                            if (!historicalCostCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                historicalCostCompositionRules.addCompositionRule(compositionRule);
//                            }
//
//                        }
//
//                    }
//
//                }
//            }
//        }
//
//        //rules creating total cost
//        //service unit rules
//        {
//            CompositionRule compositionRule = new CompositionRule();
//            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//
//            CompositionOperation compositionOperation = new CompositionOperation();
//            compositionOperation.setOperationType(CompositionOperationType.SUM);
//            compositionRule.setOperation(compositionOperation);
//
//            for (Metric m : createdMetrics) {
//
//                CompositionOperation subOperation = new CompositionOperation();
//                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM);
//                subOperation.setOperationType(CompositionOperationType.SUM);
//                subOperation.setTargetMetric(m);
//
//                compositionOperation.addCompositionOperation(subOperation);
//            }
//
//            for (Metric m : createdMetrics) {
//
//                CompositionOperation subOperation = new CompositionOperation();
//                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                subOperation.setOperationType(CompositionOperationType.KEEP);
//                subOperation.setTargetMetric(m);
//
//                compositionOperation.addCompositionOperation(subOperation);
//            }
//
//            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                costCompositionRules.addCompositionRule(compositionRule);
//            }
//
//        }
//
//        //service topology rules
//        {
//            CompositionRule compositionRule = new CompositionRule();
//            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//
//            CompositionOperation compositionOperation = new CompositionOperation();
//            compositionOperation.setOperationType(CompositionOperationType.SUM);
//            compositionRule.setOperation(compositionOperation);
//
//            {
//
//                CompositionOperation subOperation = new CompositionOperation();
//                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                subOperation.setOperationType(CompositionOperationType.SUM);
//                subOperation.setTargetMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//
//                compositionOperation.addCompositionOperation(subOperation);
//            }
//            for (Metric m : createdMetrics) {
//
//                CompositionOperation subOperation = new CompositionOperation();
//                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                subOperation.setOperationType(CompositionOperationType.KEEP);
//                subOperation.setTargetMetric(m);
//
//                compositionOperation.addCompositionOperation(subOperation);
//            }
//
//            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                costCompositionRules.addCompositionRule(compositionRule);
//            }
//
//        }
//
//        //service rules
//        {
//            CompositionRule compositionRule = new CompositionRule();
//            compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//            compositionRule.setResultingMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//
//            CompositionOperation compositionOperation = new CompositionOperation();
//            compositionOperation.setOperationType(CompositionOperationType.SUM);
//            compositionRule.setOperation(compositionOperation);
//
//            {
//
//                CompositionOperation subOperation = new CompositionOperation();
//                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                subOperation.setOperationType(CompositionOperationType.SUM);
//                subOperation.setTargetMetric(new Metric("cost_total", "costUnits", Metric.MetricType.COST));
//
//                compositionOperation.addCompositionOperation(subOperation);
//            }
//            for (Metric m : createdMetrics) {
//
//                CompositionOperation subOperation = new CompositionOperation();
//                subOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//                subOperation.setOperationType(CompositionOperationType.KEEP);
//                subOperation.setTargetMetric(m);
//
//                compositionOperation.addCompositionOperation(subOperation);
//            }
//
//            if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                costCompositionRules.addCompositionRule(compositionRule);
//            }
//
//        }
//
//        CompositionRulesConfiguration compositionRulesConfiguration = new CompositionRulesConfiguration();
//        compositionRulesConfiguration.setHistoricDataAggregationRules(historicalCostCompositionRules);
//        compositionRulesConfiguration.setMetricCompositionRules(costCompositionRules);
//        ServiceMonitoringSnapshot monitoringSnapshot = instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, ms);
//                //
//        tre sa cache 1: monitoring snapshot
//                     2. composition rules sa pot calcula total? sau nu imi trebe?
//        return compositionRulesConfiguration;
//                
//                tre sa cache 1: monitoring snapshot . am pus timestamp pe ea, tre acuma cached, (de manager cred)
//                si trebe retrieved etc
            }
        }
        ServiceMonitoringSnapshot sms = monData.get(monData.size() - 1);
        previouselyDeterminedUsage.withTotalUsageSoFar(sms);
        previouselyDeterminedUsage.withtLastUpdatedTimestampID(sms.getTimestampID());
        previouselyDeterminedUsage.withCostCompositionRules(costCompositionRules);
        return previouselyDeterminedUsage;
    }

    /**
     * 1. Check what cost functions are applicable 2. SUMS up the metrics
     * targeted by the cost functions, between the previously monitored ones and
     * the new monitored snapshot !!!! NOTE !!! It works directly on the
     * supplied previouselyDeterminedUsage
     *
     * @param cloudOfferedServices
     * @param previouselyDeterminedUsage
     * @param newMonData
     * @return
     */
    public ServiceMonitoringSnapshot updateTotalUsageSoFar(Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices, ServiceUsageSnapshot previouselyDeterminedUsage, ServiceMonitoringSnapshot newMonData) {

        if (newMonData == null) {
            return new ServiceMonitoringSnapshot();
        }

        //so, we have 2 types of cost, PERIODIC, and PER USAGE
        //usually per USAGE is payed according to some interval, such as free first GB, rest 0.12, etc
        //thus, for each USAGE cost metric, we compute for each snapshot its usage so far, and insert in the snapshot the instant cost rate
//        Map<MonitoredElement, Map<Metric, MetricValue>> usageSoFar = new ConcurrentHashMap<MonitoredElement, Map<Metric, MetricValue>>();
        //fill this up with old data from old mon snapshot
        //holds timestamp in which each mon element appears in the service
        //        Map<MonitoredElement, Long> cloudOfferedServiceInstantiationTimes = new ConcurrentHashMap<MonitoredElement, Long>();
        List<MonitoredElement.MonitoredElementLevel> levelsInOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.VM);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);

        ServiceMonitoringSnapshot monitoringSnapshot = newMonData;

        for (MonitoredElement.MonitoredElementLevel level : levelsInOrder) {

            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> vmsData = monitoringSnapshot.getMonitoredData(level);

            if (vmsData == null) {
                log.error("No monitoring data for service" + monitoringSnapshot.getMonitoredService() + " at level " + level.toString() + " timestamp " + monitoringSnapshot.getTimestampID());
                continue;
            }

            for (MonitoredElement monitoredElement : vmsData.keySet()) {

                //if just appeared, add monitored element VM in the instatiationTimes
                //update used cloud offered services, i.e., add newly added ones, and remove deleted ones
                List<UsedCloudOfferedService> monitoredElementUsedServices = monitoredElement.getCloudOfferedServices();

                //remove deallocated services
                Iterator<UsedCloudOfferedService> it = previouselyDeterminedUsage.getServicesLifetime(monitoredElement).keySet().iterator();
                while (it.hasNext()) {
                    UsedCloudOfferedService cloudOfferedService = it.next();
                    //if used service not used anymore, remove it from the map
                    if (!monitoredElementUsedServices.contains(cloudOfferedService)) {
                        it.remove();
                    }

                }

                //add newly allocated services
                for (UsedCloudOfferedService ucos : monitoredElementUsedServices) {
                    if (!previouselyDeterminedUsage.getServicesLifetime(monitoredElement).containsKey(ucos)) {
                        previouselyDeterminedUsage.withInstantiationTimes(monitoredElement, ucos, Long.parseLong(monitoringSnapshot.getTimestamp()));
                    }
                }

                Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

                for (UsedCloudOfferedService usedCloudService : monitoredElement.getCloudOfferedServices()) {
                    {

                        //from the cost functions, we extract those that should be applied.
                        //maybe some do not quality to be apply as the service does not fulfill application requirements
                        List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(usedCloudService);

                        Map<Metric, MetricValue> vmUsageSoFar = null;

                        if (previouselyDeterminedUsage.getTotalUsageSoFar().contains(level, monitoredElement)) {
                            vmUsageSoFar = previouselyDeterminedUsage.getTotalUsageSoFar().getMonitoredData(monitoredElement).getMonitoredData();
                        } else {
                            vmUsageSoFar = new HashMap<>();
                            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(monitoredElement, vmUsageSoFar);
                            previouselyDeterminedUsage.getTotalUsageSoFar().addMonitoredData(elementMonitoringSnapshot);
                        }

                        //apply cost functions
                        //start with USAGE type of cost, easier to apply. 
                        for (CostFunction cf : costFunctionsToApply) {
                            for (CostElement element : cf.getCostElements()) {

                                if (element.getType().equals(CostElement.Type.USAGE)) {
                                    //if we just added the VM, so we do not have any historical usage so far
                                    MonitoredElementMonitoringSnapshot vmMonSnapshot = vmsData.get(monitoredElement);

                                    MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());
                                    if (value != null) {

                                        if (!vmUsageSoFar.containsKey(element.getCostMetric())) {
                                            vmUsageSoFar.put(element.getCostMetric(), value.clone());

                                        } else {
                                            //else we need to sum up usage
                                            MetricValue usageSoFarForMetric = vmUsageSoFar.get(element.getCostMetric());

                                            //we should compute estimated usage over time not captured by monitoring points:
                                            //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
                                            //depending on the measurement unit of the measured metric
                                            //I.E. a pkts_out/s will be multiplied with 60
                                            //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
                                            //this works as we assume we target only metrics which change in time using PER USAGE cost functions
                                            String timePeriod = "s";

                                            if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                                timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                            }

                                            //check amount of time in millis between two measurement units
                                            Long currentTimestamp = Long.parseLong(monitoringSnapshot.getTimestamp());
                                            Long previousTimestamp = Long.parseLong(newMonData.getTimestamp());
                                            Long timeIntervalInMillis = (currentTimestamp - previousTimestamp) / 1000;

                                            //convert to seconds
                                            Long periodsBetweenPrevAndCurrentTimestamp = 0l;

                                            //must standardise these somehow
                                            if (timePeriod.equals("s")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis;
                                            } else if (timePeriod.equals("m")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 60;
                                            } else if (timePeriod.equals("h")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 3600;
                                            } else if (timePeriod.equals("d")) {
                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 86400;
                                            }

                                            //if metric does not have period, than its a metric which ACCUMULATES, I.E., show summed up hsitorical usage by itself
                                            if (periodsBetweenPrevAndCurrentTimestamp <= 1) {
                                                usageSoFarForMetric.sum(value);

                                            } else {
                                                //if more than one period between recordings, need to compute usage for non-monitored periods
                                                //we compute average for intermediary

                                                MetricValue prevValue = vmUsageSoFar.get(element.getCostMetric());
                                                MetricValue average = prevValue.clone();
                                                prevValue.sum(value);
                                                average.divide(2);
                                                //add average for intermediary monitoring points
                                                //TODO: add option for adding other plug-ins for filling up missing data, and computing accuracy of estimation
                                                average.multiply(periodsBetweenPrevAndCurrentTimestamp.intValue());
                                                //add monitored points
                                                average.sum(prevValue);
                                                average.sum(value);
                                                usageSoFarForMetric.setValue(average.getValue());

                                            }

                                        }
                                    } else {
                                        log.error("Cost metric {} was not found on VM {}", element.getCostMetric().getName(), monitoredElement.getId());
                                    }

                                    //if cost is per usage, it is computed by instant monitoring cost.
//                                        continue;
                                }
                            }
                        }
                    }

                }
            }

        }

        return previouselyDeterminedUsage.getTotalUsageSoFar();
    }

    public Map<UsedCloudOfferedService, List<CostFunction>> getApplicableCostFunctions(Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices, MonitoredElement monitoredElement) {

        Map<UsedCloudOfferedService, List<CostFunction>> map = new HashMap<>();

        for (UsedCloudOfferedService usedCloudService : monitoredElement.getCloudOfferedServices()) {
            List<CostFunction> costFunctionsToApply = new ArrayList<CostFunction>();
            map.put(usedCloudService, costFunctionsToApply);
            //get service cost scheme
            List<CostFunction> costFunctions = null;

            if (cloudOfferedServices.containsKey(usedCloudService.getCloudProviderID())) {
                Map<UUID, ServiceUnit> cloudServices = cloudOfferedServices.get(usedCloudService.getCloudProviderID());
                if (cloudServices.containsKey(usedCloudService.getId())) {
                    ServiceUnit cloudService = cloudServices.get(usedCloudService.getId());
                    costFunctions = cloudService.getCostFunctions();
                } else {
                    log.warn("Cloud service {} with UUID {} of cloud provider {} with UUID {} not present in cloud offered services with size {}", new Object[]{
                        usedCloudService.getName(), usedCloudService.getId(), usedCloudService.getCloudProviderName(), usedCloudService.getCloudProviderID(), "" + cloudServices.size()});
                }

            } else {
                log.warn("Cloud provider {} with UUID {} not present in cloud offered services {}", new Object[]{usedCloudService.getCloudProviderName(), usedCloudService.getCloudProviderID(), "" + cloudOfferedServices.keySet().size()});
            }

            if (costFunctions == null) {
                log.warn("UsedCloudOfferedService with ID {} not found in cloud offered services", usedCloudService.getId());
            } else {
                //from the cost functions, we extract those that should be applied.
                //maybe some do not quality to be applied as the service does not fulfill application requirements

                for (CostFunction cf : costFunctions) {
                    //if cost function is to be applied no mather what (does not depend on the service being used in conjunction with another service)
                    //means getAppliedInConjunctionWith() returns empty 
                    if (cf.getAppliedInConjunctionWith().isEmpty()) {
                        costFunctionsToApply.add(cf);
                    } else {
                                //else need to check if it is used in conjunction with the mentioned

                        //can be diff entities: For example, VM type A costs X if has RAM 1, CPU 2, and used with Storage Y
                        List<ServiceUnit> tobeAppliedInConjunctionWithServiceUnit = cf.getAppliedInConjunctionWithServiceUnit();
                        List<Resource> tobeAppliedInConjunctionWithResource = cf.getAppliedInConjunctionWithResource();
                        List<Quality> tobeAppliedInConjunctionWithQuality = cf.getAppliedInConjunctionWithQuality();

                        //NEED TO MATCH Resources
                        Map<Metric, MetricValue> serviceResourceProperties = usedCloudService.getResourceProperties();
                        //check if ALL properties match
                        //reduce all Resource to one large property map
                        Map<Metric, MetricValue> tobeAppliedInConjunctionWithResourceProperties = new ConcurrentHashMap<Metric, MetricValue>();
                        for (Resource r : tobeAppliedInConjunctionWithResource) {
                            tobeAppliedInConjunctionWithResourceProperties.putAll(r.getProperties());
                        }

                        boolean resourcesMatch = true;

                        if (tobeAppliedInConjunctionWithResourceProperties.size() <= serviceResourceProperties.size()) {
                            for (Metric m : tobeAppliedInConjunctionWithResourceProperties.keySet()) {
                                if (serviceResourceProperties.containsKey(m)) {
                                    if (tobeAppliedInConjunctionWithResourceProperties.get(m).equals(serviceResourceProperties.get(m))) {
                                        //good
                                    } else {
                                        //no match
                                        resourcesMatch = false;
                                        break;
                                    }
                                } else {
                                    //no match
                                    resourcesMatch = false;
                                    break;
                                }
                            }
                            //if we reached here with no break, then we have a match and apply cost

                        } else {
                            resourcesMatch = false;
                            //no match
                        }

                        //NEED TO MATCH Quality
                        Map<Metric, MetricValue> serviceQualityProperties = usedCloudService.getQualityProperties();
                        Map<Metric, MetricValue> tobeAppliedInConjunctionWithQualityProperties = new ConcurrentHashMap<Metric, MetricValue>();
                        for (Quality q : tobeAppliedInConjunctionWithQuality) {
                            tobeAppliedInConjunctionWithResourceProperties.putAll(q.getProperties());
                        }

                        boolean qualityMatch = true;

                        if (tobeAppliedInConjunctionWithQualityProperties.size() <= serviceQualityProperties.size()) {
                            for (Metric m : tobeAppliedInConjunctionWithQualityProperties.keySet()) {
                                if (serviceQualityProperties.containsKey(m)) {
                                    if (tobeAppliedInConjunctionWithQualityProperties.get(m).equals(serviceQualityProperties.get(m))) {
                                        //good
                                    } else {
                                        //no match
                                        qualityMatch = false;
                                        break;
                                    }
                                } else {
                                    //no match
                                    qualityMatch = false;
                                    break;
                                }
                            }
                            //if we reached here with no break, then we have a match and apply cost

                        } else {
                            qualityMatch = false;
                            //no match
                        }

                        //NEED TO MATCH InConjunctionWith other services
                        boolean serviceUnitInConjunctionMatch = true;
                        //if empty vm is not related to anything
                        Collection<Relationship> relationships = monitoredElement.getRelationships(Relationship.RelationshipType.InConjunctionWith);
                        if ((!tobeAppliedInConjunctionWithServiceUnit.isEmpty()) && relationships.isEmpty()) {
                            serviceUnitInConjunctionMatch = false;
                        } else {
                            //here we need to check if used in conjunction with the right cloud service

                            if (tobeAppliedInConjunctionWithServiceUnit.size() <= relationships.size()) {

                                for (ServiceUnit unit : tobeAppliedInConjunctionWithServiceUnit) {
                                    boolean unitMatched = false;
                                    for (Relationship r : relationships) {
                                        if (r.getTo().getName().equals(unit.getName())) {
                                            unitMatched = true;
                                        }
                                    }
                                    //if we have not found that even one in conjunction was not found, then we do not apply cost scheme
                                    if (!unitMatched) {
                                        serviceUnitInConjunctionMatch = false;
                                        break;
                                    }
                                }

                            } else {
                                //not enough in conjunction => no match
                                serviceUnitInConjunctionMatch = false;
                            }

                        }

                        //if we have all resource, quality, other services to be applied
                        //in conjunction with, then apply cost scheme
                        if (resourcesMatch && qualityMatch && serviceUnitInConjunctionMatch) {
                            costFunctionsToApply.add(cf);
                        }

                    }
                }
            }
        }

        return map;

    }

    public CompositionRulesConfiguration createCompositionRulesForCostPerPeriod(final List<ServiceUnit> cloudOfferedServices, final MonitoredElement monitoredElement) {
        log.debug("CompositionRulesConfiguration createCompositionRulesForCostPerPeriod not implemented yet");
        return new CompositionRulesConfiguration();
    }

    private static final Metric ELEMENT_COST_METRIC = new Metric("element_cost", "costUnits", Metric.MetricType.COST);

    /**
     * Only computes the current cost rate for cost element reported per USAGE.
     * It is the fastest to compute, as it requires no historical information.
     * Computing PERIODIC cost requires computing lifetime, and thus, its
     * slower.
     *
     * Thus, we provide methods to obtain three different cost snapshots:
     * instant cost rate per usage, total cost until now per periodic, and
     * complete
     *
     * @param cloudOfferedServices
     * @param monitoredElement
     * @param totalUsageSoFar total usage so far with current structure. used in
     * determining instant applicable cost pricing scheme according to cost
     * intervals
     * @param currentTimesnapshot string representation of the Long timestamp of
     * the current monitoring snapshot. Used to determine lifetime so far
     * @return
     */
    public CompositionRulesBlock createCompositionRulesForInstantUsageCost(final Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices,
            final MonitoredElement monitoredElement, final ServiceUsageSnapshot totalUsageSoFar, final String currentTimesnapshot) {

        CompositionRulesBlock costCompositionRules = new CompositionRulesBlock();

        Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

        {
            for (UsedCloudOfferedService service : monitoredElement.getCloudOfferedServices()) {
                {

                    //from the cost functions, we extract those that should be applied.
                    //maybe some do not quality to be apply as the service does not fulfill application requirements
                    List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(service);

                    //start with USAGE type of cost, easier to apply. 
                    for (CostFunction cf : costFunctionsToApply) {
                        MonitoredElementMonitoringSnapshot vmMonSnapshot = totalUsageSoFar.getTotalUsageSoFar().getMonitoredData(monitoredElement);

                        for (CostElement element : cf.getCostElements()) {

                            MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());

                            if (value != null) {
                                if (element.getType().equals(CostElement.Type.USAGE)) {
                                    //instant snapshot composition rule 
                                    {
                                        CompositionRule compositionRule = new CompositionRule();
                                        compositionRule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                                        compositionRule.addTargetMonitoredElementIDS(monitoredElement.getId());
                                        String timePeriod = "s";

                                        if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                            timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                        }
                                        compositionRule.setResultingMetric(new Metric("cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST));
                                        CompositionOperation compositionOperation = new CompositionOperation();
                                        compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
                                        MetricValue costForValue = value.clone();
                                        costForValue.multiply(element.getCostForCostMetricValue(value));

                                        compositionOperation.setValue(costForValue.getValueRepresentation());
                                        compositionRule.setOperation(compositionOperation);

                                        if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                                            costCompositionRules.addCompositionRule(compositionRule);
                                        }
                                    }

                                } else if (element.getType().equals(CostElement.Type.PERIODIC)) {

                                    //we should compute estimated usage over time not captured by monitoring points:
                                    //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
                                    //depending on the measurement unit of the measured metric
                                    //I.E. a pkts_out/s will be multiplied with 60
                                    //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
                                    //this works as we assume we target only metrics which change in time using PER USAGE cost functions
                                    String timePeriod = "s";

                                    if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                        timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                    }

                                    Long currentTimestamp = Long.parseLong(currentTimesnapshot);
                                    Long instantiationTimestamp = totalUsageSoFar.getInstantiationTime(monitoredElement, service);

                                    //convert to seconds
                                    Long timeIntervalInMillis = (currentTimestamp - instantiationTimestamp) / 1000;

                                    long costPeriodsFromCreation = 0;

                                    //must standardise these somehow
                                    if (timePeriod.equals("s")) {
                                        costPeriodsFromCreation = timeIntervalInMillis;
                                    } else if (timePeriod.equals("m")) {
                                        costPeriodsFromCreation = timeIntervalInMillis / 60;
                                    } else if (timePeriod.equals("h")) {
                                        costPeriodsFromCreation = timeIntervalInMillis / 3600;
                                    } else if (timePeriod.equals("d")) {
                                        costPeriodsFromCreation = timeIntervalInMillis / 86400;
                                    }

                                    MetricValue totalCostFromCreation = new MetricValue(element.getCostForCostMetricValue(new MetricValue(costPeriodsFromCreation)));

                                    Metric cost = new Metric("cost_" + element.getCostMetric().getName() + "_for_" + service.getName(), "costUnits/" + timePeriod, Metric.MetricType.COST);

                                    CompositionRule compositionRule = new CompositionRule();
                                    compositionRule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                                    compositionRule.addTargetMonitoredElementIDS(monitoredElement.getId());

                                    compositionRule.setResultingMetric(cost);
                                    CompositionOperation compositionOperation = new CompositionOperation();

                                    compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
                                    compositionOperation.setValue(totalCostFromCreation.getValueRepresentation());
                                    compositionRule.setOperation(compositionOperation);

                                    if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                                        costCompositionRules.addCompositionRule(compositionRule);
                                    }

                                }

                            } else {
                                log.warn("Metric {} not found in element {}", new Object[]{element.getCostMetric().getName(), monitoredElement.getName()});
                            }
                        }
                    }
                }

            }
        }

        //here I need to create rules that aggregate cost from the element's children and create cost metric at a higher level
        {
            final List<CompositionRule> childrenCostCompositionRules = Collections.synchronizedList(new ArrayList<CompositionRule>());
            //call recursively on all children the compute cost method

            ExecutorService es = Executors.newCachedThreadPool();
            List<Callable<Object>> todo = new ArrayList<>();

            for (final MonitoredElement child : monitoredElement.getContainedElements()) {

                Callable c = Executors.callable(new Runnable() {

                    @Override
                    public void run() {
                        CompositionRulesBlock childRules = createCompositionRulesForInstantUsageCost(cloudOfferedServices, child, totalUsageSoFar, currentTimesnapshot);
                        //do not add duplicate rules
                        //add cost rate rules
                        for (CompositionRule childRule : childRules.getCompositionRules()) {
                            if (!childrenCostCompositionRules.contains(childRule)) {
                                childrenCostCompositionRules.add(childRule);
                            }
                        }

                    }
                });
                todo.add(c);
            }
            try {
                List<Future<Object>> answers = es.invokeAll(todo);
            } catch (InterruptedException ex) {
                log.error(ex.getMessage(), ex);
            }

            {
                costCompositionRules.getCompositionRules().addAll(childrenCostCompositionRules);

            }

            if (!monitoredElement.getContainedElements().isEmpty()) {

                {
                    CompositionRule children_cost_rule = new CompositionRule();
                    children_cost_rule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                    children_cost_rule.addTargetMonitoredElementIDS(monitoredElement.getId());
                    children_cost_rule.setResultingMetric(new Metric("children_cost", "costUnits", Metric.MetricType.COST));

                    //we sum up each of the metrics from the children                
                    //one big  SUM operation
                    {
                        CompositionOperation sumOperation = new CompositionOperation();
                        sumOperation.setOperationType(CompositionOperationType.SUM);
                        children_cost_rule.setOperation(sumOperation);

                        for (MonitoredElement element : monitoredElement.getContainedElements()) {

                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(element.getLevel());
                            compositionOperation.addMetricSourceMonitoredElementID(element.getId());
                            compositionOperation.setTargetMetric(ELEMENT_COST_METRIC);
                            compositionOperation.setOperationType(CompositionOperationType.KEEP);
                            sumOperation.addCompositionOperation(compositionOperation);

                        }
                        costCompositionRules.addCompositionRule(children_cost_rule);
                    }
                }

            }

            //compute instant cost for element
            {
                CompositionRule element_cost_rule = new CompositionRule();
                element_cost_rule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                element_cost_rule.addTargetMonitoredElementIDS(monitoredElement.getId());
                element_cost_rule.setResultingMetric(new Metric("element_cost", "costUnits", Metric.MetricType.COST));

                //we sum up each of the metrics from the children                
                //one big  SUM operation
                {
                    CompositionOperation sumOperation = new CompositionOperation();
                    sumOperation.setOperationType(CompositionOperationType.SUM);
                    element_cost_rule.setOperation(sumOperation);

                    for (CompositionRule rule : costCompositionRules.getCompositionRules()) {

                        // only its rules, not also the rules from the children
                        // the issue is that I recursivelyc reate a list, not a tree  of rules, and the
                        // list contains all rules for all the subtree of this element
                        if (rule.getTargetMonitoredElementIDs().contains(monitoredElement.getId())) {

                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(rule.getTargetMonitoredElementLevel());
                            compositionOperation.setTargetMetric(rule.getResultingMetric());
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            sumOperation.addCompositionOperation(compositionOperation);
                        }

                    }
                    //if we have no children or metrics which we add to the rule, do not create the rule
                    if (!sumOperation.getSubOperations().isEmpty()) {
                        costCompositionRules.addCompositionRule(element_cost_rule);
                    }
                }

            }

        }

        return costCompositionRules;
    }

    public CostEvalEngine withInstantMonitoringDataEnrichmentEngine(final DataAggregationEngine instantMonitoringDataEnrichmentEngine) {
        this.instantMonitoringDataEnrichmentEngine = instantMonitoringDataEnrichmentEngine;
        return this;

    }

    public class SnapshotEnrichmentReport {

        private ServiceMonitoringSnapshot monitoringSnapshot;
        private CompositionRulesBlock compositionRulesBlock;

        public ServiceMonitoringSnapshot getMonitoringSnapshot() {
            return monitoringSnapshot;
        }

        public CompositionRulesBlock getCompositionRulesBlock() {
            return compositionRulesBlock;
        }

        public SnapshotEnrichmentReport(ServiceMonitoringSnapshot monitoringSnapshot, CompositionRulesBlock compositionRulesBlock) {
            this.monitoringSnapshot = monitoringSnapshot;
            this.compositionRulesBlock = compositionRulesBlock;
        }

    }

}
