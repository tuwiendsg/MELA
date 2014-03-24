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
package at.ac.tuwien.dsg.mela.analysisservice.persistence;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class PersistenceDelegate {

    static final Logger log = LoggerFactory.getLogger(PersistenceDelegate.class);

    @Autowired
    private PersistenceSQLAccess persistenceSQLAccess;

    public PersistenceDelegate() {
    }

    public void writeElasticitySpace(ElasticitySpace elasticitySpace, String monitoringSequenceID) {
        persistenceSQLAccess.writeElasticitySpace(elasticitySpace, monitoringSequenceID);
    }

    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID) {

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace(monitoringSequenceID);

        //update space with new data
        ConfigurationXMLRepresentation cfg = this.getLatestConfiguration();

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
            List<ServiceMonitoringSnapshot> dataFromTimestamp = this.extractMonitoringData(monitoringSequenceID);

            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
            fct.setRequirements(requirements);
            fct.trainElasticitySpace(dataFromTimestamp);
            space = fct.getElasticitySpace();

            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
            space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

            //persist cached space
            this.writeElasticitySpace(space, monitoringSequenceID);
        } else {
            //else read max 1000 monitoring data records at a time, train space, and repeat as needed

            //if space is not null, update it with new data
            List<ServiceMonitoringSnapshot> dataFromTimestamp = null;

            //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
            do {
                dataFromTimestamp = this.extractMonitoringData(space.getTimestampID(), monitoringSequenceID);
                //check if new data has been collected between elasticity space querries
                if (!dataFromTimestamp.isEmpty()) {
                    ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
                    fct.setRequirements(requirements);
                    fct.trainElasticitySpace(space, dataFromTimestamp, requirements);
                    //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                    space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

                    //persist cached space
                    this.writeElasticitySpace(space, monitoringSequenceID);
                }

            } while (!dataFromTimestamp.isEmpty());

        }

        return space;
    }

    public List<ServiceMonitoringSnapshot> extractLastXMonitoringDataSnapshots(int x, String monitoringSequenceID) {

        return persistenceSQLAccess.extractLastXMonitoringDataSnapshots(x, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(String startTime, String endTime, String monitoringSequenceID) {

        return persistenceSQLAccess.extractMonitoringDataByTimeInterval(startTime, endTime, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(int timestamp, String monitoringSequenceID) {
        return persistenceSQLAccess.extractMonitoringData(timestamp, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractMonitoringData(monitoringSequenceID);

    }

    public List<Metric> getAvailableMetrics(MonitoredElement monitoredElement, String monitoringSequenceID) {
        return persistenceSQLAccess.getAvailableMetrics(monitoredElement, monitoringSequenceID);
    }

    public ConfigurationXMLRepresentation getLatestConfiguration() {
        return persistenceSQLAccess.getLatestConfiguration();

    }

    public ServiceMonitoringSnapshot extractLatestMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractLatestMonitoringData(monitoringSequenceID);
    }

}
