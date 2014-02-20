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
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractDataSource;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractPoolingDataSource;
import at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import java.util.Map;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 * Used in replaying monitoring data
 *
 */
//TODO: refine the class. now is for tests and methods do not use supplied params, is a mess
public class ReplayFromSQLDataAccess extends AbstractDataSource {

    private int timestamp = 0;

    public ReplayFromSQLDataAccess(Map<String, String> configuration) {
        super(configuration);
        sqlAccess = new PersistenceSQLAccess("mela", "mela", Configuration.getDataServiceIP(), Configuration.getDataServicePort(), configuration.get("serviceID"));
    }

    public MonitoringData getMonitoringData() throws DataAccessException {
        return sqlAccess.getRawMonitoringData(configuration.get("serviceID"), "" + timestamp++);
    }

    private PersistenceSQLAccess sqlAccess;
//
//    public static ReplayFromSQLDataAccess createInstance(String monitoredElementID) {
//        return new ReplayFromSQLDataAccess(monitoredElementID);
//    }
//
//    private ReplayFromSQLDataAccess(String monitoredElementID) {
//        sqlAccess = new PersistenceSQLAccess("mela", "mela", Configuration.getDataServiceIP(), Configuration.getDataServicePort(), monitoredElementID);
//    }
//
//    @Override
//    public ServiceMonitoringSnapshot getStructuredMonitoredData(MonitoredElement monitoredElement) {
//        return sqlAccess.extractLatestMonitoringData();
//    }
//
//    public Collection<ServiceMonitoringSnapshot> getAllStructuredMonitoredData(MonitoredElement monitoredElement) {
//        return sqlAccess.extractMonitoringData();
//    }
//
//    public Collection<ServiceMonitoringSnapshot> getAllStructuredMonitoredData(MonitoredElement monitoredElement, int startindex, int endIndex) {
//        return sqlAccess.extractMonitoringData(startindex, endIndex);
//    }
//
//    @Override
//    public MonitoredElementMonitoringSnapshot getSingleElementMonitoredData(MonitoredElement monitoredElement) {
//        ServiceMonitoringSnapshot sms = sqlAccess.extractLatestMonitoringData();
//        return sms.getMonitoredData(monitoredElement);
//    }

}
