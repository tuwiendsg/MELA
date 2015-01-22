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
package at.ac.tuwien.dsg.mela.dataservice.persistence;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationUtility;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.Event;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class PersistenceDelegate {

    @Autowired
    private PersistenceSQLAccess persistenceSQLAccess;

    @Autowired
    private ConfigurationUtility configurationUtility;

    public PersistenceDelegate() {
    }

    public void writeMonitoringSequenceId(String sequenceId) {
        persistenceSQLAccess.writeMonitoringSequenceId(sequenceId);
    }

    public void writeRawMonitoringData(String timestamp, Collection<MonitoringData> monitoringData, String monitoringSequenceID) {
        persistenceSQLAccess.writeRawMonitoringData(timestamp, monitoringData, monitoringSequenceID);
    }

    public void writeInTimestamp(String timestamp, MonitoredElement serviceStructure, String monitoringSequenceID) {
        persistenceSQLAccess.writeInTimestamp(timestamp, serviceStructure, monitoringSequenceID);
    }

    public void writeMonitoringData(String timestamp, ServiceMonitoringSnapshot monitoringSnapshot, String monitoringSequenceID) {
        persistenceSQLAccess.writeMonitoringData(timestamp, monitoringSnapshot, monitoringSequenceID);
    }

    public List<Integer> getTimestampIDs(String monitoringSequenceID) {
        return persistenceSQLAccess.getTimestampIDs(monitoringSequenceID);
    }

    public List<String> getMonitoringSequencesIDs() {
        return persistenceSQLAccess.getMonitoringSequencesIDs();
    }

    public List<Event> getUnreadEvents(String serviceID) {
        return persistenceSQLAccess.getUnreadEvents(serviceID);
    }

    public void writeEvents(String serviceID, List<Event> events) {
        persistenceSQLAccess.writeEvents(serviceID, events);
    }

    public void markEventsAsRead(String serviceID, List<Event> events) {
        persistenceSQLAccess.markEventsAsRead(serviceID, events);
    }

    public MonitoringData getRawMonitoringData(String monitoringSequenceID, String timestampID) {
        return persistenceSQLAccess.getRawMonitoringData(monitoringSequenceID, timestampID);
    }

    public ConfigurationXMLRepresentation getLatestConfiguration(String monitoringSequenceID) {
        ConfigurationXMLRepresentation configurationXMLRepresentation = persistenceSQLAccess.getLatestConfiguration(monitoringSequenceID);

        if (configurationXMLRepresentation == null) {
            return configurationUtility.createDefaultConfiguration();
        } else {
            return configurationXMLRepresentation;
        }
    }

    /**
     * @param monitoringSequenceID IF of the service for which configuration
     * will be stored
     * @param configurationXMLRepresentation the used MELA configuration to be
     * persisted in XML and reused
     */
    public void writeConfiguration(String monitoringSequenceID, final ConfigurationXMLRepresentation configurationXMLRepresentation) {
        persistenceSQLAccess.writeConfiguration(monitoringSequenceID, configurationXMLRepresentation);
    }

    public ServiceMonitoringSnapshot extractLatestMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractLatestMonitoringData(monitoringSequenceID);
    }

    public void removeMonitoringSequenceId(String serviceID) {
        persistenceSQLAccess.removeMonitoringSequenceId(serviceID);
    }

    public List<ServiceMonitoringSnapshot> extractLastXMonitoringDataSnapshots(int x, String monitoringSequenceID) {

        return persistenceSQLAccess.extractLastXMonitoringDataSnapshots(x, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(long startTimestampID, long endTimestampID, String monitoringSequenceID) {

        return persistenceSQLAccess.extractMonitoringDataByTimeInterval(startTimestampID, endTimestampID, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimestampIDsInterval(int starTimestampID, int endTimestampID, String monitoringSequenceID) {
        return persistenceSQLAccess.extractMonitoringDataByTimestampIDsInterval(starTimestampID, endTimestampID, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(int timestamp, String monitoringSequenceID) {
        return persistenceSQLAccess.extractMonitoringData(timestamp, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataFromTimestamp(long timestamp, String monitoringSequenceID) {

        return persistenceSQLAccess.extractMonitoringDataFromTimestamp(timestamp, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractMonitoringData(monitoringSequenceID);

    }

}
