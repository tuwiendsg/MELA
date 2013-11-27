
///**
// * Contains the old version in which we figured out the structure of the application from a metric deployed in each VM
// * Good when replaying monitoring data, as it shows new VMs being added/removed.
// */
 
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
package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl;

import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataAccess.DataSourceI;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.ClusterInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.HostInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.MetricInfo;
import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.AbstractDataAccess;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
public class DataAccesForTestsOnly extends AbstractDataAccess {

    private DataSourceI gangliaDataSourceI;

    private DataAccesForTestsOnly(DataSourceI gangliaDataSourceI) {
        this.gangliaDataSourceI = gangliaDataSourceI;
    }

    public static DataAccesForTestsOnly createInstance() {
        
        String accessType = Configuration.getMonitoringDataAccessMethod();
        
        if (accessType.equalsIgnoreCase("Ganglia")) {
            DataSourceI dataSource = new LocalGangliaLiveDataSource();
            return new DataAccesForTestsOnly(dataSource);
        } else if (accessType.equalsIgnoreCase("RemoteGanglia")) {
            DataSourceI dataSource = new RemoteGangliaLiveDataSource();
            return new DataAccesForTestsOnly(dataSource);
        } else if (accessType.equalsIgnoreCase("JCatascopia")) {
            Configuration.getLogger(DataAccesForTestsOnly.class).log(Priority.ERROR, "JCatascopia adapter not yet implemented. Using dummy.");
            return new DataAccesForTestsOnly(new DummyDataSource());
        } else if (accessType.equalsIgnoreCase("Replay")) {
            String monitoringSeqID = Configuration.getStoredMonitoringSequenceID();
            DataSourceI dataSourceI = new GangliaSQLDataSource(monitoringSeqID, "mela", "mela");
            return new DataAccesForTestsOnly(dataSourceI);
        } else {
            Configuration.getLogger(DataAccesForTestsOnly.class).log(Priority.ERROR, "MELA-DataService data access mode not specified or not recognized");
            return new DataAccesForTestsOnly(new DummyDataSource());
        }
    }

    /**
     * @param MonitoredElement the root element of the Service Structure
     * hierarchy
     * @return ServiceMonitoringSnapshot containing the monitored data organized
     * both in tree and by level Searches in the Ganglia HOSTS monitoring for
     * MonitoredElement ID, and if it finds such ID searches it in the supplied
     * Service structure, after, adds the monitoring information as a
     * sub-element MonitoredElement of VM level to the element having the found
     * ID
     */
    @Override
    public synchronized ServiceMonitoringSnapshot getMonitoredData(MonitoredElement m) {

        if (m == null) {
            Configuration.getLogger(this.getClass()).log(Level.WARN, "No supplied service configuration");
            return new ServiceMonitoringSnapshot();
        }
        MonitoredElement structureRoot = m.clone();

        ClusterInfo gangliaClusterInfo = null;
        ServiceMonitoringSnapshot serviceMonitoringSnapshot = new ServiceMonitoringSnapshot();

        try {
            gangliaClusterInfo = gangliaDataSourceI.getMonitoringData();
            if (gangliaClusterInfo == null) {
                return new ServiceMonitoringSnapshot();
            }
        } catch (DataAccessException e) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, e.getMessage(), e);
            Configuration.getLogger(this.getClass()).log(Level.ERROR, "Terminating execution");
            System.exit(1);
        }

        /**
         * Linear representation of MonitoredElement hierarchical tree. also
         * maintains the three structure using the .children relationship
         * between MonitoredElementMonitoringSnapshot instances
         */
        Map<MonitoredElement, MonitoredElement> elements = new LinkedHashMap<MonitoredElement, MonitoredElement>();

        //traverse the MonitoredElement hierarchical tree in BFS and extract the serviceStructure elements
        List<MonitoredElementMonitoringSnapshot> bfsTraversalQueue = new ArrayList<MonitoredElementMonitoringSnapshot>();
        MonitoredElementMonitoringSnapshot rootMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(structureRoot, new LinkedHashMap<Metric, MetricValue>());

        bfsTraversalQueue.add(rootMonitoringSnapshot);
        serviceMonitoringSnapshot.addMonitoredData(rootMonitoringSnapshot);

        while (!bfsTraversalQueue.isEmpty()) {
            MonitoredElementMonitoringSnapshot element = bfsTraversalQueue.remove(0);
            MonitoredElement processedElement = element.getMonitoredElement();
            elements.put(processedElement, processedElement);

            for (MonitoredElement child : processedElement.getContainedElements()) //add empty monitoring data for each serviceStructure element, to serve as a place where in the future composed metrics can be added
            {
                MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(child, new LinkedHashMap<Metric, MetricValue>());
                element.addChild(MonitoredElementMonitoringSnapshot);
                serviceMonitoringSnapshot.addMonitoredData(MonitoredElementMonitoringSnapshot);
                bfsTraversalQueue.add(MonitoredElementMonitoringSnapshot);
            }

        }


        //iterate trough the GangliaCluster, extract each VM monitoring data, build an MonitoredElementMonitoringSnapshot from it and add it to the ServiceMonitoringSnapshot

        Collection<HostInfo> gangliaHostsInfo = gangliaClusterInfo.getHostsInfo();

        for (HostInfo gangliaHostInfo : gangliaHostsInfo) {
            HashMap<Metric, MetricValue> monitoredMetricValues = new LinkedHashMap<Metric, MetricValue>();
//            MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot();
            MonitoredElement monitoredElement = null;
            //represent all monitored metrics in mapToElasticitySpace
            for (MetricInfo gangliaMetricInfo : gangliaHostInfo.getMetrics()) {
                Metric metric = new Metric();
                metric.setName(gangliaMetricInfo.getName());
                metric.setMeasurementUnit(gangliaMetricInfo.getUnits());
                MetricValue metricValue = new MetricValue(gangliaMetricInfo.getConvertedValue());
                monitoredMetricValues.put(metric, metricValue);
                if (metric.getName().equals(Configuration.getMonitoredElementIDMetricName())) {
                    monitoredElement = new MonitoredElement();
                    monitoredElement.setId(gangliaMetricInfo.getValue());
                    monitoredElement.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                }
            }
            //if we have found a metric containing a MonitoredElementID, and if that ID is present in our structure
            //add it as VM level child to the found Service ID (this is the logic under our ganglia deployment so far)
            if (monitoredElement != null && elements.containsKey(monitoredElement)) {
                MonitoredElement structureElement = elements.get(monitoredElement);
                MonitoredElement vmLevelElement = new MonitoredElement();
                vmLevelElement.setId(gangliaHostInfo.getIp());
                vmLevelElement.setName(gangliaHostInfo.getIp());
                vmLevelElement.setLevel(MonitoredElement.MonitoredElementLevel.VM);
                structureElement.addElement(vmLevelElement);

                MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vmLevelElement, monitoredMetricValues);

                //also add VM monitoring info to children tree
                //TODO: CHECK THIS: not sure if this does not introduce errors with SUM. In the case of not automatic structure detection, it DOES
                serviceMonitoringSnapshot.getMonitoredData(monitoredElement).addChild(MonitoredElementMonitoringSnapshot);

                serviceMonitoringSnapshot.addMonitoredData(MonitoredElementMonitoringSnapshot);
            }
        }

        // filter the monitoredMetricValues according to the metric filters if such exist
        // the filtering is done here after collections since I iterate trough all metrics above to find the MonitoredElementID
        // which is later used to determine to which level I need to map the data
        // also i can use the code below when I get data at diff levels and I move from Ganglia
        serviceMonitoringSnapshot.applyMetricFilters(metricFilters);

        return serviceMonitoringSnapshot;

    }

    @Override
    public synchronized MonitoredElementMonitoringSnapshot getSingleElementMonitoredData(MonitoredElement MonitoredElement) {

        ClusterInfo gangliaClusterInfo = null;
        try {
            gangliaClusterInfo = gangliaDataSourceI.getMonitoringData();
            if (gangliaClusterInfo == null) {
                return new MonitoredElementMonitoringSnapshot();
            }
        } catch (DataAccessException e) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, e.getMessage(), e);
            Configuration.getLogger(this.getClass()).log(Level.ERROR, "Terminating execution");
            System.exit(1);
        }


        Collection<HostInfo> gangliaHostsInfo = gangliaClusterInfo.getHostsInfo();
        for (HostInfo gangliaHostInfo : gangliaHostsInfo) {
            HashMap<Metric, MetricValue> monitoredMetricValues = new LinkedHashMap<Metric, MetricValue>();
//            MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot();
            MonitoredElement monitoredElement = null;
            //represent all monitored metrics in mapToElasticitySpace
            for (MetricInfo gangliaMetricInfo : gangliaHostInfo.getMetrics()) {
                Metric metric = new Metric();
                metric.setName(gangliaMetricInfo.getName());
                metric.setMeasurementUnit(gangliaMetricInfo.getUnits());
                MetricValue metricValue = new MetricValue(gangliaMetricInfo.getConvertedValue());
                monitoredMetricValues.put(metric, metricValue);
                if (metric.getName().equals(Configuration.getMonitoredElementIDMetricName())) {
                    monitoredElement = new MonitoredElement();
                    monitoredElement.setId(gangliaMetricInfo.getValue());
                }
            }
            //if we have found a metric containing a MonitoredElementID, and if that ID is present in our structure
            //add it as VM level child to the found Service ID (this is the logic under our ganglia deployment so far)
            if (monitoredElement != null && MonitoredElement.equals(monitoredElement)) {

                MonitoredElementMonitoringSnapshot monitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(MonitoredElement, monitoredMetricValues);

                //filters are applied sequentially in cascade
                if (metricFilters.containsKey(MonitoredElement.getLevel())) {
                    for (MetricFilter filter : metricFilters.get(MonitoredElement.getLevel())) {
                        //if either the filter applies on all elements at one particular level (targetIDs are null or empty) either the filter targets the serviceStructure element ID
                        if (filter.getTargetMonitoredElementIDs() == null || filter.getTargetMonitoredElementIDs().size() == 0 || filter.getTargetMonitoredElementIDs().contains(MonitoredElement.getId())) {
                            monitoredElementMonitoringSnapshot.keepMetrics(filter.getMetrics());
                        }
                    }
                }
                return monitoredElementMonitoringSnapshot;

            }
        }
        return new MonitoredElementMonitoringSnapshot();
    }

    @Override
    public Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement MonitoredElement) {
        ClusterInfo gangliaClusterInfo = null;
        try {
            gangliaClusterInfo = gangliaDataSourceI.getMonitoringData();
            if (gangliaClusterInfo == null) {
                return new ArrayList<Metric>();
            }
        } catch (DataAccessException e) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, e.getMessage(), e);
            Configuration.getLogger(this.getClass()).log(Level.ERROR, "Terminating execution");
            System.exit(1);
        }


        Collection<HostInfo> gangliaHostsInfo = gangliaClusterInfo.getHostsInfo();
        for (HostInfo gangliaHostInfo : gangliaHostsInfo) {

            Map<Metric, MetricValue> monitoredMetricValues = new LinkedHashMap<Metric, MetricValue>();
//            MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot();
            MonitoredElement monitoredElement = null;
            //represent all monitored metrics in mapToElasticitySpace
            for (MetricInfo gangliaMetricInfo : gangliaHostInfo.getMetrics()) {
                Metric metric = new Metric();
                metric.setName(gangliaMetricInfo.getName());
                metric.setMeasurementUnit(gangliaMetricInfo.getUnits());
                MetricValue metricValue = new MetricValue(gangliaMetricInfo.getConvertedValue());
                monitoredMetricValues.put(metric, metricValue);
                if (metric.getName().equals(Configuration.getMonitoredElementIDMetricName())) {
                    monitoredElement = new MonitoredElement();
                    monitoredElement.setId(gangliaMetricInfo.getValue());
                }
            }
            //if we have found a metric containing a MonitoredElementID, and if that ID is present in our structure
            //add it as VM level child to the found Service ID (this is the logic under our ganglia deployment so far)
            if (monitoredElement != null && MonitoredElement.equals(monitoredElement)) {
                return monitoredMetricValues.keySet();
            }
        }
        return new ArrayList<Metric>();
    }
}
