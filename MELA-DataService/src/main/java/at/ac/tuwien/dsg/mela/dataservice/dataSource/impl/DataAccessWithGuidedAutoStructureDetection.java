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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractDataAccess;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractDataSource;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
@Service("autoStructureDetectionDataAccess")
public class DataAccessWithGuidedAutoStructureDetection extends AbstractDataAccess {

    
   @Value("${dataservice.autoStructureDetection.monitoredElementIDMetricName:serviceUnitID}")
   private String monitoredElementIDMetricName; 
    
    /**
     * Left as this in case we want to limit in the future the nr of DataAccess
     * instances we create and maybe use a pool of instances
     *
     * @return
     */
    public static DataAccessWithGuidedAutoStructureDetection createInstance() {

        return new DataAccessWithGuidedAutoStructureDetection();
    }

    private DataAccessWithGuidedAutoStructureDetection() {

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
    public synchronized ServiceMonitoringSnapshot getStructuredMonitoredData(MonitoredElement m) {

        if (m == null) {
            Logger.getLogger(DataAccessWithManualStructureManagement.class).log(Level.WARN, "No supplied service configuration");
            return new ServiceMonitoringSnapshot();
        }
        MonitoredElement structureRoot = m.clone();

		// extract all VMs from the service structure
        // Map<MonitoredElement, MonitoredElement> vms = new
        // LinkedHashMap<MonitoredElement, MonitoredElement>();
        /**
         * Linear representation of MonitoredElement hierarchical tree. also
         * maintains the three structure using the .children relationship
         * between MonitoredElementMonitoringSnapshot instances
         */
        Map<MonitoredElement, MonitoredElement> elements = new LinkedHashMap<MonitoredElement, MonitoredElement>();

        ServiceMonitoringSnapshot serviceMonitoringSnapshot = new ServiceMonitoringSnapshot();

		// traverse the MonitoredElement hierarchical tree in BFS and extract
        // the serviceStructure elements
        List<MonitoredElementMonitoringSnapshot> bfsTraversalQueue = new ArrayList<MonitoredElementMonitoringSnapshot>();
        MonitoredElementMonitoringSnapshot rootMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(structureRoot,
                new LinkedHashMap<Metric, MetricValue>());

        bfsTraversalQueue.add(rootMonitoringSnapshot);
        serviceMonitoringSnapshot.addMonitoredData(rootMonitoringSnapshot);

        // used in determining if we have specified a VM, or just ServiceUnit
        MonitoredElement lowestLevelFoundMonitoredElement = null;
        MonitoredElementMonitoringSnapshot lowestLevelFoundMonitoredSnapshot = null;

        while (!bfsTraversalQueue.isEmpty()) {
            MonitoredElementMonitoringSnapshot element = bfsTraversalQueue.remove(0);
            MonitoredElement processedElement = element.getMonitoredElement();
            elements.put(processedElement, processedElement);
            lowestLevelFoundMonitoredElement = processedElement;
            lowestLevelFoundMonitoredSnapshot = element;

			// if(processedElement.getLevel().equals(MonitoredElement.MonitoredElementLevel.VM)){
            // vms.put(processedElement, processedElement);
            // }
            for (MonitoredElement child : processedElement.getContainedElements()) // add
            // empty
            // monitoring
            // data
            // for each serviceStructure element, to serve as a place where
            // in the future composite metrics can be added
            {
                MonitoredElementMonitoringSnapshot monitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(child,
                        new LinkedHashMap<Metric, MetricValue>());
                element.addChild(monitoredElementMonitoringSnapshot);
                serviceMonitoringSnapshot.addMonitoredData(monitoredElementMonitoringSnapshot);
                bfsTraversalQueue.add(monitoredElementMonitoringSnapshot);

            }

        }

		// go through each monitored element and update the service monitoring
        // snapshot
        for (AbstractDataSource dataSource : freshestMonitoredData.keySet()) {
			// maybe in the future we use data source information, but now we
            // extract the monitored data directly

			// maybe in the future we use information from MonitoringData, but
            // now we extract the monitored data elements directly
            for (MonitoredElementData elementData : freshestMonitoredData.get(dataSource).getMonitoredElementDatas()) {

                // create MonitoredElementMonitoringSnapshot for each element
                HashMap<Metric, MetricValue> monitoredMetricValues = new LinkedHashMap<Metric, MetricValue>();

                MonitoredElement monitoredElement = null;
                // represent all monitored metrics in mapToElasticitySpace
                for (MetricInfo gangliaMetricInfo : elementData.getMetrics()) {
                    Metric metric = new Metric();
                    metric.setName(gangliaMetricInfo.getName());
                    metric.setMeasurementUnit(gangliaMetricInfo.getUnits());
                    MetricValue metricValue = new MetricValue(gangliaMetricInfo.getConvertedValue());
                    monitoredMetricValues.put(metric, metricValue);
                    if (metric.getName().equals(monitoredElementIDMetricName)) {
                        monitoredElement = new MonitoredElement();
                        monitoredElement.setId(gangliaMetricInfo.getValue());
                        monitoredElement.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                    }
                }
				// if we have found a metric containing a MonitoredElementID,
                // and if that ID is present in our structure
                // add it as VM level child to the found Service ID (this is the
                // logic under our ganglia deployment so far)
                if (monitoredElement != null && elements.containsKey(monitoredElement)) {
                    MonitoredElement structureElement = elements.get(monitoredElement);
                    MonitoredElement vmLevelElement = new MonitoredElement();

                    vmLevelElement.setId(elementData.getMonitoredElement().getId());
                    vmLevelElement.setName(elementData.getMonitoredElement().getId());
                    vmLevelElement.setLevel(MonitoredElement.MonitoredElementLevel.VM);
                    structureElement.addElement(vmLevelElement);

//                    //set monitoring source on the metric
//                    for (Metric metric : monitoredMetricValues.keySet()) {
//                        metric.setMonitoredElement(vmLevelElement);
//                    }
                    
                    MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(vmLevelElement,
                            monitoredMetricValues);

					// also add VM monitoring info to children tree
                    // TODO: CHECK THIS: not sure if this does not introduce
                    // errors with SUM. In the case of not automatic structure
                    // detection, it DOES
                    serviceMonitoringSnapshot.getMonitoredData(monitoredElement).addChild(MonitoredElementMonitoringSnapshot);

                    serviceMonitoringSnapshot.addMonitoredData(MonitoredElementMonitoringSnapshot);
                }

            }
        }

		// filter the monitoredMetricValues according to the metric filters if
        // such exist
        serviceMonitoringSnapshot.applyMetricFilters(metricFilters);

        return serviceMonitoringSnapshot;

    }

    @Override
    public synchronized MonitoredElementMonitoringSnapshot getSingleElementMonitoredData(MonitoredElement suppliedMonitoringElement) {
        throw new UnsupportedOperationException("getSingleElementMonitoredData not implemented");
    }

}
