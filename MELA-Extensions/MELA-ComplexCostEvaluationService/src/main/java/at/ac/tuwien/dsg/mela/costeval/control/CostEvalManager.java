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
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyCoefficient;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyElement;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.MonitoredElementElasticityDependency;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ServiceElasticityDependencies;
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
import at.ac.tuwien.dsg.mela.costeval.model.UnusedCostUnitsReport;
import at.ac.tuwien.dsg.mela.costeval.utils.conversion.CostJSONConverter;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.descriptionParsers.CloudDescriptionParser;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.quelle.cloudDescriptionParsers.impl.CloudFileDescriptionParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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

    @Value("${enabled.analysis.instant.cost.elasticity:false}")
    private boolean elasticitySpaceAnalysisEnables;

    @Value("${enabled.analysis.cost:true}")
    private boolean costEvaluationEnabled;

    @Value("${cost.efficiency.threshold:0.5}")
    private Double costEfficiencyThreshold;

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

                    if (costEvaluationEnabled && !costMonitoringTimers.containsKey(monSeqID)) {
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

                    if (elasticitySpaceAnalysisEnables && !costElasticityTimers.containsKey(monSeqID)) {
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

        String cloudDescriptionsPath = "./config/default/";

        List<CloudProvider> providers = new ArrayList<>();

        CloudFileDescriptionParser cloudFileDescriptionParser = (CloudFileDescriptionParser) context.getBean("cloudFileDescriptionParser");

        File folder = new File(cloudDescriptionsPath);
        for (File cloudDescriptionFile : folder.listFiles()) {
            try {
                providers.add(cloudFileDescriptionParser.getCloudProviderDescription("file:" + cloudDescriptionFile.getPath()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }

//        // list all MELA datasources from application context
//        Map<String, CloudDescriptionParser> cloudParsers = context.getBeansOfType(CloudDescriptionParser.class);
//        for (String name : cloudParsers.keySet()) {
//            CloudDescriptionParser cloudDescriptionParser = cloudParsers.get(name);
//            log.debug("Using CloudDescriptionParser '{}': {}  to update cloud description", name, cloudDescriptionParser);
//            CloudProvider provider = cloudDescriptionParser.getCloudProviderDescription();
//            providers.add(provider);
//        }
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

    public String getInstantCostJSON(String serviceID, String timestampID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractInstantUsageSnapshotByTimeIDInterval(Integer.parseInt(timestampID), Integer.parseInt(timestampID), serviceID).get(0);

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

    public MonitoredElement getCompleteStructureOfUsedServices(String serviceID) {
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return new MonitoredElement();
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshot(serviceID);

        if (serviceUsageSnapshot == null) {
            return new MonitoredElement();
        }

        return serviceUsageSnapshot.getSnapshot().getMonitoredService();

    }

    public String getTotalCostJSON(String serviceID, String timestampID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshotByTimeIDInterval(Integer.parseInt(timestampID), Integer.parseInt(timestampID), serviceID).get(0);

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

    public String getTotalCostForServiceDoubleValue(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshot(serviceID);

        if (serviceUsageSnapshot == null) {
            return "" + Double.NaN;
        }

        try {
            return serviceUsageSnapshot.getSnapshot().getMonitoredData(serviceUsageSnapshot.getSnapshot().getMonitoredService()).getMetricValue(CostEvalEngine.ELEMENT_COST_METRIC).getValueRepresentation();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getTotalCostForServiceJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public String getInstantCostForServiceDoubleValue(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractLastInstantCostSnapshot(serviceID);

        if (serviceUsageSnapshot == null) {
            return "" + Double.NaN;
        }

        try {
            return serviceUsageSnapshot.getSnapshot().getMonitoredData(serviceUsageSnapshot.getSnapshot().getMonitoredService()).getMetricValue(CostEvalEngine.ELEMENT_COST_METRIC).getValueRepresentation();
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getTotalCostForServiceJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
        }

    }

    public String getTotalCostForServiceJSON(String serviceID, String timestampID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshotByTimeIDInterval(Integer.parseInt(timestampID), Integer.parseInt(timestampID), serviceID).get(0);
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

    public String getTotalUsageForServiceJSON(String serviceID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        LifetimeEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(serviceID);

        try {
            String converted = CostJSONConverter.convertMonitoringSnapshot(serviceUsageSnapshot.getSnapshot());
            return converted;
        } catch (Exception e) {
            return e.getMessage();
        } finally {
            Date after = new Date();
            log.debug("getTotalUsageForServiceJSON time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
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

    public String getInstantCostForServiceJSONAsPieChart(String serviceID, String timestampID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractInstantUsageSnapshotByTimeIDInterval(Integer.parseInt(timestampID), Integer.parseInt(timestampID), serviceID).get(0);

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

    public String getTotalCostForServiceJSONAsPieChart(String serviceID, String timestampID) {
        Date before = new Date();
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            return "{nothing}";
        }
        CostEnrichedSnapshot serviceUsageSnapshot = persistenceDelegate.extractTotalCostSnapshotByTimeIDInterval(Integer.parseInt(timestampID), Integer.parseInt(timestampID), serviceID).get(0);

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

        //if service DI not found
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            log.debug("Service ID {} not found", serviceID);
            return null;
        }

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            log.debug("No cloud providers found in repository. Cannot compute cost");
            return null;
        }

        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        log.debug("Starting to Update And Cache Instant Cost for {}", serviceID);

        LifetimeEnrichedSnapshot previouselyDeterminedUsage = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(serviceID);

        int lastRetrievedTimestampID = (previouselyDeterminedUsage != null) ? previouselyDeterminedUsage.getLastUpdatedTimestampID() : 0;
        ServiceMonitoringSnapshot lastMonData = persistenceDelegate.extractLatestMonitoringData(serviceID);
        int lastTimestampID = (lastMonData == null) ? 0 : lastMonData.getTimestampID();

        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(lastRetrievedTimestampID, serviceID);

        if (!allMonData.isEmpty()) {

            //as I extract 1000 entries at a time to avoid memory overflow, I need to read the rest
            do {

//                log.info("Updating usage and instant cost from {} for {} snapshots, last timestamp is {}",
//                        new Object[]{lastRetrievedTimestampID, allMonData.size(), lastTimestampID});
                for (ServiceMonitoringSnapshot monitoringSnapshot : allMonData) {

//                    if (monitoringSnapshot.getTimestampID() == 720) {
//                        log.debug("Am ajuns ");
//                    }
                    Date before = new Date();
                    //update total usage so far
                    previouselyDeterminedUsage = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, previouselyDeterminedUsage, monitoringSnapshot);
                    persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(serviceID, previouselyDeterminedUsage);
                    //as the previous method has also the currently unused services, we must remove them for computing instant cost
                    LifetimeEnrichedSnapshot cleanedCostSnapshot = costEvalEngine.cleanUnusedServices(previouselyDeterminedUsage);

                    ServiceMonitoringSnapshot convertedCurrentStructure = costEvalEngine.convertToStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, monitoringSnapshot);

                    //compute composition rules to create instant cost based on total usage so far
                    final CompositionRulesBlock block = costEvalEngine.createCompositionRulesForInstantUsageCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, cleanedCostSnapshot.getSnapshot().getMonitoredService(), cleanedCostSnapshot, monitoringSnapshot.getTimestamp());
                    final ServiceMonitoringSnapshot enrichedSnapshot = costEvalEngine.applyCompositionRules(block, convertedCurrentStructure);

                    //persist instant cost
                    persistenceDelegate.persistInstantCostSnapshot(serviceID, new CostEnrichedSnapshot().withCostCompositionRules(block)
                            .withLastUpdatedTimestampID(enrichedSnapshot.getTimestampID()).withSnapshot(enrichedSnapshot));

                    Date after = new Date();
                    log.debug("Update And Cache Instant Cost for {} from timestamp {} time in ms: {} ",
                            new Object[]{serviceID, monitoringSnapshot.getTimestampID(),
                                new Date(after.getTime() - before.getTime()).getTime()
                            });

                    //compute total cost IF not computed before, or IF we have new mon data
                    if (previouselyDeterminedUsage != null) {
                        CostEnrichedSnapshot totalCost = persistenceDelegate.extractTotalCostSnapshot(serviceID);
                        //only update and persist total cost snapshot if not done before
                        if (totalCost == null || totalCost.getLastUpdatedTimestampID() != previouselyDeterminedUsage.getLastUpdatedTimestampID()) {

                            //retrieve the previousely computed total usage, as the computation of the instant cost destr
                            //            previouselyDeterminedUsage = persistenceDelegate.extractCachedServiceUsage(serviceID);
                            //create rules for metrics for total cost based on usage so far
                            CompositionRulesBlock totalCostBlock = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, previouselyDeterminedUsage, previouselyDeterminedUsage.getSnapshot().getTimestamp());
                            ServiceMonitoringSnapshot snapshotWithTotalCost = costEvalEngine.applyCompositionRules(totalCostBlock, previouselyDeterminedUsage.getSnapshot());

                            //persist mon snapshot enriched with total cost
                            persistenceDelegate.persistTotalCostSnapshot(serviceID, new CostEnrichedSnapshot().withCostCompositionRules(totalCostBlock)
                                    .withLastUpdatedTimestampID(snapshotWithTotalCost.getTimestampID()).withSnapshot(snapshotWithTotalCost));
                            log.debug("Persisted and update instant and total cost for {}", serviceID);
                        } else {
                            log.debug("Total Cost allready persisted previousely for {}, so continuing", serviceID);
                        }
                    }

                }
                lastRetrievedTimestampID = allMonData.get(allMonData.size() - 1).getTimestampID();
//                log.info("Extracting from {}, and we still have to go until {}", new Object[]{lastRetrievedTimestampID, lastTimestampID});

                allMonData = persistenceDelegate.extractMonitoringData(lastRetrievedTimestampID, serviceID);

            } while (!allMonData.isEmpty() && lastRetrievedTimestampID < lastTimestampID);
        } else {
            log.debug("No new data to evaluate cost for {}", serviceID);
        }

        Runtime.getRuntime().gc();
        return previouselyDeterminedUsage;
    }

    public ElasticitySpace updateAndGetInstantCostElasticitySpace(String serviceID) {
        Date before = new Date();

        log.debug("Extracting last InstantCostElasticitySpace time for service {}", serviceID);
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

//            boolean spaceUpdated = false;
            int currentTimestamp = 0;
            //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
            do {
                //gets data after the supplied timestamp

                int nextTimestamp = space.getEndTimestampID();
                nextTimestamp = (nextTimestamp < lastTimestampID) ? nextTimestamp : lastTimestampID;

                log.debug("UpdateAndGetInstantCostElasticitySpace time for service {} for timestamp {} from max {} ", new Object[]{serviceID, nextTimestamp, lastTimestampID});

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
//                    spaceUpdated = true;
                    persistenceDelegate.persistInstantCostElasticitySpace(space, serviceID);
                }

            } while (!dataFromTimestamp.isEmpty() && currentTimestamp < lastTimestampID);

//            //persist cached space
//            if (spaceUpdated) {
//                persistenceDelegate.persistInstantCostElasticitySpace(space, serviceID);
//            }
        }

        Date after = new Date();
        log.debug("UpdateAndGetInstantCostElasticitySpace for {} time in ms: {} ", new Object[]{serviceID, "" + new Date(after.getTime() - before.getTime()).getTime()});

        Runtime.getRuntime().gc();

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
        log.debug("UpdateAndGetInstantCostElasticityPathway time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());

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

        LifetimeEnrichedSnapshot previouselyDeterminedUsage = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(newname);

        //if space is not null, update it with new data
        List<ServiceMonitoringSnapshot> dataFromTimestamp = persistenceDelegate.extractStructuredMonitoringData(prevService.getId());

        persistenceDelegate.writeMonitoringSequenceId(newname);
        persistenceDelegate.writeConfiguration(newname, new ConfigurationXMLRepresentation().withServiceConfiguration(prevService.clone().withId(newname)).withCompositionRulesConfiguration(cfg.getCompositionRulesConfiguration()).withRequirements(cfg.getRequirements()));

        //extract usage so far only once, and just add to it.
        int lastRetrievedTimestampID = (previouselyDeterminedUsage != null) ? previouselyDeterminedUsage.getLastUpdatedTimestampID() : 0;
        int lastTimestampID = persistenceDelegate.extractLatestMonitoringData(prevService.getId()).getTimestampID();

        //as the above method retrieves in steps of 1000 the data to avoids killing the HSQL
        while (!dataFromTimestamp.isEmpty() && lastRetrievedTimestampID < lastTimestampID) {
            //gets data after the supplied timestamp

            int nextTimestamp = dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID();
            for (ServiceMonitoringSnapshot snapshot : dataFromTimestamp) {

                Date before = new Date();

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

                previouselyDeterminedUsage = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, previouselyDeterminedUsage, aggregated);
                persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(newname, previouselyDeterminedUsage);
                //persist instant cost
                LifetimeEnrichedSnapshot cleanedCostSnapshot = costEvalEngine.cleanUnusedServices(previouselyDeterminedUsage);
                aggregated = costEvalEngine.convertToStructureIncludingServicesAsCloudOfferedService(cloudProvidersMap, aggregated);
//                compute composition rules to create instant cost based on total usage so far
                CompositionRulesBlock block = costEvalEngine.createCompositionRulesForInstantUsageCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, cleanedCostSnapshot.getSnapshot().getMonitoredService(), cleanedCostSnapshot, aggregated.getTimestamp());
                ServiceMonitoringSnapshot enrichedSnapshot = costEvalEngine.applyCompositionRules(block, aggregated);
                //persist instant cost
                persistenceDelegate.persistInstantCostSnapshot(newname, new CostEnrichedSnapshot().withCostCompositionRules(block)
                        .withLastUpdatedTimestampID(enrichedSnapshot.getTimestampID()).withSnapshot(enrichedSnapshot));

                //retrieve the previousely computed total usage, as the computation of the instant cost destr
                //            previouselyDeterminedUsage = persistenceDelegate.extractCachedServiceUsage(serviceID);
                //create rules for metrics for total cost based on usage so far
                CompositionRulesBlock totalCostBlock = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, previouselyDeterminedUsage, persistenceDelegate.extractLatestMonitoringData(prevService.getId()).getTimestamp());
                ServiceMonitoringSnapshot snapshotWithTotalCost = costEvalEngine.applyCompositionRules(totalCostBlock, previouselyDeterminedUsage.getSnapshot());

                //persist mon snapshot enriched with total cost
                persistenceDelegate.persistTotalCostSnapshot(newname, new CostEnrichedSnapshot().withCostCompositionRules(totalCostBlock)
                        .withLastUpdatedTimestampID(snapshotWithTotalCost.getTimestampID()).withSnapshot(snapshotWithTotalCost));

                Date after = new Date();
                log.debug("Emulate Instant Cost for timestamp {} time in ms: {} ",
                        new Object[]{snapshot.getTimestampID(),
                            new Date(after.getTime() - before.getTime()).getTime()
                        });

            }

            //continue
            dataFromTimestamp = persistenceDelegate.extractStructuredMonitoringData(nextTimestamp, prevService.getId());

        }

//        persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(newname, previouselyDeterminedUsage);
//
//        //retrieve the previousely computed total usage, as the computation of the instant cost destr
//        //            previouselyDeterminedUsage = persistenceDelegate.extractCachedServiceUsage(serviceID);
//        //create rules for metrics for total cost based on usage so far
//        CompositionRulesBlock totalCostBlock = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesAsCloudOfferedService(cloudProvidersMap, previouselyDeterminedUsage, persistenceDelegate.extractLatestMonitoringData(prevService.getId()).getTimestamp());
//        ServiceMonitoringSnapshot snapshotWithTotalCost = costEvalEngine.applyCompositionRules(totalCostBlock, previouselyDeterminedUsage.getSnapshot());
//
//        //persist mon snapshot enriched with total cost
//        persistenceDelegate.persistTotalCostSnapshot(newname, new CostEnrichedSnapshot().withCostCompositionRules(totalCostBlock)
//                .withLastUpdatedTimestampID(snapshotWithTotalCost.getTimestampID()).withSnapshot(snapshotWithTotalCost));
    }

    public MonitoredElement recommendUnitInstanceToScaleDownBasedOnCostEfficiency(String service, String monitoredElementID, String monitoredElementLevel) {
        //get allready monitored service
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service);
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service + " not found in mon data"));
        }

        MonitoredElement serviceCFG = cfg.getServiceConfiguration();
        MonitoredElement unitToScale = null;

        //search in the config for the unit we want
        MonitoredElement.MonitoredElementLevel levelToScale = MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel);
        for (MonitoredElement element : serviceCFG) {
            if (element.getId().equals(monitoredElementID) && element.getLevel().equals(levelToScale)) {
                unitToScale = element;
                break;
            }
        }
        if (unitToScale == null) {
            throw new UncheckedException(new Throwable("Element with ID " + unitToScale.getId()
                    + " and level " + unitToScale.getLevel() + " not found for service " + service));
        }

        LifetimeEnrichedSnapshot totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));
        }
        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        List<UnusedCostUnitsReport> report = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);

        if (report.isEmpty()) {
            throw new UncheckedException(new Throwable("Nothing to scale for element with ID " + unitToScale.getId()
                    + " and level for service " + service));
        }

        UnusedCostUnitsReport best = report.get(0);

        //cost efficiency target
        if (best.getCostEfficiency() < costEfficiencyThreshold) {
            log.error("Only scale in option {}{} for {}{} for service {} has cost efficiency {} below threshold {}",
                    new Object[]{best.getUnitInstance().getId(),
                        best.getUnitInstance().getLevel(),
                        monitoredElementID,
                        monitoredElementLevel,
                        best.getCostEfficiency(),
                        costEfficiencyThreshold
                    });
            return null;
        } else {
            return best.getUnitInstance();
        }
    }

    public MonitoredElement recommendUnitInstanceToScaleDownBasedOnCostEfficiency(String service, String monitoredElementID, String monitoredElementLevel, String targetEfficiency) {
        //get allready monitored service
        Double costEfficiencyThreshold = Double.parseDouble(targetEfficiency);
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service);
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service + " not found in mon data"));
        }

        MonitoredElement serviceCFG = cfg.getServiceConfiguration();
        MonitoredElement unitToScale = null;

        //search in the config for the unit we want
        MonitoredElement.MonitoredElementLevel levelToScale = MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel);
        for (MonitoredElement element : serviceCFG) {
            if (element.getId().equals(monitoredElementID) && element.getLevel().equals(levelToScale)) {
                unitToScale = element;
                break;
            }
        }
        if (unitToScale == null) {
            throw new UncheckedException(new Throwable("Element with ID " + unitToScale.getId()
                    + " and level " + unitToScale.getLevel() + " not found for service " + service));
        }

        LifetimeEnrichedSnapshot totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));
        }
        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        List<UnusedCostUnitsReport> report = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);

        if (report.isEmpty()) {
            throw new UncheckedException(new Throwable("Nothing to scale for element with ID " + unitToScale.getId()
                    + " and level for service " + service));
        }

        UnusedCostUnitsReport best = report.get(0);

        //cost efficiency target
        if (best.getCostEfficiency() < costEfficiencyThreshold) {
            log.error("Only scale in option {}{} for {}{} for service {} has cost efficiency {} below threshold {}",
                    new Object[]{best.getUnitInstance().getId(),
                        best.getUnitInstance().getLevel(),
                        monitoredElementID,
                        monitoredElementLevel,
                        best.getCostEfficiency(),
                        costEfficiencyThreshold
                    });
            return null;
        } else {
            return best.getUnitInstance();
        }
    }

    public Double evaluateUnitInstanceCostEfficiency(String service, String monitoredElementID, String monitoredElementLevel, String unitInstanceID) {
        //get allready monitored service
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service);
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service + " not found in mon data"));
        }

        MonitoredElement serviceCFG = cfg.getServiceConfiguration();
        MonitoredElement unitToScale = null;

        //search in the config for the unit we want
        MonitoredElement.MonitoredElementLevel levelToScale = MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel);
        for (MonitoredElement element : serviceCFG) {
            if (element.getId().equals(monitoredElementID) && element.getLevel().equals(levelToScale)) {
                unitToScale = element;
                break;
            }
        }
        if (unitToScale == null) {
            throw new UncheckedException(new Throwable("Element with ID " + unitToScale.getId()
                    + " and level " + unitToScale.getLevel() + " not found for service " + service));
        }

        LifetimeEnrichedSnapshot totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));
        }
        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        List<UnusedCostUnitsReport> report = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);

        if (report.isEmpty()) {
            throw new UncheckedException(new Throwable("Nothing to scale for element with ID " + unitToScale.getId()
                    + " and level for service " + service));
        }

        for (UnusedCostUnitsReport costUnitsReport : report) {
            if (costUnitsReport.getUnitInstance().getId().equals(unitInstanceID)) {
                return costUnitsReport.getCostEfficiency();
            }
        }

        throw new UncheckedException(new Throwable(unitInstanceID + " not found for " + unitToScale.getId()
                + " and level for service " + service));

    }

    public String evaluateUnitInstancesCostEfficiency(String service, String monitoredElementID, String monitoredElementLevel, String unitInstanceIDs) {
        //get allready monitored service
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service);
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service + " not found in mon data"));
        }

        JSONArray response = new JSONArray();
        //must split the IPs. Generally seperated by -
        String[] ips = unitInstanceIDs.split("-");

        MonitoredElement serviceCFG = cfg.getServiceConfiguration();
        MonitoredElement unitToScale = null;

        //search in the config for the unit we want
        MonitoredElement.MonitoredElementLevel levelToScale = MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel);
        for (MonitoredElement element : serviceCFG) {
            if (element.getId().equals(monitoredElementID) && element.getLevel().equals(levelToScale)) {
                unitToScale = element;
                break;
            }
        }
        if (unitToScale == null) {
            throw new UncheckedException(new Throwable("Element with ID " + unitToScale.getId()
                    + " and level " + unitToScale.getLevel() + " not found for service " + service));
        }

        LifetimeEnrichedSnapshot totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));
        }
        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        List<UnusedCostUnitsReport> costEfficiencyReport = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);
        totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);
        List<UnusedCostUnitsReport> lifetimeReport = costEvalEngine.computeLifetimeInBillingPeriods(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);

        if (costEfficiencyReport.isEmpty()) {
            throw new UncheckedException(new Throwable("Nothing to scale for element with ID " + unitToScale.getId()
                    + " and level for service " + service));
        }

        for (String ip : ips) {
            for (UnusedCostUnitsReport costUnitsReport : costEfficiencyReport) {
                if (costUnitsReport.getUnitInstance().getId().equals(ip)) {
                    JSONObject object = new JSONObject();
                    object.put("ip", ip);
                    object.put("efficiency", costUnitsReport.getCostEfficiency());
                    for (UnusedCostUnitsReport lifetime : lifetimeReport) {
                        if (lifetime.getUnitInstance().getId().equals(costUnitsReport.getUnitInstance().getId())) {
                            object.put("lifetime", lifetime.getCostEfficiency());
                        }
                    }
                    response.add(object);
                    break;
                }
            }
        }

        return response.toJSONString();

    }

    public String evaluateUnitInstancesCostEfficiency(String service, String monitoredElementID, String monitoredElementLevel) {
        //get allready monitored service
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service);
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service + " not found in mon data"));
        }

        JSONArray response = new JSONArray();

        MonitoredElement serviceCFG = cfg.getServiceConfiguration();
        MonitoredElement unitToScale = null;

        //search in the config for the unit we want
        MonitoredElement.MonitoredElementLevel levelToScale = MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel);
        for (MonitoredElement element : serviceCFG) {
            if (element.getId().equals(monitoredElementID) && element.getLevel().equals(levelToScale)) {
                unitToScale = element;
                break;
            }
        }
        if (unitToScale == null) {
            throw new UncheckedException(new Throwable("Element with ID " + unitToScale.getId()
                    + " and level " + unitToScale.getLevel() + " not found for service " + service));
        }

        LifetimeEnrichedSnapshot totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));
        }
        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        List<UnusedCostUnitsReport> report = costEvalEngine.computeEffectiveUsageOfBilledServices(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);

        if (report.isEmpty()) {
            throw new UncheckedException(new Throwable("Nothing to scale for element with ID " + unitToScale.getId()
                    + " and level for service " + service));
        }

        for (UnusedCostUnitsReport costUnitsReport : report) {
            JSONObject object = new JSONObject();
            object.put("ip", costUnitsReport.getUnitInstance().getId());
            object.put("efficiency", costUnitsReport.getCostEfficiency());
            response.add(object);
        }

        return response.toJSONString();

    }

    public MonitoredElement recommendUnitInstanceToScaleDownBasedOnLifetime(String service, String monitoredElementID, String monitoredElementLevel) {
        //get allready monitored service
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service);
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service + " not found in mon data"));
        }

        MonitoredElement serviceCFG = cfg.getServiceConfiguration();
        MonitoredElement unitToScale = null;

        //search in the config for the unit we want
        MonitoredElement.MonitoredElementLevel levelToScale = MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel);
        for (MonitoredElement element : serviceCFG) {
            if (element.getId().equals(monitoredElementID) && element.getLevel().equals(levelToScale)) {
                unitToScale = element;
                break;
            }
        }
        if (unitToScale == null) {
            throw new UncheckedException(new Throwable("Element with ID " + unitToScale.getId()
                    + " and level " + unitToScale.getLevel() + " not found for service " + service));
        }

        LifetimeEnrichedSnapshot totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));
        }
        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        List<UnusedCostUnitsReport> report = costEvalEngine.computeLifetimeInBillingPeriods(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);

        if (report.isEmpty()) {
            throw new UncheckedException(new Throwable("Nothing to scale for element with ID " + unitToScale.getId()
                    + " and level for service " + service));
        }

        UnusedCostUnitsReport best = report.get(0);

        //cost efficiency target
        if (best.getCostEfficiency() < costEfficiencyThreshold) {
            log.error("Only scale in option {}{} for {}{} for service {} has cost efficiency {} below threshold {}",
                    new Object[]{best.getUnitInstance().getId(),
                        best.getUnitInstance().getLevel(),
                        monitoredElementID,
                        monitoredElementLevel,
                        best.getCostEfficiency(),
                        costEfficiencyThreshold
                    });
            return null;
        } else {
            return best.getUnitInstance();
        }
    }

    public MonitoredElement recommendUnitInstanceToScaleDownBasedOnLifetime(String service, String monitoredElementID, String monitoredElementLevel, String targetEfficiency) {

        Double costEfficiencyThreshold = Double.parseDouble(targetEfficiency);

        //get allready monitored service
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(service);
        if (cfg == null) {
            throw new UncheckedException(new Throwable("Service with ID " + service + " not found in mon data"));
        }

        MonitoredElement serviceCFG = cfg.getServiceConfiguration();
        MonitoredElement unitToScale = null;

        //search in the config for the unit we want
        MonitoredElement.MonitoredElementLevel levelToScale = MonitoredElement.MonitoredElementLevel.valueOf(monitoredElementLevel);
        for (MonitoredElement element : serviceCFG) {
            if (element.getId().equals(monitoredElementID) && element.getLevel().equals(levelToScale)) {
                unitToScale = element;
                break;
            }
        }
        if (unitToScale == null) {
            throw new UncheckedException(new Throwable("Element with ID " + unitToScale.getId()
                    + " and level " + unitToScale.getLevel() + " not found for service " + service));
        }

        LifetimeEnrichedSnapshot totalUsageSnapshot = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(service);

        final List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        if (cloudProviders == null) {
            throw new UncheckedException(new Throwable("No cloud providers found in repository. Cannot compute cost"));
        }
        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        List<UnusedCostUnitsReport> report = costEvalEngine.computeLifetimeInBillingPeriods(cloudProvidersMap, totalUsageSnapshot, "" + new Date().getTime(), unitToScale);

        if (report.isEmpty()) {
            throw new UncheckedException(new Throwable("Nothing to scale for element with ID " + unitToScale.getId()
                    + " and level for service " + service));
        }

        UnusedCostUnitsReport best = report.get(0);

        //cost efficiency target
        if (best.getCostEfficiency() < costEfficiencyThreshold) {
            log.error("Only scale in option {}{} for {}{} for service {} has cost efficiency {} below threshold {}",
                    new Object[]{best.getUnitInstance().getId(),
                        best.getUnitInstance().getLevel(),
                        monitoredElementID,
                        monitoredElementLevel,
                        best.getCostEfficiency(),
                        costEfficiencyThreshold
                    });
            return null;
        } else {
            return best.getUnitInstance();
        }
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

    public String getCompleteCostHistoryAsCSV(String serviceID) {

        StringWriter sw = new StringWriter();

        Map<MonitoredElement, Map<Metric, List<MetricValue>>> csv = new LinkedHashMap<>();

        //if space is not null, update it with new data
        List<ServiceMonitoringSnapshot> dataFromTimestamp = null;

        ServiceMonitoringSnapshot lastData = persistenceDelegate.extractLatestMonitoringData(serviceID);
        MonitoredElement service = lastData.getMonitoredService();
        int lastTimestampID = lastData.getTimestampID();

        //            boolean spaceUpdated = false;
        int currentTimestamp = 0;
        //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
        do {

            //if space is null, compute it from all aggregated monitored data recorded so far
            List<CostEnrichedSnapshot> enrichedCostSnapshots = persistenceDelegate.extractTotalUsageSnapshot(currentTimestamp, serviceID);
            dataFromTimestamp = new ArrayList<>();
            for (CostEnrichedSnapshot snapshot : enrichedCostSnapshots) {
                dataFromTimestamp.add(snapshot.getSnapshot());
            }

            //check if new data has been collected between elasticity space querries
            if (!dataFromTimestamp.isEmpty()) {
                currentTimestamp = enrichedCostSnapshots.get(enrichedCostSnapshots.size() - 1).getLastUpdatedTimestampID();

                for (ServiceMonitoringSnapshot snapshot : dataFromTimestamp) {

                    MonitoredElementMonitoringSnapshot serviceData = snapshot.getMonitoredData(service);

                    List<MonitoredElementMonitoringSnapshot> toProcessInBFS = new ArrayList<>();
                    toProcessInBFS.add(serviceData);

                    while (!toProcessInBFS.isEmpty()) {
                        MonitoredElementMonitoringSnapshot elementData = toProcessInBFS.remove(0);

                        MonitoredElement element = elementData.getMonitoredElement();

                        for (MonitoredElementMonitoringSnapshot child : elementData.getChildren()) {
                            child.getMonitoredElement().setName(element.getId() + "_" + child.getMonitoredElement().getName());
                            toProcessInBFS.add(child);
                        }

                        Map<Metric, List<MetricValue>> unitValues;
                        if (csv.containsKey(element)) {
                            unitValues = csv.get(element);
                        } else {
                            unitValues = new LinkedHashMap<>();
                            csv.put(element, unitValues);
                        }
                        Map<Metric, MetricValue> monElementata = elementData.getMonitoredData();
                        for (Metric metric : monElementata.keySet()) {
//                                if (metric.getType().equals(Metric.MetricType.COST)) {
                            List<MetricValue> metricValues;
                            if (unitValues.containsKey(metric)) {
                                metricValues = unitValues.get(metric);
                            } else {
                                metricValues = new ArrayList<>();
                                unitValues.put(metric, metricValues);
                            }
                            metricValues.add(monElementata.get(metric));
//                                }

                        }
                    }
                }
            }

        } while (!dataFromTimestamp.isEmpty() && currentTimestamp < lastTimestampID);

        List<List<String>> columns = new ArrayList<>();

        for (MonitoredElement element
                : csv.keySet()) {

            for (Metric metric : csv.get(element).keySet()) {

                List<String> column = new ArrayList<>();
                columns.add(column);

                column.add(element.getName() + "_" + metric.getName() + ":" + metric.getMeasurementUnit());

                for (MetricValue value : csv.get(element).get(metric)) {
                    column.add(value.getValueRepresentation());
                }
            }
        }

        //write to string
        try {
            BufferedWriter writer = new BufferedWriter(sw);
            boolean haveData = true;
            while (haveData) {
                haveData = false;

                for (List<String> column : columns) {
                    if (column.isEmpty()) {
                        writer.write(",");
                    } else {
                        haveData = true;
                        writer.write("," + column.remove(0));
                    }

                }
                writer.newLine();

            }

            writer.flush();
            writer.close();

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

        return sw.toString();
    }
}
