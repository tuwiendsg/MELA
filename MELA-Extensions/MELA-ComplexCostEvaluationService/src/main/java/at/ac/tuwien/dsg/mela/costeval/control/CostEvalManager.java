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
package at.ac.tuwien.dsg.mela.costeval.control;

import at.ac.tuwien.dsg.mela.costeval.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.XmlConverter;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticitySpaceXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.*;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshots;
import at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngine;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import org.json.simple.JSONArray;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 * <p/>
 * Delegates the functionality of configuring MELA for instant monitoring and
 * analysis
 */
@Service
public class CostEvalManager {

    static final Logger log = LoggerFactory.getLogger(CostEvalManager.class);

    /**
     * Value of metric that indicates that the unit has not started yet .Used to
     * avoid analyzing the elasticity space also over that metric
     */
    private static final String UNSTABLE_METRIC_VALUE = "-1";

//    private Requirements requirements;
//
//    private CompositionRulesConfiguration compositionRulesConfiguration;
//
//    private MonitoredElement serviceConfiguration;
    @Autowired
    private InstantMonitoringDataAnalysisEngine instantMonitoringDataAnalysisEngine;
    @Autowired
    private CostEvalEngine costEvalEngine;

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private JsonConverter jsonConverter;

    @Autowired
    private XmlConverter xmlConverter;

    //TODO: persist this
    private List<ServiceUnit> serviceUnits;

    {
        serviceUnits = new ArrayList<ServiceUnit>();
    }

    //in future cost casching should be done using persistence
    ServiceMonitoringSnapshot completeCost;

    @PostConstruct
    public void init() {
        if (instantMonitoringDataAnalysisEngine == null) {
            instantMonitoringDataAnalysisEngine = new InstantMonitoringDataAnalysisEngine();
        }

        if (costEvalEngine == null) {
            costEvalEngine = new CostEvalEngine();
        }

        // get latest config
//        ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceDelegate.getLatestConfiguration(serviceID);
//        persistenceDelegate.setMonitoringId(configurationXMLRepresentation.getServiceConfiguration().getId());
        // open proper sql access
        //persistenceDelegate = new PersistenceSQLAccess(configurationXMLRepresentation.getServiceConfiguration().getId());
//        setInitialServiceConfiguration(configurationXMLRepresentation.getServiceConfiguration());
//        setInitialCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());
//        setInitialRequirements(configurationXMLRepresentation.getRequirements());
    }

    protected CostEvalManager() {
    }

    public MonitoredElement getServiceConfiguration(String serviceID) {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg != null) {
            return cfg.getServiceConfiguration();
        } else {
            return new MonitoredElement();
        }
    }

//
//    private  void setInitialServiceConfiguration(MonitoredElement serviceConfiguration) {
//        this.serviceConfiguration = serviceConfiguration;
//    }
    // actually removes all VMs and Virtual Clusters from the ServiceUnit and
    // adds new ones.
    public List<ServiceUnit> getServiceUnits() {
        return serviceUnits;
    }

    public void setServiceUnits(List<ServiceUnit> serviceUnits) {
        this.serviceUnits = serviceUnits;
    }

    public void updateServiceConfiguration(MonitoredElement serviceConfiguration) {

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceConfiguration.getId());

        // extract all ServiceUnit level monitored elements from both services,
        // and replace their children
        Map<MonitoredElement, MonitoredElement> serviceUnits = new HashMap<MonitoredElement, MonitoredElement>();
        for (MonitoredElement element : cfg.getServiceConfiguration()) {
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                // remove element's children
                element.getContainedElements().clear();
                serviceUnits.put(element, element);
            }
        }

        // go trough the new service, and for each Service Unit, add its
        // children (containing both Virtual Machines and Virtual Clusters) to
        // the original service
        for (MonitoredElement element : serviceConfiguration) {
            if (serviceUnits.containsKey(element)) {
                // bad practice. breaks incapsulation
                serviceUnits.get(element).getContainedElements().addAll(element.getContainedElements());
            }
        }

    }

    public Requirements getRequirements(String serviceID) {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg != null) {
            return cfg.getRequirements();
        } else {
            return new Requirements();
        }

    }
//
//    public boolean testIfAllVMsReportMEtricsGreaterThanZero(String serviceID) {
//        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);
//        if (cxmlr != null) {
////            MonitoredElement element = cxmlr.getServiceConfiguration();
//            ServiceMonitoringSnapshot data = persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId());
//            if (data == null) {
//                log.error("Monitoring Data not found for " + serviceID);
//                throw new RuntimeException("Monitoring Data not found for " + serviceID);
//            }
//            for (MonitoredElementMonitoringSnapshot childSnapshot : data.getMonitoredData(MonitoredElement.MonitoredElementLevel.VM).values()) {
//                for (MetricValue metricValue : childSnapshot.getMonitoredData().values()) {
//                    if (metricValue.getValueType().equals(MetricValue.ValueType.NUMERIC)) {
//                        if (metricValue.compareTo(new MetricValue(0)) < 0) {
//                            return false;
//                        }
//                    }
//                }
//            }
//        } else {
//            return true;
//        }
//        return true;
//    }

    public CompositionRulesConfiguration getCompositionRulesConfiguration(String serviceID) {

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg != null) {
            return cfg.getCompositionRulesConfiguration();
        } else {
            return new CompositionRulesConfiguration();
        }

    }

//    public AnalysisReport analyzeLatestMonitoringData(String serviceID) {
//        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);
//        if (cxmlr != null) {
//            return instantMonitoringDataAnalysisEngine.analyzeRequirements(persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId()), cxmlr.getRequirements());
//        } else {
//            return new AnalysisReport(new ServiceMonitoringSnapshot(), new Requirements());
//        }
//
//    }
//
//    public Collection<Metric> getAvailableMetricsForMonitoredElement(String serviceID, MonitoredElement monitoredElement) {
//        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);
//
//        if (cxmlr != null) {
//            return persistenceDelegate.getAvailableMetrics(monitoredElement, cxmlr.getServiceConfiguration().getId());
//        } else {
//            return new ArrayList<Metric>();
//        }
//    }
    public MonitoredElementMonitoringSnapshot getLatestMonitoringData(String serviceID) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cxmlr == null) {
            return new MonitoredElementMonitoringSnapshot();
        }

        ServiceMonitoringSnapshot monitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId());
        if (monitoringSnapshot != null && !monitoringSnapshot.getMonitoredData().isEmpty()) {
            return monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next();
        } else {
            return new MonitoredElementMonitoringSnapshot();
        }
    }

    public MonitoredElementMonitoringSnapshot getLatestMonitoringData(String serviceID, MonitoredElement element) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cxmlr == null) {
            return new MonitoredElementMonitoringSnapshot();
        }
        ServiceMonitoringSnapshot monitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId());
        if (monitoringSnapshot != null && !monitoringSnapshot.getMonitoredData().isEmpty()) {
            //keep data only for element
            MonitoredElementMonitoringSnapshot data = monitoringSnapshot.getMonitoredData(element);
            data.setChildren(null);
            return data;
        } else {
            return new MonitoredElementMonitoringSnapshot();
        }
    }

//    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringData(String serviceID) {
//        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
//        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractMonitoringData(serviceID)) {
//            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
//        }
//        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
//        snapshots.setChildren(elementMonitoringSnapshots);
//        return snapshots;
//    }
//
//    public MonitoredElementMonitoringSnapshots getAggregatedMonitoringDataInTimeInterval(String serviceID, int startTimestampID, int endTimestampID) {
//        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
//        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractMonitoringDataByTimeInterval(startTimestampID, endTimestampID, serviceID)) {
//            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
//        }
//        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
//        snapshots.setChildren(elementMonitoringSnapshots);
//        return snapshots;
//    }
//
//    public MonitoredElementMonitoringSnapshots getLastXAggregatedMonitoringData(String serviceID, int count) {
//        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
//        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractLastXMonitoringDataSnapshots(count, serviceID)) {
//            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
//        }
//        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
//        snapshots.setChildren(elementMonitoringSnapshots);
//        return snapshots;
//    }
    // uses a lot of memory (all directly in memory)
    public String getElasticityPathway(String serviceID, MonitoredElement element) {
//        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
//        // if no service configuration, we can't have elasticity space function
//        // if no compositionRulesConfiguration we have no data
//        if (cfg != null && !elasticityAnalysisEnabled || cfg.getServiceConfiguration() == null && cfg.getCompositionRulesConfiguration() != null) {
//            log.warn("Elasticity analysis disabled, or no service configuration or composition rules configuration");
//            JSONObject elSpaceJSON = new JSONObject();
//            elSpaceJSON.put("name", "ElPathway");
//            return elSpaceJSON.toJSONString();
//        }

        Date before = new Date();

        // int recordsCount = persistenceDelegate.getRecordsCount();
        // first, read from the sql of monitoring data, in increments of 10, and
        // train the elasticity space function
        LightweightEncounterRateElasticityPathway elasticityPathway = null;

        List<Metric> metrics = null;

        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(serviceID);

        if (space == null) {
            log.error("Elasticity Space returned is null");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElPathway");
            return elSpaceJSON.toJSONString();
        }

        Map<Metric, List<MetricValue>> map = space.getMonitoredDataForService(element);
        if (map != null) {
            metrics = new ArrayList<Metric>(map.keySet());
            // we need to know the number of weights to add in instantiation
            elasticityPathway = new LightweightEncounterRateElasticityPathway(metrics.size());
        } else {
            log.error("Elasticity Space not found for " + element.getId());
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElPathway");
            return elSpaceJSON.toJSONString();
        }

        elasticityPathway.trainElasticityPathway(map);

        List<Neuron> neurons = elasticityPathway.getSituationGroups();
        if (metrics == null) {
            log.error("Service Element " + element.getId() + " at level " + element.getLevel() + " was not found in service structure");
            /*JSONObject elSpaceJSON = new JSONObject();
             elSpaceJSON.put("name", "Service not found"); // todo throw a ServiceNotFoundException that we can map and return a 404 in the REST API
             return elSpaceJSON.toJSONString();*/
            throw new ServiceElementNotFoundException(element);
        } else {
            String converted = jsonConverter.convertElasticityPathway(metrics, neurons);
            Date after = new Date();
            log.debug("El Pathway cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
            return converted;
        }

    }

    public ElasticityPathwayXML getElasticityPathwayInXML(String serviceID, MonitoredElement element) {

        ElasticityPathwayXML elasticityPathwayXML = new ElasticityPathwayXML();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (cfg == null || cfg.getServiceConfiguration() == null && cfg.getCompositionRulesConfiguration() != null) {
            log.warn("Elasticity analysis disabled, or no service configuration or composition rules configuration");
            return elasticityPathwayXML;
        }

        Date before = new Date();

        // int recordsCount = persistenceDelegate.getRecordsCount();
        // first, read from the sql of monitoring data, in increments of 10, and
        // train the elasticity space function
        LightweightEncounterRateElasticityPathway elasticityPathway = null;

        List<Metric> metrics = null;

        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(element.getId());

        Map<Metric, List<MetricValue>> map = space.getMonitoredDataForService(element);
        if (map != null) {
            metrics = new ArrayList<Metric>(map.keySet());
            // we need to know the number of weights to add in instantiation
            elasticityPathway = new LightweightEncounterRateElasticityPathway(metrics.size());
        }

        elasticityPathway.trainElasticityPathway(map);

        List<Neuron> neurons = elasticityPathway.getSituationGroups();
        if (metrics == null) {
            log.error("Service Element " + element.getId() + " at level " + element.getLevel() + " was not found in service structure");
            return elasticityPathwayXML;
        } else {
            elasticityPathwayXML = xmlConverter.convertElasticityPathwayToXML(metrics, neurons, element);
            Date after = new Date();
            log.debug("El Pathway cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
            return elasticityPathwayXML;
        }

    }

    public String getElasticitySpaceJSON(String serviceID, MonitoredElement element) {

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (cfg == null || cfg.getServiceConfiguration() == null && cfg.getCompositionRulesConfiguration() != null) {
            log.warn("Elasticity analysis disabled, or no service configuration or composition rules configuration");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElSpace");
            return elSpaceJSON.toJSONString();
        }

        Date before = new Date();
        ElasticitySpace space = extractAndUpdateElasticitySpace(serviceID);

        String jsonRepr = jsonConverter.convertElasticitySpace(space, element);

        Date after = new Date();
        log.debug("El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return jsonRepr;
    }

    /**
     * @param element
     * @return also contains the monitored values
     */
    public ElasticitySpaceXML getCompleteElasticitySpaceXML(String serviceID, MonitoredElement element) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return new ElasticitySpaceXML();
        }

        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(cfg.getServiceConfiguration().getId());
        ElasticitySpaceXML elasticitySpaceXML = xmlConverter.convertElasticitySpaceToXMLCompletely(space, element);
        Date after = new Date();
        log.debug("El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return elasticitySpaceXML;
    }

    /**
     * @param element
     * @return contains only the Metric and their ElasticityBoundaries
     */
    public ElasticitySpaceXML getElasticitySpaceXML(String serviceID, MonitoredElement element) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg == null) {
            return new ElasticitySpaceXML();
        }

        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(cfg.getServiceConfiguration().getId());
        ElasticitySpaceXML elasticitySpaceXML = xmlConverter.convertElasticitySpaceToXML(space, element);
        Date after = new Date();
        log.debug("El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return elasticitySpaceXML;
    }

    public String getLatestMonitoringDataEnrichedWithCostINJSON(String serviceID) {

        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }

        //TODO: compute cost as we go to avoid memory overflows
        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(serviceID);

        if (!allMonData.isEmpty()) {

            //as I extract 1000 entries at a time to avoid memory overflow, I need to read the rest
            //TODO: compute cost as we go to avoid memory overflows
            do {
                int timestampID = allMonData.get(allMonData.size() - 1).getTimestampID();
                List<ServiceMonitoringSnapshot> restOfData = persistenceDelegate.extractMonitoringData(timestampID, serviceID);
                if (restOfData.isEmpty()) {
                    break;
                } else {
                    allMonData.addAll(restOfData);
                }
            } while (true);
        }

        ServiceMonitoringSnapshot completeCostSnapshot = costEvalEngine.getLastMonSnapshotEnrichedWithCost(serviceUnits, allMonData);
        if (completeCostSnapshot == null) {
            return "{nothing}";
        }

        String converted = jsonConverter.convertMonitoringSnapshot(completeCostSnapshot);

        Date after = new Date();
        log.debug("Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return converted;
    }

    public MonitoredElementMonitoringSnapshot getTotalServiceCostXML(String serviceID) {
        //TODO: compute cost as we go to avoid memory overflows
        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(serviceID);

        if (!allMonData.isEmpty()) {

            //as I extract 1000 entries at a time to avoid memory overflow, I need to read the rest
            //TODO: compute cost as we go to avoid memory overflows
            do {
                int timestampID = allMonData.get(allMonData.size() - 1).getTimestampID();
                List<ServiceMonitoringSnapshot> restOfData = persistenceDelegate.extractMonitoringData(timestampID, serviceID);
                if (restOfData.isEmpty()) {
                    break;
                } else {
                    allMonData.addAll(restOfData);
                }
            } while (true);
        }
        ServiceMonitoringSnapshot completeCostSnapshot = costEvalEngine.getTotalCost(serviceUnits, allMonData);;
        MonitoredElementMonitoringSnapshot serviceSnapshot = completeCostSnapshot.getMonitoredData(new MonitoredElement(serviceID).withLevel(MonitoredElement.MonitoredElementLevel.SERVICE));
        return serviceSnapshot;
    }

    public String getTotalServiceCostJSON(String serviceID) {
        //TODO: compute cost as we go to avoid memory overflows
        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(serviceID);

//        if (!allMonData.isEmpty()) {
//
//            //as I extract 1000 entries at a time to avoid memory overflow, I need to read the rest
//            //TODO: compute cost as we go to avoid memory overflows
//            do {
//                int timestampID = allMonData.get(allMonData.size() - 1).getTimestampID();
//                List<ServiceMonitoringSnapshot> restOfData = persistenceDelegate.extractMonitoringData(timestampID, serviceID);
//                if (restOfData.isEmpty()) {
//                    break;
//                } else {
//                    allMonData.addAll(restOfData);
//                }
//            } while (true);
//        }
//        if ((completeCost == null) || (allMonData.get(allMonData.size() - 1).getTimestampID() > completeCost.getTimestampID())) {
        ServiceMonitoringSnapshot completeCostSnapshot = costEvalEngine.getTotalCost(serviceUnits, allMonData);;
        completeCostSnapshot.setTimestamp(allMonData.get(allMonData.size() - 1).getTimestamp());
        completeCostSnapshot.setTimestampID(allMonData.get(allMonData.size() - 1).getTimestampID());
        completeCost = completeCostSnapshot;
//        }

        if (completeCost == null) {
            return "{nothing}";
        }

        String converted = jsonConverter.convertMonitoringSnapshot(completeCost);

        Date after = new Date();
        return converted;
    }

    public MonitoredElement getLatestServiceStructure(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg == null) {
            return new MonitoredElement();
        }
        ServiceMonitoringSnapshot serviceMonitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(cfg.getServiceConfiguration().getId());
        Date after = new Date();
        log.debug("Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return serviceMonitoringSnapshot.getMonitoredService();
    }

    public String getMetricCompositionRules(String serviceID) {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg != null && cfg.getCompositionRulesConfiguration() != null) {
            return jsonConverter.convertToJSON(cfg.getCompositionRulesConfiguration().getMetricCompositionRules());
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "No composition rules yet");
            return jsonObject.toJSONString();
        }
    }

    private ElasticitySpace extractAndUpdateElasticitySpace(String serviceID) {
//        //note persistenceDelegate.extractMonitoringData returns max 1000 rows
//
//        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
//
//        if (cfg == null) {
//            return new ElasticitySpace(new MonitoredElement());
//        }

//        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(cfg.getServiceConfiguration().getId());
//        //if space == null, compute it 
//        if (space == null) {
//            //if space is null, compute it from all aggregated monitored data recorded so far
//            List<ServiceMonitoringSnapshot> dataFromTimestamp = persistenceDelegate.extractMonitoringData(cfg.getServiceConfiguration().getId());
//
//            //clean by removing all metric values which are below zero, meaning the units are not running yet
//            if (dataFromTimestamp != null) {
//                dataFromTimestamp = cleanMonData(dataFromTimestamp);
//            }
//
//            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(cfg.getServiceConfiguration());
//            fct.setRequirements(cfg.getRequirements());
//            fct.trainElasticitySpace(dataFromTimestamp);
//            space = fct.getElasticitySpace();
//
//            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
//            space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());
//
//        }
//
//        //if space is not null, update it with new data
//        List<ServiceMonitoringSnapshot> dataFromTimestamp = null;
//
//        //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
//        do {
//            dataFromTimestamp = persistenceDelegate.extractMonitoringData(space.getTimestampID(), cfg.getServiceConfiguration().getId());
//
//            //clean by removing all metric values which are below zero, meaning the units are not running yet
//            if (dataFromTimestamp != null) {
//                dataFromTimestamp = cleanMonData(dataFromTimestamp);
//            }
//
//            //check if new data has been collected between elasticity space querries
//            if (!dataFromTimestamp.isEmpty()) {
//                ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(cfg.getServiceConfiguration());
//                fct.setRequirements(cfg.getRequirements());
//                fct.trainElasticitySpace(space, dataFromTimestamp, cfg.getRequirements());
//                //set to the new space the timespaceID of the last snapshot monitored data used to compute it
//                space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());
//
//            }
//
//        } while (!dataFromTimestamp.isEmpty());
//
//        //persist cached space
//        persistenceDelegate.writeElasticitySpace(space, cfg.getServiceConfiguration().getId());
//
//        return space;
        return persistenceDelegate.extractLatestElasticitySpace(serviceID);
    }

//    /**
//     * Careful, modifies the supplied snapshots in place
//     *
//     * @param snapshots snapshots to be cleansed by the UNSTABLE_METRIC_VALUE
//     * @return snapshots
//     */
//    private List<ServiceMonitoringSnapshot> cleanMonData(List<ServiceMonitoringSnapshot> snapshots) {
//
//        for (ServiceMonitoringSnapshot monitoringSnapshot : snapshots) {
//            for (Map<MonitoredElement, MonitoredElementMonitoringSnapshot> map : monitoringSnapshot.getMonitoredData().values()) {
//                for (MonitoredElementMonitoringSnapshot elementMonitoringSnapshot : map.values()) {
//                    Iterator<Metric> it = elementMonitoringSnapshot.getMetrics().iterator();
//                    while (it.hasNext()) {
//                        Metric m = it.next();
//                        if (elementMonitoringSnapshot.getMetricValue(m).toString().contains(UNSTABLE_METRIC_VALUE)) {
//                            elementMonitoringSnapshot.getMonitoredData().remove(m);
//                        }
//                    }
//                }
//            }
//        }
//
//        return snapshots;
//    }
    public String getAllManagedServicesIDs() {

        JSONArray array = new JSONArray();

        for (String s : persistenceDelegate.getMonitoringSequencesIDs()) {
            JSONObject o = new JSONObject();
            o.put("id", s);
            array.add(o);
        }
        return array.toJSONString();
    }
}
