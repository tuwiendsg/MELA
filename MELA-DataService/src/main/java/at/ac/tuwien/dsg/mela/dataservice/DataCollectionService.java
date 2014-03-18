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

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
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
import at.ac.tuwien.dsg.mela.dataservice.config.dataSourcesManagement.DataSourceConfig;
import at.ac.tuwien.dsg.mela.dataservice.config.dataSourcesManagement.DataSourceConfigs;
import at.ac.tuwien.dsg.mela.dataservice.config.dataSourcesManagement.DataSourcesManager;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccessWithManualStructureManagement;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccessWithGuidedAutoStructureDetection;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccessWithUnguidedAutoStructureDetection;
import at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import java.lang.String;
import java.util.Collections;
import java.util.Map;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
public class DataCollectionService {
    
    private static DataCollectionService collectionService;
    
    static {
        collectionService = new DataCollectionService();
    }
    private AbstractDataAccess dataAccess;
    private Requirements requirements;
    private CompositionRulesConfiguration compositionRulesConfiguration;
    private MonitoredElement serviceConfiguration;
    // used for data Aggregation over time
    private List<ServiceMonitoringSnapshot> historicalMonitoringData;
    // used if someone wants freshest data
    // private ServiceMonitoringSnapshot latestMonitoringData;
    // interval at which RAW monitoring data is collected
    private int monitoringIntervalInSeconds = Configuration.getDataPoolingInterval();
    // interval over which raw monitoring data is aggregated.
    // example: for monitoringIntervalInSeconds at 5 seconds, and aggregation at
    // 30,
    // means 6 monitoring snapshots are aggregated into 1
    private int aggregationWindowsCount = Configuration.getDataAggregationWindows();
    private Timer monitoringTimer;
    // holding MonitoredElement name, and Actions Name
    private List<Action> actionsInExecution;
    private DataAggregationEngine instantMonitoringDataEnrichmentEngine;
    private PersistenceSQLAccess persistenceSQLAccess;
    // used in monitoring
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
        }
    };

    // private SystemControl selfReference;
    protected DataCollectionService() {
        
        instantMonitoringDataEnrichmentEngine = new DataAggregationEngine();

        // latestMonitoringData = new ServiceMonitoringSnapshot();
        historicalMonitoringData = new ArrayList<ServiceMonitoringSnapshot>();
        monitoringTimer = new Timer();
        // selfReference = this;
        actionsInExecution = Collections.synchronizedList(new ArrayList<Action>());
        
        if ((int) (monitoringIntervalInSeconds / aggregationWindowsCount) == 0) {
            aggregationWindowsCount = 1 * monitoringIntervalInSeconds;
        }

        // get latest config
        ConfigurationXMLRepresentation configurationXMLRepresentation = PersistenceSQLAccess.getLatestConfiguration("mela", "mela",
                Configuration.getDataServiceIP(), Configuration.getDataServicePort());
        
        serviceConfiguration = configurationXMLRepresentation.getServiceConfiguration();
        compositionRulesConfiguration = configurationXMLRepresentation.getCompositionRulesConfiguration();
        requirements = configurationXMLRepresentation.getRequirements();
        
        String dataAccessType = Configuration.getServiceStructureDetectionMechanism().trim().toLowerCase();
        
        if (dataAccessType.contains("auto")) {
            dataAccess = DataAccessWithUnguidedAutoStructureDetection.createInstance();
        } else if (dataAccessType.contains("guided")) {
            dataAccess = DataAccessWithGuidedAutoStructureDetection.createInstance();
        } else {
            dataAccess = DataAccessWithManualStructureManagement.createInstance();
        }
        
        startMonitoring();
    }
    
    public static DataCollectionService getInstance() {
        return collectionService;
    }
    
    public synchronized MonitoredElement getServiceConfiguration() {
        return serviceConfiguration;
    }
    
    public synchronized void addExecutingActions(List<Action> actions) {
        actionsInExecution.addAll(actions);
    }
    
    public synchronized void setConfiguration(ConfigurationXMLRepresentation configurationXMLRepresentation) {
        serviceConfiguration = configurationXMLRepresentation.getServiceConfiguration();
        setCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());
        
        requirements = configurationXMLRepresentation.getRequirements();
        monitoringTimer.cancel();
        monitoringTimer.purge();
        
        String dataAccessType = Configuration.getServiceStructureDetectionMechanism().trim().toLowerCase();
        
        if (dataAccessType.contains("auto")) {
            dataAccess = DataAccessWithUnguidedAutoStructureDetection.createInstance();
        } else if (dataAccessType.contains("guided")) {
            dataAccess = DataAccessWithGuidedAutoStructureDetection.createInstance();
        } else {
            dataAccess = DataAccessWithManualStructureManagement.createInstance();
        }
        startMonitoring();
    }
    
    public synchronized void removeExecutingActions(List<Action> actions) {
        actionsInExecution.removeAll(actions);
    }
    
    public synchronized void setServiceConfiguration(MonitoredElement serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
        monitoringTimer.cancel();
        monitoringTimer.purge();
        try {
            persistenceSQLAccess.closeConnection();
        } catch (SQLException e) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, null, e);
        }
        persistenceSQLAccess = new PersistenceSQLAccess("mela", "mela", Configuration.getDataServiceIP(), Configuration.getDataServicePort(),
                serviceConfiguration.getId());

        //when a service structure is set, switch to manual service structure management
        dataAccess = DataAccessWithManualStructureManagement.createInstance();
        startMonitoring();
    }
    
    public synchronized void setRequirements(Requirements requirements) {
        this.requirements = requirements;
        persistenceSQLAccess.writeConfiguration(new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));
    }

    // actually removes all VMs and Virtual Clusters from the ServiceUnit and
    // adds new ones.
    public synchronized void updateServiceConfiguration(MonitoredElement serviceConfiguration) {
        // extract all ServiceUnit level monitored elements from both services,
        // and replace their children
        Map<MonitoredElement, MonitoredElement> serviceUnits = new HashMap<MonitoredElement, MonitoredElement>();
        for (MonitoredElement element : this.serviceConfiguration) {
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
        persistenceSQLAccess.writeConfiguration(new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));
        
    }

    // public List<Neuron> getElPathwayGroups(Map<Metric, List<MetricValue>>
    // map) {
    // if (elasticitySpaceFunction != null && map != null) {
    // return elasticityPathway.getSituationGroups(map);
    // } else {
    // return new ArrayList<Neuron>();
    // }
    // }
    // public synchronized Requirements getRequirements() {
    // return requirements;
    // }
    //
    // public synchronized CompositionRulesConfiguration
    // getCompositionRulesConfiguration() {
    // return compositionRulesConfiguration;
    // }
    public synchronized void setCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {
        
        this.compositionRulesConfiguration = compositionRulesConfiguration;
        persistenceSQLAccess.writeConfiguration(new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));
        
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
                    dataAccess.addMetricFilter(metricFilter);
                }
            }
        }
        
    }

    // public synchronized AbstractDataAccess getDataAccess() {
    // return dataAccess;
    // }
    // public synchronized void setDataAccess(AbstractDataAccess dataAccess) {
    // this.dataAccess = dataAccess;
    // }
    public synchronized ServiceMonitoringSnapshot getRawMonitoringData() {
        if (dataAccess != null) {
            Date before = new Date();
            ServiceMonitoringSnapshot monitoredData = dataAccess.getStructuredMonitoredData(serviceConfiguration);
            Date after = new Date();
            Logger.getLogger(this.getClass()).log(Level.DEBUG,
                    "Raw monitoring data access time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
            return monitoredData;
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new ServiceMonitoringSnapshot();
        }
    }
    
    public synchronized ServiceMonitoringSnapshot getAggregatedMonitoringDataOverTime(List<ServiceMonitoringSnapshot> serviceMonitoringSnapshots) {
        if (serviceMonitoringSnapshots.size() > 1) {
            return instantMonitoringDataEnrichmentEngine.aggregateMonitoringDataOverTime(compositionRulesConfiguration, serviceMonitoringSnapshots);
        } else {
            return instantMonitoringDataEnrichmentEngine.enrichMonitoringData(compositionRulesConfiguration, serviceMonitoringSnapshots.get(0));
        }
    }
    
    public synchronized Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement MonitoredElement) {
        if (dataAccess != null) {
            return dataAccess.getAvailableMetricsForMonitoredElement(MonitoredElement);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new ArrayList<Metric>();
        }
    }
    
    public synchronized void addMetricFilter(MetricFilter metricFilter) {
        if (dataAccess != null) {
            dataAccess.addMetricFilter(metricFilter);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }
    
    public synchronized void addMetricFilters(Collection<MetricFilter> newFilters) {
        if (dataAccess != null) {
            dataAccess.addMetricFilters(newFilters);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }
    
    public synchronized void removeMetricFilter(MetricFilter metricFilter) {
        if (dataAccess != null) {
            dataAccess.removeMetricFilter(metricFilter);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }
    
    public synchronized void removeMetricFilters(Collection<MetricFilter> filtersToRemove) {
        if (dataAccess != null) {
            dataAccess.removeMetricFilters(filtersToRemove);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }
    
    public synchronized void setMonitoringIntervalInSeconds(int monitoringIntervalInSeconds) {
        this.monitoringIntervalInSeconds = monitoringIntervalInSeconds;
    }
    
    public synchronized void setNrOfMonitoringWindowsToAggregate(int aggregationIntervalInSeconds) {
        this.aggregationWindowsCount = aggregationIntervalInSeconds;
    }

    // public synchronized ServiceMonitoringSnapshot getLatestMonitoringData() {
    // return latestMonitoringData;
    // }
    public synchronized void startMonitoring() {

        // open proper sql access
        persistenceSQLAccess = new PersistenceSQLAccess("mela", "mela", Configuration.getDataServiceIP(), Configuration.getDataServicePort(),
                serviceConfiguration.getId());

        //if operation mode is replay, then do not activate data collection service
        if (Configuration.getOperationMode().equals("replay")) {
            return;
        }
        
        persistenceSQLAccess.writeConfiguration(new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));

        // read data sources configuration file
        DataSourceConfigs dataSources = DataSourcesManager.readDataSourcesConfiguration();
        Logger.getLogger(this.getClass()).log(Level.DEBUG, "Using following data sources:");
        
        for (DataSourceConfig config : dataSources.getConfigs()) {
            
            Logger.getLogger(this.getClass()).log(Level.DEBUG, config.toString());

            // transform configuration options in key-value pairs
            Map<String, String> configuration = new HashMap<String, String>();
            for (String configEntry : config.getProperties()) {
                String[] info = configEntry.split("=");
                configuration.put(info[0], info[1]);
            }
            String pathToDataSource = config.getType();

            //dinamically load data source class
            try {
                //use data source Type to loade it
                Class dataSourceImplementationClass = Class.forName(pathToDataSource);

                //get constructor which takes a Map<String,String> as configuration parameter
                Constructor<AbstractDataSource> constructor = dataSourceImplementationClass.getConstructor(Map.class);
                
                AbstractDataSource dataSourceInstance = constructor.newInstance(configuration);

                //add newly created data source
                dataAccess.addDataSource(dataSourceInstance);
            } catch (Exception e) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, e.getMessage(), e);
            }
            
        }

//        // set metric filters on data access
//        for (CompositionRule compositionRule : compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules()) {
//            // go trough each CompositionOperation and extract the source
//            // metrics
//
//            List<CompositionOperation> queue = new ArrayList<CompositionOperation>();
//            queue.add(compositionRule.getOperation());
//
//            while (!queue.isEmpty()) {
//                CompositionOperation operation = queue.remove(0);
//                queue.addAll(operation.getSubOperations());
//
//                Metric targetMetric = operation.getTargetMetric();
//                // metric can be null if a composition rule artificially creates
//                // a metric using SET_VALUE
//                if (targetMetric != null) {
//                    MetricFilter metricFilter = new MetricFilter();
//                    metricFilter.setId(targetMetric.getName() + "_Filter");
//                    metricFilter.setLevel(operation.getMetricSourceMonitoredElementLevel());
//                    Collection<Metric> metrics = new ArrayList<Metric>();
//                    metrics.add(new Metric(targetMetric.getName()));
//                    metricFilter.setMetrics(metrics);
//                    dataAccess.addMetricFilter(metricFilter);
//                }
//            }
//        }
        monitoringTimer = new Timer();
        
        task = new TimerTask() {
            @Override
            public void run() {
                if (serviceConfiguration != null) {
                    Logger.getLogger(this.getClass()).log(Level.DEBUG, "Refreshing data");
                    ServiceMonitoringSnapshot monitoringData = getRawMonitoringData();
                    
                    if (monitoringData != null) {
                        historicalMonitoringData.add(monitoringData);
                        // remove the oldest and add the new value always
                        if (historicalMonitoringData.size() > aggregationWindowsCount) {
                            historicalMonitoringData.remove(0);
                        }
                        
                        if (compositionRulesConfiguration != null) {
                            ServiceMonitoringSnapshot latestMonitoringData = getAggregatedMonitoringDataOverTime(historicalMonitoringData);
                            latestMonitoringData.setExecutingActions(actionsInExecution);

                            // write monitoring data in sql
                            Date before = new Date();
                            String timestamp = "" + new Date().getTime();

                            //persist updated configuration
                            persistenceSQLAccess.writeConfiguration(new ConfigurationXMLRepresentation(serviceConfiguration, compositionRulesConfiguration, requirements));
                            // add new timestamp

                            persistenceSQLAccess.writeInTimestamp(timestamp, serviceConfiguration.getId(), serviceConfiguration);

                            //add same timestamp on all mon data
                            //this is something as a short-hand solution
                            {
                                for (Map<MonitoredElement, MonitoredElementMonitoringSnapshot> map : latestMonitoringData.getMonitoredData().values()) {
                                    for (MonitoredElementMonitoringSnapshot mems : map.values()) {
                                        mems.setTimestamp(timestamp);
                                    }
                                }
                            }
                            // write structured monitoring data
                            persistenceSQLAccess.writeMonitoringData(timestamp, latestMonitoringData);

                            // write monitoring data directly collected
                            persistenceSQLAccess.writeRawMonitoringData(timestamp, dataAccess.getFreshestMonitoredData());

                            // update and store elasticity pathway
                            // LightweightEncounterRateElasticityPathway
                            // elasticityPathway =
                            // persistenceSQLAccess.extractLatestElasticityPathway();
                            // in future just update pathway. now recompute
                            // elasticityPathway.trainElasticityPathway(null)
                            Date after = new Date();
                            Logger.getLogger(this.getClass()).log(Level.DEBUG,
                                    "DaaS data writing time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
                            // elasticitySpaceFunction.trainElasticitySpace(latestMonitoringData);
                        }
                    } else {
                        // stop the monitoring if the data replay is done
                        // this.cancel();
                        Logger.getLogger(this.getClass()).log(Level.ERROR, "Monitoring data is NULL");
                    }
                } else {
                    Logger.getLogger(this.getClass()).log(Level.WARN, "No service configuration");
                }
            }
        };
        Logger.getLogger(this.getClass()).log(Level.DEBUG, "Scheduling data pool at " + monitoringIntervalInSeconds + " seconds");
        // repeat the monitoring every monitoringIntervalInSeconds seconds
        monitoringTimer.schedule(task, 0, monitoringIntervalInSeconds * 1000);
        
    }
    
    public synchronized void stopMonitoring() {
        try {
            persistenceSQLAccess.closeConnection();
        } catch (SQLException ex) {
            Logger.getLogger(DataCollectionService.class.getName()).log(Level.ERROR, null, ex);
        }
        // task.cancel();
        monitoringTimer.cancel();
    }
    // public synchronized String getLatestMonitoringDataINJSON() {
    // Date before = new Date();
    // String converted =
    // ConvertToJSON.convertMonitoringSnapshot(latestMonitoringData,
    // requirements, actionsInExecution);
    // Date after = new Date();
    // Logger.getLogger(this.getClass()).log(Level.WARN,
    // "Get Mon Data time in ms:  " + new Date(after.getTime() -
    // before.getTime()).getTime());
    // return converted;
    // }
    //
    // public synchronized String getMetricCompositionRules() {
    // if (compositionRulesConfiguration != null) {
    // return
    // ConvertToJSON.convertToJSON(compositionRulesConfiguration.getMetricCompositionRules());
    // } else {
    // JSONObject jsonObject = new JSONObject();
    // jsonObject.put("name", "No composition rules yet");
    // return jsonObject.toJSONString();
    // }
    // }
}
