/*
 *  Copyright 2015 Technische Universitat Wien (TUW), Distributed Systems Group E184
 * 
 *  This work was partially supported by the European Commission in terms of the 
 *  CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package at.ac.tuwien.dsg.mela.costeval.model;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Designed to capture for all monitored elements, for all used cloud offered
 * services, the instantiation time of the service, and the USAGE reported by
 * summing up other collected metrics
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class CostEnrichedSnapshot implements Serializable {

    private int lastUpdatedTimestampID;

    private Logger logger;

    {
        logger = LoggerFactory.getLogger(CostEnrichedSnapshot.class);
    }

    /**
     * For each Monitored element, we have a map of Used Cloud offered Service,
     * and the timestamp in milliseconds since epoch when the offered service
     * was instantiated
     */
    private Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> servicesLifetime;

    {
        servicesLifetime = new HashMap<>();
    }

    private ServiceMonitoringSnapshot snapshot;

    {
        snapshot = new ServiceMonitoringSnapshot();
    }
    
    
    //here just to help us draw lines
    private CompositionRulesBlock costCompositionRules;

    public int getLastUpdatedTimestampID() {
        return lastUpdatedTimestampID;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public CompositionRulesBlock getCostCompositionRules() {
        return costCompositionRules;
    }

    public void setCostCompositionRules(CompositionRulesBlock costCompositionRules) {
        this.costCompositionRules = costCompositionRules;
    }
    
    

    public void setLastUpdatedTimestampID(int lastUpdatedTimestampID) {
        this.lastUpdatedTimestampID = lastUpdatedTimestampID;
    }

    public CostEnrichedSnapshot withtLastUpdatedTimestampID(int lastUpdatedTimestampID) {
        this.lastUpdatedTimestampID = lastUpdatedTimestampID;
        return this;
    }

    public CostEnrichedSnapshot withLogger(final Logger logger) {
        this.logger = logger;
        return this;
    }

    public CostEnrichedSnapshot withSnapshot(final ServiceMonitoringSnapshot totalUsageSoFar) {
        this.snapshot = totalUsageSoFar;
        return this;
    }

    public CostEnrichedSnapshot withInstantiationTimes(MonitoredElement element, UsedCloudOfferedService cloudOfferedService, Long timestamp) {
        Map<UsedCloudOfferedService, Long> elementServices;

        if (servicesLifetime.containsKey(element)) {
            elementServices = servicesLifetime.get(element);
        } else {
            elementServices = new HashMap<>();
            servicesLifetime.put(element, elementServices);
        }

        elementServices.put(cloudOfferedService, timestamp);

        return this;
    }

    public Long getInstantiationTime(MonitoredElement element, UsedCloudOfferedService cloudOfferedService) {
        if (servicesLifetime.containsKey(element)) {
            Map<UsedCloudOfferedService, Long> elementServices = servicesLifetime.get(element);
            if (elementServices.containsKey(cloudOfferedService)) {

                return elementServices.get(cloudOfferedService);
            } else {
                logger.error("UsedCloudOfferedService with name {} and ID {} not found for element {}", new Object[]{cloudOfferedService.getName(), cloudOfferedService.getId(), element.getId()});;
                return 0l;
            }
        } else {
            logger.error("Element {} not found in snapshot", element.getId());
            return 0l;
        }
    }

    public Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> getServicesLifetime() {
        return servicesLifetime;
    }

    public Map<UsedCloudOfferedService, Long> getServicesLifetime(MonitoredElement element) {
        if (servicesLifetime.containsKey(element)) {
            return servicesLifetime.get(element);
        } else {
            return new HashMap<>();
        }
    }

    public ServiceMonitoringSnapshot getSnapshot() {
        return snapshot;
    }

    public CostEnrichedSnapshot withLastUpdatedTimestampID(final int lastUpdatedTimestampID) {
        this.lastUpdatedTimestampID = lastUpdatedTimestampID;
        return this;
    }

    public CostEnrichedSnapshot withServicesLifetime(final Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> servicesLifetime) {
        this.servicesLifetime = servicesLifetime;
        return this;
    }

    public CostEnrichedSnapshot withCostCompositionRules(final CompositionRulesBlock costCompositionRules) {
        this.costCompositionRules = costCompositionRules;
        return this;
    }

}
