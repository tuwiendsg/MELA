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
package at.ac.tuwien.dsg.mela.costeval.utils.conversion;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static scala.tools.scalap.scalax.rules.scalasig.ScalaSigParsers.entry;
import static scala.util.parsing.json.JSON.root;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class CostJSONConverter extends JsonConverter {

    static final Logger logger = LoggerFactory.getLogger(CostJSONConverter.class);

    public String toJSONForRadialPieChart(ServiceMonitoringSnapshot costEnrichedSnapshot) {

        DecimalFormat df4 = new DecimalFormat("0.####");
        DecimalFormat df2 = new DecimalFormat("0.##");

//        CompositionRulesBlock block = costEnrichedSnapshot.getCostCompositionRules();
        ServiceMonitoringSnapshot sms = costEnrichedSnapshot;

//        //map pf resulting metrics
//        //Map<Level, Map<MonitoredElementID,Map<ResultingMetric,Rule>>>
//        Map<MonitoredElement.MonitoredElementLevel, Map<String, Map<Metric, CompositionRule>>> resultingMetrics = new HashMap<>();
//
//        for (CompositionRule compositionRule : block.getCompositionRules()) {
//          
//            Metric resultingMetric = compositionRule.getResultingMetric();
//            MonitoredElement.MonitoredElementLevel level = MonitoredElement.MonitoredElementLevel.valueOf(resultingMetric.getMonitoredElementLevel());
//            Map<String, Map<Metric, CompositionRule>> levelMap;
//
//            if (resultingMetrics.containsKey(level)) {
//                levelMap = resultingMetrics.get(level);
//            } else {
//                levelMap = new HashMap<>();
//                resultingMetrics.put(level, levelMap);
//            }
//
//            Map<Metric, CompositionRule> elementRules;
//            if (levelMap.containsKey(resultingMetric.getMonitoredElementID())) {
//                elementRules = levelMap.get(resultingMetric.getMonitoredElementID());
//            }else{
//                elementRules = new HashMap<>();
//                levelMap.put(resultingMetric.getMonitoredElementID(), elementRules);
//            }
//            elementRules.put(resultingMetric, compositionRule);
//        }
//        
        //
        //next 2 metrics used to discriminate between children cost and element cost, and other cost metrics which are
        //represented as leafs (have no children as they are computed directly on the element)
        //element cost sums up those, and the children cost
        //and the children cost summs up the element_cost from the children
        //total cost of element is represented as "element_cost" metric
        Metric totalElementCostMetric = new Metric("element_cost", "costUnits", Metric.MetricType.COST);

        //total children cost for each element is represented as "children_cost" metric
        Metric totalChildrenCostMetric = new Metric("children_cost", "costUnits", Metric.MetricType.COST);

//     
        List<MonitoredElement> elementsStack = new ArrayList<>();
        List<JSONObject> elementsJSONStack = new ArrayList<>();

        JSONObject root = new JSONObject();

        {
            MonitoredElement element = sms.getMonitoredService();
            elementsStack.add(element);
            root.put("name", element.getId());
            root.put("uniqueID", UUID.randomUUID().toString());
            root.put("level", element.getLevel().toString());
        }

        elementsJSONStack.add(root);

        while (!elementsStack.isEmpty()) {
            MonitoredElement currentElement = elementsStack.remove(0);
            JSONObject currentElementJSON = elementsJSONStack.remove(0);
            JSONArray childrenJSON = new JSONArray();

            //so, the cost of the element is determined by the cost of the children, and its cost elements
            //we also apply total cost in case element has no children
            MonitoredElementMonitoringSnapshot snapshot = sms.getMonitoredData(currentElement);

            for (Metric m : snapshot.getMetrics()) {

                //we are interested only in cost metrics
                if (!m.getType().equals(Metric.MetricType.COST)) {
                    continue;
                }

                //if total element cost, add it to the json, in case it has no children
                if (m.equals(totalElementCostMetric)) {
                    MetricValue value = snapshot.getMetricValue(m);
                    String valueRepresentation = value.getValueRepresentation();
                    currentElementJSON.put("displayValue", valueRepresentation);
                    currentElementJSON.put("size", valueRepresentation);
                    currentElementJSON.put("freshness", value.getFreshness());
                    //we do not represent the total cost metric. instead we create an entry for the Monitored Element in the diagram
                    currentElementJSON.put("level", currentElement.getLevel().toString());
                    currentElementJSON.put("uniqueID", UUID.randomUUID().toString());
                } else if (m.equals(totalChildrenCostMetric)) {
                    //if child, I need to expand its children
                    JSONObject child = new JSONObject();
                    child.put("name", m.getName());//+"(" + m.getMeasurementUnit()+")");
                    MetricValue value = snapshot.getMetricValue(m);
                    String valueRepresentation = value.getValueRepresentation();
                    child.put("displayValue", valueRepresentation);
                    child.put("size", valueRepresentation);
                    child.put("level", "metric");
                    child.put("freshness", value.getFreshness());
                    child.put("uniqueID", UUID.randomUUID().toString());

                    JSONArray childChildrenJSON = new JSONArray();
                    for (MonitoredElement childElement : currentElement.getContainedElements()) {
                        JSONObject childChild = new JSONObject();
                        childChild.put("name", childElement.getId());
                        childChild.put("level", childElement.getLevel().toString());
                        currentElementJSON.put("freshness", value.getFreshness());
                        childChild.put("uniqueID", currentElement.getId() + "_" + currentElement.getLevel().toString());

                        //add the child only if there is monitoring data for it
                        if (sms.contains(childElement.getLevel(), childElement)) {
                            elementsJSONStack.add(childChild);
                            elementsStack.add(childElement);
                            childChildrenJSON.add(childChild);
                        }
                    }
                    child.put("children", childChildrenJSON);
                    childrenJSON.add(child);

                } else {
                    //here means the metric is a final leaf
                    JSONObject child = new JSONObject();
                    child.put("name", m.getName());//+"(" + m.getMeasurementUnit()+")");
                    MetricValue value = snapshot.getMetricValue(m);
                    String valueRepresentation = value.getValueRepresentation();
                    child.put("displayValue", valueRepresentation);
                    child.put("size", valueRepresentation);
                    child.put("level", "metric");
                    child.put("freshness", value.getFreshness());
                    child.put("uniqueID", UUID.randomUUID().toString());

                    childrenJSON.add(child);

                }
            }
            currentElementJSON.put("children", childrenJSON);

        }

        return root.toJSONString();
    }

    public static String convertServiceStructureSnapshot(MonitoredElement rootElement, Map<UUID, Map<UUID, CloudOfferedService>> cloudServices) {

        if (rootElement == null) {
            return "";
        }

        JSONObject root = new JSONObject();
        root.put("name", rootElement.getId());
        root.put("type", "" + rootElement.getLevel());

        //going trough the serviceStructure element tree in a BFS manner
        List<MyPair> processing = new ArrayList<>();
        processing.add(new MyPair(rootElement, root));

        while (!processing.isEmpty()) {
            MyPair myPair = processing.remove(0);
            JSONObject object = myPair.jsonObject;
            MonitoredElement element = myPair.MonitoredElement;

            JSONArray children = (JSONArray) object.get("children");
            if (children == null) {
                children = new JSONArray();

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

            for (UsedCloudOfferedService usedService : element.getCloudOfferedServices()) {
                CloudOfferedService service;
                if (cloudServices.containsKey(usedService.getCloudProviderID())) {
                    Map<UUID, CloudOfferedService> cloudOfferedServices = cloudServices.get(usedService.getCloudProviderID());
                    if (cloudOfferedServices.containsKey(usedService.getId())) {
                        service = cloudOfferedServices.get(usedService.getId());
                    } else {
                        logger.error("Cloud Provider {}{}  not containing service {}{} in providers", new Object[]{usedService.getCloudProviderName(), usedService.getCloudProviderID(), usedService.getName(), usedService.getId()});
                        continue;
                    }
                } else {
                    logger.error("Cloud Provider {}{} not found in providers", new Object[]{usedService.getCloudProviderName(), usedService.getCloudProviderID()});
                    continue;
                }

                JSONObject offeredServiceObject = new JSONObject();

                offeredServiceObject.put("name", service.getCategory() + "." + service.getSubcategory() + "." + service.getName());
                offeredServiceObject.put("type", "UsedService");
                children.add(offeredServiceObject);
            }

            if (children.size() > 0) {
                object.put("children", children);
            }

        }

        return root.toJSONString();

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
