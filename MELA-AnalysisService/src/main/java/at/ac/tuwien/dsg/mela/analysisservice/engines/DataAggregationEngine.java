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
package at.ac.tuwien.dsg.mela.analysisservice.engines;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.analysisservice.utils.Configuration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
public class DataAggregationEngine {

    private List<MonitoredElement.MonitoredElementLevel> serviceLevelProcessingOrder;

    {
        serviceLevelProcessingOrder = new ArrayList<MonitoredElement.MonitoredElementLevel>();
        serviceLevelProcessingOrder.add(MonitoredElement.MonitoredElementLevel.VM);
        serviceLevelProcessingOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
        serviceLevelProcessingOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
        serviceLevelProcessingOrder.add(MonitoredElement.MonitoredElementLevel.SERVICE);
    }

    /**
     * @param compositionRulesConfiguration the metric composition rules to be
     * applied on the serviceStructure monitoring snapshot
     * @param serviceMonitoringSnapshot simple serviceStructure monitoring data
     * @return monitoring data enriched with composite metrics
     */
    public ServiceMonitoringSnapshot enrichMonitoringData(final CompositionRulesConfiguration compositionRulesConfiguration, final ServiceMonitoringSnapshot serviceMonitoringSnapshot) {
        if (serviceMonitoringSnapshot == null) {
            return null;
        } else if (compositionRulesConfiguration == null || compositionRulesConfiguration.getMetricCompositionRules() == null) {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "CompositionRulesConfiguration either null, missing composition rules, or target service ID");
            return serviceMonitoringSnapshot;
        }
        CompositionRulesBlock compositionRulesBlock = compositionRulesConfiguration.getMetricCompositionRules();

        ArrayList<CompositionRule> metricCompositionRules = compositionRulesBlock.getCompositionRules();

        //sort the rules after their level (the ENUM levels are declared from SERVICE to VM. Hope this is the right way
        Collections.sort(metricCompositionRules, new Comparator<CompositionRule>() {
            public int compare(CompositionRule o1, CompositionRule o2) {
                return -o1.getTargetMonitoredElementLevel().compareTo(o2.getTargetMonitoredElementLevel());
            }
        });

        //apply each composition rule in sequence
        for (CompositionRule compositionRule : metricCompositionRules) {
            compositionRule.apply(serviceMonitoringSnapshot);
        }

//        //start applying composition from VM level upwards
//        //for each level, apply all rules that target the level
//        for (MonitoredElement.MonitoredElementLevel level : serviceLevelProcessingOrder) {
//
//            //for each rule block, get target elements, and apply composition rules if the rules target this level
//            for (final CompositionRulesBlock compositionRulesBlockBlock : compositionRulesBlockCollection) {
//                if (compositionRulesBlockBlock.getTargetMonitoredElementLevel().equals(level)) {
//
//                    final MonitoredElement.MonitoredElementLevel targetLevel = compositionRulesBlockBlock.getTargetMonitoredElementLevel();
//                    final Collection<String> targetMonitoredElementIDs = compositionRulesBlockBlock.getTargetMonitoredElementIDs();
//
//                    final Map<MonitoredElement, MonitoredElementMonitoringSnapshot> targetElements = serviceMonitoringSnapshot.getMonitoredData(targetLevel, targetMonitoredElementIDs);
//
//                    List<Thread> elementCompositionThreads = new ArrayList<Thread>();
//
//                    for (final MonitoredElement targetElement : targetElements.keySet()) {
//                        //for each composition rule extract metric source elements
////                Thread targetElementCompositionThread = new Thread() {
////                    @Override
////                    public void run() {
//                        for (CompositionRule compositionRule : compositionRulesBlockBlock.getCompositionRules()) {
//
//                            MonitoredElement.MonitoredElementLevel sourceLevel = compositionRule.getMetricSourceMonitoredElementLevel();
//                            Collection<String> sourceMonitoredElementIDs = compositionRule.getMetricSourceMonitoredElementIDs();
//
//                            //extract source metric values from CHILDREN of target MonitoredElement
//                            //get all serviceStructure element metrics for the targeted level
//                            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> sourceLevelData = serviceMonitoringSnapshot.getMonitoredData(sourceLevel, sourceMonitoredElementIDs);
//
//                            //get the children having that level because maybe the rule is for the entire level and Here i get the data only for 1 element
//                            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> metricSourceElements = new HashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>();
//
//                            //traverse the children in a BFS manner
//                            {
//                                List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
//                                queue.add(targetElement);
//                                while (!queue.isEmpty()) {
//                                    MonitoredElement se = queue.remove(0);
//                                    queue.addAll(se.getContainedElements());
//
//                                    //if I find a child that is at the correct level (sourceLevelData), add it in the metricSourceElements
//                                    if (sourceLevelData.containsKey(se)) {
//                                        metricSourceElements.put(se, sourceLevelData.get(se));
//                                    }
//                                }
//                            }
//
//
//                            Metric targetMetric = compositionRule.getTargetMetric();
//
//                            List<MetricValue> values = new ArrayList<MetricValue>();
//
//                            //extract the values for the composed metric for each targeted MonitoredElement
//                            for (MonitoredElementMonitoringSnapshot sourceSnapshot : metricSourceElements.values()) {
//                                Map<Metric, MetricValue> sourceData = sourceSnapshot.getMonitoredData();
//
//                                if (sourceData.containsKey(targetMetric)) {
//                                    values.add(sourceData.get(targetMetric).clone());
//                                }
//
//                            }
//
//                            //go trough all composition operations for this rule and check if it references some metric instead of value
//                            //if it does, then search the metric at the TARGET level and add the current metric value to the operation
//                            Map<Metric, MetricValue> metricsForTargetElement = targetElements.get(targetElement).getMonitoredData();
//                            for (CompositionOperation compositionOperation : compositionRule.getOperations()) {
//                                String metricName = compositionOperation.getMetricName();
//
//                                if (metricName != null) {
//                                    Metric referencedMetric = new Metric(metricName);
//                                    if (metricsForTargetElement.containsKey(referencedMetric)) {
//                                        compositionOperation.setValue(metricsForTargetElement.get(referencedMetric).getValueRepresentation());
//                                    }
//                                }
//                            }
//
//                            if (values.size() > 0) {
//                                MetricValue composedValue = compositionRule.apply(values);
//
//                                //insert the new aggregation for the targeted unit
//                                targetElements.get(targetElement).getMonitoredData().put(compositionRule.getResultingMetric(), composedValue.clone());
//
//                            } else {
//                                //Configuration.getLogger(this.getClass()).log(Level.WARN, "Target metric " + targetMetric + " not found in level " + sourceLevel + " in components " + sourceMonitoredElementIDs + " for element" + targetElement.getId());
//                            }
//                        }
//                    }
//
////                };
////                elementCompositionThreads.add(targetElementCompositionThread);
////                targetElementCompositionThread.start();
////            }
//
//                    for (Thread thread : elementCompositionThreads) {
//                        try {
//                            thread.join();
//                        } catch (InterruptedException e) {
//
//                        }
//                    }
//                }
//
//            }
//        }

        return serviceMonitoringSnapshot;

    }

    /**
     * NOTE: CURRENTLY only aggregates at VM level and the rules are supplied at
     * Service Unit level The VM level aggregation rules in time are applied.
     * Then the instant data rules are applied to get higher level aggregated
     * data
     *
     * @param compositionRulesConfiguration rules to aggregate instant data
     * @param compositionRules takes composition rules which specify for
     * ServiceUNIT ID what rules to apply at the VM level.
     * @param serviceMonitoringSnapshots
     * @return
     */
    public ServiceMonitoringSnapshot aggregateMonitoringDataOverTime(final CompositionRulesConfiguration compositionRulesConfiguration, final List<ServiceMonitoringSnapshot> serviceMonitoringSnapshots) {
        if (serviceMonitoringSnapshots == null) {
            return new ServiceMonitoringSnapshot();
        }
//        ServiceMonitoringSnapshot composedMonitoringSnapshot = new ServiceMonitoringSnapshot();


        Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, List<MonitoredElementMonitoringSnapshot>>> dataToAggregate;
        {
            dataToAggregate = new EnumMap<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, List<MonitoredElementMonitoringSnapshot>>>(MonitoredElement.MonitoredElementLevel.class);
            dataToAggregate.put(MonitoredElement.MonitoredElementLevel.VM, new LinkedHashMap<MonitoredElement, List<MonitoredElementMonitoringSnapshot>>());
            dataToAggregate.put(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT, new LinkedHashMap<MonitoredElement, List<MonitoredElementMonitoringSnapshot>>());
            dataToAggregate.put(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY, new LinkedHashMap<MonitoredElement, List<MonitoredElementMonitoringSnapshot>>());
            dataToAggregate.put(MonitoredElement.MonitoredElementLevel.SERVICE, new LinkedHashMap<MonitoredElement, List<MonitoredElementMonitoringSnapshot>>());
        }

        //go trough supplied monitoring snapshots
        for (ServiceMonitoringSnapshot serviceMonitoringSnapshot : serviceMonitoringSnapshots) {
            Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> monitoredData = serviceMonitoringSnapshot.getMonitoredData();
            if (monitoredData == null || monitoredData.isEmpty()) {
                Configuration.getLogger(this.getClass()).log(Level.WARN, "No monitoring data in theserviceMonitoringSnapshot ");
                continue;
            }
            //extract for each Level the monitored data
            for (MonitoredElement.MonitoredElementLevel level : monitoredData.keySet()) {
                Map<MonitoredElement, MonitoredElementMonitoringSnapshot> dataForLevel = monitoredData.get(level);
                if (dataForLevel == null || dataForLevel.isEmpty()) {
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "No monitoring data in the serviceMonitoringSnapshot for Level:" + level);
                    continue;
                }

                Map<MonitoredElement, List<MonitoredElementMonitoringSnapshot>> dataToAggregateForLevel = dataToAggregate.get(level);

                //for each monitored MonitoredElement at each level add monitoring data
                for (MonitoredElement monitoredElement : dataForLevel.keySet()) {
                    if (dataToAggregateForLevel.containsKey(monitoredElement)) {
                        dataToAggregateForLevel.get(monitoredElement).add(dataForLevel.get(monitoredElement));
                    } else {
                        List<MonitoredElementMonitoringSnapshot> snapshotList = new ArrayList<MonitoredElementMonitoringSnapshot>();
                        snapshotList.add(dataForLevel.get(monitoredElement));
                        dataToAggregateForLevel.put(monitoredElement, snapshotList);
                    }
                }
            }
        }

        //filter elements that do not have the same number of monitoring snapshots. I.E. elements that disappeared are removed, ones which appeared are kept.
        for (Map<MonitoredElement, List<MonitoredElementMonitoringSnapshot>> datas : dataToAggregate.values()) {
            List<MonitoredElement> elementsThatHaveDisappeared = new ArrayList<MonitoredElement>();
            for (MonitoredElement MonitoredElement : datas.keySet()) {
                if (datas.get(MonitoredElement).size() < serviceMonitoringSnapshots.size()) {
                    elementsThatHaveDisappeared.add(MonitoredElement);
                }
            }
            for (MonitoredElement elementToRemove : elementsThatHaveDisappeared) {
                datas.remove(elementToRemove);
            }
        }

        ServiceMonitoringSnapshot composedMonitoringSnapshot = new ServiceMonitoringSnapshot();

        //create composite monitoring data after the last service we have
        Map<MonitoredElement, List<MonitoredElementMonitoringSnapshot>> serviceLevelDataToAggregate = dataToAggregate.get(MonitoredElement.MonitoredElementLevel.SERVICE);
//        if (serviceLevelDataToAggregate == null) {
//            Configuration.getLogger(this.getClass()).log(Level.WARN, "No service level data found to compose historical data");
//            if (!serviceMonitoringSnapshots.isEmpty()) {
//                //return last monitored data
//                return serviceMonitoringSnapshots.get(serviceMonitoringSnapshots.size() - 1);
//            } else {
//                return composedMonitoringSnapshot;
//            }
//        }

        List<MonitoredElement> encounteredServices = new ArrayList<MonitoredElement>(serviceLevelDataToAggregate.keySet());
        if (encounteredServices.isEmpty()) {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "No service level data found to compose historical data");
            if (!serviceMonitoringSnapshots.isEmpty()) {
                //return last monitored data
                return serviceMonitoringSnapshots.get(serviceMonitoringSnapshots.size() - 1);
            } else {
                return composedMonitoringSnapshot;
            }
        }
        
        MonitoredElement lastEncounteredElement = encounteredServices.get(encounteredServices.size() - 1);

        /**
         * Linear representation of MonitoredElement hierarchical tree. also
         * maintains the three structure using the .children relationship
         * between MonitoredElementMonitoringSnapshot instances
         */
        Map<MonitoredElement, MonitoredElement> elements = new LinkedHashMap<MonitoredElement, MonitoredElement>();

        //traverse the MonitoredElement hierarchical tree in BFS and extract the serviceStructure elements
        List<MonitoredElementMonitoringSnapshot> bfsTraversalQueue = new ArrayList<MonitoredElementMonitoringSnapshot>();
        MonitoredElementMonitoringSnapshot rootMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(lastEncounteredElement, new LinkedHashMap<Metric, MetricValue>());

        bfsTraversalQueue.add(rootMonitoringSnapshot);
        composedMonitoringSnapshot.addMonitoredData(rootMonitoringSnapshot);

        while (!bfsTraversalQueue.isEmpty()) {
            MonitoredElementMonitoringSnapshot element = bfsTraversalQueue.remove(0);
            MonitoredElement processedElement = element.getMonitoredElement();
            elements.put(processedElement, processedElement);

            for (MonitoredElement child : processedElement.getContainedElements()) //add empty monitoring data for each serviceStructure element, to serve as a place where in the future composed metrics can be added
            {
                MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(child, new LinkedHashMap<Metric, MetricValue>());
                element.addChild(MonitoredElementMonitoringSnapshot);
                composedMonitoringSnapshot.addMonitoredData(MonitoredElementMonitoringSnapshot);
                bfsTraversalQueue.add(MonitoredElementMonitoringSnapshot);
            }

        }

//
//        //also maintain children structure
//
//        for (MonitoredElement.MonitoredElementLevel level : dataToAggregate.keySet()) {
//            for (MonitoredElement MonitoredElement : dataToAggregate.get(level).keySet()) {
//                MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(MonitoredElement, new HashMap<Metric, MetricValue>());
//                composedMonitoringSnapshot.addMonitoredData(MonitoredElementMonitoringSnapshot);
//            }
//        }

        //if no composition rules, return  only service structure (to display nice)
        if (compositionRulesConfiguration == null || compositionRulesConfiguration.getHistoricMetricCompositionRules() == null) {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "CompositionRulesConfiguration either null, missing composition rules, or target service ID");
            return composedMonitoringSnapshot;
        }

        //CURRENTLY only aggregates at VM level and the rules are supplied at Service Unit level
        for (CompositionRule compositionRule : compositionRulesConfiguration.getHistoricMetricCompositionRules().getCompositionRules()) {

            Collection<String> targetServiceUnits = compositionRule.getTargetMonitoredElementIDs();
            for (MonitoredElement monitoredElement : dataToAggregate.get(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT).keySet()) {
                //if this rule block also targets this serviceStructure unit
                if (targetServiceUnits == null || targetServiceUnits.isEmpty() || targetServiceUnits.contains(monitoredElement.getId())) {
                    //extract serviceStructure unit VM level children and aggregate their data
                    for (MonitoredElement child : monitoredElement.getContainedElements()) {
                        if (child.getLevel().equals(MonitoredElement.MonitoredElementLevel.VM)) {
                            Map<MonitoredElement, List<MonitoredElementMonitoringSnapshot>> vmDataToAggregate = dataToAggregate.get(MonitoredElement.MonitoredElementLevel.VM);

                            //for each child apply aggregation rule
                            if (vmDataToAggregate != null
                                    || vmDataToAggregate.containsKey(child)) {
                                List<MonitoredElementMonitoringSnapshot> childData = vmDataToAggregate.get(child);

                                //for each metric extract list of values to be aggregated
                                Map<Metric, List<MetricValue>> valuesForEachMetric = new HashMap<Metric, List<MetricValue>>();
                                for (MonitoredElementMonitoringSnapshot childSnapshot : childData) {
                                    if (childSnapshot.getMonitoredData() == null) {
                                        continue;
                                    }
                                    for (Map.Entry<Metric, MetricValue> entry : childSnapshot.getMonitoredData().entrySet()) {
                                        if (valuesForEachMetric.containsKey(entry.getKey())) {
                                            valuesForEachMetric.get(entry.getKey()).add(entry.getValue().clone());
                                        } else {
                                            List<MetricValue> values = new ArrayList<MetricValue>();
                                            values.add(entry.getValue().clone());
                                            valuesForEachMetric.put(entry.getKey(), values);
                                        }
                                    }
                                }
                                //apply aggregation rules
                                HashMap<Metric, MetricValue> compositeData = new HashMap<Metric, MetricValue>();
                                CompositionOperation operation = compositionRule.getOperation();
                                Metric targetMetric = operation.getTargetMetric();
                                if (valuesForEachMetric.containsKey(targetMetric)) {
                                    MetricValue value = operation.apply(valuesForEachMetric.get(targetMetric));
                                    if (value != null) {
                                        compositeData.put(targetMetric, value);
                                    } else {
                                        Configuration.getLogger(this.getClass()).log(Level.WARN, "Operation" + operation.getOperationType() + " for " + operation.getTargetMetric() + " returned null");
                                    }
                                }
                                //add aggregated data to the composed snapshot
                                MonitoredElementMonitoringSnapshot childElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(child, compositeData);
                                composedMonitoringSnapshot.addMonitoredData(childElementMonitoringSnapshot);

                            }

                        }
                    }
                }
            }
        }

//        for (CompositionRule historicalCompositionRule : compositionRulesConfiguration.getHistoricDataAggregationRules().getCompositionRules()) {
//            historicalCompositionRule.apply(composedMonitoringSnapshot);
//        }

        //enrich the composite data since the composite only composes VM level data, thus we need to aggregate it to get Service level data and etc
        return enrichMonitoringData(compositionRulesConfiguration, composedMonitoringSnapshot);

    }
}
