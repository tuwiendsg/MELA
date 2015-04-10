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
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter;
import static at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter.convertMonitoringSnapshot;
import static at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter.convertToJSON;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
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

        ServiceMonitoringSnapshot sms = costEnrichedSnapshot;

        /**
         * Restructure as following: Take out all VMs. For each VM, take its
         * used Cloud Offered Service, and put it in the Unit instead of the VM.
         * Of course, when system scales, we have different Service instances
         * used by different VMs, so we must sum up the metrics for each used
         * service This way we get an overview over the unit's cost, and not on
         * the VM cost.
         */
        //get all units, and get all of their VMs, and replace them with used Services
        {
            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> unitsData = sms.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
            for (MonitoredElement unit : unitsData.keySet()) {

                MonitoredElementMonitoringSnapshot unitSnapshot = unitsData.get(unit);

                List<MonitoredElementMonitoringSnapshot> addedChildrenServicesInsteadOfVMS = new ArrayList<>();

                for (MonitoredElement childVMs : unit.getContainedElements()) {
                    //interested only in volatile VMs
                    if (!childVMs.getLevel().equals(MonitoredElement.MonitoredElementLevel.VM)) {
                        continue;
                    }

                    for (UsedCloudOfferedService service : childVMs.getCloudOfferedServices()) {

                        MonitoredElement usedCloudServiceMonitoredElement = new MonitoredElement()
                                //here we use ID not instance UUID as we want all service instances to be aggregated as cost into the one used service
                                .withId(service.getId().toString()  + "_" + unit.getId()) //also add unit ID as we might have SAME service type for different VMs
                                .withName(service.getName())
                                .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                        MonitoredElementMonitoringSnapshot serviceSnapshot;
                        if (sms.contains(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE, usedCloudServiceMonitoredElement)) {
                            serviceSnapshot = sms.getMonitoredData(usedCloudServiceMonitoredElement);
                        } else {
                            serviceSnapshot = new MonitoredElementMonitoringSnapshot(usedCloudServiceMonitoredElement);
                            sms.addMonitoredData(serviceSnapshot);
                            addedChildrenServicesInsteadOfVMS.add(serviceSnapshot);
                        }

                        //get cost metrics from service instance
                        MonitoredElement usedCloudServiceInstance = new MonitoredElement()
                                //here we use ID not instance UUID as we want all service instances to be aggregated as cost into the one used service
                                .withId(service.getInstanceUUID().toString())
                                .withName(service.getName())
                                .withLevel(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE);

                        MonitoredElementMonitoringSnapshot serviceInstanceMetrics = sms.getMonitoredData(usedCloudServiceInstance);
                        for (Metric metric : serviceInstanceMetrics.getMetrics()) {
                            if (serviceSnapshot.containsMetric(metric)) {
                                MetricValue oldValue = serviceSnapshot.getMetricValue(metric);
                                MetricValue newValue = serviceInstanceMetrics.getMetricValue(metric);
                                //aggregate cost metrics as SUM
                                oldValue.sum(newValue);
                                serviceSnapshot.putMetric(metric, oldValue);

                            } else {
                                serviceSnapshot.putMetric(metric, serviceInstanceMetrics.getMetricValue(metric));
                            }
                        }

                    }
                }

                unit.getContainedElements().clear();

                //remove all VMs from mon data and etc, and replace with newly added stuff
                for (MonitoredElementMonitoringSnapshot s : unitSnapshot.getChildren()) {
                    if (sms.getMonitoredData().containsKey(s.getMonitoredElement().getLevel())) {
                        sms.getMonitoredData().get(s.getMonitoredElement().getLevel()).remove(s.getMonitoredElement());
                    }
                }
                unitSnapshot.getChildren().clear();

                for (MonitoredElementMonitoringSnapshot added : addedChildrenServicesInsteadOfVMS) {
                    if (sms.getMonitoredData().containsKey(added.getMonitoredElement().getLevel())) {
                        sms.getMonitoredData().get(added.getMonitoredElement().getLevel()).put(added.getMonitoredElement(), added);
                    } else {
                        Map<MonitoredElement, MonitoredElementMonitoringSnapshot> s = new HashMap<>();
                        s.put(added.getMonitoredElement(), added);
                        sms.getMonitoredData().put(added.getMonitoredElement().getLevel(), s);
                    }
                    unit.addElement(added.getMonitoredElement());
                }

                unitSnapshot.getChildren().addAll(addedChildrenServicesInsteadOfVMS);
            }
        }

        //
        //next 2 metrics used to discriminate between children cost and element cost, and other cost metrics which are
        //represented as leafs (have no children as they are computed directly on the element)
        //element cost sums up those, and the children cost
        //and the children cost summs up the element_cost from the children
        //total cost of element is represented as "element_cost" metric
        Metric totalElementCostMetric = new Metric("element_cost", "costUnits", Metric.MetricType.COST);

        //total children cost for each element is represented as "children_cost" metric
        Metric totalChildrenCostMetric = new Metric("children_cost", "costUnits", Metric.MetricType.COST);

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
//                    //if child, I need to expand its children
//                    JSONObject child = new JSONObject();
//                    child.put("name", m.getName());//+"(" + m.getMeasurementUnit()+")");
                    MetricValue value = snapshot.getMetricValue(m);
//                    String valueRepresentation = value.getValueRepresentation();
//                    child.put("displayValue", valueRepresentation);
//                    child.put("size", valueRepresentation);
//                    child.put("level", "metric");
//                    child.put("freshness", value.getFreshness());
//                    child.put("uniqueID", UUID.randomUUID().toString());
//
//                    JSONArray childChildrenJSON = new JSONArray();
                    for (MonitoredElement childElement : currentElement.getContainedElements()) {
                        JSONObject childChild = new JSONObject();
                        childChild.put("name", (childElement.getName().length() > 0) ? childElement.getName() : childElement.getId());
                        childChild.put("level", childElement.getLevel().toString());
                        currentElementJSON.put("freshness", value.getFreshness());
                        childChild.put("uniqueID", currentElement.getId() + "_" + currentElement.getLevel().toString());

                        //add the child only if there is monitoring data for it
                        if (sms.contains(childElement.getLevel(), childElement)) {
                            elementsJSONStack.add(childChild);
                            elementsStack.add(childElement);
                            childrenJSON.add(childChild);
                        }
                    }
//                    child.put("children", childChildrenJSON);
//                    childrenJSON.add(child);

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
                offeredServiceObject.put("type", MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE.toString());
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
