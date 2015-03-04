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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
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
import at.ac.tuwien.dsg.mela.costeval.model.LifetimeEnrichedSnapshot;
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
                new CompositionRulesConfiguration().withMetricCompositionRules(block), monitoringSnapshot.clone());
    }

    public void setInstantMonitoringDataEnrichmentEngine(DataAggregationEngine instantMonitoringDataEnrichmentEngine) {
        this.instantMonitoringDataEnrichmentEngine = instantMonitoringDataEnrichmentEngine;
    }

    public Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersToMap(List<CloudProvider> cloudProviders) {

        Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices = new HashMap<UUID, Map<UUID, CloudOfferedService>>();

        for (CloudProvider cloudProvider : cloudProviders) {
            Map<UUID, CloudOfferedService> cloudUnits = new HashMap<UUID, CloudOfferedService>();

            cloudOfferedServices.put(cloudProvider.getUuid(), cloudUnits);

            for (CloudOfferedService unit : cloudProvider.getCloudOfferedServices()) {
                cloudUnits.put(unit.getUuid(), unit);
            }

        }

        return cloudOfferedServices;
    }

    public SnapshotEnrichmentReport enrichMonSnapshotWithInstantUsageCost(List<CloudProvider> cloudOfferedServices, ServiceMonitoringSnapshot monitoringSnapshot, LifetimeEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

        Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServicesMap = cloudProvidersToMap(cloudOfferedServices);

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

//    /**
//     * 1. Check what cost functions are applicable 2. SUMS up the metrics
//     * targeted by the cost functions, between the previously monitored ones and
//     * the new monitored snapshot !!!! NOTE !!! It works directly on the
//     * supplied previouselyDeterminedUsage
//     *
//     * @param cloudOfferedServices
//     * @param previouselyDeterminedUsage
//     * @param newMonData
//     * @return
//     */
//    public ServiceMonitoringSnapshot updateTotalUsageSoFarWithCurrentStructure(Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices, CostEnrichedSnapshot previouselyDeterminedUsage, ServiceMonitoringSnapshot newMonData) {
//
//        if (newMonData == null) {
//            return new ServiceMonitoringSnapshot();
//        }
//
//        //so, we have 2 types of cost, PERIODIC, and PER USAGE
//        //usually per USAGE is payed according to some interval, such as free first GB, rest 0.12, etc
//        //thus, for each USAGE cost metric, we compute for each snapshot its usage so far, and insert in the snapshot the instant cost rate
////        Map<MonitoredElement, Map<Metric, MetricValue>> usageSoFar = new ConcurrentHashMap<MonitoredElement, Map<Metric, MetricValue>>();
//        //fill this up with old data from old mon snapshot
//        //holds timestamp in which each mon element appears in the service
//        //        Map<MonitoredElement, Long> cloudOfferedServiceInstantiationTimes = new ConcurrentHashMap<MonitoredElement, Long>();
//        List<MonitoredElement.MonitoredElementLevel> levelsInOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.VM);
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);
//
//        ServiceMonitoringSnapshot monitoringSnapshot = newMonData;
//
//        for (MonitoredElement.MonitoredElementLevel level : levelsInOrder) {
//
//            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> vmsData = monitoringSnapshot.getMonitoredData(level);
//
//            if (vmsData == null) {
//                log.error("No monitoring data for service" + monitoringSnapshot.getMonitoredService() + " at level " + level.toString() + " timestamp " + monitoringSnapshot.getTimestampID());
//                continue;
//            }
//
//            for (MonitoredElement monitoredElement : vmsData.keySet()) {
//
//                //if just appeared, add monitored element VM in the instatiationTimes
//                //update used cloud offered services, i.e., add newly added ones, and remove deleted ones
//                List<UsedCloudOfferedService> monitoredElementUsedServices = monitoredElement.getCloudOfferedServices();
//
//                //remove deallocated services
//                Iterator<UsedCloudOfferedService> it = previouselyDeterminedUsage.getInstantiationTimes(monitoredElement).keySet().iterator();
//                while (it.hasNext()) {
//                    UsedCloudOfferedService cloudOfferedService = it.next();
//                    //if used service not used anymore, remove it from the map
//                    if (!monitoredElementUsedServices.contains(cloudOfferedService)) {
//                        it.remove();
//                    }
//
//                }
//
//                //add newly allocated services
//                for (UsedCloudOfferedService ucos : monitoredElementUsedServices) {
//                    if (!previouselyDeterminedUsage.getInstantiationTimes(monitoredElement).containsKey(ucos)) {
//                        previouselyDeterminedUsage.withInstantiationTime(monitoredElement, ucos, Long.parseLong(monitoringSnapshot.getTimestamp()));
//                    }
//                }
//
//                Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);
//
//                for (UsedCloudOfferedService usedCloudService : monitoredElement.getCloudOfferedServices()) {
//                    {
//
//                        //from the cost functions, we extract those that should be applied.
//                        //maybe some do not quality to be apply as the service does not fulfill application requirements
//                        List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(usedCloudService);
//
//                        Map<Metric, MetricValue> vmUsageSoFar = null;
//
//                        if (previouselyDeterminedUsage.getSnapshot().contains(level, monitoredElement)) {
//                            vmUsageSoFar = previouselyDeterminedUsage.getSnapshot().getMonitoredData(monitoredElement).getMonitoredData();
//                        } else {
//                            vmUsageSoFar = new HashMap<>();
//                            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(monitoredElement, vmUsageSoFar);
//                            previouselyDeterminedUsage.getSnapshot().addMonitoredData(elementMonitoringSnapshot);
//                        }
//
//                        //apply cost functions
//                        //start with USAGE type of cost, easier to apply. 
//                        for (CostFunction cf : costFunctionsToApply) {
//                            for (CostElement element : cf.getCostElements()) {
//
//                                if (element.getType().equals(CostElement.Type.USAGE)) {
//                                    //if we just added the VM, so we do not have any historical usage so far
//                                    MonitoredElementMonitoringSnapshot vmMonSnapshot = vmsData.get(monitoredElement);
//
//                                    MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());
//                                    if (value != null) {
//
//                                        if (!vmUsageSoFar.containsKey(element.getCostMetric())) {
//                                            vmUsageSoFar.put(element.getCostMetric(), value.clone());
//
//                                        } else {
//                                            //else we need to sum up usage
//                                            MetricValue usageSoFarForMetric = vmUsageSoFar.get(element.getCostMetric());
//
//                                            //we should compute estimated usage over time not captured by monitoring points:
//                                            //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
//                                            //depending on the measurement unit of the measured metric
//                                            //I.E. a pkts_out/s will be multiplied with 60
//                                            //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
//                                            //this works as we assume we target only metrics which change in time using PER USAGE cost functions
//                                            String timePeriod = "s";
//
//                                            if (element.getCostMetric().getMeasurementUnit().contains("/")) {
//                                                timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
//                                            }
//
//                                            //check amount of time in millis between two measurement units
//                                            Long currentTimestamp = Long.parseLong(monitoringSnapshot.getTimestamp());
//                                            Long previousTimestamp = Long.parseLong(newMonData.getTimestamp());
//                                            Long timeIntervalInMillis = (currentTimestamp - previousTimestamp) / 1000;
//
//                                            //convert to seconds
//                                            Long periodsBetweenPrevAndCurrentTimestamp = 0l;
//
//                                            //must standardise these somehow
//                                            if (timePeriod.equals("s")) {
//                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis;
//                                            } else if (timePeriod.equals("m")) {
//                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 60;
//                                            } else if (timePeriod.equals("h")) {
//                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 3600;
//                                            } else if (timePeriod.equals("d")) {
//                                                periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 86400;
//                                            }
//
//                                            //if metric does not have period, than its a metric which ACCUMULATES, I.E., show summed up hsitorical usage by itself
//                                            if (periodsBetweenPrevAndCurrentTimestamp <= 1) {
//                                                usageSoFarForMetric.sum(value);
//
//                                            } else {
//                                                //if more than one period between recordings, need to compute usage for non-monitored periods
//                                                //we compute average for intermediary
//
//                                                MetricValue prevValue = vmUsageSoFar.get(element.getCostMetric());
//                                                MetricValue average = prevValue.clone();
//                                                prevValue.sum(value);
//                                                average.divide(2);
//                                                //add average for intermediary monitoring points
//                                                //TODO: add option for adding other plug-ins for filling up missing data, and computing accuracy of estimation
//                                                average.multiply(periodsBetweenPrevAndCurrentTimestamp.intValue());
//                                                //add monitored points
//                                                average.sum(prevValue);
//                                                average.sum(value);
//                                                usageSoFarForMetric.setValue(average.getValue());
//
//                                            }
//
//                                        }
//                                    } else {
//                                        log.error("Cost metric {} was not found on VM {}", element.getCostMetric().getName(), monitoredElement.getId());
//                                    }
//
//                                    //if cost is per usage, it is computed by instant monitoring cost.
////                                        continue;
//                                }
//                            }
//                        }
//                    }
//
//                }
//            }
//
//        }
//
//        return previouselyDeterminedUsage.getSnapshot();
//    }
    /**
     * This removes the unused services from the complete historical information
     *
     *
     * @param toClean
     * @return
     */
    public LifetimeEnrichedSnapshot cleanUnusedServices(LifetimeEnrichedSnapshot toClean) {

//        if (toClean == null) {
//            return new LifetimeEnrichedSnapshot();
//        }
//
//        LifetimeEnrichedSnapshot ces = toClean.clone();
//
//        //so, we have 2 types of cost, PERIODIC, and PER USAGE
//        //usually per USAGE is payed according to some interval, such as free first GB, rest 0.12, etc
//        //thus, for each USAGE cost metric, we compute for each snapshot its usage so far, and insert in the snapshot the instant cost rate
////        Map<MonitoredElement, Map<Metric, MetricValue>> usageSoFar = new ConcurrentHashMap<MonitoredElement, Map<Metric, MetricValue>>();
//        //fill this up with old data from old mon snapshot
//        //holds timestamp in which each mon element appears in the service
//        //        Map<MonitoredElement, Long> cloudOfferedServiceInstantiationTimes = new ConcurrentHashMap<MonitoredElement, Long>();
//        List<MonitoredElement.MonitoredElementLevel> levelsInOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.VM);
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);
//
//        for (MonitoredElement monitoredElement : ces.getSnapshot().getMonitoredService()) {
//
//            //if just appeared, add monitored element VM in the instatiationTimes
//            //update used cloud offered services, i.e., add newly added ones, and remove deleted ones
//            List<UsedCloudOfferedService> monitoredElementUsedServices = monitoredElement.getCloudOfferedServices();
//
//            //remove deallocated services
//            Iterator<UsedCloudOfferedService> it = ces.getDeallocationTimes(monitoredElement).keySet().iterator();
//            while (it.hasNext()) {
//                UsedCloudOfferedService cloudOfferedService = it.next();
//                //if used service not used anymore, remove it from the map
//                if (monitoredElementUsedServices.contains(cloudOfferedService)) {
//                    monitoredElementUsedServices.remove(cloudOfferedService);
//                }
//
//            }
//
//        }
//
//        return ces;
        return toClean;
    }

    public LifetimeEnrichedSnapshot updateTotalUsageSoFarWithCompleteStructureIncludingServicesASVMTypes(Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices, LifetimeEnrichedSnapshot previousUsage, ServiceMonitoringSnapshot currentMonData) {

        if (currentMonData == null) {
            return new LifetimeEnrichedSnapshot();
        }

        if (previousUsage == null) {
            LifetimeEnrichedSnapshot updatedSnapshot = new LifetimeEnrichedSnapshot()
                    .withSnapshot(convertToStructureIncludingServicesASVMTypes(cloudOfferedServices, currentMonData))
                    .withLastUpdatedTimestampID(currentMonData.getTimestampID());
            return updatedSnapshot;
        }

        LifetimeEnrichedSnapshot updatedSnapshot = new LifetimeEnrichedSnapshot()
                .withSnapshot(previousUsage.getSnapshot().clone())
                .withLastUpdatedTimestampID(currentMonData.getTimestampID());

        updatedSnapshot.withDeallocationTimes(previousUsage.getDeallocationTimes());
        updatedSnapshot.withInstantiationTimes(previousUsage.getInstantiationTimes());

        ServiceMonitoringSnapshot usageSoFarSnapshot = updatedSnapshot.getSnapshot();

        ServiceMonitoringSnapshot currentDataConverted = convertToStructureIncludingServicesASVMTypes(cloudOfferedServices, currentMonData);

        MonitoredElementMonitoringSnapshot previousUsageData = usageSoFarSnapshot.getMonitoredData(usageSoFarSnapshot.getMonitoredService());

        MonitoredElementMonitoringSnapshot currentUsageData = currentDataConverted.getMonitoredData(currentDataConverted.getMonitoredService());

        //I want to go trough each element
        //for each used cloud offered service
        //create a CLOUD_OFFERED_SERVICE and attach it to the element using the service
        //More0over, for all applicable cost functions, for each cost element
        //move the metrics from the element to the service
        //create composition rules that create cost for the offered service
        for (MonitoredElementMonitoringSnapshot snapshot : previousUsageData) {
            MonitoredElement element = snapshot.getMonitoredElement();

            //do not analyze elements I just added. 
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE)) {
                continue;
            }

            Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, element);

            //update usage
            for (UsedCloudOfferedService usedCloudService : element.getCloudOfferedServices()) {

                //not move relevant monitoring data
                List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(usedCloudService);

                for (CostFunction cf : costFunctionsToApply) {
                    for (CostElement costElement : cf.getCostElements()) {

                        if (costElement.getType().equals(CostElement.Type.USAGE)) {

                            MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                                    .withId(usedCloudService.getInstanceUUID().toString())
                                    .withName(usedCloudService.getName())
                                    .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
                            MonitoredElementMonitoringSnapshot usedServiceDataSoFar = usageSoFarSnapshot.getMonitoredData(usedCloudServiceMonitoredElement);
                            MonitoredElementMonitoringSnapshot usedServiceNewData = currentDataConverted.getMonitoredData(usedCloudServiceMonitoredElement);

                            //if usedServiceNewData does not contain data, than service no linger in use
                            if (usedServiceDataSoFar.getMonitoredData().containsKey(costElement.getCostMetric()) && usedServiceNewData.getMonitoredData().containsKey(costElement.getCostMetric())) {
                                MetricValue usedServiceDataSoFarMetricValue = usedServiceDataSoFar.getMonitoredData().get(costElement.getCostMetric());
                                MetricValue usedServiceNewDataMetricValue = usedServiceNewData.getMonitoredData().get(costElement.getCostMetric());

                                String timePeriod = "";

                                if (costElement.getCostMetric().getMeasurementUnit().contains("/")) {
                                    timePeriod = costElement.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                }

                                //check amount of time in millis between two measurement units
                                Long currentTimestamp = Long.parseLong(currentDataConverted.getTimestamp());

                                //check if monitoring has left some time intervals un-monitored
                                //if so, then fill up missing 
                                Long previousTimestamp = Long.parseLong(previousUsage.getSnapshot().getTimestamp());
                                Long timeIntervalInMillis = (currentTimestamp - previousTimestamp) / 1000;

                                //convert to seconds
                                Long periodsBetweenPrevAndCurrentTimestamp = 0l;

                                //if metric does not have period, than its a metric which ACCUMULATES, I.E., show summed up historical usage by itself
                                if (timePeriod.length() == 0) {
                                    usedServiceDataSoFarMetricValue.setValue(usedServiceNewDataMetricValue.getValue());
                                    usedServiceNewDataMetricValue.setFreshness(usedServiceNewDataMetricValue.getFreshness());
                                    usedServiceNewDataMetricValue.setTimeSinceCollection(usedServiceNewDataMetricValue.getTimeSinceCollection());
                                    usedServiceNewDataMetricValue.setTimeSinceCollection(usedServiceNewDataMetricValue.getTimeSinceCollection());
                                    continue;
                                } else if (timePeriod.equals("s")) {
                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis;
                                } else if (timePeriod.equals("m")) {
                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 60;
                                } else if (timePeriod.equals("h")) {
                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 3600;
                                } else if (timePeriod.equals("d")) {
                                    periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 86400;
                                }

                                if (periodsBetweenPrevAndCurrentTimestamp <= 1) {
                                    usedServiceDataSoFarMetricValue.sum(usedServiceNewDataMetricValue);

                                } else {
                                                //if more than one period between recordings, need to compute usage for non-monitored periods
                                    //we compute average for intermediary

                                    //get average between two last readings and use it to fill the gap of un-monitored data
                                    MetricValue average = usedServiceNewDataMetricValue.clone();
                                    average.sum(usedServiceNewDataMetricValue);
                                    average.divide(2);
                                    //add average for intermediary monitoring points
                                    //TODO: add option for adding other plug-ins for filling up missing data, and computing accuracy of estimation
                                    average.multiply(periodsBetweenPrevAndCurrentTimestamp.intValue());
                                    //add monitored points
                                    average.sum(usedServiceNewDataMetricValue);
                                    average.sum(usedServiceNewDataMetricValue);
                                    usedServiceDataSoFarMetricValue.setValue(average.getValue());

                                }
                            }

                        }//else we do not care, as we allready updated the instantiation times
                    }
                }

            }
        }

        //update structure
        //need to check if the same element in updated structure is also present in the same place in the old structure
        //if not, we need to move it
        //update structure
        MonitoredElement currentService = currentDataConverted.getMonitoredService();

        for (MonitoredElement elementInCurrentStructure : currentService) {
            //need to check if the same element in updated structure is also present in the same place in the old structure
            //if not, we need to move it
            //so we check to see if the elements have same children
            for (MonitoredElement elementInOldStructure : previousUsageData.getMonitoredElement()) {
                if (elementInCurrentStructure.equals(elementInOldStructure)) {
                    //check if they have same children
                    for (MonitoredElement childOfElementInCurrentStructure : elementInCurrentStructure.getContainedElements()) {
                        //if new element not seen before to same parent, add it
                        if (!elementInOldStructure.getContainedElements().contains(childOfElementInCurrentStructure)) {

                            //due to the fact that I wanted to keep synchronzied the tree structure with the map one
                            //now I need to add all children to the map, not only this element.
                            //also, very important, I need to add it as child in the snapshot of the parent
                            //which is a pain, as I need to do a manual BFS traversal, as I need the parent to add the children snapshot to.
                            List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<>();
                            MonitoredElementMonitoringSnapshot childElementMonitoringSnapshot = currentDataConverted.getMonitoredData(childOfElementInCurrentStructure);

                            //add element to parent structure
                            elementInOldStructure.withContainedElement(childOfElementInCurrentStructure);
                            //add monitored snapshot as child of aprent mon snapshot
                            usageSoFarSnapshot.getMonitoredData(elementInOldStructure).addChild(childElementMonitoringSnapshot);
                            //add mon snapshot directly in map
                            usageSoFarSnapshot.addMonitoredData(childElementMonitoringSnapshot);

                            elementMonitoringSnapshots.add(childElementMonitoringSnapshot);

                            while (!elementMonitoringSnapshots.isEmpty()) {
                                MonitoredElementMonitoringSnapshot parentSnapshot = elementMonitoringSnapshots.remove(0);
                                //find parent in old struct
                                if (usageSoFarSnapshot.contains(parentSnapshot.getMonitoredElement().getLevel(), parentSnapshot.getMonitoredElement())) {

//                                    MonitoredElementMonitoringSnapshot parentSnapshotinOldStruct = usageSoFarSnapshot.getMonitoredData(parentSnapshot.getMonitoredElement());
                                    Collection<MonitoredElementMonitoringSnapshot> currentParentChilds = parentSnapshot.getChildren();

                                    for (MonitoredElementMonitoringSnapshot currentParentChild : currentParentChilds) {
                                        //add element to parent structure
//                                        parentSnapshotinOldStruct.getMonitoredElement().withContainedElement(currentParentChild.getMonitoredElement());
                                        //add monitored snapshot as child of parent mon snapshot - not needed i already did this above when i added all child tree
//                                        parentSnapshotinOldStruct.addChild(currentParentChild);
                                        //add mon snapshot directly in map
                                        usageSoFarSnapshot.addMonitoredData(currentParentChild);

                                        elementMonitoringSnapshots.add(currentParentChild);

                                    }
                                } else {
                                    log.error("Element for {} not found in prev mon data ", new Object[]{parentSnapshot.getMonitoredElement().getName()});
                                }

                            }

                        }
                    }
                    break;
                }
            }
        }

        //update used and unused services
        for (MonitoredElement monitoredElement : previousUsage.getSnapshot().getMonitoredService()) {

            if (currentUsageData == null) {
                log.error("No monitoring data for " + monitoredElement + " at level " + monitoredElement.getLevel().toString() + " timestamp " + currentDataConverted.getTimestampID());
                continue;
            }

            //if element not here anymore, must mark all used services as dead
            if (!currentMonData.contains(monitoredElement.getLevel(), monitoredElement)) {
                for (UsedCloudOfferedService ucos : monitoredElement.getCloudOfferedServices()) {
                    previousUsage.withDeallocationTime(monitoredElement, ucos, Long.parseLong(currentDataConverted.getTimestamp()));
                }
            } else {
                MonitoredElement equivalentElementFromCurrentData = currentMonData.getMonitoredData(monitoredElement).getMonitoredElement();
                //if element still exists, check its services
                for (UsedCloudOfferedService ucos : monitoredElement.getCloudOfferedServices()) {
                    //remove services not in use anymore
                    if (!equivalentElementFromCurrentData.getCloudOfferedServices().contains(ucos)) {
                        previousUsage.withDeallocationTime(monitoredElement, ucos, Long.parseLong(currentDataConverted.getTimestamp()));
                    }
                }

            }

        }

        //add newly allocated services
        for (MonitoredElement elementInCurrentData : currentMonData.getMonitoredService()) {
            for (UsedCloudOfferedService ucos : elementInCurrentData.getCloudOfferedServices()) {
                if (!previousUsage.getInstantiationTimes(elementInCurrentData).containsKey(ucos)) {
                    previousUsage.withInstantiationTime(elementInCurrentData, ucos, Long.parseLong(currentDataConverted.getTimestamp()));
                }
            }
        }

        //updated timestamp of prev snapshot to latest
        updatedSnapshot.getSnapshot().withTimestamp(currentMonData.getTimestamp()).withTimestampID(currentMonData.getTimestampID());

        return updatedSnapshot;

    }

    public CompositionRulesBlock createCompositionRulesForTotalCostIncludingServicesASVMTypes(final Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices,
            final LifetimeEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

        List<MonitoredElement.MonitoredElementLevel> levelsInOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.VM);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
        levelsInOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);

        CompositionRulesBlock costCompositionRules = new CompositionRulesBlock();

        ServiceMonitoringSnapshot monitoringSnapshot = totalUsageSoFar.getSnapshot();

        for (MonitoredElement.MonitoredElementLevel level : levelsInOrder) {

            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> levelData = totalUsageSoFar.getSnapshot().getMonitoredData(level);

            if (levelData == null) {
                log.error("No monitoring data for service" + monitoringSnapshot.getMonitoredService() + " at level " + level.toString() + " timestamp " + monitoringSnapshot.getTimestampID());
                continue;
            }

            for (MonitoredElement monitoredElement : levelData.keySet()) {

                Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

                {
                    for (UsedCloudOfferedService service : monitoredElement.getCloudOfferedServices()) {

                        List<CompositionRule> offeredServiceRules = new ArrayList<>();

                        MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                                .withId(service.getInstanceUUID().toString())
                                .withName(service.getName())
                                .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                        MonitoredElementMonitoringSnapshot usedServiceDataSoFar = monitoringSnapshot.getMonitoredData(usedCloudServiceMonitoredElement);

                        //from the cost functions, we extract those that should be applied.
                        //maybe some do not quality to be apply as the service does not fulfill application requirements
                        List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(service);

                        //start with USAGE type of cost, easier to apply. 
                        for (CostFunction cf : costFunctionsToApply) {

                            for (CostElement element : cf.getCostElements()) {

                                if (element.getType().equals(CostElement.Type.USAGE)) {

                                    MetricValue value = usedServiceDataSoFar.getMonitoredData().get(element.getCostMetric());

                                    if (value != null) {
                                        //instant snapshot composition rule 
                                        {
                                            CompositionRule compositionRule = new CompositionRule();
                                            compositionRule.setTargetMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());
                                            compositionRule.addTargetMonitoredElementIDS(usedCloudServiceMonitoredElement.getId());
                                            String timePeriod = "s";

                                            if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                                timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                            }
                                            //"total_" must be there, as currnetly hashcode on Metric is only on "name", so if I have allready the instant cost,
                                            // and I put again this total cost metric with same name, will not replace it.
                                            //TODO: address this
                                            compositionRule.setResultingMetric(new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST));
                                            CompositionOperation compositionOperation = new CompositionOperation();
                                            compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
                                            compositionOperation.setTargetMetric(element.getCostMetric());
                                            compositionOperation.addMetricSourceMonitoredElementID(usedCloudServiceMonitoredElement.getId());
                                            compositionOperation.setMetricSourceMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());

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

                                            if (!offeredServiceRules.contains(compositionRule)) {
                                                offeredServiceRules.add(compositionRule);
                                            }
                                        }
                                    } else {
                                        log.warn("Metric {} not found in element {}", new Object[]{element.getCostMetric().getName(), monitoredElement.getName()});
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
                                    Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

                                    CompositionRule compositionRule = new CompositionRule();
                                    compositionRule.setTargetMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());
                                    compositionRule.addTargetMonitoredElementIDS(usedCloudServiceMonitoredElement.getId());

                                    compositionRule.setResultingMetric(cost);
                                    CompositionOperation compositionOperation = new CompositionOperation();

                                    compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
                                    compositionOperation.setValue(costForValue.getValueRepresentation());
                                    compositionRule.setOperation(compositionOperation);

                                    if (!offeredServiceRules.contains(compositionRule)) {
                                        offeredServiceRules.add(compositionRule);
                                    }

                                }

                            }
                        }

                        //add rule that aggregates all prev rules in Element Cost
                        {
                            CompositionRule offeredService_element_cost_rule = new CompositionRule();
                            offeredService_element_cost_rule.setTargetMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());
                            offeredService_element_cost_rule.addTargetMonitoredElementIDS(usedCloudServiceMonitoredElement.getId());
                            offeredService_element_cost_rule.setResultingMetric(ELEMENT_COST_METRIC);

                            //we sum up each of the metrics from the children                
                            //one big  SUM operation
                            {
                                CompositionOperation sumOperation = new CompositionOperation();
                                sumOperation.setOperationType(CompositionOperationType.SUM);
                                offeredService_element_cost_rule.setOperation(sumOperation);

                                for (CompositionRule rule : offeredServiceRules) {

                                    // only its rules, not also the rules from the children
                                    // the issue is that I recursively create a list, not a tree  of rules, and the
                                    // list contains all rules for all the subtree of this element
                                    if (rule.getTargetMonitoredElementIDs().contains(usedCloudServiceMonitoredElement.getId())) {

                                        CompositionOperation compositionOperation = new CompositionOperation();
                                        compositionOperation.setMetricSourceMonitoredElementLevel(rule.getTargetMonitoredElementLevel());
                                        compositionOperation.setTargetMetric(rule.getResultingMetric());
                                        compositionOperation.setOperationType(CompositionOperationType.SUM);
                                        sumOperation.addCompositionOperation(compositionOperation);
                                    }

                                }
                                //if we have no children or metrics which we add to the rule, do not create the rule
                                if (!sumOperation.getSubOperations().isEmpty()) {
                                    offeredServiceRules.add(offeredService_element_cost_rule);
                                }
                            }

                        }

                        for (CompositionRule rule : offeredServiceRules) {
                            if (!costCompositionRules.getCompositionRules().contains(rule)) {
                                costCompositionRules.addCompositionRule(rule);
                            }
                        }

                    }
                }

                //here I need to create rules that aggregate cost from the element's children and create cost metric at a higher level
                {
//                    final List<CompositionRule> childrenCostCompositionRules = Collections.synchronizedList(new ArrayList<CompositionRule>());
//                    //call recursively on all children the compute cost method
//
//                    ExecutorService es = Executors.newCachedThreadPool();
//                    List<Callable<Object>> todo = new ArrayList<>();
//
//                    for (final MonitoredElement child : monitoredElement.getContainedElements()) {
//
//                        Callable c = Executors.callable(new Runnable() {
//
//                            @Override
//                            public void run() {
//                                CompositionRulesBlock childRules = createCompositionRulesForTotalCost(cloudOfferedServices, child, totalUsageSoFar, currentTimesnapshot);
//                                //do not add duplicate rules
//                                //add cost rate rules
//                                for (CompositionRule childRule : childRules.getCompositionRules()) {
//                                    if (!childrenCostCompositionRules.contains(childRule)) {
//                                        childrenCostCompositionRules.add(childRule);
//                                    }
//                                }
//
//                            }
//                        });
//                        todo.add(c);
//                    }
//                    try {
//                        List<Future<Object>> answers = es.invokeAll(todo);
//                    } catch (InterruptedException ex) {
//                        log.error(ex.getMessage(), ex);
//                    }
//
//                    {
//                        costCompositionRules.getCompositionRules().addAll(childrenCostCompositionRules);
//
//                    }

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

    public CompositionRulesBlock createCompositionRulesForInstantUsageCostIncludingServicesASVMTypes(final Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices,
            final MonitoredElement monitoredElement, final LifetimeEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

        CompositionRulesBlock costCompositionRules = new CompositionRulesBlock();

        ServiceMonitoringSnapshot monitoringSnapshot = totalUsageSoFar.getSnapshot();

        Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

        {
            for (UsedCloudOfferedService service : monitoredElement.getCloudOfferedServices()) {
                {

                    List<CompositionRule> offeredServiceRules = new ArrayList<>();

                    MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                            .withId(service.getInstanceUUID().toString())
                            .withName(service.getName())
                            .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                    MonitoredElementMonitoringSnapshot usedServiceDataSoFar = monitoringSnapshot.getMonitoredData(usedCloudServiceMonitoredElement);

                    //from the cost functions, we extract those that should be applied.
                    //maybe some do not quality to be apply as the service does not fulfill application requirements
                    List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(service);

                    //start with USAGE type of cost, easier to apply. 
                    for (CostFunction cf : costFunctionsToApply) {

                        for (CostElement element : cf.getCostElements()) {

                            if (element.getType().equals(CostElement.Type.USAGE)) {
                                MetricValue value = usedServiceDataSoFar.getMonitoredData().get(element.getCostMetric());

                                //instant snapshot composition rule 
                                if (value != null) {
                                    {
                                        CompositionRule compositionRule = new CompositionRule();
                                        compositionRule.setTargetMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());
                                        compositionRule.addTargetMonitoredElementIDS(usedCloudServiceMonitoredElement.getId());
                                        String timePeriod = "s";

                                        if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                            timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                        }
                                        compositionRule.setResultingMetric(new Metric("cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST));
                                        CompositionOperation compositionOperation = new CompositionOperation();

//                                        compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
//                                        MetricValue costForValue = value.clone();
//                                        costForValue.multiply(element.getCostForCostMetricValue(value));
//
//                                        compositionOperation.setValue(costForValue.getValueRepresentation());
//                                        compositionRule.setOperation(compositionOperation);
//
//                                        if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                            costCompositionRules.addCompositionRule(compositionRule);
//                                        }
                                        compositionOperation.setOperationType(CompositionOperationType.MUL);
                                        compositionOperation.setTargetMetric(element.getCostMetric());
                                        compositionOperation.addMetricSourceMonitoredElementID(usedCloudServiceMonitoredElement.getId());
                                        compositionOperation.setMetricSourceMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());
                                        compositionOperation.setValue(element.getCostForCostMetricValue(value).toString());
                                        compositionRule.setOperation(compositionOperation);

                                        if (!offeredServiceRules.contains(compositionRule)) {
                                            offeredServiceRules.add(compositionRule);
                                        }

                                    }
                                } else {
                                    log.warn("Metric {} not found in element {}", new Object[]{element.getCostMetric().getName(), monitoredElement.getName()});
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

                                Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST);

                                CompositionRule compositionRule = new CompositionRule();
                                compositionRule.setTargetMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());
                                compositionRule.addTargetMonitoredElementIDS(usedCloudServiceMonitoredElement.getId());

                                compositionRule.setResultingMetric(cost);
                                CompositionOperation compositionOperation = new CompositionOperation();

                                compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
                                compositionOperation.setValue(totalCostFromCreation.getValueRepresentation());
                                compositionRule.setOperation(compositionOperation);

                                if (!offeredServiceRules.contains(compositionRule)) {
                                    offeredServiceRules.add(compositionRule);
                                }

                            }

                        }
                    }

                    //add rule that aggregates all prev rules in Element Cost
                    {
                        CompositionRule offeredService_element_cost_rule = new CompositionRule();
                        offeredService_element_cost_rule.setTargetMonitoredElementLevel(usedCloudServiceMonitoredElement.getLevel());
                        offeredService_element_cost_rule.addTargetMonitoredElementIDS(usedCloudServiceMonitoredElement.getId());
                        offeredService_element_cost_rule.setResultingMetric(ELEMENT_COST_METRIC);

                        //we sum up each of the metrics from the children                
                        //one big  SUM operation
                        {
                            CompositionOperation sumOperation = new CompositionOperation();
                            sumOperation.setOperationType(CompositionOperationType.SUM);
                            offeredService_element_cost_rule.setOperation(sumOperation);

                            for (CompositionRule rule : offeredServiceRules) {

                                // only its rules, not also the rules from the children
                                // the issue is that I recursively create a list, not a tree  of rules, and the
                                // list contains all rules for all the subtree of this element
                                if (rule.getTargetMonitoredElementIDs().contains(usedCloudServiceMonitoredElement.getId())) {

                                    CompositionOperation compositionOperation = new CompositionOperation();
                                    compositionOperation.setMetricSourceMonitoredElementLevel(rule.getTargetMonitoredElementLevel());
                                    compositionOperation.setTargetMetric(rule.getResultingMetric());
                                    compositionOperation.setOperationType(CompositionOperationType.SUM);
                                    sumOperation.addCompositionOperation(compositionOperation);
                                }

                            }
                            //if we have no children or metrics which we add to the rule, do not create the rule
                            if (!sumOperation.getSubOperations().isEmpty()) {
                                offeredServiceRules.add(offeredService_element_cost_rule);
                            }
                        }

                    }

                    for (CompositionRule rule : offeredServiceRules) {
                        if (!costCompositionRules.getCompositionRules().contains(rule)) {
                            costCompositionRules.addCompositionRule(rule);
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
                        CompositionRulesBlock childRules = createCompositionRulesForInstantUsageCostIncludingServicesASVMTypes(cloudOfferedServices, child, totalUsageSoFar, currentTimesnapshot);
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

    /**
     * 1. Check what cost functions are applicable 2. SUMS up the metrics
     * targeted by the cost functions, between the previously monitored ones and
     * the new monitored snapshot !!!! NOTE !!! It works directly on the
     * supplied previouselyDeterminedUsage
     *
     * This also does an interesting thing. As Services Used can
     * appear/dissappear, it adds in the newMonData the services which are found
     * also in previousUsage
     *
     * @param cloudOfferedServices
     * @param previousUsage
     * @param monData
     * @return
     */
    public LifetimeEnrichedSnapshot updateTotalUsageSoFarWithCompleteStructure(Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices, LifetimeEnrichedSnapshot previousUsage, ServiceMonitoringSnapshot monData) {

        if (monData == null) {
            return new LifetimeEnrichedSnapshot();
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

        ServiceMonitoringSnapshot currentMonitoringSnapshot = monData.clone();

        LifetimeEnrichedSnapshot updatedSnapshot = new LifetimeEnrichedSnapshot()
                .withSnapshot(previousUsage.getSnapshot())
                .withLastUpdatedTimestampID(currentMonitoringSnapshot.getTimestampID());
        updatedSnapshot.withDeallocationTimes(previousUsage.getDeallocationTimes());
        updatedSnapshot.withInstantiationTimes(previousUsage.getInstantiationTimes());

        //we consider the current structure the last updated structure, as maybe the user rearanged the structure (e.g., moved/redefined topologies)
        for (MonitoredElement monitoredElement : previousUsage.getSnapshot().getMonitoredService()) {

            MonitoredElementMonitoringSnapshot previousUsageData = updatedSnapshot.getSnapshot().getMonitoredData(monitoredElement);
            MonitoredElementMonitoringSnapshot currentUsageData = monData.getMonitoredData(monitoredElement);

            if (currentUsageData == null) {
                log.error("No monitoring data for " + monitoredElement + " at level " + monitoredElement.getLevel().toString() + " timestamp " + currentMonitoringSnapshot.getTimestampID());
                continue;
            }

            //if just appeared, add monitored element VM in the instatiationTimes
            //update used cloud offered services, i.e., add newly added ones, and remove deleted ones
            List<UsedCloudOfferedService> monitoredElementCurrentlyUsedServices = monitoredElement.getCloudOfferedServices();
//                List<UsedCloudOfferedService> monitoredElementNoLongerUsedServices = new ArrayList<>();

            //mark deallocated services
            Iterator<UsedCloudOfferedService> it = updatedSnapshot.getInstantiationTimes(monitoredElement).keySet().iterator();
            while (it.hasNext()) {
                UsedCloudOfferedService cloudOfferedService = it.next();

                //if used service not used anymore, mark it as dead
                if (!monitoredElementCurrentlyUsedServices.contains(cloudOfferedService)) {
                    updatedSnapshot.withDeallocationTime(monitoredElement, cloudOfferedService, Long.parseLong(currentMonitoringSnapshot.getTimestamp()));
                    //need to add the removed service to the current structure, so we have the usage so far on it
                    monitoredElementCurrentlyUsedServices.add(cloudOfferedService);
                }

            }

            //add newly allocated services
            for (UsedCloudOfferedService ucos : monitoredElementCurrentlyUsedServices) {
                if (!updatedSnapshot.getInstantiationTimes(monitoredElement).containsKey(ucos)) {
                    updatedSnapshot.withInstantiationTime(monitoredElement, ucos, Long.parseLong(currentMonitoringSnapshot.getTimestamp()));
                }
            }

            Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, monitoredElement);

            //update usage only on the services still in use
            for (UsedCloudOfferedService usedCloudService : monitoredElementCurrentlyUsedServices) {
                {

                    //if service no longer in use we do not update anything, and just continue
                    if (previousUsage.hasDeallocationTime(monitoredElement, usedCloudService)) {
                        continue;
                    }

                    //from the cost functions, we extract those that should be applied.
                    //maybe some do not quality to be apply as the service does not fulfill application requirements
                    List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(usedCloudService);

//                        Map<Metric, MetricValue> vmUsageSoFar = null;
//
//                        if (updatedSnapshot.getSnapshot().contains(level, monitoredElement)) {
//                            vmUsageSoFar = updatedSnapshot.getSnapshot().getMonitoredData(monitoredElement).getMonitoredData();
//                        } else {
//                            vmUsageSoFar = new HashMap<>();
//                            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(monitoredElement, vmUsageSoFar);
//                            updatedSnapshot.getSnapshot().addMonitoredData(elementMonitoringSnapshot);
//                        }
                    //apply cost functions
                    //start with USAGE type of cost, easier to apply. 
                    for (CostFunction cf : costFunctionsToApply) {
                        for (CostElement element : cf.getCostElements()) {

                            if (element.getType().equals(CostElement.Type.USAGE)) {

                                MetricValue currentElementValue = currentUsageData.getMetricValue(element.getCostMetric());
                                MetricValue usageSoFarForMetric = previousUsageData.getMetricValue(element.getCostMetric());

                                if (currentElementValue != null) {

                                    //if we just added the VM, so we do not have any historical usage so far
//                                        if (!vmUsageSoFar.containsKey(element.getCostMetric())) {
//                                            vmUsageSoFar.put(element.getCostMetric(), previousElementValue.clone());
//
//                                        } else 
                                    {
                                            //else we need to sum up usage

                                        //we should compute estimated usage over time not captured by monitoring points:
                                        //I.E. I measure every 1 minute, usage over 1 minute must be extrapolated
                                        //depending on the measurement unit of the measured metric
                                        //I.E. a pkts_out/s will be multiplied with 60
                                        //if not contain / then it does not have measurement unit over time , and we ASSUME it is per second
                                        //this works as we assume we target only metrics which change in time using PER USAGE cost functions
                                        String timePeriod = "";

                                        if (element.getCostMetric().getMeasurementUnit().contains("/")) {
                                            timePeriod = element.getCostMetric().getMeasurementUnit().split("/")[1].toLowerCase();
                                        }

                                        //check amount of time in millis between two measurement units
                                        Long currentTimestamp = Long.parseLong(currentMonitoringSnapshot.getTimestamp());

                                        //check if monitoring has left some time intervals un-monitored
                                        //if so, then fill up missing 
                                        Long previousTimestamp = Long.parseLong(previousUsage.getSnapshot().getTimestamp());
                                        Long timeIntervalInMillis = (currentTimestamp - previousTimestamp) / 1000;

                                        //convert to seconds
                                        Long periodsBetweenPrevAndCurrentTimestamp = 0l;

                                        //if metric does not have period, than its a metric which ACCUMULATES, I.E., show summed up historical usage by itself
                                        if (timePeriod.length() == 0) {
                                            usageSoFarForMetric.setValue(currentElementValue.getValue());
                                            usageSoFarForMetric.setFreshness(currentElementValue.getFreshness());
                                            usageSoFarForMetric.setTimeSinceCollection(currentElementValue.getTimeSinceCollection());
                                            usageSoFarForMetric.setTimeSinceCollection(currentElementValue.getTimeSinceCollection());
                                            continue;
                                        } else if (timePeriod.equals("s")) {
                                            periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis;
                                        } else if (timePeriod.equals("m")) {
                                            periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 60;
                                        } else if (timePeriod.equals("h")) {
                                            periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 3600;
                                        } else if (timePeriod.equals("d")) {
                                            periodsBetweenPrevAndCurrentTimestamp = timeIntervalInMillis / 86400;
                                        }

                                        if (periodsBetweenPrevAndCurrentTimestamp <= 1) {
                                            usageSoFarForMetric.sum(currentElementValue);

                                        } else {
                                                //if more than one period between recordings, need to compute usage for non-monitored periods
                                            //we compute average for intermediary

                                            //get average between two last readings and use it to fill the gap of un-monitored data
                                            MetricValue average = usageSoFarForMetric.clone();
                                            average.sum(currentElementValue);
                                            average.divide(2);
                                            //add average for intermediary monitoring points
                                            //TODO: add option for adding other plug-ins for filling up missing data, and computing accuracy of estimation
                                            average.multiply(periodsBetweenPrevAndCurrentTimestamp.intValue());
                                            //add monitored points
                                            average.sum(usageSoFarForMetric);
                                            average.sum(currentElementValue);
                                            usageSoFarForMetric.setValue(average.getValue());

                                        }

                                    }
                                } else {
                                    log.error("Cost metric {} was not found on {} on previous mon data", element.getCostMetric().getName(), monitoredElement.getId());
                                }

                                //if cost is per usage, it is computed by instant monitoring cost.
//                                        continue;
                            }
                        }
                    }
                }

            }

            //usually VMs appear/dissapear, but also topologies can be restructured
            //so check if some new appeared whichw e need to add
            //if current mon data has some newly added services, and also has the
//                if (currentUsageData.containsKey(monitoredElement)) {
            for (MonitoredElementMonitoringSnapshot childSnapshot : currentUsageData.getChildren()) {
                MonitoredElement child = childSnapshot.getMonitoredElement();

                //if previousely monitored element is in currrent data, i.e., was not deleted, ignore it
                if (!previousUsage.getSnapshot().contains(child.getLevel(), child)) {
                    previousUsageData.getMonitoredElement().withContainedElement(child);
                    Long currentTimestamp = Long.parseLong(monData.getTimestamp());
                    for (UsedCloudOfferedService cloudOfferedService : child.getCloudOfferedServices()) {
                        updatedSnapshot.withInstantiationTime(child, cloudOfferedService, currentTimestamp);
                    }
                    //interesting. I split that stupid map in such a way that i do not search for the parent where to add a particular
                    //snapshot, so I must add the child both to the parent, and in the map level, to keep things consistent
                    //it is a hassle, but is faster to retrieve data if we keep the map
                    previousUsageData.addChild(childSnapshot);
                    previousUsage.getSnapshot().getMonitoredData(child.getLevel()).put(child, childSnapshot);
//                        }
                }
            }
        }

        //updated timestamp of prev snapshot to latest
        updatedSnapshot.getSnapshot().withTimestamp(monData.getTimestamp()).withTimestampID(monData.getTimestampID());

        return updatedSnapshot;
    }

    public ServiceMonitoringSnapshot convertToStructureIncludingServicesASVMTypes(Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices, ServiceMonitoringSnapshot monData) {

        if (monData == null) {
            return new ServiceMonitoringSnapshot();
        }
        ServiceMonitoringSnapshot currentMonData = monData.clone();
        MonitoredElementMonitoringSnapshot monitoringData = currentMonData.getMonitoredData(currentMonData.getMonitoredService());

        //I want to go trough each element
        //for each used cloud offered service
        //create a CLOUD_OFFERED_SERVICE and attach it to the element using the service
        //Moreover, for all applicable cost functions, for each cost element
        //move the metrics from the element to the service
        //create composition rules that create cost for the offered service
        for (MonitoredElementMonitoringSnapshot snapshot : monitoringData) {
            MonitoredElement element = snapshot.getMonitoredElement();
            Map<Metric, MetricValue> monitoredData = snapshot.getMonitoredData();

            //do not analyze elements I just added. 
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE)) {
                continue;
            }

            Map<UsedCloudOfferedService, List<CostFunction>> applicableCostFunctions = getApplicableCostFunctions(cloudOfferedServices, element);

            for (UsedCloudOfferedService usedCloudService : element.getCloudOfferedServices()) {
                MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                        .withId(usedCloudService.getInstanceUUID().toString())
                        .withName(usedCloudService.getName())
                        .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);
                //add the service as an element to the VM
                element.withContainedElement(usedCloudServiceMonitoredElement);

                MonitoredElementMonitoringSnapshot usedCloudServiceSnapshot = new MonitoredElementMonitoringSnapshot(usedCloudServiceMonitoredElement);

                snapshot.addChild(usedCloudServiceSnapshot);

                //not move relevant monitoring data
                List<CostFunction> costFunctionsToApply = applicableCostFunctions.get(usedCloudService);

                for (CostFunction cf : costFunctionsToApply) {
                    for (CostElement costElement : cf.getCostElements()) {

                        if (costElement.getType().equals(CostElement.Type.USAGE)) {
                            Metric costMetric = costElement.getCostMetric();
                            if (monitoredData.containsKey(costMetric)) {
                                MetricValue metricValue = monitoredData.remove(costMetric);
                                usedCloudServiceSnapshot.getMonitoredData().put(costMetric, metricValue);
                            }
                        }//else if cost is periodic, I do not need to move metrics
                    }
                }

            }
        }

        ServiceMonitoringSnapshot convertedMonitoringSnapshot = new ServiceMonitoringSnapshot();
        convertedMonitoringSnapshot.withTimestamp(currentMonData.getTimestamp());
        convertedMonitoringSnapshot.withTimestampID(currentMonData.getTimestampID());

        //yes, it is stupid to do it, i should give up on this snapshot shit
        for (MonitoredElementMonitoringSnapshot elementMonitoringSnapshot : monitoringData) {
            convertedMonitoringSnapshot.addMonitoredData(elementMonitoringSnapshot);
        }

        return convertedMonitoringSnapshot;
    }

    public Map<UsedCloudOfferedService, List<CostFunction>> getApplicableCostFunctions(Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices, MonitoredElement monitoredElement) {

        Map<UsedCloudOfferedService, List<CostFunction>> map = new HashMap<>();

        for (UsedCloudOfferedService usedCloudService : monitoredElement.getCloudOfferedServices()) {
            List<CostFunction> costFunctionsToApply = new ArrayList<CostFunction>();
            map.put(usedCloudService, costFunctionsToApply);
            //get service cost scheme
            List<CostFunction> costFunctions = null;

            if (cloudOfferedServices.containsKey(usedCloudService.getCloudProviderID())) {
                Map<UUID, CloudOfferedService> cloudServices = cloudOfferedServices.get(usedCloudService.getCloudProviderID());
                if (cloudServices.containsKey(usedCloudService.getId())) {
                    CloudOfferedService cloudService = cloudServices.get(usedCloudService.getId());
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
                    if (cf.getAppliedIfServiceInstanceUses().isEmpty()) {
                        costFunctionsToApply.add(cf);
                    } else {
                                //else need to check if it is used in conjunction with the mentioned

                        //can be diff entities: For example, VM type A costs X if has RAM 1, CPU 2, and used with Storage Y
                        List<CloudOfferedService> tobeAppliedInConjunctionWithCloudOfferedService = cf.getAppliedIfServiceInstanceUsesCloudOfferedServices();
                        List<Resource> tobeAppliedInConjunctionWithResource = cf.getAppliedIfServiceInstanceUsesResource();
                        List<Quality> tobeAppliedInConjunctionWithQuality = cf.getAppliedIfServiceInstanceUsesQuality();

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
                        if ((!tobeAppliedInConjunctionWithCloudOfferedService.isEmpty()) && relationships.isEmpty()) {
                            serviceUnitInConjunctionMatch = false;
                        } else {
                            //here we need to check if used in conjunction with the right cloud service

                            if (tobeAppliedInConjunctionWithCloudOfferedService.size() <= relationships.size()) {

                                for (CloudOfferedService unit : tobeAppliedInConjunctionWithCloudOfferedService) {
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
    public CompositionRulesBlock createCompositionRulesForInstantUsageCost(final Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices,
            final MonitoredElement monitoredElement, final LifetimeEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

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

                            if (element.getType().equals(CostElement.Type.USAGE)) {
                                //instant snapshot composition rule 
                                if (value != null) {
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

//                                        compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
//                                        MetricValue costForValue = value.clone();
//                                        costForValue.multiply(element.getCostForCostMetricValue(value));
//
//                                        compositionOperation.setValue(costForValue.getValueRepresentation());
//                                        compositionRule.setOperation(compositionOperation);
//
//                                        if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
//                                            costCompositionRules.addCompositionRule(compositionRule);
//                                        }
                                        compositionOperation.setOperationType(CompositionOperationType.MUL);
                                        compositionOperation.setTargetMetric(element.getCostMetric());
                                        compositionOperation.addMetricSourceMonitoredElementID(monitoredElement.getId());
                                        compositionOperation.setMetricSourceMonitoredElementLevel(monitoredElement.getLevel());
                                        compositionOperation.setValue(element.getCostForCostMetricValue(value).toString());
                                        compositionRule.setOperation(compositionOperation);

                                        if (!costCompositionRules.getCompositionRules().contains(compositionRule)) {
                                            costCompositionRules.addCompositionRule(compositionRule);
                                        }

                                    }
                                } else {
                                    log.warn("Metric {} not found in element {}", new Object[]{element.getCostMetric().getName(), monitoredElement.getName()});
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

                                Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits/" + timePeriod, Metric.MetricType.COST);

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

    public CompositionRulesBlock createCompositionRulesForTotalCost(final Map<UUID, Map<UUID, CloudOfferedService>> cloudOfferedServices,
            final LifetimeEnrichedSnapshot totalUsageSoFar, final String currentTimesnapshot) {

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

                                    if (element.getType().equals(CostElement.Type.USAGE)) {

                                        MetricValue value = vmMonSnapshot.getMetricValue(element.getCostMetric());

                                        if (value != null) {
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
                                                compositionRule.setResultingMetric(new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST));
                                                CompositionOperation compositionOperation = new CompositionOperation();
                                                compositionOperation.setOperationType(CompositionOperationType.SET_VALUE);
                                                compositionOperation.setTargetMetric(element.getCostMetric());
                                                compositionOperation.addMetricSourceMonitoredElementID(monitoredElement.getId());
                                                compositionOperation.setMetricSourceMonitoredElementLevel(monitoredElement.getLevel());

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
                                        } else {
                                            log.warn("Metric {} not found in element {}", new Object[]{element.getCostMetric().getName(), monitoredElement.getName()});
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
                                        Metric cost = new Metric("cost_" + element.getCostMetric().getName(), "costUnits", Metric.MetricType.COST);

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

                                }
                            }
                        }

                    }
                }

                //here I need to create rules that aggregate cost from the element's children and create cost metric at a higher level
                {
//                    final List<CompositionRule> childrenCostCompositionRules = Collections.synchronizedList(new ArrayList<CompositionRule>());
//                    //call recursively on all children the compute cost method
//
//                    ExecutorService es = Executors.newCachedThreadPool();
//                    List<Callable<Object>> todo = new ArrayList<>();
//
//                    for (final MonitoredElement child : monitoredElement.getContainedElements()) {
//
//                        Callable c = Executors.callable(new Runnable() {
//
//                            @Override
//                            public void run() {
//                                CompositionRulesBlock childRules = createCompositionRulesForTotalCost(cloudOfferedServices, child, totalUsageSoFar, currentTimesnapshot);
//                                //do not add duplicate rules
//                                //add cost rate rules
//                                for (CompositionRule childRule : childRules.getCompositionRules()) {
//                                    if (!childrenCostCompositionRules.contains(childRule)) {
//                                        childrenCostCompositionRules.add(childRule);
//                                    }
//                                }
//
//                            }
//                        });
//                        todo.add(c);
//                    }
//                    try {
//                        List<Future<Object>> answers = es.invokeAll(todo);
//                    } catch (InterruptedException ex) {
//                        log.error(ex.getMessage(), ex);
//                    }
//
//                    {
//                        costCompositionRules.getCompositionRules().addAll(childrenCostCompositionRules);
//
//                    }

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
