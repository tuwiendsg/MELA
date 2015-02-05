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
package at.ac.tuwien.dsg.mela.costeval.persistence;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class PersistenceDelegate {

    static final Logger log = LoggerFactory.getLogger(PersistenceDelegate.class);

    @Autowired
    private PersistenceSQLAccess persistenceSQLAccess;

    @Value("#{melaDBConnector}")
    private DataSource dataSource;

    protected JdbcTemplate jdbcTemplate;

    public PersistenceDelegate() {
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        log.debug("Creating new JdbcTemplate with datasource {}", dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void writeElasticitySpace(ElasticitySpace elasticitySpace, String monitoringSequenceID) {
        try {
            persistenceSQLAccess.writeElasticitySpace(elasticitySpace, monitoringSequenceID);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID) {

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace(monitoringSequenceID);

        //update space with new data
        ConfigurationXMLRepresentation cfg = this.getLatestConfiguration(monitoringSequenceID);

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
            space.setStartTimestampID(dataFromTimestamp.get(0).getTimestampID());
            space.setEndTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

            //persist cached space
            this.writeElasticitySpace(space, monitoringSequenceID);
        } else {
            //else read max 1000 monitoring data records at a time, train space, and repeat as needed

            //if space is not null, update it with new data
            List<ServiceMonitoringSnapshot> dataFromTimestamp = null;

            //used to detect last snapshot timestamp, and then extratc new data until that timestamp
            ServiceMonitoringSnapshot monitoringSnapshot = this.extractLatestMonitoringData(monitoringSequenceID);

            Integer lastTimestampID = (monitoringSnapshot == null) ? Integer.MAX_VALUE : monitoringSnapshot.getTimestampID();

            boolean spaceUpdated = false;

            //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
            do {
                //gets data after the supplied timestamp
                dataFromTimestamp = this.extractMonitoringData(space.getEndTimestampID(), monitoringSequenceID);

                if (dataFromTimestamp != null) {

                    //remove all data after monitoringSnapshot timestamp
                    Iterator<ServiceMonitoringSnapshot> it = dataFromTimestamp.iterator();
                    while (it.hasNext()) {

                        Integer timestampID = it.next().getTimestampID();
                        if (timestampID > lastTimestampID) {
                            it.remove();
                        }

                    }
                }
                //check if new data has been collected between elasticity space querries
                if (!dataFromTimestamp.isEmpty()) {
                    ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
                    fct.setRequirements(requirements);
                    fct.trainElasticitySpace(space, dataFromTimestamp, requirements);
                    //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                    space.setEndTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());
                    spaceUpdated = true;
                }

            } while (!dataFromTimestamp.isEmpty());

            //persist cached space
            if (spaceUpdated) {
                this.writeElasticitySpace(space, monitoringSequenceID);
            }
        }

        return space;
    }

    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID, final int startTimestampID, final int endTimestampID) {

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace(monitoringSequenceID);

        //update space with new data
        ConfigurationXMLRepresentation cfg = this.getLatestConfiguration(monitoringSequenceID);

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

            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
            fct.setRequirements(requirements);
            List<ServiceMonitoringSnapshot> dataFromTimestamp = new ArrayList<ServiceMonitoringSnapshot>();

            int currentStart = startTimestampID;
            do {
                dataFromTimestamp = this.extractMonitoringDataByTimeInterval(currentStart, endTimestampID, monitoringSequenceID);
                fct.trainElasticitySpace(dataFromTimestamp);
                currentStart += 1000; //advance start timestamp with 1000, as we read max 1000 records at once
            } while (dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID() < endTimestampID);

            space = fct.getElasticitySpace();

            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
            space.setStartTimestampID(startTimestampID);
            space.setEndTimestampID(endTimestampID);

            //check how much data we actually extracted, as the extraction limits data to 1000
            //persist cached space
            this.writeElasticitySpace(space, monitoringSequenceID);

        }
        return space;

    }

    public List<ServiceMonitoringSnapshot> extractLastXMonitoringDataSnapshots(int x, String monitoringSequenceID) {

        return persistenceSQLAccess.extractLastXMonitoringDataSnapshots(x, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(int startTimestampID, int endTimestampID, String monitoringSequenceID) {

        return persistenceSQLAccess.extractMonitoringDataByTimeInterval(startTimestampID, endTimestampID, monitoringSequenceID);
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

    public ConfigurationXMLRepresentation getLatestConfiguration(String monitoringSequenceID) {
        return persistenceSQLAccess.getLatestConfiguration(monitoringSequenceID);

    }

    public ServiceMonitoringSnapshot extractLatestMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractLatestMonitoringData(monitoringSequenceID);
    }

    public List<String> getMonitoringSequencesIDs() {
        return persistenceSQLAccess.getMonitoringSequencesIDs();
    }

    //    CaschedHistoricalUsage (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int,
    //    data  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
    public CostEnrichedSnapshot extractTotalUsageSnapshot(String serviceID) {
        String sql = "SELECT CaschedHistoricalUsage.timestampID, CaschedHistoricalUsage.data from CaschedHistoricalUsage where "
                + "ID=(SELECT MAX(ID) from CaschedHistoricalUsage where monSeqID=?);";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        List<CostEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots.get(0);
        }
    }

    public void persistTotalUsageSnapshot(String serviceID, CostEnrichedSnapshot serviceUsageSnapshot) {

//        //delete old cached
//        {
//            String sql = "DELETE FROM CaschedHistoricalUsage WHERE monseqid=?";
//            jdbcTemplate.update(sql, serviceID);
//        }
        {
            String sql = "INSERT INTO CaschedHistoricalUsage (monseqid, timestampID, data) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, serviceID, serviceUsageSnapshot.getLastUpdatedTimestampID(), serviceUsageSnapshot);
        }

    }

    public CostEnrichedSnapshot extractInstantCostSnapshot(String serviceID) {
        String sql = "SELECT InstantCostHistory.timestampID, InstantCostHistory.data from InstantCostHistory where "
                + "ID=(SELECT MAX(ID) from InstantCostHistory where monSeqID=?);";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        List<CostEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots.get(0);
        }
    }

    public void persistInstantCostSnapshot(String serviceID, CostEnrichedSnapshot serviceUsageSnapshot) {

//        //delete old cached
//        {
//            String sql = "DELETE FROM CaschedHistoricalUsage WHERE monseqid=?";
//            jdbcTemplate.update(sql, serviceID);
//        }
        {
            String sql = "INSERT INTO InstantCostHistory (monseqid, timestampID, data) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, serviceID, serviceUsageSnapshot.getLastUpdatedTimestampID(), serviceUsageSnapshot);
        }

    }

    public CostEnrichedSnapshot extractTotalCostSnapshot(String serviceID) {
        String sql = "SELECT TotalCostHistory.timestampID, TotalCostHistory.data from TotalCostHistory where "
                + "ID=(SELECT MAX(ID) from TotalCostHistory where monSeqID=?);";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        List<CostEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots.get(0);
        }
    }

    public void persistTotalCostSnapshot(String serviceID, CostEnrichedSnapshot serviceUsageSnapshot) {

//        //delete old cached
//        {
//            String sql = "DELETE FROM CaschedHistoricalUsage WHERE monseqid=?";
//            jdbcTemplate.update(sql, serviceID);
//        }
        {
            String sql = "INSERT INTO TotalCostHistory (monseqid, timestampID, data) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, serviceID, serviceUsageSnapshot.getLastUpdatedTimestampID(), serviceUsageSnapshot);
        }

    }

    private CostEnrichedSnapshot mapToServiceUsageSnapshot(ResultSet rs) throws SQLException {
        int sTimestamp = rs.getInt(1);
        Object data = rs.getObject(2);

        //if array of bytes as mysql returns
        if (data instanceof byte[]) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) data);
                ObjectInput in = new ObjectInputStream(bis);
                CostEnrichedSnapshot snapshot = (CostEnrichedSnapshot) in.readObject();
                snapshot.setLastUpdatedTimestampID(sTimestamp);
                return snapshot;
            } catch (ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new CostEnrichedSnapshot();
            } catch (IOException ex) {
                log.info(ex.getMessage(), ex);
                return new CostEnrichedSnapshot();
            }
        } else {
            //can convert and return with H2 and HyperSQL adapters
            CostEnrichedSnapshot snapshot = (CostEnrichedSnapshot) rs.getObject(2);
            snapshot.setLastUpdatedTimestampID(sTimestamp);
            return snapshot;
        }
    }

}
