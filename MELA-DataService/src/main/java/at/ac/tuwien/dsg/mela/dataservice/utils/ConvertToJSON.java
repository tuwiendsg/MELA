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
package at.ac.tuwien.dsg.mela.dataservice.utils;

import at.ac.tuwien.dsg.mela.common.requirements.Condition;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
public class ConvertToJSON {

    private static DecimalFormat df4 = new DecimalFormat("0.####");
    private static DecimalFormat df2 = new DecimalFormat("0.##");

  
    public static String convertMonitoringSnapshot(ServiceMonitoringSnapshot serviceMonitoringSnapshot, Requirements requirements, Map<MonitoredElement, String> actionsInExecution) {

        if (serviceMonitoringSnapshot == null) {
            return "";
        }

        //transform the requirements list in a map which can be used when building the JSON representation
        Map<String, Map<Metric, List<Requirement>>> requirementsMap = new HashMap<String, Map<Metric, List<Requirement>>>();
        if (requirements != null) {
            //for each requirement
            for (Requirement requirement : requirements.getRequirements()) {
                //for each service element ID targeted by the requirement
                for (String id : requirement.getTargetMonitoredElementIDs()) {
                    if (requirementsMap.containsKey(id)) {
                        Map<Metric, List<Requirement>> reqMap = requirementsMap.get(id);
                        if (reqMap.containsKey(requirement.getMetric())) {
                            reqMap.get(requirement.getMetric()).add(requirement);
                        } else {
                            List<Requirement> list = new ArrayList<Requirement>();
                            list.add(requirement);
                            reqMap.put(requirement.getMetric(), list);
                        }
                    } else {
                        Map<Metric, List<Requirement>> reqMap = new HashMap<Metric, List<Requirement>>();
                        List<Requirement> list = new ArrayList<Requirement>();
                        list.add(requirement);
                        reqMap.put(requirement.getMetric(), list);
                        requirementsMap.put(id, reqMap);
                    }
                }
            }
        }


        //assumes there is one SERVICE level element per snapshot
        Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> monitoredData = serviceMonitoringSnapshot.getMonitoredData();
        if (monitoredData != null && monitoredData.containsKey(MonitoredElement.MonitoredElementLevel.SERVICE)) {

            //access root service element
            MonitoredElement rootElement = monitoredData.get(MonitoredElement.MonitoredElementLevel.SERVICE).keySet().iterator().next();

            JSONObject root = new JSONObject();
            root.put("name", rootElement.getId());
            root.put("type", "" + rootElement.getLevel());

            //going trough the serviceStructure element tree in a BFS manner
            List<MyPair> processing = new ArrayList<MyPair>();
            processing.add(new MyPair(rootElement, root));

            while (!processing.isEmpty()) {
                MyPair myPair = processing.remove(0);
                JSONObject object = myPair.jsonObject;
                MonitoredElement element = myPair.MonitoredElement;

                if (actionsInExecution.containsKey(element)) {
                    object.put("attention", true);
                    object.put("actionName", actionsInExecution.get(element));
                }

                JSONArray children = (JSONArray) object.get("children");
                if (children == null) {
                    children = new JSONArray();
                    object.put("children", children);
                }

                //add children
                for (MonitoredElement child : element.getContainedElements()) {
                    JSONObject childElement = new JSONObject();
                    childElement.put("name", child.getId());
                    childElement.put("type", "" + child.getLevel());
                    JSONArray childrenChildren = new JSONArray();
                    childElement.put("children", childrenChildren);
                    processing.add(new MyPair(child, childElement));
                    children.add(childElement);
                }

                JSONArray metrics = new JSONArray();
                if (monitoredData.containsKey(element.getLevel())) {
                    MonitoredElementMonitoringSnapshot monitoredElementMonitoringSnapshot = monitoredData.get(element.getLevel()).get(element);

                    //add metrics
                    for (Map.Entry<Metric, MetricValue> entry : monitoredElementMonitoringSnapshot.getMonitoredData().entrySet()) {
                        JSONObject metric = new JSONObject();
                        if (entry.getKey().getName().contains("serviceID")) {
                            continue;
                        }
                        metric.put("name", entry.getValue().getValueRepresentation() + " [ " + entry.getKey().getName() + " (" + entry.getKey().getMeasurementUnit() + ") ]");
                        metric.put("type", "metric");

                        //if we have requirements for this service element ID
                        if (requirementsMap.containsKey(element.getId())) {
                            Map<Metric, List<Requirement>> reqMap = requirementsMap.get(element.getId());

                            //if we have requirement for this metric
                            if (reqMap.containsKey(entry.getKey())) {
                                List<Requirement> list = reqMap.get(entry.getKey());
                                //for all metric requirements, add the metric conditions in JSON
                                JSONArray conditions = new JSONArray();

                                for (Requirement requirement : list) {
                                    for (Condition condition : requirement.getConditions()) {
                                        JSONObject conditionJSON = new JSONObject();
                                        conditionJSON.put("name", "MUST BE " + condition.toString());
                                        conditionJSON.put("type", "requirement");
                                        conditions.add(conditionJSON);
                                    }
                                }
                                metric.put("children", conditions);
                            }
                        }

//                JSONObject value = new JSONObject();
//                value.put("name",""+entry.getValue().getValueRepresentation());
//                value.put("type","metric");
//
//                JSONArray valueChildren = new JSONArray();
//                valueChildren.add(value);
//
//                metric.put("children", valueChildren);

                        children.add(metric);
                    }

//                    //add requirements
//                    if (requirementsMap.containsKey(element.getId())) {
//                        for (Requirement requirement : requirementsMap.get(element.getId())) {
//                            JSONObject requirementJSON = new JSONObject();
//                            requirementJSON.put("name", requirement.getMetric().getName());
//                            requirementJSON.put("type", "requirement");
//                            //for all conditions add children to requirement (if more conditions per metric)
//                            JSONArray conditions = new JSONArray();
//                            for (Condition condition : requirement.getConditions()) {
//                                JSONObject conditionJSON = new JSONObject();
//                                conditionJSON.put("name", condition.toString());
//                                conditionJSON.put("type", "condition");
//                                conditions.add(conditionJSON);
//                            }
//                            requirementJSON.put("children", conditions);
//                            children.add(requirementJSON);
//                        }
//
//                    }
                }
//            object.put("metrics", metrics);


            }



            String string = root.toJSONString();
//            string = string.replaceAll("\"", "!");

            return string;
        } else {
            JSONObject root = new JSONObject();
            root.put("name", "No monitoring data");
            return "{\"name\":\"No Data\",\"type\":\"METRIC\"}";
        }
    }

    //convert metrics to JSON
    public static String convertToJSON(CompositionRulesBlock compositionRulesBlock) {

        JSONArray compositionRules = new JSONArray();

        for (CompositionRule compositionRule : compositionRulesBlock.getCompositionRules()) {
            Metric resultingMetric = compositionRule.getResultingMetric();
            if (resultingMetric == null) {
                continue;
            }

            if (compositionRule.getTargetMonitoredElementLevel() == null) {
                System.out.println(resultingMetric.getName() + " " + resultingMetric.getMeasurementUnit());
            }

            JSONObject jsonMetric = new JSONObject();
            jsonMetric.put("name", resultingMetric.getName());
            jsonMetric.put("targetLevel", compositionRule.getTargetMonitoredElementLevel().toString());
            jsonMetric.put("targetMonitoredElementIDs", compositionRule.getTargetMonitoredElementIDs());

            JSONArray children = new JSONArray();


            List<CompositionOperation> operations = new ArrayList<CompositionOperation>();
            operations.add(compositionRule.getOperation());
            while (!operations.isEmpty()) {
                CompositionOperation operation = operations.remove(0);
                operations.addAll(operation.getSubOperations());
                if (operation.getTargetMetric() != null) {
                    JSONObject targetRest = new JSONObject();
                    Metric targetMetric = operation.getTargetMetric();
                    targetRest.put("name", targetMetric.getName());
                    targetRest.put("targetLevel", operation.getMetricSourceMonitoredElementLevel().toString());
                    targetRest.put("targetMonitoredElementIDs", operation.getMetricSourceMonitoredElementIDs());
                    children.add(targetRest);
                }
            }
            jsonMetric.put("children", children);
            compositionRules.add(jsonMetric);
        }

        return compositionRules.toJSONString();
    }

    private static class MyPair {

        public MonitoredElement MonitoredElement;
        public JSONObject jsonObject;

        private MyPair() {
        }

        public MyPair(MonitoredElement MonitoredElement, JSONObject jsonObject) {
            this.MonitoredElement = MonitoredElement;
            this.jsonObject = jsonObject;
        }
    }
}
