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

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.costeval.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.XmlConverter;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.ServiceElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.*;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngine;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.model.LifetimeEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.utils.conversion.CostJSONConverter;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.descriptionParsers.CloudDescriptionParser;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
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
import java.util.logging.Level;
import org.apache.cxf.common.i18n.UncheckedException;
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
    private CostJSONConverter jsonConverter;

    @Autowired
    private XmlConverter xmlConverter;

    @Value("#{dataAccess}")
    private DataAccess dataAccess;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataAggregationEngine instantMonitoringDataEnrichmentEngine;

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

    @Value("${data.caching.interval:1}")
    private int cachingIntervalInSeconds;

    private Map<String, Timer> costMonitoringTimers;
    private Map<String, Timer> costElasticityTimers;

    private Timer checkForAddedServices;

    {
        costMonitoringTimers = new ConcurrentHashMap<String, Timer>();
        costElasticityTimers = new ConcurrentHashMap<String, Timer>();
    }

    public PersistenceDelegate getPersistenceDelegate() {
        return persistenceDelegate;
    }

    public void setPersistenceDelegate(PersistenceDelegate persistenceDelegate) {
        this.persistenceDelegate = persistenceDelegate;
    }

    public void removeService(String serviceID) {
        this.persistenceDelegate.removeService(serviceID);
    }

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

                    if (!costMonitoringTimers.containsKey(monSeqID)) {
                        final Timer timer = new Timer(true);

                        TimerTask cacheUsageSoFarTask = new TimerTask() {
//
                            @Override
                            public void run() {
                                if (persistenceDelegate.getLatestConfiguration(monSeqID) == null) {
                                    timer.cancel();
                                    costMonitoringTimers.remove(monSeqID);
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

                        costMonitoringTimers.put(monSeqID, timer);
                    }

                    if (!costElasticityTimers.containsKey(monSeqID)) {
                        final Timer timer = new Timer(true);

                        TimerTask cacheUsageSoFarTask = new TimerTask() {
//
                            @Override
                            public void run() {
                                if (persistenceDelegate.getLatestConfiguration(monSeqID) == null) {
                                    timer.cancel();
                                    costElasticityTimers.remove(monSeqID);
                                } else {
                                    try {
                                        updateAndGetInstantCostElasticitySpace(monSeqID);
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };

                        timer.schedule(cacheUsageSoFarTask, 0, cachingIntervalInSeconds * 1000);

                        costElasticityTimers.put(monSeqID, timer);
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

    public String getStructureWithUsedCloudOfferedServices(String serviceID) {

        Date before = new Date();

        MonitoredElement structure = persistenceDelegate.getLatestConfiguration(serviceID).getServiceConfiguration();
        if (structure == null) {
            return "{nothing}";
        }

        String converted = CostJSONConverter.convertServiceStructureSnapshot(structure, costEvalEngine.cloudProvidersToMap(CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService())));

        Date after = new Date();
        return converted;
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

    public String getInstantCostJSON(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractLastInstantCostSnapshot(serviceID);

        if (serviceUsageSnapshot == null) {
            return "{nothing}";
        }

        try {
            String converted = jsonConverter.convertMonitoringSnapshotAndCompositionRules(serviceUsageSnapshot.getSnapshot(), serviceUsageSnapshot.getCostCompositionRules());
            return converted;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getServiceUsageInJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public String getTotalCostJSON(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshot(serviceID);

        if (serviceUsageSnapshot == null) {
            return "{nothing}";
        }

        try {
            String converted = jsonConverter.convertMonitoringSnapshotAndCompositionRules(serviceUsageSnapshot.getSnapshot(), serviceUsageSnapshot.getCostCompositionRules());
            return converted;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getServiceUsageInJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public String getTotalCostForServiceJSON(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshot(serviceID);
        serviceUsageSnapshot.getCostCompositionRules().getCompositionRules().addAll(cfg.getCompositionRulesConfiguration().getMetricCompositionRules().getCompositionRules());
        if (serviceUsageSnapshot == null) {
            return "{nothing}";
        }

        try {
            String converted = CostJSONConverter.convertMonitoringSnapshotAndCompositionRules(serviceUsageSnapshot.getSnapshot(), serviceUsageSnapshot.getCostCompositionRules());
            return converted;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getTotalCostForServiceJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public String getTotalCostForServiceJSONAsPieChart(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshot(serviceID);

        if (serviceUsageSnapshot == null) {
            return "{nothing}";
        }

        try {
            String converted = jsonConverter.toJSONForRadialPieChart(serviceUsageSnapshot.getSnapshot());
            return converted;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getTotalCostForServiceJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public String getInstantCostForServiceJSONAsPieChart(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractLastInstantCostSnapshot(serviceID);

        if (serviceUsageSnapshot == null) {
            return "{nothing}";
        }

        try {
            String converted = jsonConverter.toJSONForRadialPieChart(serviceUsageSnapshot.getSnapshot());
            return converted;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getTotalCostForServiceJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public LifetimeEnrichedSnapshot updateAndCacheHistoricalServiceUsageForInstantCostPerUsage(final String serviceID) {
        Date before = new Date();

        //if service DI not found
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            log.debug("Service ID {} not found", serviceID);
            return null;
        }

        LifetimeEnrichedSnapshot previouselyDeterminedUsage = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(serviceID);

        int lastRetrievedTimestampID = (previouselyDeterminedUsage != null) ? previouselyDeterminedUsage.getLastUpdatedTimestampID() : 0;

        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(lastRetrievedTimestampID, serviceID);

        if (!allMonData.isEmpty()) {
            if (previouselyDeterminedUsage == null) {
                ServiceMonitoringSnapshot data = allMonData.remove(0);
                previouselyDeterminedUsage = new LifetimeEnrichedSnapshot().withSnapshot(data).withLastUpdatedTimestampID(data.getTimestampID());
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

        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        log.debug("Updating usage and instant cost for {} snapshots", allMonData.size());

        for (ServiceMonitoringSnapshot monitoringSnapshot : allMonData) {
            //compute total usage so far
            previouselyDeterminedUsage = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesASVMTypes(cloudProvidersMap, previouselyDeterminedUsage, monitoringSnapshot);

            //persist the total usage
//            persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(serviceID, previouselyDeterminedUsage);
            //as the previous method has also the currently unused services, we must remove them for computing instant cost
            LifetimeEnrichedSnapshot cleanedCostSnapshot = costEvalEngine.cleanUnusedServices(previouselyDeterminedUsage);

            //compute composition rules to create instant cost based on total usage so far
            CompositionRulesBlock block = costEvalEngine.createCompositionRulesForInstantUsageCostIncludingServicesASVMTypes(cloudProvidersMap, cleanedCostSnapshot.getSnapshot().getMonitoredService(), cleanedCostSnapshot, monitoringSnapshot.getTimestamp());
            ServiceMonitoringSnapshot enrichedSnapshot = costEvalEngine.applyCompositionRules(block, costEvalEngine.convertToStructureIncludingServicesASVMTypes(cloudProvidersMap, monitoringSnapshot));

            //persist instant cost
            persistenceDelegate.persistInstantCostSnapshot(serviceID, new CostEnrichedSnapshot().withCostCompositionRules(block)
                    .withLastUpdatedTimestampID(enrichedSnapshot.getTimestampID()).withSnapshot(enrichedSnapshot));

            //retrieve the previousely computed total usage, as the computation of the instant cost destr
//            previouselyDeterminedUsage = persistenceDelegate.extractCachedServiceUsage(serviceID);
            //create rules for metrics for total cost based on usage so far
            CompositionRulesBlock totalCostBlock = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesASVMTypes(cloudProvidersMap, previouselyDeterminedUsage, monitoringSnapshot.getTimestamp());
            ServiceMonitoringSnapshot snapshotWithTotalCost = costEvalEngine.applyCompositionRules(totalCostBlock, previouselyDeterminedUsage.getSnapshot());

//            persist mon snapshot enriched with total cost
            persistenceDelegate.persistTotalCostSnapshot(serviceID, new CostEnrichedSnapshot().withCostCompositionRules(totalCostBlock)
                    .withLastUpdatedTimestampID(snapshotWithTotalCost.getTimestampID()).withSnapshot(snapshotWithTotalCost));
        }

        Date after = new Date();
        log.debug("UpdateAndCacheEvaluatedServiceUsageWithCurrentStructure time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());

        return previouselyDeterminedUsage;
    }

    //TODO: update and get instant cost
    public ElasticitySpace updateAndGetInstantCostElasticitySpace(String serviceID) {
        Date before = new Date();

        ElasticitySpace space = persistenceDelegate.extractLatestInstantCostElasticitySpace(serviceID);

        //update space with new data
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            log.error("Retrieved empty configuration.");
            return null;
        } else if (cfg.getRequirements() == null) {
            log.error("Retrieved configuration does not contain Requirements.");
            return null;
        } else if (cfg.getServiceConfiguration() == null) {
            log.error("Retrieved configuration does not contain Service Configuration.");
            return null;
        }
        Requirements requirements = cfg.getRequirements();
        MonitoredElement serviceConfiguration = cfg.getServiceConfiguration();

        //if space == null, compute it 
        if (space == null) {

            //if space is null, compute it from all aggregated monitored data recorded so far
            List<CostEnrichedSnapshot> enrichedCostSnapshots = persistenceDelegate.extractInstantUsageSnapshot(serviceID);
            if (!enrichedCostSnapshots.isEmpty()) {

                List<ServiceMonitoringSnapshot> dataFromTimestamp = new ArrayList<>();
                for (CostEnrichedSnapshot snapshot : enrichedCostSnapshots) {
                    dataFromTimestamp.add(snapshot.getSnapshot());
                }

                ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
                fct.setRequirements(requirements);

                fct.trainElasticitySpace(dataFromTimestamp);
                space = fct.getElasticitySpace();

                //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                space.setStartTimestampID(dataFromTimestamp.get(0).getTimestampID());
                space.setEndTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

                //persist cached space
                persistenceDelegate.persistInstantCostElasticitySpace(space, serviceID);
            }
        } else {
            //else read max 1000 monitoring data records at a time, train space, and repeat as needed

            //if space is not null, update it with new data
            List<ServiceMonitoringSnapshot> dataFromTimestamp = null;

            //used to detect last snapshot timestamp, and then extratc new data until that timestamp
            ServiceMonitoringSnapshot monitoringSnapshot = persistenceDelegate.extractLastInstantCostSnapshot(serviceID).getSnapshot();

            Integer lastTimestampID = (monitoringSnapshot == null) ? Integer.MAX_VALUE : monitoringSnapshot.getTimestampID();

            boolean spaceUpdated = false;
            int currentTimestamp = 0;
            //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
            do {
                //gets data after the supplied timestamp

                int nextTimestamp = space.getEndTimestampID() + 1000;
                nextTimestamp = (nextTimestamp < lastTimestampID) ? nextTimestamp : lastTimestampID;

                //if space is null, compute it from all aggregated monitored data recorded so far
                List<CostEnrichedSnapshot> enrichedCostSnapshots = persistenceDelegate.extractInstantUsageSnapshot(nextTimestamp, serviceID);
                dataFromTimestamp = new ArrayList<>();
                for (CostEnrichedSnapshot snapshot : enrichedCostSnapshots) {
                    dataFromTimestamp.add(snapshot.getSnapshot());
                }

                currentTimestamp = nextTimestamp;

                //check if new data has been collected between elasticity space querries
                if (!dataFromTimestamp.isEmpty()) {
                    ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
                    fct.setRequirements(requirements);
                    fct.trainElasticitySpace(space, dataFromTimestamp, requirements);
                    //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                    space.setEndTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());
                    spaceUpdated = true;
                }

            } while (!dataFromTimestamp.isEmpty() && currentTimestamp < lastTimestampID);

            //persist cached space
            if (spaceUpdated) {
                persistenceDelegate.persistInstantCostElasticitySpace(space, serviceID);
            }
        }

        Date after = new Date();
        log.debug("updateAndGetInstantCostElasticitySpace time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());

        return space;
    }

    public ServiceElasticityPathway updateAndGetInstantCostElasticityPathway(String serviceID) {

        Date before = new Date();

        final ElasticitySpace space = persistenceDelegate.extractLatestInstantCostElasticitySpace(serviceID);

        if (space == null) {
            log.error("Elasticity Space returned is null");
            return new ServiceElasticityPathway();
        }

        final ServiceElasticityPathway completePathway = new ServiceElasticityPathway();
        completePathway.setTimestampID(space.getEndTimestampID());

        List<Thread> threads = new ArrayList<>();

        for (final Map.Entry<MonitoredElement, Map<Metric, List<MetricValue>>> entry : space.getMonitoringData().entrySet()) {

            Thread t = new Thread() {
                @Override
                public void run() {
                    Map<Metric, List<MetricValue>> map = entry.getValue();
                    LightweightEncounterRateElasticityPathway elasticityPathway = new LightweightEncounterRateElasticityPathway(map.size());
                    elasticityPathway.trainElasticityPathway(map);
                    completePathway.addPathway(entry.getKey(), elasticityPathway);
                }
            };

            threads.add(t);
            t.setDaemon(true);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(CostEvalManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        persistenceDelegate.persistInstantCostElasticityPathway(completePathway, serviceID);

        Date after = new Date();
        log.debug("updateAndGetInstantCostElasticityPathway time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());

        return completePathway;
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

        ElasticitySpace space = persistenceDelegate.extractLatestInstantCostElasticitySpace(serviceID);

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

        ElasticitySpace space = persistenceDelegate.extractLatestInstantCostElasticitySpace(element.getId());

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

    public String getInstantCostSpaceJSON(String serviceID, String monitoredElementID, String monitoredElementLevel) {

        MonitoredElement element = new MonitoredElement(monitoredElementID)
                .withLevel(MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel));

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
        ElasticitySpace space = persistenceDelegate.extractLatestInstantCostElasticitySpace(serviceID);

        String jsonRepr = jsonConverter.convertElasticitySpace(space, element);

        Date after = new Date();
        log.debug("El Space cpt time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        return jsonRepr;
    }

    public MonitoredElementMonitoringSnapshot getTotalServiceCostXML(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);
        if (cfg == null) {
            return new MonitoredElementMonitoringSnapshot();
        }

        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshot(serviceID);

        ServiceMonitoringSnapshot completeCostSnapshot = costEvalEngine.applyCompositionRules(serviceUsageSnapshot.getCostCompositionRules(), serviceUsageSnapshot.getSnapshot());

        MonitoredElementMonitoringSnapshot serviceSnapshot = completeCostSnapshot.getMonitoredData(new MonitoredElement(serviceID).withLevel(MonitoredElement.MonitoredElementLevel.SERVICE));
        Date after = new Date();
        log.debug("getTotalServiceCostXML time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
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

    public void emulateServiceWithOtherUsedCloudOfferedServices(MonitoredElement service, String newname) {
        Date before = new Date();
        Map<MonitoredElement, List<UsedCloudOfferedService>> usedServicesMap = new HashMap<>();
        for (MonitoredElement element : service) {
            usedServicesMap.put(element, element.getCloudOfferedServices());
        }

        //get allready monitored service
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service.getId());
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service.getId() + " not found in mon data"));
        }

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));

        }

        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        CompositionRulesConfiguration compositionRulesConfiguration = cfg.getCompositionRulesConfiguration();

        //for each Structured Monitoring Information Stored, we need to extract it, update used services, aggregate it, then apply cost rules
        MonitoredElement prevService = cfg.getServiceConfiguration();

        //if space is not null, update it with new data
        List<ServiceMonitoringSnapshot> dataFromTimestamp = persistenceDelegate.extractStructuredMonitoringData(prevService.getId());

        //extract usage so far only once, and just add to it.
        LifetimeEnrichedSnapshot usageSoFar = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(newname);
        persistenceDelegate.writeMonitoringSequenceId(newname);
        persistenceDelegate.writeConfiguration(newname, new ConfigurationXMLRepresentation().withServiceConfiguration(prevService.clone().withId(newname)).withCompositionRulesConfiguration(cfg.getCompositionRulesConfiguration()).withRequirements(cfg.getRequirements()));

        //as the above method retrieves in steps of 1000 the data to avoids killing the HSQL
        while (!dataFromTimestamp.isEmpty()) {
            //gets data after the supplied timestamp

            int nextTimestamp = dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID();
            for (ServiceMonitoringSnapshot snapshot : dataFromTimestamp) {
                MonitoredElement snapshotServicfCFG = snapshot.getMonitoredService();
                MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = snapshot.getMonitoredData(snapshotServicfCFG);
                //change used cloud services
                for (MonitoredElement element : snapshotServicfCFG) {
                    if (usedServicesMap.containsKey(element)) {
                        element.setCloudOfferedServices(usedServicesMap.get(element));
                    } else {
                        //maybe in this proposed structure the element does not use services anymore
                        element.getCloudOfferedServices().clear();
                    }
                }
                snapshot.getMonitoredData().remove(MonitoredElement.MonitoredElementLevel.SERVICE);
                snapshotServicfCFG.setId(newname);
                elementMonitoringSnapshot.withMonitoredElement(snapshotServicfCFG);
                snapshot.addMonitoredData(elementMonitoringSnapshot);

                //persist new added struct
                persistenceDelegate.writeInTimestamp(snapshot.getTimestamp(), snapshotServicfCFG, newname);
                //aggregate struct data
                ServiceMonitoringSnapshot aggregated = instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, snapshot);
                //update total usage
                LifetimeEnrichedSnapshot updatedUsage;
                if (usageSoFar == null) {
                    updatedUsage = new LifetimeEnrichedSnapshot().withSnapshot(aggregated).withLastUpdatedTimestampID(aggregated.getTimestampID());
                } else {
                    updatedUsage = costEvalEngine.updateTotalUsageSoFarWithCompleteStructure(cloudProvidersMap, usageSoFar, aggregated);
                }
                //persist instant cost
                LifetimeEnrichedSnapshot cleanedCostSnapshot = costEvalEngine.cleanUnusedServices(updatedUsage);

                //compute composition rules to create instant cost based on total usage so far
                CompositionRulesBlock block = costEvalEngine.createCompositionRulesForInstantUsageCost(cloudProvidersMap, cleanedCostSnapshot.getSnapshot().getMonitoredService(), cleanedCostSnapshot, aggregated.getTimestamp());
                ServiceMonitoringSnapshot enrichedSnapshot = costEvalEngine.applyCompositionRules(block, aggregated);

                //persist instant cost
                persistenceDelegate.persistInstantCostSnapshot(newname, new CostEnrichedSnapshot().withCostCompositionRules(block)
                        .withLastUpdatedTimestampID(enrichedSnapshot.getTimestampID()).withSnapshot(enrichedSnapshot));

                //create rules for metrics for total cost based on usage so far
                CompositionRulesBlock totalCostBlock = costEvalEngine.createCompositionRulesForTotalCost(cloudProvidersMap, updatedUsage, aggregated.getTimestamp());
                ServiceMonitoringSnapshot snapshotWithTotalCost = costEvalEngine.applyCompositionRules(totalCostBlock, updatedUsage.getSnapshot());

                //persist mon snapshot enriched with total cost 
                persistenceDelegate.persistTotalCostSnapshot(newname, new CostEnrichedSnapshot().withCostCompositionRules(totalCostBlock)
                        .withLastUpdatedTimestampID(snapshotWithTotalCost.getTimestampID()).withSnapshot(snapshotWithTotalCost));

            }
            //continue

            dataFromTimestamp = persistenceDelegate.extractStructuredMonitoringData(nextTimestamp, prevService.getId());

        }

        //update elasticity space for instant and total cost
        updateAndGetInstantCostElasticitySpace(newname);

        //update instant and total cost elasticity spaces and persist them
//        ServiceMonitoringSnapshot serviceMonitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(cfg.getServiceConfiguration().getId());
        Date after = new Date();
        log.debug("emulateServiceWithOtherUsedCloudOfferedServices:  " + new Date(after.getTime() - before.getTime()).getTime());

    }

    public CostEvalManager withInstantMonitoringDataAnalysisEngine(final InstantMonitoringDataAnalysisEngine instantMonitoringDataAnalysisEngine) {
        this.instantMonitoringDataAnalysisEngine = instantMonitoringDataAnalysisEngine;
        return this;
    }

    public CostEvalManager withCostEvalEngine(final CostEvalEngine costEvalEngine) {
        this.costEvalEngine = costEvalEngine;
        return this;
    }

    public CostEvalManager withPersistenceDelegate(final PersistenceDelegate persistenceDelegate) {
        this.persistenceDelegate = persistenceDelegate;
        return this;
    }

    public CostEvalManager withJsonConverter(final CostJSONConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
        return this;
    }

    public CostEvalManager withXmlConverter(final XmlConverter xmlConverter) {
        this.xmlConverter = xmlConverter;
        return this;
    }

    public CostEvalManager withDataAccess(final DataAccess dataAccess) {
        this.dataAccess = dataAccess;
        return this;
    }

    public CostEvalManager withContext(final ApplicationContext context) {
        this.context = context;
        return this;
    }

    public CostEvalManager withInstantMonitoringDataEnrichmentEngine(final DataAggregationEngine instantMonitoringDataEnrichmentEngine) {
        this.instantMonitoringDataEnrichmentEngine = instantMonitoringDataEnrichmentEngine;
        return this;
    }

    public CostEvalManager withThreadExecutorService(final ExecutorService threadExecutorService) {
        this.threadExecutorService = threadExecutorService;
        return this;
    }

    public CostEvalManager withCachingIntervalInSeconds(final int cachingIntervalInSeconds) {
        this.cachingIntervalInSeconds = cachingIntervalInSeconds;
        return this;
    }

    public void setInstantMonitoringDataAnalysisEngine(InstantMonitoringDataAnalysisEngine instantMonitoringDataAnalysisEngine) {
        this.instantMonitoringDataAnalysisEngine = instantMonitoringDataAnalysisEngine;
    }

    public void setCostEvalEngine(CostEvalEngine costEvalEngine) {
        this.costEvalEngine = costEvalEngine;
    }

    public void setJsonConverter(CostJSONConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    public void setXmlConverter(XmlConverter xmlConverter) {
        this.xmlConverter = xmlConverter;
    }

    public void setDataAccess(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public void setInstantMonitoringDataEnrichmentEngine(DataAggregationEngine instantMonitoringDataEnrichmentEngine) {
        this.instantMonitoringDataEnrichmentEngine = instantMonitoringDataEnrichmentEngine;
    }

    public void setThreadExecutorService(ExecutorService threadExecutorService) {
        this.threadExecutorService = threadExecutorService;
    }

}
