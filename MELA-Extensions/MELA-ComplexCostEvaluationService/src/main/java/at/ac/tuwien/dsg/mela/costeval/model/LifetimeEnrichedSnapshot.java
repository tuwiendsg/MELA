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
public class LifetimeEnrichedSnapshot implements Serializable {

    private int lastUpdatedTimestampID;

    private Logger logger;

    {
        logger = LoggerFactory.getLogger(LifetimeEnrichedSnapshot.class);
    }

    /**
     * For each Monitored element, we have a map of Used Cloud offered Service,
     * and the timestamp in milliseconds since epoch when the offered service
     * was instantiated
     */
    private Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> instantiationTimes;

    {
        instantiationTimes = new HashMap<>();
    }

    //marks the time at which the service was deallocated, i.e. no longer used
    private Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> deallocationTimes;

    {
        deallocationTimes = new HashMap<>();
    }

    private ServiceMonitoringSnapshot snapshot;

    {
        snapshot = new ServiceMonitoringSnapshot();
    }

    public int getLastUpdatedTimestampID() {
        return lastUpdatedTimestampID;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void setLastUpdatedTimestampID(int lastUpdatedTimestampID) {
        this.lastUpdatedTimestampID = lastUpdatedTimestampID;
    }

    public LifetimeEnrichedSnapshot withtLastUpdatedTimestampID(int lastUpdatedTimestampID) {
        this.lastUpdatedTimestampID = lastUpdatedTimestampID;
        return this;
    }

    public LifetimeEnrichedSnapshot withLogger(final Logger logger) {
        this.logger = logger;
        return this;
    }

    public LifetimeEnrichedSnapshot withSnapshot(final ServiceMonitoringSnapshot totalUsageSoFar) {
        this.snapshot = totalUsageSoFar;
        for (MonitoredElement element : totalUsageSoFar.getMonitoredService()) {
            for (UsedCloudOfferedService cloudOfferedService : element.getCloudOfferedServices()) {
                withInstantiationTime(element, cloudOfferedService, Long.parseLong(totalUsageSoFar.getTimestamp()));
            }
        }
        return this;
    }

    public LifetimeEnrichedSnapshot withInstantiationTime(MonitoredElement element, UsedCloudOfferedService cloudOfferedService, Long timestamp) {
        Map<UsedCloudOfferedService, Long> elementServices;

        if (instantiationTimes.containsKey(element)) {
            elementServices = instantiationTimes.get(element);
        } else {
            elementServices = new HashMap<>();
            instantiationTimes.put(element, elementServices);
        }

        elementServices.put(cloudOfferedService, timestamp);

        return this;
    }

    public Long getInstantiationTime(MonitoredElement element, UsedCloudOfferedService cloudOfferedService) {
        if (instantiationTimes.containsKey(element)) {
            Map<UsedCloudOfferedService, Long> elementServices = instantiationTimes.get(element);
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

    public boolean hasInstantiationTime(MonitoredElement element, UsedCloudOfferedService cloudOfferedService) {
        if (instantiationTimes.containsKey(element)) {
            Map<UsedCloudOfferedService, Long> elementServices = instantiationTimes.get(element);
            if (elementServices.containsKey(cloudOfferedService)) {
                return true;
            } else {
                logger.error("UsedCloudOfferedService with name {} and ID {} not found for element {}", new Object[]{cloudOfferedService.getName(), cloudOfferedService.getId(), element.getId()});;
                return false;
            }
        } else {
            logger.error("Element {} not found in snapshot", element.getId());
            return false;
        }
    }

    public Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> getInstantiationTimes() {
        return instantiationTimes;
    }

    public Map<UsedCloudOfferedService, Long> getInstantiationTimes(MonitoredElement element) {
        if (instantiationTimes.containsKey(element)) {
            return instantiationTimes.get(element);
        } else {
            return new HashMap<>();
        }
    }

    public LifetimeEnrichedSnapshot withDeallocationTime(MonitoredElement element, UsedCloudOfferedService cloudOfferedService, Long timestamp) {
        Map<UsedCloudOfferedService, Long> elementServices;

        if (deallocationTimes.containsKey(element)) {
            elementServices = deallocationTimes.get(element);
        } else {
            elementServices = new HashMap<>();
            deallocationTimes.put(element, elementServices);
        }

        elementServices.put(cloudOfferedService, timestamp);

        return this;
    }

    public Long getDeallocationTime(MonitoredElement element, UsedCloudOfferedService cloudOfferedService) {
        if (deallocationTimes.containsKey(element)) {
            Map<UsedCloudOfferedService, Long> elementServices = deallocationTimes.get(element);
            if (elementServices.containsKey(cloudOfferedService)) {

                return elementServices.get(cloudOfferedService);
            } else {
                logger.error("UsedCloudOfferedService with name {} and ID {} not found for element {}", new Object[]{cloudOfferedService.getName(), cloudOfferedService.getId(), element.getId()});;
                return 0l;
            }
        } else {
//            logger.error("Element {} not found in snapshot", element.getId());
            return 0l;
        }
    }

    public boolean hasDeallocationTime(MonitoredElement element, UsedCloudOfferedService cloudOfferedService) {
        if (deallocationTimes.containsKey(element)) {
            Map<UsedCloudOfferedService, Long> elementServices = deallocationTimes.get(element);
            if (elementServices.containsKey(cloudOfferedService)) {
                return true;
            } else {
                logger.error("UsedCloudOfferedService with name {} and ID {} not found for element {}", new Object[]{cloudOfferedService.getName(), cloudOfferedService.getId(), element.getId()});;
                return false;
            }
        } else {
//            logger.error("Element {} not found in snapshot", element.getId());
            return false;
        }
    }

    public Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> getDeallocationTimes() {
        return deallocationTimes;
    }

    public Map<UsedCloudOfferedService, Long> getDeallocationTimes(MonitoredElement element) {
        if (deallocationTimes.containsKey(element)) {
            return deallocationTimes.get(element);
        } else {
            return new HashMap<>();
        }
    }

    public ServiceMonitoringSnapshot getSnapshot() {
        return snapshot;
    }

    public LifetimeEnrichedSnapshot withLastUpdatedTimestampID(final int lastUpdatedTimestampID) {
        this.lastUpdatedTimestampID = lastUpdatedTimestampID;
        return this;
    }

    public LifetimeEnrichedSnapshot withInstantiationTimes(final Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> instantiationTimes) {
        this.instantiationTimes = instantiationTimes;
        return this;
    }

    public LifetimeEnrichedSnapshot withDeallocationTimes(final Map<MonitoredElement, Map<UsedCloudOfferedService, Long>> deallocationTimes) {
        this.deallocationTimes = deallocationTimes;
        return this;
    }

    public LifetimeEnrichedSnapshot clone() {
        LifetimeEnrichedSnapshot ces = new LifetimeEnrichedSnapshot()
                .withSnapshot(this.snapshot.clone())
                .withDeallocationTimes(this.deallocationTimes)
                .withInstantiationTimes(this.instantiationTimes);
        return ces;
    }

}
