/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.utils.converters;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Action;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Condition;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class Converter {

    public static String convertMonitoringSnapshotWithoutVM(ServiceMonitoringSnapshot serviceMonitoringSnapshot) {

        if (serviceMonitoringSnapshot == null) {
            return "";
        }

        //transform the requirements list in a map which can be used when building the JSON representation
        Map<String, Map<Metric, List<Requirement>>> requirementsMap = new HashMap<String, Map<Metric, List<Requirement>>>();

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
                if (element.getLevel() == MonitoredElement.MonitoredElementLevel.VM) {
                    continue;
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

                    List<Action> actions = monitoredElementMonitoringSnapshot.getExecutingActions();
                    //currently showing only first action
                    String actionsName = "";

                    for (Action a : actions) {
                        actionsName += a.getAction() + ";";
                    }
                    if (!actions.isEmpty()) {
                        object.put("attention", true);
                        object.put("actionName", actionsName);
                    }

                    //add metrics
                    for (Map.Entry<Metric, MetricValue> entry : monitoredElementMonitoringSnapshot.getMonitoredData().entrySet()) {
                        JSONObject metric = new JSONObject();
                        if (entry.getKey().getName().contains("serviceID")) {
                            continue;
                        }
                        metric.put("name", entry.getKey().getName() + " (" + entry.getKey().getMeasurementUnit() + ")");
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
