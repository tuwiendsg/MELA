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
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
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

    public ServiceMonitoringSnapshot applyCompositionRules(CompositionRulesBlock block, ServiceMonitoringSnapshot monitoringSnapshot) {
        return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(
                new CompositionRulesConfiguration().withMetricCompositionRules(block), monitoringSnapshot);
    }

    public void setInstantMonitoringDataEnrichmentEngine(DataAggregationEngine instantMonitoringDataEnrichmentEngine) {
        this.instantMonitoringDataEnrichmentEngine = instantMonitoringDataEnrichmentEngine;
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

    public SnapshotEnrichmentReport enrichMonSnapshotWithInstantUsageCost(List<CloudProvider> cloudOfferedServices, ServiceMonitoringSnapshot monitoringSnapshot, CostEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

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
    public ServiceMonitoringSnapshot updateTotalUsageSoFar(Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices, CostEnrichedSnapshot previouselyDeterminedUsage, ServiceMonitoringSnapshot newMonData) {

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

                        if (previouselyDeterminedUsage.getSnapshot().contains(level, monitoredElement)) {
                            vmUsageSoFar = previouselyDeterminedUsage.getSnapshot().getMonitoredData(monitoredElement).getMonitoredData();
                        } else {
                            vmUsageSoFar = new HashMap<>();
                            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(monitoredElement, vmUsageSoFar);
                            previouselyDeterminedUsage.getSnapshot().addMonitoredData(elementMonitoringSnapshot);
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

        return previouselyDeterminedUsage.getSnapshot();
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

    private static final Metric ELEMENT_COST_METRIC = new Metric("element_cost", "costUnits", Metric.MetricType.COST);
    private static final Metric CHILDREN_COST_METRIC = new Metric("children_cost", "costUnits", Metric.MetricType.COST);

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
     * @param cloudOfferedServices cloud procing schemes
     * @param monitoredElement current monitoring snapshot on which we compute
     * cost
     * @param totalUsageSoFar total usage so far with current structure. used in
     * determining instant applicable cost pricing scheme according to cost
     * intervals
     * @param currentTimesnapshot string representation of the Long timestamp of
     * the current monitoring snapshot. Used to determine lifetime so far
     * @return
     */
    public CompositionRulesBlock createCompositionRulesForInstantUsageCost(final Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices,
            final MonitoredElement monitoredElement, final CostEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

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
                        MonitoredElementMonitoringSnapshot vmMonSnapshot = totalUsageSoFar.getSnapshot().getMonitoredData(monitoredElement);

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

                                    Long costPeriodsFromCreation = 0l;

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
                    children_cost_rule.setResultingMetric(CHILDREN_COST_METRIC);

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
                element_cost_rule.setResultingMetric(ELEMENT_COST_METRIC);

                //we sum up each of the metrics from the children                
                //one big  SUM operation
                {
                    CompositionOperation sumOperation = new CompositionOperation();
                    sumOperation.setOperationType(CompositionOperationType.SUM);
                    element_cost_rule.setOperation(sumOperation);

                    for (CompositionRule rule : costCompositionRules.getCompositionRules()) {

                        // only its rules, not also the rules from the children
                        // the issue is that I recursively create a list, not a tree  of rules, and the
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

    public CompositionRulesBlock createCompositionRulesForTotalCost(final Map<UUID, Map<UUID, ServiceUnit>> cloudOfferedServices,
            final CostEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

        List<MonitoredElement.MonitoredElementLevel> levelsInOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.VM);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);

        CompositionRulesBlock costCompositionRules = new CompositionRulesBlock();

        ServiceMonitoringSnapshot monitoringSnapshot = totalUsageSoFar.getSnapshot();

        for (MonitoredElement.MonitoredElementLevel level : levelsInOrder) {

            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> vmsData = totalUsageSoFar.getSnapshot().getMonitoredData(level);

            if (vmsData == null) {
                log.error("No monitoring data for service" + monitoringSnapshot.getMonitoredService() + " at level " + level.toString() + " timestamp " + monitoringSnapshot.getTimestampID());
                continue;
            }

            for (MonitoredElement monitoredElement : vmsData.keySet()) {

                Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

                {
                    for (UsedCloudOfferedService service : monitoredElement.getCloudOfferedServices()) {
                        {

                            //from the cost functions, we extract those that should be applied.
                            //maybe some do not quality to be apply as the service does not fulfill application requirements
                            List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(service);

                            //start with USAGE type of cost, easier to apply. 
                            for (CostFunction cf : costFunctionsToApply) {
                                MonitoredElementMonitoringSnapshot vmMonSnapshot = totalUsageSoFar.getSnapshot().getMonitoredData(monitoredElement);

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
                                                //"total_" must be there, as currnetly hashcode on Metric is only on "name", so if I have allready the instant cost,
                                                // and I put again this total cost metric with same name, will not replace it.
                                                //TODO: address this
                                                compositionRule.setResultingMetric(new Metric("total_cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST));
                                                CompositionOperation compositionOperation = new CompositionOperation();
                                                compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);

                                                //we need to go trough all cost element interval, and apply correct cost for each interval
                                                MetricValue metricUsageSoFar = value.clone();
                                                MetricValue costForValue = new MetricValue(0l);
                                                Map<MetricValue, Double> costIntervalFunction = element.getCostIntervalFunction();

                                                List<MetricValue> costIntervalsInAscendingOrder = element.getCostIntervalsInAscendingOrder();
                                                for (int i = 0; i < costIntervalsInAscendingOrder.size(); i++) {

                                                    MetricValue costIntervalElement = costIntervalsInAscendingOrder.get(i);

                                                    if (costIntervalElement.compareTo(metricUsageSoFar) > 0) {
                                                        MetricValue costForThisInterval = metricUsageSoFar.clone();
                                                        costForThisInterval.multiply(costIntervalFunction.get(costIntervalElement));
                                                        costForValue.sum(costForThisInterval);
                                                        break;
                                                    } else {
                                                        Double usageBetweenLastAndCurrentInterval = null;
                                                        if (i > 0) {
                                                            MetricValue tmp = costIntervalElement.clone();
                                                            tmp.sub(costIntervalsInAscendingOrder.get(i - 1));
                                                            usageBetweenLastAndCurrentInterval = ((Number) tmp.getValue()).doubleValue();
                                                        } else {
                                                            usageBetweenLastAndCurrentInterval = ((Number) costIntervalElement.getValue()).doubleValue();
                                                        }

                                                        metricUsageSoFar.sub(usageBetweenLastAndCurrentInterval);
                                                        MetricValue costForThisInterval = new MetricValue(usageBetweenLastAndCurrentInterval);
                                                        costForThisInterval.multiply(costIntervalFunction.get(costIntervalElement));
                                                        costForValue.sum(costForThisInterval);
                                                    }
                                                }

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

                                            Long costPeriodsFromCreation = 0l;

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

                                            //we need to go trough all cost element interval, and apply correct cost for each interval
                                            MetricValue metricUsageSoFar = new MetricValue(costPeriodsFromCreation);
                                            MetricValue costForValue = new MetricValue(0l);
                                            Map<MetricValue, Double> costIntervalFunction = element.getCostIntervalFunction();

                                            List<MetricValue> costIntervalsInAscendingOrder = element.getCostIntervalsInAscendingOrder();
                                            for (int i = 0; i < costIntervalsInAscendingOrder.size(); i++) {

                                                MetricValue costIntervalElement = costIntervalsInAscendingOrder.get(i);

                                                if (costIntervalElement.compareTo(metricUsageSoFar) > 0) {
                                                    MetricValue costForThisInterval = metricUsageSoFar.clone();
                                                    costForThisInterval.multiply(costIntervalFunction.get(costIntervalElement));
                                                    costForValue.sum(costForThisInterval);
                                                    break;
                                                } else {
                                                    Double usageBetweenLastAndCurrentInterval = null;
                                                    if (i > 0) {
                                                        MetricValue tmp = costIntervalElement.clone();
                                                        tmp.sub(costIntervalsInAscendingOrder.get(i - 1));
                                                        usageBetweenLastAndCurrentInterval = ((Number) tmp.getValue()).doubleValue();
                                                    } else {
                                                        usageBetweenLastAndCurrentInterval = ((Number) costIntervalElement.getValue()).doubleValue();
                                                    }

                                                    metricUsageSoFar.sub(usageBetweenLastAndCurrentInterval);
                                                    MetricValue costForThisInterval = new MetricValue(usageBetweenLastAndCurrentInterval);
                                                    costForThisInterval.multiply(costIntervalFunction.get(costIntervalElement));
                                                    costForValue.sum(costForThisInterval);
                                                }
                                            }

                                            //"total_" must be there, as currnetly hashcode on Metric is only on "name", so if I have allready the instant cost,
                                            // and I put again this total cost metric with same name, will not replace it.
                                            //TODO: address this
                                            Metric cost = new Metric("total_cost_" + element.getCostMetric().getName() + "_for_" + service.getName(), "costUnits", Metric.MetricType.COST);

                                            CompositionRule compositionRule = new CompositionRule();
                                            compositionRule.setTargetMonitoredElementLevel(monitoredElement.getLevel());
                                            compositionRule.addTargetMonitoredElementIDS(monitoredElement.getId());

                                            compositionRule.setResultingMetric(cost);
                                            CompositionOperation compositionOperation = new CompositionOperation();

                                            compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
                                            compositionOperation.setValue(costForValue.getValueRepresentation());
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
//                            children_cost_rule.addTargetMonitoredElementIDS(monitoredElement.getId());
                            children_cost_rule.setResultingMetric(CHILDREN_COST_METRIC);

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
                        element_cost_rule.setResultingMetric(ELEMENT_COST_METRIC);

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
