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

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.ServiceElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.model.LifetimeEnrichedSnapshot;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
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

    public void setPersistenceSQLAccess(PersistenceSQLAccess persistenceSQLAccess) {
        this.persistenceSQLAccess = persistenceSQLAccess;
    }

    @PostConstruct
    public void init() {
        log.debug("Creating new JdbcTemplate with datasource {}", dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public ElasticitySpace extractLatestInstantCostElasticitySpace(String monitoringSequenceID) {
        String sql = "SELECT startTimestampID, endTimestampID, elasticitySpace from InstantCostElasticitySpace where monSeqID=? and ID=(SELECT MAX(ID) from InstantCostElasticitySpace where monSeqID=?);";
        RowMapper<ElasticitySpace> rowMapper = new RowMapper<ElasticitySpace>() {
            public ElasticitySpace mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSpace(rs);
            }
        };

        //get last space
        List<ElasticitySpace> space = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, monitoringSequenceID);
        if (space.isEmpty()) {
            return null;
        } else {
            return space.get(0);
        }
    }

    public void persistInstantCostElasticitySpace(ElasticitySpace elasticitySpace, String monitoringSequenceID) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(out));
            objectOutputStream.writeObject(elasticitySpace);
            objectOutputStream.close();
        } catch (IOException ex) {
            log.info(ex.getMessage(), ex);
            return;
        }

        byte[] zipped = out.toByteArray();

        String sql = "DELETE FROM InstantCostElasticitySpace WHERE monseqid=?";
        jdbcTemplate.update(sql, elasticitySpace.getService().getId());

        //add new entry
        sql = "INSERT INTO InstantCostElasticitySpace (monSeqID, startTimestampID, endTimestampID, elasticitySpace) "
                + "VALUES "
                + "( (SELECT ID FROM MonitoringSeq WHERE id='"
                + monitoringSequenceID + "')" + ", ? , ? " + ", ? )";

        jdbcTemplate.update(sql, elasticitySpace.getStartTimestampID(), elasticitySpace.getEndTimestampID(), zipped);

    }

    public void persistInstantCostElasticityPathway(ServiceElasticityPathway elasticityPathway, String monitoringSequenceID) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(out));
            objectOutputStream.writeObject(elasticityPathway);
            objectOutputStream.close();
        } catch (IOException ex) {
            log.info(ex.getMessage(), ex);
            return;
        }

        byte[] zipped = out.toByteArray();

        String sql = "DELETE FROM InstantCostElasticityPathway WHERE monseqid=?";
        jdbcTemplate.update(sql, monitoringSequenceID);

        //add new entry
        sql = "INSERT INTO InstantCostElasticityPathway (monSeqID, timestampID, elasticityPathway) "
                + "VALUES "
                + "( (SELECT ID FROM MonitoringSeq WHERE id='"
                + monitoringSequenceID + "')" + ", ? , ? )";

        jdbcTemplate.update(sql, elasticityPathway.getTimestampID(), zipped);

    }

    public ServiceElasticityPathway extractInstantCostElasticityPathway(String monitoringSequenceID) {
        String sql = "SELECT timestampID, elasticityPathway from InstantCostElasticityPathway where monSeqID=? and ID=(SELECT MAX(ID) from InstantCostElasticityPathway where monSeqID=?);";
        RowMapper<ServiceElasticityPathway> rowMapper = new RowMapper<ServiceElasticityPathway>() {
            public ServiceElasticityPathway mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToPathway(rs);
            }
        };

        //get last space
        List<ServiceElasticityPathway> space = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, monitoringSequenceID);
        if (space.isEmpty()) {
            return null;
        } else {
            return space.get(0);
        }
    }

//    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID, final int startTimestampID, final int endTimestampID) {
//
//        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace(monitoringSequenceID);
//
//        //update space with new data
//        ConfigurationXMLRepresentation cfg = this.getLatestConfiguration(monitoringSequenceID);
//
//        if (cfg == null) {
//            log.error("Retrieved empty configuration.");
//            return null;
//        } else if (cfg.getRequirements() == null) {
//            log.error("Retrieved configuration does not contain Requirements.");
//            return null;
//        } else if (cfg.getServiceConfiguration() == null) {
//            log.error("Retrieved configuration does not contain Service Configuration.");
//            return null;
//        }
//        Requirements requirements = cfg.getRequirements();
//        MonitoredElement serviceConfiguration = cfg.getServiceConfiguration();
//
//        //if space == null, compute it 
//        if (space == null) {
//
//            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
//            fct.setRequirements(requirements);
//            List<ServiceMonitoringSnapshot> dataFromTimestamp = new ArrayList<ServiceMonitoringSnapshot>();
//
//            int currentStart = startTimestampID;
//            do {
//                dataFromTimestamp = this.extractMonitoringDataByTimeInterval(currentStart, endTimestampID, monitoringSequenceID);
//                fct.trainElasticitySpace(dataFromTimestamp);
//                currentStart += 1000; //advance start timestamp with 1000, as we read max 1000 records at once
//            } while (dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID() < endTimestampID);
//
//            space = fct.getElasticitySpace();
//
//            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
//            space.setStartTimestampID(startTimestampID);
//            space.setEndTimestampID(endTimestampID);
//
//            //check how much data we actually extracted, as the extraction limits data to 1000
//            //persist cached space
//            this.writeElasticitySpace(space, monitoringSequenceID);
//
//        }
//        return space;
//
//    }
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

    public List<CostEnrichedSnapshot> extractInstantUsageSnapshotByTimeInterval(int startTimestampID, int endTimestampID, String serviceID) {

        String sql = "SELECT InstantCostHistory.timestampID, InstantCostHistory.data from InstantCostHistory INNER JOIN Timestamp "
                + "ON InstantCostHistory.timestampID= Timestamp.ID  where " + " Timestamp.timestamp >= ? "
                + "AND Timestamp.timestamp <=  ? AND InstantCostHistory.monSeqID=?;";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        List<CostEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, startTimestampID, endTimestampID, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots;
        }
    }

    public List<CostEnrichedSnapshot> extractTotalCostSnapshotByTimeInterval(int startTimestampID, int endTimestampID, String serviceID) {

        String sql = "SELECT TotalCostHistory.timestampID, TotalCostHistory.data from TotalCostHistory INNER JOIN Timestamp "
                + "ON TotalCostHistory.timestampID= Timestamp.ID  where " + " Timestamp.timestamp >= ? "
                + "AND Timestamp.timestamp <=  ? AND TotalCostHistory.monSeqID=?;";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        List<CostEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, startTimestampID, endTimestampID, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots;
        }
    }

    public List<CostEnrichedSnapshot> extractInstantUsageSnapshotByTimeIDInterval(int startTimestampID, int endTimestampID, String serviceID) {

        String sql = "SELECT InstantCostHistory.timestampID, InstantCostHistory.data from InstantCostHistory INNER JOIN Timestamp "
                + "ON InstantCostHistory.timestampID= Timestamp.ID  where " + " Timestamp.ID >= ? "
                + "AND Timestamp.ID <=  ? AND InstantCostHistory.monSeqID=?;";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        List<CostEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, startTimestampID, endTimestampID, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots;
        }
    }

    public List<CostEnrichedSnapshot> extractTotalCostSnapshotByTimeIDInterval(int startTimestampID, int endTimestampID, String serviceID) {

        String sql = "SELECT TotalCostHistory.timestampID, TotalCostHistory.data from TotalCostHistory INNER JOIN Timestamp "
                + "ON TotalCostHistory.timestampID= Timestamp.ID  where " + " Timestamp.ID >= ? "
                + "AND Timestamp.ID <=  ? AND TotalCostHistory.monSeqID=?;";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        List<CostEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, startTimestampID, endTimestampID, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots;
        }
    }

    public List<CostEnrichedSnapshot> extractInstantUsageSnapshot(int timestamp, String monitoringSequenceID) {
        String sql = "SELECT InstantCostHistory.timestampID, InstantCostHistory.data from InstantCostHistory INNER JOIN Timestamp "
                + "ON InstantCostHistory.timestampID= Timestamp.ID where InstantCostHistory.monSeqID=? and InstantCostHistory.timestampID > ? LIMIT 1000;";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, timestamp);

    }

    public List<CostEnrichedSnapshot> extractInstantUsageSnapshot(String monitoringSequenceID) {

        String sql = "SELECT InstantCostHistory.timestampID, InstantCostHistory.data from InstantCostHistory INNER JOIN Timestamp "
                + "ON InstantCostHistory.timestampID= Timestamp.ID where InstantCostHistory.monSeqID=? LIMIT 1000;";
        RowMapper<CostEnrichedSnapshot> rowMapper = new RowMapper<CostEnrichedSnapshot>() {
            public CostEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToServiceUsageSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);
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

    public void persistTotalUsageWithCurrentStructureSnapshot(String serviceID, CostEnrichedSnapshot serviceUsageSnapshot) {

//        //delete old cached
//        {
//            String sql = "DELETE FROM CaschedHistoricalUsage WHERE monseqid=?";
//            jdbcTemplate.update(sql, serviceID);
//        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(out));
            objectOutputStream.writeObject(serviceUsageSnapshot);
            objectOutputStream.close();
        } catch (IOException ex) {
            log.info(ex.getMessage(), ex);
            return;
        }

        byte[] zipped = out.toByteArray();

        {
            String sql = "INSERT INTO CaschedHistoricalUsage (monseqid, timestampID, data) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, serviceID, serviceUsageSnapshot.getLastUpdatedTimestampID(), zipped);
        }

    }

    /**
     * the submitted snapshot should have no composite cost
     *
     * @param serviceID
     * @param serviceUsageSnapshot
     */
    public void persistTotalUsageWithCompleteHistoricalStructureSnapshot(String serviceID, LifetimeEnrichedSnapshot serviceUsageSnapshot) {

//        //delete old cached
//        {
//            String sql = "DELETE FROM CaschedHistoricalUsage WHERE monseqid=?";
//            jdbcTemplate.update(sql, serviceID);
//        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(out));
            objectOutputStream.writeObject(serviceUsageSnapshot);
            objectOutputStream.close();
        } catch (IOException ex) {
            log.info(ex.getMessage(), ex);
            return;
        }

        byte[] zipped = out.toByteArray();

        {
            String sql = "INSERT INTO CaschedCompleteHistoricalUsage (monseqid, timestampID, data) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, serviceID, serviceUsageSnapshot.getLastUpdatedTimestampID(), zipped);
        }

    }

    public LifetimeEnrichedSnapshot extractTotalUsageWithCompleteHistoricalStructureSnapshot(String serviceID) {
        String sql = "SELECT CaschedCompleteHistoricalUsage.timestampID, CaschedCompleteHistoricalUsage.data from CaschedCompleteHistoricalUsage where "
                + "ID=(SELECT MAX(ID) from CaschedCompleteHistoricalUsage where monSeqID=?);";
        RowMapper<LifetimeEnrichedSnapshot> rowMapper = new RowMapper<LifetimeEnrichedSnapshot>() {
            public LifetimeEnrichedSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToLifetimeEnrichedSnapshot(rs);
            }
        };

        List<LifetimeEnrichedSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, serviceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots.get(0);
        }
    }

    public CostEnrichedSnapshot extractLastInstantCostSnapshot(String serviceID) {
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
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(out));
            objectOutputStream.writeObject(serviceUsageSnapshot);
            objectOutputStream.close();
        } catch (IOException ex) {
            log.info(ex.getMessage(), ex);
            return;
        }

        byte[] zipped = out.toByteArray();

        {
            String sql = "INSERT INTO InstantCostHistory (monseqid, timestampID, data) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, serviceID, serviceUsageSnapshot.getLastUpdatedTimestampID(), zipped);
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        try {
            objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(out));
            objectOutputStream.writeObject(serviceUsageSnapshot);
            objectOutputStream.close();
        } catch (IOException ex) {
            log.info(ex.getMessage(), ex);
            return;
        }

        byte[] zipped = out.toByteArray();

//        //delete old cached
//        {
//            String sql = "DELETE FROM CaschedHistoricalUsage WHERE monseqid=?";
//            jdbcTemplate.update(sql, serviceID);
//        }
        {
            String sql = "INSERT INTO TotalCostHistory (monseqid, timestampID, data) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, serviceID, serviceUsageSnapshot.getLastUpdatedTimestampID(), zipped);
        }

    }

    private CostEnrichedSnapshot mapToServiceUsageSnapshot(ResultSet rs) throws SQLException {
        int sTimestamp = rs.getInt(1);
        Object data = rs.getObject(2);

        //if array of bytes as mysql returns
        if (data instanceof byte[]) {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream((byte[]) data)));
                CostEnrichedSnapshot snapshot = (CostEnrichedSnapshot) inputStream.readObject();
                snapshot.setLastUpdatedTimestampID(sTimestamp);
                return snapshot;
            } catch (IOException | ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new CostEnrichedSnapshot();
            }
        } else {
            log.info("Can't deserialize returned space of class {}", data.getClass());
            //can convert and return with H2 and HyperSQL adapters
            CostEnrichedSnapshot snapshot = (CostEnrichedSnapshot) rs.getObject(2);
            snapshot.setLastUpdatedTimestampID(sTimestamp);
            return snapshot;
        }
    }

    private LifetimeEnrichedSnapshot mapToLifetimeEnrichedSnapshot(ResultSet rs) throws SQLException {
        int sTimestamp = rs.getInt(1);
        Object data = rs.getObject(2);

        //if array of bytes as mysql returns
        if (data instanceof byte[]) {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream((byte[]) data)));
                LifetimeEnrichedSnapshot snapshot = (LifetimeEnrichedSnapshot) inputStream.readObject();
                snapshot.setLastUpdatedTimestampID(sTimestamp);
                return snapshot;
            } catch (ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new LifetimeEnrichedSnapshot();
            } catch (IOException ex) {
                log.info(ex.getMessage(), ex);
                return new LifetimeEnrichedSnapshot();
            }
        } else {
            log.info("Can't deserialize returned space of class {}", data.getClass());
            //can convert and return with H2 and HyperSQL adapters
            LifetimeEnrichedSnapshot snapshot = (LifetimeEnrichedSnapshot) rs.getObject(2);
            snapshot.setLastUpdatedTimestampID(sTimestamp);
            return snapshot;
        }
    }

    private ElasticitySpace mapToSpace(ResultSet rs) throws SQLException {

        int startTimestamp = rs.getInt(1);
        int endTimestamp = rs.getInt(2);
        Object data = rs.getObject(3);

        //if array of bytes as mysql returns
        if (data instanceof byte[]) {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream((byte[]) data)));
                ElasticitySpace space = (ElasticitySpace) inputStream.readObject();
                space.setStartTimestampID(startTimestamp);
                space.setEndTimestampID(endTimestamp);
                return space;
            } catch (IOException | ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new ElasticitySpace();
            }
        } else {
//            //can convert and space with H2 and HyperSQL adapters
//            ElasticitySpace space = (ElasticitySpace) rs.getObject(3);
//            space.setStartTimestampID(startTimestamp);
//            space.setEndTimestampID(endTimestamp);
//            return space;
            log.info("Can't deserialize returned space of class {}", data.getClass());
            return new ElasticitySpace();
        }
    }

    private ServiceElasticityPathway mapToPathway(ResultSet rs) throws SQLException {

        int timestamp = rs.getInt(1);
        Object data = rs.getObject(2);

        //if array of bytes as mysql returns
        if (data instanceof byte[]) {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream((byte[]) data)));
                ServiceElasticityPathway space = (ServiceElasticityPathway) inputStream.readObject();
                space.setTimestampID(timestamp);
                return space;
            } catch (IOException | ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new ServiceElasticityPathway();
            }
        } else {
            log.info("Can't deserialize returned space of class {}", data.getClass());
            //can convert and space with H2 and HyperSQL adapters
            ServiceElasticityPathway space = (ServiceElasticityPathway) rs.getObject(3);
            space.setTimestampID(timestamp);
            return space;
        }
    }

    public void removeService(String serviceID) {
        String checkIfExistsSql = "select count(1) from MonitoringSeq where ID=?";

        RowMapper<Long> rowMapper = new RowMapper<Long>() {
            public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getLong(1);
            }

        };

        if (jdbcTemplate.queryForObject(checkIfExistsSql, rowMapper, serviceID) == 1) {
            {
                log.debug("Removing InstantCostElasticityPathway for " + serviceID);
                String sql = "delete from InstantCostElasticitySpace where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
            {
                log.debug("Removing InstantCostElasticitySpace for " + serviceID);
                String sql = "delete from InstantCostElasticitySpace where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
            {
                log.debug("Removing TotalCostHistory for " + serviceID);
                String sql = "delete from TotalCostHistory where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing InstantCostHistory for " + serviceID);
                String sql = "delete from InstantCostHistory where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
            {
                log.debug("Removing CaschedHistoricalUsage for " + serviceID);
                String sql = "delete from CaschedHistoricalUsage where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
            {
                log.debug("Removing CaschedCompleteHistoricalUsage for " + serviceID);
                String sql = "delete from CaschedCompleteHistoricalUsage where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            persistenceSQLAccess.removeMonitoringSequenceId(serviceID);
        } else {
            log.debug("sequenceId " + serviceID + " not found from in MontoringSeq");
        }

    }

    public List<ServiceMonitoringSnapshot> extractStructuredMonitoringData(int timestamp, String monitoringSequenceID) {
        return persistenceSQLAccess.extractStructuredMonitoringData(timestamp, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractStructuredMonitoringDataFromTimestamp(long timestamp, String monitoringSequenceID) {

        return persistenceSQLAccess.extractStructuredMonitoringDataFromTimestamp(timestamp, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractStructuredMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractStructuredMonitoringData(monitoringSequenceID);

    }

    public void writeMonitoringSequenceId(String sequenceId) {
        persistenceSQLAccess.writeMonitoringSequenceId(sequenceId);
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

    public void writeInTimestamp(String timestamp, MonitoredElement serviceStructure, String monitoringSequenceID) {
        persistenceSQLAccess.writeInTimestamp(timestamp, serviceStructure, monitoringSequenceID);
    }

}
