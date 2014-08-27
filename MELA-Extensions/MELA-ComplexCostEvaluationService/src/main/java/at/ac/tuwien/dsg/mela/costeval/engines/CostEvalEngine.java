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
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Relationship;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import com.google.common.util.concurrent.Monitor;

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
    public ServiceMonitoringSnapshot enrichWithCost(List<ServiceUnit> cloudOfferedServices, List<ServiceMonitoringSnapshot> monData) {

        //udpates monData in place
        CompositionRulesConfiguration compositionRulesConfiguration = enrichMonSnapshotsWithCost(cloudOfferedServices, monData);

        for (ServiceMonitoringSnapshot monitoringSnapshot : monData) {
            //compute cost for each service unit, topology and service for each snapshot
            monitoringSnapshot = instantMonitoringDataEnrichmentEngine.enrichMonitoringData(
                    compositionRulesConfiguration, monitoringSnapshot);
        }

        ServiceMonitoringSnapshot complete = instantMonitoringDataEnrichmentEngine.aggregateMonitoringDataOverTime(compositionRulesConfiguration, monData);
        complete.setTimestamp(monData.get(monData.size() - 1).getTimestamp());
        complete.setTimestampID(monData.get(monData.size() - 1).getTimestampID());

        return complete;
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
        CompositionRulesConfiguration cfg = enrichMonSnapshotsWithCost(cloudOfferedServices, monData);

        return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(cfg, monData.get(monData.size() - 1));

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
    public CompositionRulesConfiguration enrichMonSnapshotsWithCost(List<ServiceUnit> cloudOfferedServices, List<ServiceMonitoringSnapshot> monData) {

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

                                                Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnit", Metric.MetricType.COST);

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
                                                    Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnit", Metric.MetricType.COST);

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
                                                    Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnit", Metric.MetricType.COST);

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

                                            Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnit", Metric.MetricType.COST);

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
                        CompositionRule compositionRule = new CompositionRule();
                        compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                        compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                        CompositionOperation compositionOperation = new CompositionOperation();
                        compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.VM);
                        compositionOperation.setTargetMetric(created);
                        compositionOperation.setOperationType(CompositionOperationType.SUM);
                        compositionRule.setOperation(compositionOperation);

                        costCompositionRules.addCompositionRule(compositionRule);
                        historicalCostCompositionRules.addCompositionRule(compositionRule);

                    }
                    //service topology rules
                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM) || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                        CompositionRule compositionRule = new CompositionRule();
                        compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                        compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                        CompositionOperation compositionOperation = new CompositionOperation();
                        compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                        compositionOperation.setTargetMetric(created);
                        compositionOperation.setOperationType(CompositionOperationType.SUM);
                        compositionRule.setOperation(compositionOperation);

                        costCompositionRules.addCompositionRule(compositionRule);
                        historicalCostCompositionRules.addCompositionRule(compositionRule);

                    }
                    //service rules
                    if (level.equals(MonitoredElement.MonitoredElementLevel.VM) || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                            || level.equals(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)) {
                        CompositionRule compositionRule = new CompositionRule();
                        compositionRule.setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
                        compositionRule.setResultingMetric(new Metric(created.getName(), created.getMeasurementUnit(), Metric.MetricType.COST));
                        CompositionOperation compositionOperation = new CompositionOperation();
                        compositionOperation.setMetricSourceMonitoredElementLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                        compositionOperation.setTargetMetric(created);
                        compositionOperation.setOperationType(CompositionOperationType.SUM);
                        compositionRule.setOperation(compositionOperation);

                        costCompositionRules.addCompositionRule(compositionRule);
                        historicalCostCompositionRules.addCompositionRule(compositionRule);

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

            costCompositionRules.addCompositionRule(compositionRule);
            historicalCostCompositionRules.addCompositionRule(compositionRule);

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

            costCompositionRules.addCompositionRule(compositionRule);
            historicalCostCompositionRules.addCompositionRule(compositionRule);

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

            costCompositionRules.addCompositionRule(compositionRule);
            historicalCostCompositionRules.addCompositionRule(compositionRule);

        }

        CompositionRulesConfiguration compositionRulesConfiguration = new CompositionRulesConfiguration();
        compositionRulesConfiguration.setHistoricDataAggregationRules(historicalCostCompositionRules);
        compositionRulesConfiguration.setMetricCompositionRules(costCompositionRules);

        return compositionRulesConfiguration;

    }

}
