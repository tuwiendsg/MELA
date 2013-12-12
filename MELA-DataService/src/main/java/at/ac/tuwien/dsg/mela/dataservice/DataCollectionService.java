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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.JMSException;

import org.apache.log4j.Level;

import at.ac.tuwien.dsg.mela.common.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.AbstractDataAccess;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccess;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccessWithAutoStructureDetection;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import at.ac.tuwien.dsg.mela.dataservice.utils.ConvertToJSON;

import org.json.simple.JSONObject;

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
    //used for data Aggregation over time
    private List<ServiceMonitoringSnapshot> historicalMonitoringData;
    //used if somewone wants freshest data
    private ServiceMonitoringSnapshot latestMonitoringData;
    //interval at which RAW monitoring data is collected
    private int monitoringIntervalInSeconds = Configuration.getDataPoolingInterval();
    //interval over which raw monitoring data is aggregated.
    //example: for monitoringIntervalInSeconds at 5 seconds, and aggregation at 30, 
    //means 6 monitoring snapshots are aggregated into 1
    private int aggregationWindowsCount = Configuration.getDataAggregationWindows();
    private Timer monitoringTimer;
    //holding MonitoredElement name, and Actions Name
    private Map<MonitoredElement, List<String>> actionsInExecution;
    private DataAggregationEngine instantMonitoringDataEnrichmentEngine;
    private AggregatedMonitoringDataSQLAccess aggregatedMonitoringDataSQLAccess;
    //used in monitoring 
    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
        }
    };
//    private SystemControl selfReference;

    protected DataCollectionService() {

        if (Configuration.automatedStructureDetection()) {
            dataAccess = DataAccessWithAutoStructureDetection.createInstance();
        } else {
            dataAccess = DataAccess.createInstance();
        }

        instantMonitoringDataEnrichmentEngine = new DataAggregationEngine();

//        latestMonitoringData = new ServiceMonitoringSnapshot();
        historicalMonitoringData = new ArrayList<ServiceMonitoringSnapshot>();
        monitoringTimer = new Timer();
//        selfReference = this;
        actionsInExecution = new ConcurrentHashMap<MonitoredElement,  List<String>>();
        startMonitoring();

        if ((int) (monitoringIntervalInSeconds / aggregationWindowsCount) == 0) {
            aggregationWindowsCount = 1 * monitoringIntervalInSeconds;
        }

        aggregatedMonitoringDataSQLAccess = new AggregatedMonitoringDataSQLAccess("mela", "mela");
        ConfigurationXMLRepresentation configurationXMLRepresentation = aggregatedMonitoringDataSQLAccess.getLatestConfiguration();
        serviceConfiguration = configurationXMLRepresentation.getServiceConfiguration();
        setCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());

        requirements = configurationXMLRepresentation.getRequirements();
    }

    public static DataCollectionService getInstance() {
        return collectionService;
    }

    public synchronized MonitoredElement getServiceConfiguration() {
        return serviceConfiguration;
    }

    public synchronized void addExecutingAction(String targetEntityID,  List<String> actionName) {
        MonitoredElement element = new MonitoredElement(targetEntityID);
        actionsInExecution.put(element, actionName);
    }

    public synchronized void setConfiguration(ConfigurationXMLRepresentation configurationXMLRepresentation) {
        serviceConfiguration = configurationXMLRepresentation.getServiceConfiguration();
        setCompositionRulesConfiguration(configurationXMLRepresentation.getCompositionRulesConfiguration());

        requirements = configurationXMLRepresentation.getRequirements();

        startMonitoring();
    }

    public synchronized void removeExecutingAction(String targetEntityID, List<String> actionName) {
        MonitoredElement element = new MonitoredElement(targetEntityID);
        if (actionsInExecution.containsKey(element)) {
            actionsInExecution.get(element).removeAll(actionName);
        }  
    }
 

    public synchronized void setServiceConfiguration(MonitoredElement serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
        monitoringTimer.cancel();
        monitoringTimer.purge();
        startMonitoring();
    }

    public synchronized void setRequirements(Requirements requirements) {
        this.requirements = requirements;
    }

    //actually removes all VMs and Virtual Clusters from the ServiceUnit and adds new ones.
    public synchronized void updateServiceConfiguration(MonitoredElement serviceConfiguration) {
        //extract all ServiceUnit level monitored elements from both services, and replace their children  
        Map<MonitoredElement, MonitoredElement> serviceUnits = new HashMap<MonitoredElement, MonitoredElement>();
        for (MonitoredElement element : this.serviceConfiguration) {
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                //remove element's children
                element.getContainedElements().clear();
                serviceUnits.put(element, element);
            }
        }

        //go trough the new service, and for each Service Unit, add its children (containing both Virtual Machines and Virtual Clusters) to the original service
        for (MonitoredElement element : serviceConfiguration) {
            if (serviceUnits.containsKey(element)) {
                //bad practice. breaks encapsulation
                serviceUnits.get(element).getContainedElements().addAll(element.getContainedElements());
            }
        }

    }

//    public List<Neuron> getElPathwayGroups(Map<Metric, List<MetricValue>> map) {
//        if (elasticitySpaceFunction != null && map != null) {
//            return elasticityPathway.getSituationGroups(map);
//        } else {
//            return new ArrayList<Neuron>();
//        }
//    }
//    public synchronized Requirements getRequirements() {
//        return requirements;
//    }
// 
//    public synchronized CompositionRulesConfiguration getCompositionRulesConfiguration() {
//        return compositionRulesConfiguration;
//    }
    public synchronized void setCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) {
        if (dataAccess != null) {
            dataAccess.getMetricFilters().clear();
            //add data access metric filters for the source of each composition rule
            for (CompositionRule compositionRule : compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules()) {
                //go trough each CompositionOperation and extract the source metrics

                List<CompositionOperation> queue = new ArrayList<CompositionOperation>();
                queue.add(compositionRule.getOperation());

                while (!queue.isEmpty()) {
                    CompositionOperation operation = queue.remove(0);
                    queue.addAll(operation.getSubOperations());

                    Metric targetMetric = operation.getTargetMetric();
                    //metric can be null if a composition rule artificially creates a metric using SET_VALUE
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
            this.compositionRulesConfiguration = compositionRulesConfiguration;
        } else {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl."
                    + "Metric filters to get metrics targeted by composition rules will not be added");
            this.compositionRulesConfiguration = compositionRulesConfiguration;
        }

    }

//    public synchronized AbstractDataAccess getDataAccess() {
//        return dataAccess;
//    }
//    public synchronized void setDataAccess(AbstractDataAccess dataAccess) {
//        this.dataAccess = dataAccess;
//    }
    public synchronized ServiceMonitoringSnapshot getRawMonitoringData() {
        if (dataAccess != null) {
            Date before = new Date();
            ServiceMonitoringSnapshot monitoredData = dataAccess.getMonitoredData(serviceConfiguration);
            Date after = new Date();
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Raw monitoring data access time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
            return monitoredData;
        } else {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
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
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
            return new ArrayList<Metric>();
        }
    }

    public synchronized void addMetricFilter(MetricFilter metricFilter) {
        if (dataAccess != null) {
            dataAccess.addMetricFilter(metricFilter);
        } else {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void addMetricFilters(Collection<MetricFilter> newFilters) {
        if (dataAccess != null) {
            dataAccess.addMetricFilters(newFilters);
        } else {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void removeMetricFilter(MetricFilter metricFilter) {
        if (dataAccess != null) {
            dataAccess.removeMetricFilter(metricFilter);
        } else {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void removeMetricFilters(Collection<MetricFilter> filtersToRemove) {
        if (dataAccess != null) {
            dataAccess.removeMetricFilters(filtersToRemove);
        } else {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Data Access source not set yet on SystemControl");
        }
    }

    public synchronized void setMonitoringIntervalInSeconds(int monitoringIntervalInSeconds) {
        this.monitoringIntervalInSeconds = monitoringIntervalInSeconds;
    }

    public synchronized void setNrOfMonitoringWindowsToAggregate(int aggregationIntervalInSeconds) {
        this.aggregationWindowsCount = aggregationIntervalInSeconds;
    }

//    public synchronized ServiceMonitoringSnapshot getLatestMonitoringData() {
//        return latestMonitoringData;
//    }
    public synchronized void startMonitoring() {
        monitoringTimer = new Timer();

        task = new TimerTask() {
            @Override
            public void run() {
                if (serviceConfiguration != null) {
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "Refreshing data");
                    ServiceMonitoringSnapshot monitoringData = getRawMonitoringData();

                    if (monitoringData != null) {
                        historicalMonitoringData.add(monitoringData);
                        //remove the oldest and add the new value always
                        if (historicalMonitoringData.size() > aggregationWindowsCount) {
                            historicalMonitoringData.remove(0);
                        }

                        if (compositionRulesConfiguration != null) {
                            latestMonitoringData = getAggregatedMonitoringDataOverTime(historicalMonitoringData);
                            for(MonitoredElement element : actionsInExecution.keySet()){
                                latestMonitoringData.setExecutingActions(element,actionsInExecution.get(element));
                            }
                        }

                        //if we have no composition function, we have no metrics, so it does not make sense to train the elasticity space
                        if (compositionRulesConfiguration != null) {
                            //write monitoring data in sql
                            Date before = new Date();
                            aggregatedMonitoringDataSQLAccess.writeMonitoringData(latestMonitoringData);
                            Date after = new Date();
                            Configuration.getLogger(this.getClass()).log(Level.WARN, "DaaS data writing time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
//                            elasticitySpaceFunction.trainElasticitySpace(latestMonitoringData);
                        }
                    } else {
                        //stop the monitoring if the data replay is done
//                        this.cancel();
                        Configuration.getLogger(this.getClass()).log(Level.ERROR, "Monitoring data is NULL");
                    }
                } else {
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "No service configuration");
                }
            }
        };
        Configuration.getLogger(this.getClass()).log(Level.WARN, "Scheduling data pool at " + monitoringIntervalInSeconds + " seconds");
        //repeat the monitoring every monitoringIntervalInSeconds seconds 
        monitoringTimer.schedule(task, 0, monitoringIntervalInSeconds * 1000);

    }

    public synchronized void stopMonitoring() {
        task.cancel();
    }
//    public synchronized String getLatestMonitoringDataINJSON() {
//        Date before = new Date();
//        String converted = ConvertToJSON.convertMonitoringSnapshot(latestMonitoringData, requirements, actionsInExecution);
//        Date after = new Date();
//        Configuration.getLogger(this.getClass()).log(Level.WARN, "Get Mon Data time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());
//        return converted;
//    }
//
//    public synchronized String getMetricCompositionRules() {
//        if (compositionRulesConfiguration != null) {
//            return ConvertToJSON.convertToJSON(compositionRulesConfiguration.getMetricCompositionRules());
//        } else {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("name", "No composition rules yet");
//            return jsonObject.toJSONString();
//        }
//    }
}
