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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class CostEvalEngine {

    @Autowired
    private DataAggregationEngine instantMonitoringDataEnrichmentEngine;

    static final Logger log = LoggerFactory.getLogger(CostEvalEngine.class);

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
    public ServiceMonitoringSnapshot getTotalCost(List<ServiceUnit> cloudOfferedServices, List<ServiceMonitoringSnapshot> monData) {

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
    public ServiceMonitoringSnapshot getLastMonSnapshotEnrichedWithCost(List<ServiceUnit> cloudOfferedServices, List<ServiceMonitoringSnapshot> monData) {

        //udpates monData in place
        CompositionRulesConfiguration cfg = createAndApplyCompositionRulesForComputingCost(cloudOfferedServices, monData);

        return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(cfg, monData.get(monData.size() - 1));

    }

    public ServiceMonitoringSnapshot enrichMonSnapshotWithInstantCostPerUsage(List<ServiceUnit> cloudOfferedServices, ServiceMonitoringSnapshot monitoringSnapshot) {

        //updates monData in place
        CompositionRulesConfiguration compositionRulesConfiguration = createCompositionRulesForCostPerUsage(cloudOfferedServices, monitoringSnapshot.getMonitoredService());

        Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> metricFilters = new HashMap<>();

        // set metric filters on data access
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
        monitoringSnapshot.applyMetricFilters(metricFilters);

        return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, monitoringSnapshot);

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
    public CompositionRulesConfiguration createAndApplyCompositionRulesForComputingCost(List<ServiceUnit> cloudOfferedServices, List<ServiceMonitoringSnapshot> monData) {

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

                    for (UsedCloudOfferedService service : monitoredElement.getCloudOfferedServices()) {
                        //get service cost scheme
                        List<CostFunction> costFunctions = null;
                        for (ServiceUnit su : cloudOfferedServices) {

                            //Services are compared using their NAME, Maybe not good. Can also compare using ID
                            if ((su.getName() != null && su.getName().equals(service.getName())) || (su.getId() != null && su.getId().equals(service.getId()))) {
                                costFunctions = su.getCostFunctions();
                                break;
                            }

                        }

                        if (costFunctions == null) {
                            log.warn("UsedCloudOfferedService with ID {} not found in cloud offered services", service.getId());
                        } else {

                            //from the cost functions, we extratc those that should be applied.
                            //maybe some do not quality to be apply as the service does not fulfill application requirements
                            List<CostFunction> costFunctionsToApply = new ArrayList<CostFunction>();

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
                                    Map<Metric, MetricValue> serviceResourceProperties = service.getResourceProperties();
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
                                    Map<Metric, MetricValue> serviceQualityProperties = service.getQualityProperties();
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
                        //apply cost functions

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

    public CompositionRulesConfiguration createCompositionRulesForCostPerPeriod(final List<ServiceUnit> cloudOfferedServices, final MonitoredElement monitoredElement) {
        log.debug("CompositionRulesConfiguration createCompositionRulesForCostPerPeriod not implemented yet");
        return new CompositionRulesConfiguration();
    }

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
     * @return
     */
    public CompositionRulesConfiguration createCompositionRulesForCostPerUsage(final List<ServiceUnit> cloudOfferedServices, final MonitoredElement monitoredElement) {

        CompositionRulesConfiguration compositionRulesConfiguration = new CompositionRulesConfiguration();
        CompositionRulesBlock instantCostCompositionRules = compositionRulesConfiguration.getMetricCompositionRules();
        CompositionRulesBlock historicalCostCompositionRules = compositionRulesConfiguration.getHistoricMetricCompositionRules();

        {
            for (UsedCloudOfferedService service : monitoredElement.getCloudOfferedServices()) {
                //get service cost scheme
                List<CostFunction> costFunctions = null;
                for (ServiceUnit su : cloudOfferedServices) {

                    //Services are compared using their NAME, Maybe not good. Can also compare using ID
                    if ((su.getName() != null && su.getName().equals(service.getName())) || (su.getId() != null && su.getId().equals(service.getId()))) {
                        costFunctions = su.getCostFunctions();
                        break;
                    }

                }

                if (costFunctions == null) {
                    log.warn("UsedCloudOfferedService with ID {} not found in cloud offered services", service.getId());
                } else {

                    //from the cost functions, we extratc those that should be applied.
                    //maybe some do not quality to be apply as the service does not fulfill application requirements
                    List<CostFunction> costFunctionsToApply = new ArrayList<CostFunction>();

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
                            Map<Metric, MetricValue> serviceResourceProperties = service.getResourceProperties();
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
                            Map<Metric, MetricValue> serviceQualityProperties = service.getQualityProperties();
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
                            //apply cost functions

                    //start with USAGE type of cost, easier to apply. 
                    for (CostFunction cf : costFunctionsToApply) {
                        for (CostElement element : cf.getCostElements()) {
                            if (element.getType().equals(CostElement.Type.USAGE)) {
                                //instant snapshot composition rule 
                                {
                                    CompositionRule compositionRule = new CompositionRule();
                                    compositionRule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                                    String timePeriod = "s";

                                    if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                        timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                    }
                                    compositionRule.setResultingMetric(new Metric("instant_cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST));
                                    CompositionOperation compositionOperation = new CompositionOperation();
                                    compositionOperation.setMetricSourceMonitoredElementLevel(monitoredElement.getLevel());
                                    compositionOperation.setTargetMetric(element.getCostMetric());
                                    compositionOperation.setOperationType(CompositionOperationType.SUM);
                                    compositionRule.setOperation(compositionOperation);

                                    if (!instantCostCompositionRules.getCompositionRules().contains(compositionRule)) {
                                        instantCostCompositionRules.addCompositionRule(compositionRule);
                                    }
                                }

                                //historical composition rule
                                {
                                    CompositionRule compositionRule = new CompositionRule();
                                    compositionRule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                                    String timePeriod = "s";
                                    if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                        timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                    }

                                    compositionRule.setResultingMetric(new Metric("total_cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST));
                                    CompositionOperation compositionOperation = new CompositionOperation();
                                    compositionOperation.setMetricSourceMonitoredElementLevel(monitoredElement.getLevel());
                                    compositionOperation.setTargetMetric(new Metric("cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST));
                                    compositionOperation.setOperationType(CompositionOperationType.SUM);
                                    compositionRule.setOperation(compositionOperation);

                                    if (!historicalCostCompositionRules.getCompositionRules().contains(compositionRule)) {
                                        historicalCostCompositionRules.addCompositionRule(compositionRule);
                                    }
                                }

                            } else if (element.getType().equals(CostElement.Type.PERIODIC)) {
                                //not addressed by this methid, as it requires historical knowledge, and is slower to compute
                                //so we have provided a seperate method for creating those rules and applying them
                            }
                        }
                    }
                }

            }
        }

        //here I need to create rules that aggregate cost from the element's chidren and create cost metric at a higher level
        {
            final List<CompositionRule> childrenCostCompositionRules = Collections.synchronizedList(new ArrayList<CompositionRule>());
            final List<CompositionRule> childrenHistoricalCostCompositionRules = Collections.synchronizedList(new ArrayList<CompositionRule>());
            //call recursively on all children the compute cost method

            ExecutorService es = Executors.newCachedThreadPool();
            List<Callable<Object>> todo = new ArrayList<>();

            for (final MonitoredElement child : monitoredElement.getContainedElements()) {

                Callable c = Executors.callable(new Runnable() {

                    @Override
                    public void run() {
                        CompositionRulesConfiguration childRules = createCompositionRulesForCostPerUsage(cloudOfferedServices, child);
                        //do not add duplicate rules
                        //add cost rate rules
                        for (CompositionRule childRule : childRules.getMetricCompositionRules().getCompositionRules()) {
                            if (!childrenCostCompositionRules.contains(childRule)) {
                                childrenCostCompositionRules.add(childRule);
                            }
                        }
                        //add total cost rules
                        for (CompositionRule childRule : childRules.getHistoricMetricCompositionRules().getCompositionRules()) {
                            if (!childrenHistoricalCostCompositionRules.contains(childRule)) {
                                childrenHistoricalCostCompositionRules.add(childRule);
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
                instantCostCompositionRules.getCompositionRules().addAll(childrenCostCompositionRules);
                historicalCostCompositionRules.getCompositionRules().addAll(childrenHistoricalCostCompositionRules);
            }

            if (!monitoredElement.getContainedElements().isEmpty()) {
                CompositionRule children_cost_rule = new CompositionRule();
                children_cost_rule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                children_cost_rule.setResultingMetric(new Metric("instant_children_cost", "costUnits", Metric.MetricType.COST));

                //we sum up each of the metrics from the children                
                //one big  SUM operation
                {
                    CompositionOperation sumOperation = new CompositionOperation();
                    sumOperation.setOperationType(CompositionOperationType.SUM);
                    children_cost_rule.setOperation(sumOperation);

                    for (CompositionRule childRule : childrenCostCompositionRules) {

                        CompositionOperation compositionOperation = new CompositionOperation();
                        compositionOperation.setMetricSourceMonitoredElementLevel(childRule.getTargetMonitoredElementLevel());
                        compositionOperation.setTargetMetric(childRule.getResultingMetric());
                        compositionOperation.setOperationType(CompositionOperationType.SUM);
                        sumOperation.addCompositionOperation(compositionOperation);

                    }
                }
                instantCostCompositionRules.addCompositionRule(children_cost_rule);

                //compute instant cost for element
                {
                    CompositionRule instant_element_cost_rule = new CompositionRule();
                    instant_element_cost_rule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                    instant_element_cost_rule.setResultingMetric(new Metric("instant_element_cost", "costUnits", Metric.MetricType.COST));

                    //we sum up each of the metrics from the children                
                    //one big  SUM operation
                    {
                        CompositionOperation sumOperation = new CompositionOperation();
                        sumOperation.setOperationType(CompositionOperationType.SUM);
                        instant_element_cost_rule.setOperation(sumOperation);

                        for (CompositionRule rule : instantCostCompositionRules.getCompositionRules()) {

                            CompositionOperation compositionOperation = new CompositionOperation();
                            compositionOperation.setMetricSourceMonitoredElementLevel(rule.getTargetMonitoredElementLevel());
                            compositionOperation.setTargetMetric(rule.getResultingMetric());
                            compositionOperation.setOperationType(CompositionOperationType.SUM);
                            sumOperation.addCompositionOperation(compositionOperation);

                        }
                    }
                    instantCostCompositionRules.addCompositionRule(instant_element_cost_rule);

                }
            }

            {
                CompositionRule children_cost_rule = new CompositionRule();
                children_cost_rule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                children_cost_rule.setResultingMetric(new Metric("total_children_cost", "costUnits", Metric.MetricType.COST));

                //we sum up each of the metrics from the children                
                //one big  SUM operation
                {
                    CompositionOperation sumOperation = new CompositionOperation();
                    sumOperation.setOperationType(CompositionOperationType.SUM);
                    children_cost_rule.setOperation(sumOperation);

                    for (CompositionRule childRule : childrenHistoricalCostCompositionRules) {

                        CompositionOperation compositionOperation = new CompositionOperation();
                        compositionOperation.setMetricSourceMonitoredElementLevel(childRule.getTargetMonitoredElementLevel());
                        compositionOperation.setTargetMetric(childRule.getResultingMetric());
                        compositionOperation.setOperationType(CompositionOperationType.SUM);
                        sumOperation.addCompositionOperation(compositionOperation);

                    }
                }
                historicalCostCompositionRules.addCompositionRule(children_cost_rule);
                {
                    CompositionRule elementTotalCostRule = new CompositionRule();
                    elementTotalCostRule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                    elementTotalCostRule.setResultingMetric(new Metric("total_element_cost", "costUnits", Metric.MetricType.COST));

                    //we sum up each of the metrics from the children                
                    //one big  SUM operation
                    {
                        CompositionOperation sumOperation = new CompositionOperation();
                        sumOperation.setOperationType(CompositionOperationType.SUM);
                        elementTotalCostRule.setOperation(sumOperation);

                        {
                            for (CompositionRule rule : historicalCostCompositionRules.getCompositionRules()) {

                                CompositionOperation compositionOperation = new CompositionOperation();
                                compositionOperation.setMetricSourceMonitoredElementLevel(rule.getTargetMonitoredElementLevel());
                                compositionOperation.setTargetMetric(rule.getResultingMetric());
                                compositionOperation.setOperationType(CompositionOperationType.SUM);
                                sumOperation.addCompositionOperation(compositionOperation);

                            }
                        }
                        historicalCostCompositionRules.addCompositionRule(elementTotalCostRule);

                    }

                }

            }

        }

        return compositionRulesConfiguration;

    }

}
