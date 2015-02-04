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

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.costeval.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.XmlConverter;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticitySpaceXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.*;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngine;
import at.ac.tuwien.dsg.mela.costeval.model.ServiceUsageSnapshot;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import at.ac.tuwien.dsg.quelle.descriptionParsers.CloudDescriptionParser;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

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

    @Value("#{dataAccess}")
    private DataAccess dataAccess;

    @Autowired
    private ApplicationContext context;

    private ExecutorService threadExecutorService;

//    //TODO: persist this
//    //TODO: transform this in a map so it can actually be indexed and searched fast.
//    //Mam<CloudProviderUUID, ServiceUnit UUID, 
//    private Map<UUID, Map<UUID, ServiceUnit>> serviceUnits;
//
//    {
//        serviceUnits = new HashMap<>();
//    }
    //in future cost casching should be done using persistence
    private ServiceMonitoringSnapshot completeCost;

    {
        //if memory usage too high, bound thread pool
        threadExecutorService = Executors.newCachedThreadPool();
    }

    protected CostEvalManager() {
    }

    @Value("${data.caching.interval:1}")
    private int cachingIntervalInSeconds;

    private Map<String, Timer> monitoringTimers;

    private Timer checkForAddedServices;

    {
        monitoringTimers = new ConcurrentHashMap<String, Timer>();
    }

//        TimerTask momMemUsageTask = new TimerTask() {
//
//            @Override
//            public void run() {
//                MELAPerfMonitor.logMemoryUsage(performanceLog);
//            }
//        };
//
//        monitoringMemUsageTimer.scheduleAtFixedRate(momMemUsageTask, 0, 60000);
    @PostConstruct
    public void init() {
        if (instantMonitoringDataAnalysisEngine == null) {
            instantMonitoringDataAnalysisEngine = new InstantMonitoringDataAnalysisEngine();
        }

        if (costEvalEngine == null) {
            costEvalEngine = new CostEvalEngine();
        }

        updateCloudProvidersDescription();

        //only adds new services. the removal is done by the services' timers themselves
        checkForAddedServices = new Timer(true);
        TimerTask checkForAddedServicesTask = new TimerTask() {
//
            @Override
            public void run() {
                //read all existing services and create for them caching timers of service usage so far
                for (final String monSeqID : persistenceDelegate.getMonitoringSequencesIDs()) {

                    if (!monitoringTimers.containsKey(monSeqID)) {
                        final Timer timer = new Timer(true);

                        TimerTask cacheUsageSoFarTask = new TimerTask() {
//
                            @Override
                            public void run() {
                                if (persistenceDelegate.getLatestConfiguration(monSeqID) == null) {
                                    timer.cancel();
                                    monitoringTimers.remove(monSeqID);
                                } else {
                                    try {
                                        updateAndCacheHistoricalServiceUsageForInstantCostPerUsage(monSeqID);
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };

                        timer.schedule(cacheUsageSoFarTask, 0, cachingIntervalInSeconds * 1000);

                        monitoringTimers.put(monSeqID, timer);
                    }
                }
            }
        };

        checkForAddedServices.scheduleAtFixedRate(checkForAddedServicesTask, 0, cachingIntervalInSeconds * 1000);

        // get latest config
//        ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceDelegate.getLatestConfiguration(serviceID);
//        persistenceDelegate.setMonitoringId(configurationXMLRepresentation.getServiceConfiguration().getId());
        // open proper sql access
        //persistenceDelegate = new PersistenceSQLAccess(configurationXMLRepresentation.getServiceConfiguration().getId());
//        setInitialServiceConfiguration(configurationXMLRepresentation.getServiceConfiguration());
//        setInitialCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());
//        setInitialRequirements(configurationXMLRepresentation.getRequirements());
    }

    public void updateCloudProvidersDescription() {

        List<CloudProvider> providers = new ArrayList<>();

        // list all MELA datasources from application context
        Map<String, CloudDescriptionParser> cloudParsers = context.getBeansOfType(CloudDescriptionParser.class);
        for (String name : cloudParsers.keySet()) {
            CloudDescriptionParser cloudDescriptionParser = cloudParsers.get(name);
            log.debug("Using CloudDescriptionParser '{}': {}  to update cloud description", name, cloudDescriptionParser);
            CloudProvider provider = cloudDescriptionParser.getCloudProviderDescription();
            providers.add(provider);
        }

        CloudProviderDAO.persistCloudProviders(providers, dataAccess.getGraphDatabaseService());

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
    public void addCloudProviders(List<CloudProvider> cloudProviders) {
        CloudProviderDAO.persistCloudProviders(cloudProviders, dataAccess.getGraphDatabaseService());
    }

    public void addCloudProvider(CloudProvider cloudProvider) {
        CloudProviderDAO.persistCloudProvider(cloudProvider, dataAccess.getGraphDatabaseService());
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

    public String getInstantUsageCostJSON(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        ServiceUsageSnapshot serviceUsageSnapshot = persistenceDelegate.extractLastInstantCost(serviceID);

        if (serviceUsageSnapshot == null) {
            return "{nothing}";
        }

        try {
            String converted = jsonConverter.convertMonitoringSnapshotAndCompositionRules(serviceUsageSnapshot.getTotalUsageSoFar(), serviceUsageSnapshot.getCostCompositionRules());
            return converted;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getServiceUsageInJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public ServiceUsageSnapshot updateAndCacheHistoricalServiceUsageForInstantCostPerUsage(final String serviceID) {
        Date before = new Date();

        //if service DI not found
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            log.debug("Service ID {} not found", serviceID);
            return null;
        }

        ServiceUsageSnapshot previouselyDeterminedUsage = persistenceDelegate.extractCachedServiceUsage(serviceID);

        int lastRetrievedTimestampID = (previouselyDeterminedUsage != null) ? previouselyDeterminedUsage.getLastUpdatedTimestampID() : 0;

        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(lastRetrievedTimestampID, serviceID);

        if (!allMonData.isEmpty()) {
            if (previouselyDeterminedUsage == null) {
                ServiceMonitoringSnapshot data = allMonData.remove(0);
                previouselyDeterminedUsage = new ServiceUsageSnapshot().withTotalUsageSoFar(data).withLastUpdatedTimestampID(data.getTimestampID());
            } else {
                log.debug("Nothing cached or monitored for Service ID  {}", serviceID);
                return null;
            }
            //as I extract 1000 entries at a time to avoid memory overflow, I need to read the rest
            do {
                lastRetrievedTimestampID = allMonData.get(allMonData.size() - 1).getTimestampID();
                List<ServiceMonitoringSnapshot> restOfData = persistenceDelegate.extractMonitoringData(lastRetrievedTimestampID, serviceID);
                if (restOfData.isEmpty()) {
                    break;
                } else {
                    allMonData.addAll(restOfData);
                }
            } while (true);
        }

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            log.debug("No cloud providers found in repository. Cannot compute cost");
            return null;
        }

        //does only instantCost
        if (previouselyDeterminedUsage == null) {
            log.debug("Updated cached ServiceUsageSnapshot is NULL. Something happened.");
            return null;
        }

        Map<UUID, Map<UUID, ServiceUnit>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        log.debug("Updating usage and instant cost for {} snapshots", allMonData.size());

        for (ServiceMonitoringSnapshot monitoringSnapshot : allMonData) {
            //update total usage so far and persist

            ServiceMonitoringSnapshot updatedTotalUsageSoFar = costEvalEngine.updateTotalUsageSoFar(cloudProvidersMap, previouselyDeterminedUsage, monitoringSnapshot);

            previouselyDeterminedUsage.withTotalUsageSoFar(updatedTotalUsageSoFar);
            previouselyDeterminedUsage.withtLastUpdatedTimestampID(updatedTotalUsageSoFar.getTimestampID());

            persistenceDelegate.persistCachedServiceUsage(serviceID, previouselyDeterminedUsage);

            CompositionRulesBlock block = costEvalEngine.createCompositionRulesForInstantUsageCost(cloudProvidersMap, cfg.getServiceConfiguration(), previouselyDeterminedUsage, serviceID);
            ServiceMonitoringSnapshot enrichedSnapshot = costEvalEngine.applyCompositionRules(block, monitoringSnapshot);

            persistenceDelegate.persistInstantCost(serviceID, new ServiceUsageSnapshot().withCostCompositionRules(block)
                    .withLastUpdatedTimestampID(enrichedSnapshot.getTimestampID()).withTotalUsageSoFar(enrichedSnapshot));
        }

        Date after = new Date();
        log.debug("UpdateAndCacheEvaluatedServiceUsageWithCurrentStructure time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());

        return previouselyDeterminedUsage;
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

  
 

    public MonitoredElementMonitoringSnapshot getTotalServiceCostXML(String serviceID) {

        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(serviceID);

        if (!allMonData.isEmpty()) {

            //as I extract 1000 entries at a time to avoid memory overflow, I need to read the rest
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

        List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {

            return new MonitoredElementMonitoringSnapshot();
        }

        ServiceMonitoringSnapshot completeCostSnapshot = costEvalEngine.getTotalCost(cloudProviders, allMonData);;
        MonitoredElementMonitoringSnapshot serviceSnapshot = completeCostSnapshot.getMonitoredData(new MonitoredElement(serviceID).withLevel(MonitoredElement.MonitoredElementLevel.SERVICE));
        return serviceSnapshot;
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
