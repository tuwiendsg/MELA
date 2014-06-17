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
import at.ac.tuwien.dsg.mela.analysisservice.util.converters.JsonConverter;
import at.ac.tuwien.dsg.mela.analysisservice.util.converters.XmlConverter;
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

    @PostConstruct
    public void init() {
        instantMonitoringDataAnalysisEngine = new InstantMonitoringDataAnalysisEngine();

        // get latest config
//        ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceDelegate.getLatestConfiguration();
//        persistenceDelegate.setMonitoringId(configurationXMLRepresentation.getServiceConfiguration().getId());
        // open proper sql access
        //persistenceDelegate = new PersistenceSQLAccess(configurationXMLRepresentation.getServiceConfiguration().getId());
//        setInitialServiceConfiguration(configurationXMLRepresentation.getServiceConfiguration());
//        setInitialCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());
//        setInitialRequirements(configurationXMLRepresentation.getRequirements());
    }

    protected ElasticityAnalysisManager() {
    }

    public synchronized void addExecutingAction(String targetEntityID, String actionName) {
        melaApi.addExecutingAction(targetEntityID, actionName);
    }

    public synchronized void removeExecutingAction(String targetEntityID, String actionName) {
        melaApi.removeExecutingAction(targetEntityID, actionName);
    }

    public synchronized void setServiceConfiguration(MonitoredElement serviceConfiguration) {
//        this.serviceConfiguration = serviceConfiguration;
//        persistenceDelegate.refresh(); // = new PersistenceSQLAccess("mela", "mela", Configuration.getDataServiceIP(), Configuration.getDataServicePort(), serviceConfiguration.getId());
        melaApi.sendServiceStructure(serviceConfiguration);
    }

    public synchronized MonitoredElement getServiceConfiguration() {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        if (cfg != null) {
            return cfg.getServiceConfiguration();
        } else {
            return new MonitoredElement();
        }
    }
//
//    private synchronized void setInitialServiceConfiguration(MonitoredElement serviceConfiguration) {
//        this.serviceConfiguration = serviceConfiguration;
//    }
    // actually removes all VMs and Virtual Clusters from the ServiceUnit and
    // adds new ones.

    public synchronized void updateServiceConfiguration(MonitoredElement serviceConfiguration) {

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();

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

    public synchronized Requirements getRequirements() {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        if (cfg != null) {
            return cfg.getRequirements();
        } else {
            return new Requirements();
        }

    }

    public Boolean testIfAllVMsReportMEtricsGreaterThanZero(MonitoredElement element) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration();
        if (cxmlr != null) {
            ServiceMonitoringSnapshot data = persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId());
            if (data.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT).containsKey(element)) {
                MonitoredElementMonitoringSnapshot mems = data.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT).get(element);
                for (MonitoredElementMonitoringSnapshot childSnapshot : mems.getChildren()) {
                    for (MetricValue metricValue : childSnapshot.getMonitoredData().values()) {
                        if (metricValue.compareTo(new MetricValue(0)) < 0) {
                            return false;
                        }
                    }
                }
            } else {
                log.error("Monitoring Data not found for " + element.getId());
                throw new RuntimeException("Monitoring Data not found for " + element.getId());
            }
        } else {
            return true;
        }
        return true;
    }

    public synchronized void setRequirements(Requirements requirements) {
        melaApi.sendRequirements(requirements);

    }

    public synchronized CompositionRulesConfiguration getCompositionRulesConfiguration() {

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        if (cfg != null) {
            return cfg.getCompositionRulesConfiguration();
        } else {
            return new CompositionRulesConfiguration();
        }

    }

    public synchronized void setCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {

        melaApi.sendCompositionRules(compositionRulesConfiguration);
    }

    public synchronized AnalysisReport analyzeLatestMonitoringData() {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration();
        if (cxmlr != null) {
            return instantMonitoringDataAnalysisEngine.analyzeRequirements(persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId()), cxmlr.getRequirements());
        } else {
            return new AnalysisReport(new ServiceMonitoringSnapshot(), new Requirements());
        }

    }

    public synchronized Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement monitoredElement) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration();

        if (cxmlr != null) {
            return persistenceDelegate.getAvailableMetrics(monitoredElement, cxmlr.getServiceConfiguration().getId());
        } else {
            return new ArrayList<Metric>();
        }
    }

    public synchronized MonitoredElementMonitoringSnapshot getLatestMonitoringData() {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration();
        ServiceMonitoringSnapshot monitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(cxmlr.getServiceConfiguration().getId());
        if (monitoringSnapshot != null && !monitoringSnapshot.getMonitoredData().isEmpty()) {
            return monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next();
        } else {
            return new MonitoredElementMonitoringSnapshot();
        }
    }

    public synchronized MonitoredElementMonitoringSnapshot getLatestMonitoringData(MonitoredElement element) {
        ConfigurationXMLRepresentation cxmlr = persistenceDelegate.getLatestConfiguration();
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

    public synchronized MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringData() {
        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractMonitoringData(getServiceConfiguration().getId())) {
            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
        }
        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
        snapshots.setChildren(elementMonitoringSnapshots);
        return snapshots;
    }

    public synchronized MonitoredElementMonitoringSnapshots getAggregatedMonitoringDataInTimeInterval(String startTimestamp, String endTimestamp) {
        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractMonitoringDataByTimeInterval(startTimestamp, endTimestamp, getServiceConfiguration().getId())) {
            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
        }
        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
        snapshots.setChildren(elementMonitoringSnapshots);
        return snapshots;
    }

    public synchronized MonitoredElementMonitoringSnapshots getLastXAggregatedMonitoringData(int count) {
        List<MonitoredElementMonitoringSnapshot> elementMonitoringSnapshots = new ArrayList<MonitoredElementMonitoringSnapshot>();
        for (ServiceMonitoringSnapshot monitoringSnapshot : persistenceDelegate.extractLastXMonitoringDataSnapshots(count, getServiceConfiguration().getId())) {
            elementMonitoringSnapshots.add(monitoringSnapshot.getMonitoredData(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next());
        }
        MonitoredElementMonitoringSnapshots snapshots = new MonitoredElementMonitoringSnapshots();
        snapshots.setChildren(elementMonitoringSnapshots);
        return snapshots;
    }

    // uses a lot of memory (all directly in memory)
    public synchronized String getElasticityPathway(MonitoredElement element) {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (!elasticityAnalysisEnabled || cfg.getServiceConfiguration() == null && cfg.getCompositionRulesConfiguration() != null) {
            log.warn("Elasticity analysis disabled, or no service configuration or composition rules configuration");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElPathway");
            return elSpaceJSON.toJSONString();
        }

        Date before = new Date();

        // int recordsCount = persistenceDelegate.getRecordsCount();
        // first, read from the sql of monitoring data, in increments of 10, and
        // train the elasticity space function
        LightweightEncounterRateElasticityPathway elasticityPathway = null;

        List<Metric> metrics = null;

        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(cfg.getServiceConfiguration().getId());

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

    public synchronized ElasticityPathwayXML getElasticityPathwayInXML(MonitoredElement element) {

        ElasticityPathwayXML elasticityPathwayXML = new ElasticityPathwayXML();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (!elasticityAnalysisEnabled || cfg.getServiceConfiguration() == null && cfg.getCompositionRulesConfiguration() != null) {
            log.warn("Elasticity analysis disabled, or no service configuration or composition rules configuration");
            return elasticityPathwayXML;
        }

        Date before = new Date();

        // int recordsCount = persistenceDelegate.getRecordsCount();
        // first, read from the sql of monitoring data, in increments of 10, and
        // train the elasticity space function
        LightweightEncounterRateElasticityPathway elasticityPathway = null;

        List<Metric> metrics = null;

        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(cfg.getServiceConfiguration().getId());

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

    public synchronized String getElasticitySpaceJSON(MonitoredElement element) {

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        // if no service configuration, we can't have elasticity space function
        // if no compositionRulesConfiguration we have no data
        if (!elasticityAnalysisEnabled || cfg.getServiceConfiguration() == null && cfg.getCompositionRulesConfiguration() != null) {
            log.warn("Elasticity analysis disabled, or no service configuration or composition rules configuration");
            JSONObject elSpaceJSON = new JSONObject();
            elSpaceJSON.put("name", "ElSpace");
            return elSpaceJSON.toJSONString();
        }

        Date before = new Date();
        ElasticitySpace space = extractAndUpdateElasticitySpace();

        String jsonRepr = jsonConverter.convertElasticitySpace(space, element);

        Date after = new Date();
        log.debug("El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return jsonRepr;
    }

    /**
     * @param element
     * @return also contains the monitored values
     */
    public synchronized ElasticitySpaceXML getCompleteElasticitySpaceXML(MonitoredElement element) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
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
    public synchronized ElasticitySpaceXML getElasticitySpaceXML(MonitoredElement element) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(cfg.getServiceConfiguration().getId());
        ElasticitySpaceXML elasticitySpaceXML = xmlConverter.convertElasticitySpaceToXML(space, element);
        Date after = new Date();
        log.debug("El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return elasticitySpaceXML;
    }

    public synchronized String getLatestMonitoringDataINJSON() {
        
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        ServiceMonitoringSnapshot serviceMonitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(cfg.getServiceConfiguration().getId());
        Map<Requirement, Map<MonitoredElement, Boolean>> reqAnalysisResult = instantMonitoringDataAnalysisEngine.analyzeRequirements(serviceMonitoringSnapshot, cfg.getRequirements()).getRequirementsAnalysisResult();

        String converted = jsonConverter.convertMonitoringSnapshot(serviceMonitoringSnapshot, cfg.getRequirements(), reqAnalysisResult);

        Date after = new Date();
        log.debug("Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return converted;
    }

    public synchronized MonitoredElement getLatestServiceStructure() {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        ServiceMonitoringSnapshot serviceMonitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(cfg.getServiceConfiguration().getId());
        Date after = new Date();
        log.debug("Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return serviceMonitoringSnapshot.getMonitoredService();
    }

    public synchronized String getMetricCompositionRules() {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();

        if (cfg.getCompositionRulesConfiguration() != null) {
            return jsonConverter.convertToJSON(cfg.getCompositionRulesConfiguration().getMetricCompositionRules());
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", "No composition rules yet");
            return jsonObject.toJSONString();
        }
    }

    private ElasticitySpace extractAndUpdateElasticitySpace() {
        //note persistenceDelegate.extractMonitoringData returns max 1000 rows

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration();
        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(cfg.getServiceConfiguration().getId());

        //if space == null, compute it 
        if (space == null) {
            //if space is null, compute it from all aggregated monitored data recorded so far
            List<ServiceMonitoringSnapshot> dataFromTimestamp = persistenceDelegate.extractMonitoringData(cfg.getServiceConfiguration().getId());

            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(cfg.getServiceConfiguration());
            fct.setRequirements(cfg.getRequirements());
            fct.trainElasticitySpace(dataFromTimestamp);
            space = fct.getElasticitySpace();

            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
            space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

        }

        //if space is not null, update it with new data
        List<ServiceMonitoringSnapshot> dataFromTimestamp = null;

        //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
        do {
            dataFromTimestamp = persistenceDelegate.extractMonitoringData(space.getTimestampID(), cfg.getServiceConfiguration().getId());
            //check if new data has been collected between elasticity space querries
            if (!dataFromTimestamp.isEmpty()) {
                ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(cfg.getServiceConfiguration());
                fct.setRequirements(cfg.getRequirements());
                fct.trainElasticitySpace(space, dataFromTimestamp, cfg.getRequirements());
                //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

            }

        } while (!dataFromTimestamp.isEmpty());

        //persist cached space
        persistenceDelegate.writeElasticitySpace(space, cfg.getServiceConfiguration().getId());

        return space;
    }
}
