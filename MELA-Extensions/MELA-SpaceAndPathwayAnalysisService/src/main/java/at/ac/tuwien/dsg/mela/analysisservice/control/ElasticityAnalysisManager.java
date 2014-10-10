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
package at.ac.tuwien.dsg.mela.analysisservice.control;

import at.ac.tuwien.dsg.mela.analysisservice.connectors.MelaDataServiceConfigurationAPIConnector;
import at.ac.tuwien.dsg.mela.analysisservice.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.XmlConverter;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticitySpaceXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.*;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshots;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONArray;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 * <p/>
 * Delegates the functionality of configuring MELA for instant monitoring and
 * analysis
 */
@Service
public class ElasticityAnalysisManager {

    static final Logger log = LoggerFactory.getLogger(ElasticityAnalysisManager.class);

    /**
     * Value of metric that indicates that the unit has not started yet .Used to
     * avoid analyzing the elasticity space also over that metric
     */
    private static final String UNSTABLE_METRIC_VALUE = "-1";

    @Value("${analysisservice.elasticityanalysis:true}")
    private boolean elasticityAnalysisEnabled;

    @Autowired
    private MelaDataServiceConfigurationAPIConnector melaApi;

//    private Requirements requirements;
//
//    private CompositionRulesConfiguration compositionRulesConfiguration;
//
//    private MonitoredElement serviceConfiguration;
    @Autowired
    private InstantMonitoringDataAnalysisEngine instantMonitoringDataAnalysisEngine;

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private JsonConverter jsonConverter;

    @Autowired
    private XmlConverter xmlConverter;

    private Map<MonitoredElement, Timer> elasticitySpaceComputationTimers;

    private Timer updateManagedServicesTimer;

    {
        elasticitySpaceComputationTimers = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() {
        instantMonitoringDataAnalysisEngine = new InstantMonitoringDataAnalysisEngine();

        //read all existing Service IDs and start monitoring timers for them
        for (String monSeqID : persistenceDelegate.getMonitoringSequencesIDs()) {
            ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceDelegate.getLatestConfiguration(monSeqID);
            final MonitoredElement serviceConfiguration = configurationXMLRepresentation.getServiceConfiguration();
            if (!elasticitySpaceComputationTimers.containsKey(serviceConfiguration)) {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        persistenceDelegate.updateAndGetElasticitySpace(serviceConfiguration.getId());
                    }

                };
                Timer timer = new Timer(true);
                timer.schedule(task, 0, 5000);
                elasticitySpaceComputationTimers.put(serviceConfiguration, timer);
            }
        }

        TimerTask updateManagedServicesTask = new TimerTask() {
            @Override
            public void run() {
                List<String> ids = persistenceDelegate.getMonitoringSequencesIDs();
                //cleanup timers for removed services
                for (MonitoredElement element : elasticitySpaceComputationTimers.keySet()) {
                    if (!ids.contains(element.getId())) {
                        elasticitySpaceComputationTimers.remove(element).cancel();
                    }
                }

                for (String monSeqID : persistenceDelegate.getMonitoringSequencesIDs()) {

                    ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceDelegate.getLatestConfiguration(monSeqID);
                    final MonitoredElement serviceConfiguration = configurationXMLRepresentation.getServiceConfiguration();
                    if (!elasticitySpaceComputationTimers.containsKey(serviceConfiguration)) {
                        TimerTask task = new TimerTask() {
                            @Override
                            public void run() {
                                persistenceDelegate.updateAndGetElasticitySpace(serviceConfiguration.getId());
                            }

                        };
                        Timer timer = new Timer(true);
                        timer.schedule(task, 0, 5000);
                        elasticitySpaceComputationTimers.put(serviceConfiguration, timer);
                    }
                }
            }

        };

        updateManagedServicesTimer = new Timer(true);
        updateManagedServicesTimer.schedule(updateManagedServicesTask, 5000, 5000);

//         get latest config
//        ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceDelegate.getLatestConfiguration(serviceID);
//        persistenceDelegate.setMonitoringId(configurationXMLRepresentation.getServiceConfiguration().getId());
//         open proper sql access
//        persistenceDelegate = new PersistenceSQLAccess(configurationXMLRepresentation.getServiceConfiguration().getId());
//        setInitialServiceConfiguration(configurationXMLRepresentation.getServiceConfiguration());
//        setInitialCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());
//        setInitialRequirements(configurationXMLRepresentation.getRequirements());
    }

    protected ElasticityAnalysisManager() {
    }

    public void addExecutingAction(String serviceID, String targetEntityID, String actionName) {
        melaApi.addExecutingAction(serviceID, targetEntityID, actionName);
    }

    public void removeExecutingAction(String serviceID, String targetEntityID, String actionName) {
        melaApi.removeExecutingAction(serviceID, targetEntityID, actionName);
    }

    public void setServiceConfiguration(final MonitoredElement serviceConfiguration) {
//        this.serviceConfiguration = serviceConfiguration;
//        persistenceDelegate.refresh(); // = new PersistenceSQLAccess("mela", "mela", Configuration.getDataServiceIP(), Configuration.getDataServicePort(), serviceConfiguration.getId());

        if (serviceConfiguration != null) {
            melaApi.sendServiceStructure(serviceConfiguration);

        }

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

        melaApi.sendUpdatedServiceStructure(cfg.getServiceConfiguration());

    }

    public Requirements getRequirements(String serviceID) {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg != null) {
            return cfg.getRequirements();
        } else {
            return new Requirements();
        }

    }

    public boolean testIfAllVMsReportMEtricsGreaterThanZero(String serviceID) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cxmlr != null) {
//            MonitoredElement element = cxmlr.getServiceConfiguration();
            ServiceMonitoringSnapshot data = persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId());
            if (data == null) {
                log.error("Monitoring Data not found for " + serviceID);
                throw new RuntimeException("Monitoring Data not found for " + serviceID);
            }
            for (MonitoredElementMonitoringSnapshot childSnapshot : data.getMonitoredData(MonitoredElement.MonitoredElementLevel.VM).values()) {
                for (MetricValue metricValue : childSnapshot.getMonitoredData().values()) {
                    if (metricValue.getValueType().equals(MetricValue.ValueType.NUMERIC)) {
                        if (metricValue.compareTo(new MetricValue(0)) < 0) {
                            return false;
                        }
                    }
                }
            }
        } else {
            return true;
        }
        return true;
    }

    public void setRequirements(Requirements requirements) {
        melaApi.sendRequirements(requirements);

    }

    public CompositionRulesConfiguration getCompositionRulesConfiguration(String serviceID) {

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg != null) {
            return cfg.getCompositionRulesConfiguration();
        } else {
            return new CompositionRulesConfiguration();
        }

    }

    public void setCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {

        melaApi.sendCompositionRules(compositionRulesConfiguration);
    }

    public AnalysisReport analyzeLatestMonitoringData(String serviceID) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cxmlr != null) {
            return instantMonitoringDataAnalysisEngine.analyzeRequirements(persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId()), cxmlr.getRequirements());
        } else {
            return new AnalysisReport(new ServiceMonitoringSnapshot(), new Requirements());
        }

    }

    public Collection<Metric> getAvailableMetricsForMonitoredElement(String serviceID, MonitoredElement monitoredElement) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cxmlr != null) {
            return persistenceDelegate.getAvailableMetrics(monitoredElement, cxmlr.getServiceConfiguration().getId());
        } else {
            return new ArrayList<Metric>();
        }
    }

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

    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringData(String serviceID) {
        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractMonitoringData(serviceID)) {
            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
        }
        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
        snapshots.setChildren(elementMonitoringSnapshots);
        return snapshots;
    }

    public MonitoredElementMonitoringSnapshots getAggregatedMonitoringDataInTimeInterval(String serviceID, int startTimestampID, int endTimestampID) {
        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractMonitoringDataByTimeInterval(startTimestampID, endTimestampID, serviceID)) {
            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
        }
        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
        snapshots.setChildren(elementMonitoringSnapshots);
        return snapshots;
    }

    public MonitoredElementMonitoringSnapshots getLastXAggregatedMonitoringData(String serviceID, int count) {
        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractLastXMonitoringDataSnapshots(count, serviceID)) {
            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
        }
        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
        snapshots.setChildren(elementMonitoringSnapshots);
        return snapshots;
    }

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
        if (cfg == null || !elasticityAnalysisEnabled || cfg.getServiceConfiguration() == null && cfg.getCompositionRulesConfiguration() != null) {
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

//        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (!elasticityAnalysisEnabled) {
            log.warn("Elasticity analysis disabled, or no service configuration or composition rules configuration");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElSpace");
            return elSpaceJSON.toJSONString();
        }

        Date before = new Date();
        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(serviceID);

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

        ElasticitySpace space = persistenceDelegate.updateAndGetElasticitySpace(serviceID);
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

        ElasticitySpace space = persistenceDelegate.updateAndGetElasticitySpace(serviceID);
        ElasticitySpaceXML elasticitySpaceXML = xmlConverter.convertElasticitySpaceToXML(space, element);
        Date after = new Date();
        log.debug("El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return elasticitySpaceXML;
    }

    public String getLatestMonitoringDataINJSON(String serviceID) {

        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }

        ServiceMonitoringSnapshot serviceMonitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(serviceID);
        if (serviceMonitoringSnapshot == null) {
            return "{nothing}";
        }
        Map<Requirement, Map<MonitoredElement, Boolean>> reqAnalysisResult = instantMonitoringDataAnalysisEngine.analyzeRequirements(serviceMonitoringSnapshot, cfg.getRequirements()).getRequirementsAnalysisResult();

        String converted = jsonConverter.convertMonitoringSnapshot(serviceMonitoringSnapshot, cfg.getRequirements(), reqAnalysisResult);

        Date after = new Date();
        log.debug("Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
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

    private ElasticitySpace extractElasticitySpace(String serviceID) {
//        
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
