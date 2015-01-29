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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractPollingDataSource;

import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Action;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceDelegate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Timer;
import javax.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

//todo: there should be an app id for replay mode
/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class ReplayFromSQLDataAccess extends AbstractPollingDataSource {

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    private Iterator<Integer> timestampIterator;
    private String monitoringSequenceID;

    static final org.slf4j.Logger log = LoggerFactory.getLogger(ReplayFromSQLDataAccess.class);

    @PostConstruct
    public void init() {
        log.debug("Initializing ReplayFromSQLDataAccess bean");
        timestampIterator = persistenceDelegate.getTimestampIDs(monitoringSequenceID).iterator();
    }

    @Override
    public Long getRateAtWhichDataShouldBeRead() {
        return (long) getPollingIntervalMs();
    }

    public MonitoringData getMonitoringData() throws DataAccessException {

        if (!timestampIterator.hasNext()) {
            log.debug("Recorded monitoring data ended. Can't replay anymore");
            return new MonitoringData();
        } else {
            Integer id = timestampIterator.next();

            MonitoringData data = persistenceDelegate.getRawMonitoringData(monitoringSequenceID, "" + id);

            log.debug("Getting monitoring data for id {} =  {}", id, data);
            return data;
        }

    }

    public String getMonitoringSequenceID() {
        return monitoringSequenceID;
    }

    public void setMonitoringSequenceID(String monitoringSequenceID) {
        this.monitoringSequenceID = monitoringSequenceID;
    }

    @Override
    public String toString() {
        return "ReplayDataSource{"
                + ", for=" + monitoringSequenceID
                + ", pollingInterval=" + getPollingIntervalMs()
                + "}";
    }

}
