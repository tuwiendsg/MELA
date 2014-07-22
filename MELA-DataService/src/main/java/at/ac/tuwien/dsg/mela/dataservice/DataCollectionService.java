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
package at.ac.tuwien.dsg.mela.dataservice;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Action;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractDataAccess;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractDataSource;
import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccessWithManualStructureManagement;

import at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 */
@Service
@DependsOn("persistenceSQLAccess")
public class DataCollectionService {

    static final Logger log = LoggerFactory.getLogger(DataCollectionService.class);

//    @Value("#{${dataaccess.automaticstructuredetection} ? @autoUnguidedStructureDetectionDataAccess : @defaultDataAccess}")
//    private AbstractDataAccess dataAccess;
//    @Value("${dataaccess.automaticstructuredetection")
//    private boolean automaticStructureDetection;
    /**
     * Key is ID of service monitored, and value its personal data access object
     */
    private Map<String, AbstractDataAccess> dataAccesses;

    {
        dataAccesses = new ConcurrentHashMap<String, AbstractDataAccess>();
    }

    /**
     * key = ID of service for which Requirements are stored in values field
     */
    private Map<String, Requirements> requirementsConfiguration;

    {
        requirementsConfiguration = new ConcurrentHashMap();
    }

    /**
     * key = ID of service for which Rules are stored in values field
     */
    private Map<String, CompositionRulesConfiguration> compositionRulesConfigurations;

    {
        compositionRulesConfigurations = new ConcurrentHashMap();
    }
    /**
     * key = ID of service for which Service structure is stored in values field
     */
    private Map<String, MonitoredElement> serviceConfigurations;

    {
        serviceConfigurations = new ConcurrentHashMap();
    }

    // used for data Aggregation over time
    //key is service ID
    //temporarily removed
//    private Map<String, List<ServiceMonitoringSnapshot>> historicalMonitoringDatas;
//
//    {
//        historicalMonitoringDatas = new ConcurrentHashMap<String, List<ServiceMonitoringSnapshot>>();
//    }
    // used if someone wants freshest data
    // private ServiceMonitoringSnapshot latestMonitoringData;
    // interval at which RAW monitoring data is collected
    @Value("${monitoring.polling.interval:1}")
    private int monitoringIntervalInSeconds;

    // interval over which raw monitoring data is aggregated.
    // example: for monitoringIntervalInSeconds at 5 seconds, and aggregation at
    // 30,
    // means 6 monitoring snapshots are aggregated into 1
    @Value("${monitoring.aggregation.windowsize:2}")
    private int aggregationWindowsCount;

    @Value("${dataservice.behavior.monitoring}")
    private boolean monitoring;

    /**
     * Key is ID of the service monitored by the monitoring timer
     */
    private Map<String, Timer> monitoringTimers;

    {
        monitoringTimers = new ConcurrentHashMap<String, Timer>();
    }

    // holding MonitoredElement name, and Actions Name
    private Map<String, List<Action>> actionsInExecution;

    {
        actionsInExecution = new ConcurrentHashMap<String, List<Action>>();
    }

    @Autowired
    private DataAggregationEngine instantMonitoringDataEnrichmentEngine;

    @Autowired
    private PersistenceDelegate persistenceSQLAccess;

    @Autowired
    private ApplicationContext context;

//    // used in monitoring
//    private TimerTask task = new TimerTask() {
//        @Override
//        public void run() {
//        }
//    };
    private ExecutorService scheduler;

    @PostConstruct
    public void init() {
        log.debug("Initializing DataCollectionService");
        actionsInExecution = new HashMap<String, List<Action>>();

        if ((monitoringIntervalInSeconds / aggregationWindowsCount) == 0) {
            aggregationWindowsCount = monitoringIntervalInSeconds;
        }

        scheduler = Executors.newFixedThreadPool(100);

        //read all existing Service IDs and start monitoring timers for them
        for (String monSeqID : persistenceSQLAccess.getMonitoringSequencesIDs()) {

            ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceSQLAccess.getLatestConfiguration(monSeqID);
            serviceConfigurations.put(monSeqID, configurationXMLRepresentation.getServiceConfiguration());
            compositionRulesConfigurations.put(monSeqID, configurationXMLRepresentation.getCompositionRulesConfiguration());
            requirementsConfiguration.put(monSeqID, configurationXMLRepresentation.getRequirements());

            startMonitoring(monSeqID);

        }
    }

    public synchronized Collection<MonitoredElement> getServiceConfiguration() {
        return serviceConfigurations.values();
    }

    public synchronized void addExecutingActions(String serviceID, List<Action> actions) {
        if (actionsInExecution.containsKey(serviceID)) {
            List<Action> inExecution = actionsInExecution.get(serviceID);
            inExecution.addAll(actions);
        } else {
            List<Action> inExecution = Collections.synchronizedList(new ArrayList<Action>());
            inExecution.addAll(actions);
            actionsInExecution.put(serviceID, inExecution);
        }

    }

    public synchronized void removeExecutingActions(String serviceID, List<Action> actions) {
        if (actionsInExecution.containsKey(serviceID)) {
            List<Action> inExecution = actionsInExecution.get(serviceID);
            inExecution.removeAll(actions);
        }
    }

    public synchronized void addConfiguration(ConfigurationXMLRepresentation configurationXMLRepresentation) {

        MonitoredElement serviceConfiguration = configurationXMLRepresentation.getServiceConfiguration();

        serviceConfigurations.put(serviceConfiguration.getId(), serviceConfiguration);

        setCompositionRulesConfiguration(serviceConfiguration.getId(), configurationXMLRepresentation.getCompositionRulesConfiguration());

        persistenceSQLAccess.writeMonitoringSequenceId(serviceConfiguration.getId());
        persistenceSQLAccess.writeConfiguration(serviceConfiguration.getId(), configurationXMLRepresentation);

        requirementsConfiguration.put(serviceConfiguration.getId(), configurationXMLRepresentation.getRequirements());

        if (monitoringTimers.containsKey(serviceConfiguration.getId())) {
            Timer t = monitoringTimers.get(serviceConfiguration.getId());
            t.cancel();
            t.purge();
        }

        startMonitoring(serviceConfiguration.getId());
    }

    public synchronized void addService(MonitoredElement serviceConfiguration) {

        serviceConfigurations.put(serviceConfiguration.getId(), serviceConfiguration);

        //check if this is update on previous service
        if (monitoringTimers.containsKey(serviceConfiguration.getId())) {
            Timer t = monitoringTimers.get(serviceConfiguration.getId());
            t.cancel();
            t.purge();
        }

        CompositionRulesConfiguration compositionRulesConfiguration = (compositionRulesConfigurations.containsKey(serviceConfiguration.getId()))
                ? compositionRulesConfigurations.get(serviceConfiguration.getId()) : new CompositionRulesConfiguration();

        Requirements requirements = (requirementsConfiguration.containsKey(serviceConfiguration.getId()))
                ? requirementsConfiguration.get(serviceConfiguration.getId()) : new Requirements();

        persistenceSQLAccess.writeMonitoringSequenceId(serviceConfiguration.getId());
        persistenceSQLAccess.writeConfiguration(serviceConfiguration.getId(), new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));
        setCompositionRulesConfiguration(serviceConfiguration.getId(), compositionRulesConfiguration);
        startMonitoring(serviceConfiguration.getId());
    }

    public synchronized void addRequirements(String serviceID, Requirements requirements) {

        if (!serviceConfigurations.containsKey(serviceID)) {
            log.error("Service with id \"" + serviceID + "\" not found");
            return;
        }
        MonitoredElement serviceConfiguration = serviceConfigurations.get(serviceID);

        CompositionRulesConfiguration compositionRulesConfiguration = (compositionRulesConfigurations.containsKey(serviceConfiguration.getId()))
                ? compositionRulesConfigurations.get(serviceConfiguration.getId()) : new CompositionRulesConfiguration();

        requirementsConfiguration.put(serviceID, requirements);

        persistenceSQLAccess.writeConfiguration(serviceID, new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));

    }

    // actually removes all VMs and Virtual Clusters from the ServiceUnit and
    // adds new ones.
    public synchronized void updateServiceConfiguration(MonitoredElement serviceConfiguration) {

        if (!serviceConfigurations.containsKey(serviceConfiguration.getId())) {
            log.error("Service with id \"" + serviceConfiguration.getId() + "\" not found");
            return;
        }

        // extract all ServiceUnit level monitored elements from both services,
        // and replace their children
        Map<MonitoredElement, MonitoredElement> serviceUnits = new ConcurrentHashMap<MonitoredElement, MonitoredElement>();
        for (MonitoredElement element : serviceConfigurations.get(serviceConfiguration.getId())) {
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
                // bad practice. breaks encapsulation
                serviceUnits.get(element).getContainedElements().addAll(element.getContainedElements());
            }
        }

        CompositionRulesConfiguration compositionRulesConfiguration = (compositionRulesConfigurations.containsKey(serviceConfiguration.getId()))
                ? compositionRulesConfigurations.get(serviceConfiguration.getId()) : new CompositionRulesConfiguration();

        Requirements requirements = (requirementsConfiguration.containsKey(serviceConfiguration.getId()))
                ? requirementsConfiguration.get(serviceConfiguration.getId()) : new Requirements();

        persistenceSQLAccess.writeConfiguration(serviceConfiguration.getId(), new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));

    }

    public synchronized void setCompositionRulesConfiguration(String serviceID, CompositionRulesConfiguration compositionRulesConfiguration) {

        if (!serviceConfigurations.containsKey(serviceID)) {
            log.error("Service with id \"" + serviceID + "\" not found");
            return;
        }
        MonitoredElement serviceConfiguration = serviceConfigurations.get(serviceID);
        compositionRulesConfigurations.put(serviceID, compositionRulesConfiguration);
        Requirements requirements = (requirementsConfiguration.containsKey(serviceConfiguration.getId()))
                ? requirementsConfiguration.get(serviceConfiguration.getId()) : new Requirements();

        persistenceSQLAccess.writeConfiguration(serviceID, new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));

        AbstractDataAccess dataAccess = null;
        if (!dataAccesses.containsKey(serviceID)) {

            dataAccess = DataAccessWithManualStructureManagement.createInstance();
            dataAccesses.put(serviceID, dataAccess);

            // list all MELA datasources from application context
            //maybe in future add specific source for specific service
            Map<String, AbstractDataSource> dataSources = context.getBeansOfType(AbstractDataSource.class);

            for (String dataSourceName : dataSources.keySet()) {
                AbstractDataSource dataSource = dataSources.get(dataSourceName);
                log.debug("Found Datasource '{}': {}", dataSourceName, dataSource);
                dataAccess.addDataSource(dataSource);
            }

        } else {
            dataAccess = dataAccesses.get(serviceID);
        }

        dataAccess.getMetricFilters().clear();

        // set metric filters on data access
        for (CompositionRule compositionRule : compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules()) {
            // go trough each CompositionOperation and extract the source
            // metrics

            List<CompositionOperation> queue = new ArrayList<CompositionOperation>();
            queue.add(compositionRule.getOperation());

            while (!queue.isEmpty()) {
                CompositionOperation operation = queue.remove(0);
                queue.addAll(operation.getSubOperations());
                Metric targetMetric = operation.getTargetMetric();
                // metric can be null if a composition rule artificially creates
                // a metric using SET_VALUE
                if (targetMetric != null) {
                    MetricFilter metricFilter = new MetricFilter();
                    metricFilter.setId(targetMetric.getName() + "_Filter");
                    metricFilter.setLevel(operation.getMetricSourceMonitoredElementLevel());
                    Collection<Metric> metrics = new ArrayList<Metric>();
                    metrics.add(new Metric(targetMetric.getName()));
                    metricFilter.setMetrics(metrics);

                    if (monitoring) {
                        dataAccess.addMetricFilter(metricFilter);
                    }
                }
            }
        }
    }

    public synchronized ServiceMonitoringSnapshot getRawMonitoringData(String serviceID) {
        if (!dataAccesses.containsKey(serviceID)) {
            log.error("Data Access not found for service with id \"" + serviceID + "\" not found");
            return new ServiceMonitoringSnapshot();
        }
        AbstractDataAccess dataAccess = dataAccesses.get(serviceID);
        if (dataAccess != null) {
            if (!serviceConfigurations.containsKey(serviceID)) {
                log.error("Service with id \"" + serviceID + "\" not found");
                return new ServiceMonitoringSnapshot();
            }
            MonitoredElement serviceConfiguration = serviceConfigurations.get(serviceID);

            Date before = new Date();
            ServiceMonitoringSnapshot monitoredData = dataAccess.getStructuredMonitoredData(serviceConfiguration);
            Date after = new Date();
            log.debug("Raw monitoring data access time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
            return monitoredData;
        } else {
            log.warn("Data Access source not set yet on SystemControl");
            return new ServiceMonitoringSnapshot();
        }
    }

    public ServiceMonitoringSnapshot getAggregatedMonitoringDataOverTime(String serviceID, List<ServiceMonitoringSnapshot> serviceMonitoringSnapshots) {
        if (!serviceConfigurations.containsKey(serviceID)) {
            log.error("Service with id \"" + serviceID + "\" not found");
            return new ServiceMonitoringSnapshot();
        }
        CompositionRulesConfiguration compositionRulesConfiguration = (compositionRulesConfigurations.containsKey(serviceID))
                ? compositionRulesConfigurations.get(serviceID) : new CompositionRulesConfiguration();

        if (serviceMonitoringSnapshots.size() > 1) {
            return instantMonitoringDataEnrichmentEngine.aggregateMonitoringDataOverTime(compositionRulesConfiguration, serviceMonitoringSnapshots);
        } else {
            return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, serviceMonitoringSnapshots.get(0));
        }
    }

    public Collection<Metric> getAvailableMetricsForMonitoredElement(String serviceID, MonitoredElement MonitoredElement) {
        if (!dataAccesses.containsKey(serviceID)) {
            log.error("Data Access not found for service with id \"" + serviceID + "\" not found");
            return new ArrayList<Metric>();
        }
        AbstractDataAccess dataAccess = dataAccesses.get(serviceID);
        if (dataAccess != null) {
            return dataAccess.getAvailableMetricsForMonitoredElement(MonitoredElement);
        } else {
            log.warn("Data Access source not set yet on SystemControl");
            return new ArrayList<Metric>();
        }
    }

    public synchronized void addMetricFilter(String serviceID, MetricFilter metricFilter) {
        if (!dataAccesses.containsKey(serviceID)) {
            log.error("Data Access not found for service with id \"" + serviceID + "\" not found");
            return;
        }
        AbstractDataAccess dataAccess = dataAccesses.get(serviceID);
        if (dataAccess != null) {
            dataAccess.addMetricFilter(metricFilter);
        } else {
            log.warn("Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void addMetricFilters(String serviceID, Collection<MetricFilter> newFilters) {
        if (!dataAccesses.containsKey(serviceID)) {
            log.error("Data Access not found for service with id \"" + serviceID + "\" not found");
            return;
        }
        AbstractDataAccess dataAccess = dataAccesses.get(serviceID);
        if (dataAccess != null) {
            dataAccess.addMetricFilters(newFilters);
        } else {
            log.warn("Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void removeMetricFilter(String serviceID, MetricFilter metricFilter) {
        if (!dataAccesses.containsKey(serviceID)) {
            log.error("Data Access not found for service with id \"" + serviceID + "\" not found");
            return;
        }
        AbstractDataAccess dataAccess = dataAccesses.get(serviceID);
        if (dataAccess != null) {
            dataAccess.removeMetricFilter(metricFilter);
        } else {
            log.warn("Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void removeMetricFilters(String serviceID, Collection<MetricFilter> filtersToRemove) {
        if (!dataAccesses.containsKey(serviceID)) {
            log.error("Data Access not found for service with id \"" + serviceID + "\" not found");
            return;
        }
        AbstractDataAccess dataAccess = dataAccesses.get(serviceID);
        if (dataAccess != null) {
            dataAccess.removeMetricFilters(filtersToRemove);
        } else {
            log.warn("Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void setMonitoringIntervalInSeconds(int monitoringIntervalInSeconds) {
        this.monitoringIntervalInSeconds = monitoringIntervalInSeconds;
    }

    public synchronized void setNrOfMonitoringWindowsToAggregate(int aggregationIntervalInSeconds) {
        this.aggregationWindowsCount = aggregationIntervalInSeconds;
    }

    public synchronized void startMonitoring(final String serviceID) {

        if (!serviceConfigurations.containsKey(serviceID)) {
            return;
        }

        if (monitoring) {

            //reapply composition rules
            AbstractDataAccess dataAccess = null;
            if (!dataAccesses.containsKey(serviceID)) {
                dataAccess = DataAccessWithManualStructureManagement.createInstance();
                dataAccesses.put(serviceID, dataAccess);

                // list all MELA datasources from application context
                //maybe in future add specific source for specific service
                Map<String, AbstractDataSource> dataSources = context.getBeansOfType(AbstractDataSource.class);

                for (String dataSourceName : dataSources.keySet()) {
                    AbstractDataSource dataSource = dataSources.get(dataSourceName);
                    log.debug("Found Datasource '{}': {}", dataSourceName, dataSource);
                    dataAccess.addDataSource(dataSource);
                }

            } else {
                dataAccess = dataAccesses.get(serviceID);
            }
            dataAccess.getMetricFilters().clear();

            if (compositionRulesConfigurations.containsKey(serviceID)) {
                // set metric filters on data access
                for (CompositionRule compositionRule : compositionRulesConfigurations.get(serviceID).getMetricCompositionRules().getCompositionRules()) {
                    // go trough each CompositionOperation and extract the source
                    // metrics

                    List<CompositionOperation> queue = new ArrayList<CompositionOperation>();
                    queue.add(compositionRule.getOperation());

                    while (!queue.isEmpty()) {
                        CompositionOperation operation = queue.remove(0);
                        queue.addAll(operation.getSubOperations());
                        Metric targetMetric = operation.getTargetMetric();
                        // metric can be null if a composition rule artificially creates
                        // a metric using SET_VALUE
                        if (targetMetric != null) {
                            MetricFilter metricFilter = new MetricFilter();
                            metricFilter.setId(targetMetric.getName() + "_Filter");
                            metricFilter.setLevel(operation.getMetricSourceMonitoredElementLevel());
                            Collection<Metric> metrics = new ArrayList<Metric>();
                            metrics.add(new Metric(targetMetric.getName()));
                            metricFilter.setMetrics(metrics);

                            if (monitoring) {
                                dataAccess.addMetricFilter(metricFilter);
                            }
                        }
                    }
                }
            }

            log.debug("Starting monitoring for serviceConfiguration {}", serviceID);

            Timer monitoringTimer = new Timer();
            monitoringTimers.put(serviceID, monitoringTimer);

            final Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    if (serviceConfigurations.containsKey(serviceID)) {
                        log.debug("Refreshing data");
                        ServiceMonitoringSnapshot monitoringData = getRawMonitoringData(serviceID);

                        if (monitoringData != null) {
                            List<ServiceMonitoringSnapshot> dataToAggregate = null;

//                            if (historicalMonitoringDatas.containsKey(serviceID)) {
//                                dataToAggregate = historicalMonitoringDatas.get(serviceID);
//                            } else {
                            dataToAggregate = new ArrayList<ServiceMonitoringSnapshot>();
//                                historicalMonitoringDatas.put(serviceID, dataToAggregate);
//                            }

                            dataToAggregate.add(monitoringData);
                            // remove the oldest and add the new value always
                            if (dataToAggregate.size() > aggregationWindowsCount) {
                                dataToAggregate.remove(0);
                            }
                            Date beforeAggregation = new Date();
                            ServiceMonitoringSnapshot latestMonitoringData = getAggregatedMonitoringDataOverTime(serviceID, dataToAggregate);
                            Date afterAggregation = new Date();

                            log.debug("Data aggregation time time in ms:  " + new Date(afterAggregation.getTime() - beforeAggregation.getTime()).getTime());

                            if (actionsInExecution.containsKey(serviceID)) {
                                latestMonitoringData.setExecutingActions(actionsInExecution.get(serviceID));
                            }

                            // write monitoring data in sql
                            Date before = new Date();
                            String timestamp = "" + new Date().getTime();

                            Date beforePersisting = new Date();
                            MonitoredElement serviceConfiguration = serviceConfigurations.get(serviceID);

                            Date beforeTimestamp = new Date();
                            persistenceSQLAccess.writeInTimestamp(timestamp, serviceConfiguration, serviceConfiguration.getId());
                            Date afterTimestamp = new Date();

                            log.debug("Timestamp persistence time in ms:  " + new Date(afterTimestamp.getTime() - beforeTimestamp.getTime()).getTime());

                            //add same timestamp on all mon data
                            //this is something as a short-hand solution
                            {
                                for (Map<MonitoredElement, MonitoredElementMonitoringSnapshot> map : latestMonitoringData.getMonitoredData().values()) {
                                    for (MonitoredElementMonitoringSnapshot mems : map.values()) {
                                        mems.setTimestamp(timestamp);
                                    }
                                }
                            }
                            Date beforeMonData = new Date();
                            // write structured monitoring data
                            persistenceSQLAccess.writeMonitoringData(timestamp, latestMonitoringData, serviceConfiguration.getId());
                            Date afterMonData = new Date();
                            log.debug("Aggregated monitoring data persistence time in ms:  " + new Date(afterMonData.getTime() - beforeMonData.getTime()).getTime());

//                            //temporarily due to performance reasons, raw data is not stored anymore (takes too long to store raw data)
//                            Date beforeRawData = new Date();
//                            // write monitoring data directly collected
//                            persistenceSQLAccess.writeRawMonitoringData(timestamp, dataAccesses.get(serviceID).getFreshestMonitoredData(), serviceConfiguration.getId());
//                            Date afterRawData = new Date();
//                            log.debug("Raw monitoring data persistence time in ms:  " + new Date(afterRawData.getTime() - beforeRawData.getTime()).getTime());
                            Date afterPersisting = new Date();

                            log.debug("Data persistence time in ms:  " + new Date(afterPersisting.getTime() - beforePersisting.getTime()).getTime());

                            // update and store elasticity pathway
                            // LightweightEncounterRateElasticityPathway
                            // elasticityPathway =
                            // persistenceSQLAccess.extractLatestElasticityPathway();
                            // in future just update pathway. now recompute
                            // elasticityPathway.trainElasticityPathway(null)
                            Date after = new Date();
                            log.debug("Complete data monitoring/aggregating/persisting cycle time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
                            // elasticitySpaceFunction.trainElasticitySpace(latestMonitoringData);
//                                }

                        } else {
                            // stop the monitoring if the data replay is done
                            // this.cancel();
                            log.error("Monitoring data is NULL");
                        }
                    } else {
                        log.warn("No service configuration");
                    }
                }
            };

            TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    scheduler.submit(runnable);
                }
            };

            log.debug("Scheduling data pool at " + monitoringIntervalInSeconds + " seconds");
            // repeat the monitoring every monitoringIntervalInSeconds seconds
            monitoringTimer.schedule(task, 0, monitoringIntervalInSeconds * 1000);

        }

    }

    public synchronized void stopMonitoring() {
        for (Timer timer : monitoringTimers.values()) {
            timer.cancel();
            timer.purge();
        }
    }

}
