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
package at.ac.tuwien.dsg.mela.common.persistence;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.CollectedMetricValue;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.Event;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class PersistenceSQLAccess {

    static final Logger log = LoggerFactory.getLogger(PersistenceSQLAccess.class);

    @Value("#{melaDBConnector}")
    private DataSource dataSource;

    protected JdbcTemplate jdbcTemplate;

    public PersistenceSQLAccess() {
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    
    
    @PostConstruct
    public void init() {
        log.debug("Creating new JdbcTemplate with datasource {}", dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void writeMonitoringSequenceId(String sequenceId) {

        String checkIfExistsSql = "select count(1) from MonitoringSeq where ID=?";

        RowMapper<Long> rowMapper = new RowMapper<Long>() {
            public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getLong(1);
            }
        };

        if (jdbcTemplate.queryForObject(checkIfExistsSql, rowMapper, sequenceId) < 1) {
            log.debug("Inserting sequenceId into MontoringSeq");
            String sql = "insert into MonitoringSeq (ID) VALUES (?)";
            jdbcTemplate.update(sql, sequenceId);

        }
    }

    public void removeMonitoringSequenceId(String serviceID) {
        String checkIfExistsSql = "select count(1) from MonitoringSeq where ID=?";

        RowMapper<Long> rowMapper = new RowMapper<Long>() {
            public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getLong(1);
            }
        };

        if (jdbcTemplate.queryForObject(checkIfExistsSql, rowMapper, serviceID) == 1) {
            {
                log.debug("  Removing Events for " + serviceID);
                String sql = "delete from Events where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
            {
                log.debug("Removing ElasticityDependency for " + serviceID);
                String sql = "delete from ElasticityDependency where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing ElasticityDependency for " + serviceID);
                String sql = "delete from ElasticityDependency where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
            {
                log.debug("Removing ElasticityPathway for " + serviceID);
                String sql = "delete from ElasticityPathway where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing ElasticitySpace for " + serviceID);
                String sql = "delete from ElasticitySpace where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing AggregatedData for " + serviceID);
                String sql = "delete from AggregatedData where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing Configuration for " + serviceID);
                String sql = "delete from Configuration where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing RawCollectedData for " + serviceID);
                String sql = "delete from RawCollectedData where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
            {
                log.debug("Removing StructuredCollectedData for " + serviceID);
                String sql = "delete from StructuredCollectedData where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing Timestamp for " + serviceID);
                String sql = "delete from Timestamp where monSeqID= ?";
                jdbcTemplate.update(sql, serviceID);
            }

            {
                log.debug("Removing sequenceId from MonitoringSeq");
                String sql = "delete from MonitoringSeq where ID= ?";
                jdbcTemplate.update(sql, serviceID);
            }
        } else {
            log.debug("sequenceId " + serviceID + " not found from in MontoringSeq");
        }

    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * @param monitoringData MonitoringData objects collected from different
     * data sources
     */
    public void writeRawMonitoringData(String timestamp, Collection<MonitoringData> monitoringData, String monitoringSequenceID) {

        String sql = "insert into RawCollectedData (monSeqID, timestampID, metricName, metricUnit, metrictype, value, monitoredElementID, monitoredElementLevel) "
                + "VALUES "
                + "( (select ID from MonitoringSeq where id='"
                + monitoringSequenceID
                + "')"
                + ", ( select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='"
                + monitoringSequenceID
                + "')"
                + " AND timestamp=? )" + ",?,?,?,?,?,?)";

        for (MonitoringData data : monitoringData) {
            // for all monitored metrics insert in the metric values
            for (MonitoredElementData elementData : data.getMonitoredElementDatas()) {
                MonitoredElement element = elementData.getMonitoredElement();

                for (CollectedMetricValue metricInfo : elementData.getMetrics()) {
                    jdbcTemplate.update(sql, timestamp, metricInfo.getName(),
                            metricInfo.getUnits(), metricInfo.getType(), metricInfo.getValue(),
                            element.getId(), element.getLevel().toString());
                }
            }
        }

    }

    public void writeInTimestamp(String timestamp, MonitoredElement serviceStructure, String monitoringSequenceID) {
        String sql = "insert into Timestamp (monSeqID, timestamp, serviceStructure) VALUES ( (SELECT ID from MonitoringSeq where id='" + monitoringSequenceID + "'), ?,?)";
        final StringWriter stringWriter = new StringWriter();
        JAXBContext jAXBContext;
        try {
            jAXBContext = JAXBContext.newInstance(MonitoredElement.class);
            jAXBContext.createMarshaller().marshal(serviceStructure, stringWriter);
        } catch (JAXBException ex) {
            log.error(ex.getMessage(), ex);

        }

        jdbcTemplate.update(sql, timestamp, stringWriter.toString());
    }

    public void writeMonitoringData(String timestamp, ServiceMonitoringSnapshot monitoringSnapshot, String monitoringSequenceID) {
        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence
        String sql = "INSERT INTO AggregatedData (data, monSeqID, timestampID) "
                + "VALUES (?, ?, (SELECT ID from Timestamp where timestamp=? AND monSeqID=?))";

        jdbcTemplate.update(sql, monitoringSnapshot, monitoringSequenceID, timestamp, monitoringSequenceID);
    }
    
    /**
     * Aimed for writing structrued mon data, but NOT aggregated. Usefull in replaying mon data.
     * @param timestamp
     * @param monitoringSnapshot
     * @param monitoringSequenceID 
     */
    public void writeStructuredMonitoringData(String timestamp, ServiceMonitoringSnapshot monitoringSnapshot, String monitoringSequenceID) {
        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence
        String sql = "INSERT INTO StructuredCollectedData (data, monSeqID, timestampID) "
                + "VALUES (?, ?, (SELECT ID from Timestamp where timestamp=? AND monSeqID=?))";

        jdbcTemplate.update(sql, monitoringSnapshot, monitoringSequenceID, timestamp, monitoringSequenceID);
    }
  

    public void writeElasticitySpace(ElasticitySpace elasticitySpace, String monitoringSequenceID) {

        //delete previous entry
//        String sql = "DELETE FROM ElasticitySpace WHERE monseqid=? and startTimestampID=? and endTimestampID=?";
//       jdbcTemplate.update(sql, elasticitySpace.getService().getId(), elasticitySpace.getStartTimestampID(), elasticitySpace.getEndTimestampID());
        //delete all previous spaces.
        String sql = "DELETE FROM ElasticitySpace WHERE monseqid=?";
        jdbcTemplate.update(sql, elasticitySpace.getService().getId());

        //add new entry
        sql = "INSERT INTO ElasticitySpace (monSeqID, startTimestampID, endTimestampID, elasticitySpace) "
                + "VALUES "
                + "( (SELECT ID FROM MonitoringSeq WHERE id='"
                + monitoringSequenceID + "')" + ", ? , ? " + ", ? )";

        jdbcTemplate.update(sql, elasticitySpace.getStartTimestampID(), elasticitySpace.getEndTimestampID(), elasticitySpace);

    }

//    public void writeElasticityPathway(String timestamp, LightweightEncounterRateElasticityPathway elasticityPathway, String monitoringSequenceID) {
//        String sql = "insert into ElasticitySpace (monSeqID, timestampID, elasticitySpace) " + "VALUES " + "( (select ID from MonitoringSeq where id='"
//                + monitoringSequenceID + "')" + ", ? " + ", ? )";
//
//        jdbcTemplate.update(sql, monitoringSequenceID);
//
//        sql = "insert into ElasticityPathway (monSeqID, timestampID, elasticityPathway) " + "VALUES "
//                + "( (select ID from MonitoringSeq where id='" + monitoringSequenceID + "')"
//                + ", (select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='" + monitoringSequenceID + "')"
//                + " AND timestamp= ? )" + ", ?)";
//
//        jdbcTemplate.update(sql, timestamp, elasticityPathway);
//    }
    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID) {
        String sql = "SELECT startTimestampID, endTimestampID, elasticitySpace from ElasticitySpace where monSeqID=? and ID=(SELECT MAX(ID) from ElasticitySpace where monSeqID=?);";
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

    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID, int startTimestampID, int endTimestampID) {
        String sql = "SELECT startTimestampID, endTimestampID, elasticitySpace from ElasticitySpace where monSeqID=? and "
                + "ID=(SELECT MAX(ID) from ElasticitySpace where monSeqID=? and startTimestampID=? and endTimestampID=?);";
        RowMapper<ElasticitySpace> rowMapper = new RowMapper<ElasticitySpace>() {
            public ElasticitySpace mapRow(ResultSet rs, int rowNum) throws SQLException {

                return mapToSpace(rs);

            }
        };

        //get last space
        List<ElasticitySpace> space = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, monitoringSequenceID, startTimestampID, endTimestampID);
        if (space.isEmpty()) {
            return null;
        } else {
            return space.get(0);
        }
    }

    /**
     *
     * @param monitoringSequenceID
     * @return IDs for all timestamps recorded for the supplied
     * monitoringSequenceID
     */
    public List<Integer> getTimestampIDs(String monitoringSequenceID) {
        String sql = "SELECT id from Timestamp where monSeqID=?;";
        RowMapper<Integer> rowMapper = new RowMapper<Integer>() {
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                Integer id = rs.getInt(1);

                return id;
            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);
    }

    public MonitoringData getRawMonitoringData(String monitoringSequenceID, String timestampID) {
        MonitoringData monitoringData = new MonitoringData();

        String sql = "SELECT timestampID, metricName, metricUnit,metricType, value, monitoredElementID from RAWCOLLECTEDDATA where monSeqID=? and timestampID = ?; ";

        final Map<String, MonitoredElementData> retrievedData = new HashMap<String, MonitoredElementData>();

        RowMapper<MonitoredElementData> rowMapper = new RowMapper<MonitoredElementData>() {

            public MonitoredElementData mapRow(ResultSet resultSet, int rowNum) throws SQLException {
                String metricName = resultSet.getString("metricName");
                String metricUnit = resultSet.getString("metricUnit");
                String metricType = resultSet.getString("metricType");
                String value = resultSet.getString("value");
                String monitoredElementID = resultSet.getString("monitoredElementID");

                if (retrievedData.containsKey(monitoredElementID)) {
                    MonitoredElementData monitoredElementData = retrievedData.get(monitoredElementID);
                    CollectedMetricValue info = new CollectedMetricValue();
                    info.setName(metricName);
                    info.setValue(value);
                    info.setUnits(metricUnit);
                    info.setType(metricType);
                    monitoredElementData.addMetric(info);

                    return monitoredElementData;
                } else {
                    MonitoredElement monitoredElement = new MonitoredElement(value);
                    monitoredElement.setLevel(MonitoredElement.MonitoredElementLevel.VM);

                    MonitoredElementData monitoredElementData = new MonitoredElementData();
                    monitoredElementData.setMonitoredElement(monitoredElement);

                    CollectedMetricValue info = new CollectedMetricValue();
                    info.setName(metricName);
                    info.setValue(value);
                    info.setUnits(metricUnit);
                    info.setType(metricType);
                    monitoredElementData.addMetric(info);

                    retrievedData.put(monitoredElementID, monitoredElementData);

                    return monitoredElementData;
                }

            }

        };

        jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, timestampID);

        monitoringData.addMonitoredElementDatas(retrievedData.values());

        return monitoringData;
    }

    public LightweightEncounterRateElasticityPathway extractLatestElasticityPathway(String monitoringSequenceID) {
        String sql = "SELECT elasticityPathway from ElasticityPathway where monSeqID=?;";
        RowMapper<LightweightEncounterRateElasticityPathway> rowMapper = new RowMapper<LightweightEncounterRateElasticityPathway>() {
            public LightweightEncounterRateElasticityPathway mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToPathway(rs);
            }
        };

        List<LightweightEncounterRateElasticityPathway> pathways = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);
        if (pathways.isEmpty()) {
            return null;
        } else {
            return pathways.get(0);
        }

    }

    /**
     * @param startIndex from which monitored entry ID to start extracting
     * @param count max number of elements to return
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractMonitoringData(int startIndex, int count, String monitoringSequenceID) {
        String sql = "SELECT AggregatedData.timestampID, Timestamp.timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID  where " + "AggregatedData.ID > (?) AND AggregatedData.ID < (?) AND AggregatedData.monSeqID=(?);";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, startIndex, startIndex + count, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractLastXMonitoringDataSnapshots(int x, String monitoringSequenceID) {

        int minIimestampID = 0;
        int maxIimestampID = 0;

        {
            String getMinTimestampIDSQL = "SELECT MIN(id) from Timestamp where monSeqID=?;";
            RowMapper<Integer> getMinTimestampRowMapper = new RowMapper<Integer>() {
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }
            };

            List<Integer> ints = jdbcTemplate.query(getMinTimestampIDSQL, getMinTimestampRowMapper, monitoringSequenceID);
            if (ints.isEmpty()) {
                minIimestampID = 0;
            } else {
                minIimestampID = ints.get(0);
            }

        }

        {
            String getMaxTimestampIDSQL = "SELECT MAX(id) from Timestamp where monSeqID=?;";
            RowMapper<Integer> getMaxTimestampRowMapper = new RowMapper<Integer>() {
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }
            };

            List<Integer> ints = jdbcTemplate.query(getMaxTimestampIDSQL, getMaxTimestampRowMapper, monitoringSequenceID);
            if (ints.isEmpty()) {
                maxIimestampID = 0;
            } else {
                maxIimestampID = ints.get(0);
            }
        }

        int timestampIDToSelectFrom = (maxIimestampID - x) >= 0 ? maxIimestampID - x : minIimestampID;

        String getLastXAggregatedDataSQL = "SELECT AggregatedData.timestampID, Timestamp.Timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID  where " + "AggregatedData.ID > (?) AND AggregatedData.ID < (?) AND AggregatedData.monSeqID=(?);";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(getLastXAggregatedDataSQL, rowMapper, timestampIDToSelectFrom, maxIimestampID, monitoringSequenceID);

    }
    
  public List<ServiceMonitoringSnapshot> extractMonitoringDataFromTimestamp(long timestamp, String monitoringSequenceID) {

        String sql = "SELECT AggregatedData.timestampID, Timestamp.timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID  where " + " Timestamp.timestamp >= ? "
                + "AND AggregatedData.monSeqID=?;";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, timestamp+"",  monitoringSequenceID);

    }
  public List<ServiceMonitoringSnapshot> extractStructuredMonitoringDataFromTimestamp(long timestamp, String monitoringSequenceID) {

        String sql = "SELECT StructuredCollectedData.timestampID, Timestamp.timestamp, StructuredCollectedData.data from StructuredCollectedData INNER JOIN Timestamp "
                + "ON StructuredCollectedData.timestampID= Timestamp.ID  where " + " Timestamp.timestamp >= ? "
                + "AND StructuredCollectedData.monSeqID=?;";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, timestamp+"",  monitoringSequenceID);

    }
  
    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(long timestamp, long endTimestampID, String monitoringSequenceID) {

        String sql = "SELECT AggregatedData.timestampID, Timestamp.timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID  where " + " Timestamp.timestamp >= ? "
                + "AND Timestamp.timestamp <=  ? AND AggregatedData.monSeqID=?;";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, ""+timestamp, ""+endTimestampID, monitoringSequenceID);

    }
    
     public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimestampIDsInterval(int starTimestampID, int endTimestampID, String monitoringSequenceID) {

        String sql = "SELECT AggregatedData.timestampID, Timestamp.timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID  where " + " Timestamp.ID >= ? "
                + "AND Timestamp.ID <=  ? AND AggregatedData.monSeqID=?;";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, ""+starTimestampID, ""+endTimestampID, monitoringSequenceID);

    }

    /**
     * @return returns maximum count elements
     */
    public ServiceMonitoringSnapshot extractLatestMonitoringData(String monitoringSequenceID) {
        String sql = "SELECT AggregatedData.timestampID, Timestamp.timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID  where " + "AggregatedData.timestampID = (SELECT MAX(timestampID) from AggregatedData where AggregatedData.monSeqID=?);";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        List<ServiceMonitoringSnapshot> snapshots = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);
        if (snapshots.isEmpty()) {
            return null;
        } else {
            return snapshots.get(0);
        }

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(int timestamp, String monitoringSequenceID) {
        String sql = "SELECT AggregatedData.timestampID, Timestamp.timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID where AggregatedData.monSeqID=? and AggregatedData.timestampID > ? LIMIT 1000;";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, timestamp);
    }
    
    public List<ServiceMonitoringSnapshot> extractStructuredMonitoringData(int timestamp, String monitoringSequenceID) {
        String sql = "SELECT StructuredCollectedData.timestampID, Timestamp.timestamp, StructuredCollectedData.data from StructuredCollectedData INNER JOIN Timestamp "
                + "ON StructuredCollectedData.timestampID= Timestamp.ID where StructuredCollectedData.monSeqID=? and StructuredCollectedData.timestampID > ? LIMIT 1000;";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, timestamp);
    }

    /**
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractMonitoringData(String monitoringSequenceID) {
        String sql = "SELECT AggregatedData.timestampID, Timestamp.timestamp, AggregatedData.data from AggregatedData INNER JOIN Timestamp "
                + "ON AggregatedData.timestampID= Timestamp.ID where AggregatedData.monSeqID=? LIMIT 1000;";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);

            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);

    }

    /**
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractStructuredMonitoringData(String monitoringSequenceID) {
        String sql = "SELECT StructuredCollectedData.timestampID, Timestamp.timestamp, StructuredCollectedData.data from StructuredCollectedData INNER JOIN Timestamp "
                + "ON StructuredCollectedData.timestampID= Timestamp.ID where StructuredCollectedData.monSeqID=? LIMIT 1000;";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return mapToSnapshot(rs);

            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);

    }

    public List<Metric> getAvailableMetrics(MonitoredElement monitoredElement, String monitoringSequenceID) {
        String sql = "SELECT metricName, metricUnit, metrictype  from RawCollectedData where "
                + "timestampID = (SELECT MAX(ID) from Timestamp where monSeqID=?)"
                + " AND monitoredElementID=? AND monitoredElementLevel=?;";

        RowMapper<Metric> rowMapper = new RowMapper<Metric>() {
            public Metric mapRow(ResultSet rs, int rowNum) throws SQLException {
                String metricName = rs.getString("metricName");
                String metricUnit = rs.getString("metricUnit");
                return new Metric(metricName, metricUnit);
            }
        };

        return jdbcTemplate.query(sql, rowMapper,
                monitoringSequenceID,
                monitoredElement.getId(),
                monitoredElement.getLevel().toString());

    }

    public ConfigurationXMLRepresentation getLatestConfiguration(String serviceID) {
        String sql = "SELECT configuration from Configuration where ID=(Select max(ID) from Configuration where monSeqID=?)";
        ConfigurationXMLRepresentation configurationXMLRepresentation = null;
        try {
            RowMapper<String> rowMapper = new RowMapper<String>() {
                public String mapRow(ResultSet rs, int i) throws SQLException {
                    return new DefaultLobHandler().getClobAsString(rs, "configuration");
                }
            };

            List<String> configs = jdbcTemplate.query(sql, rowMapper, serviceID);

            for (String config : configs) {
                JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
                configurationXMLRepresentation = (ConfigurationXMLRepresentation) context.createUnmarshaller()
                        .unmarshal(new StringReader(config));
            }

        } catch (BadSqlGrammarException e) {
            log.error("Cannot load configuration from database: " + e.getMessage());
        } catch (JAXBException e) {
            log.error("Cannot unmarshall configuration in XML object: " + e.getMessage());
        }

        return configurationXMLRepresentation;

    }

    /**
     * @param serviceID ID of the service for which the configuration will be
     * inserted
     * @param configurationXMLRepresentation the used MELA configuration to be
     * persisted in XML and reused
     */
    public void writeConfiguration(final String serviceID, final ConfigurationXMLRepresentation configurationXMLRepresentation) {
        final StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
            context.createMarshaller().marshal(configurationXMLRepresentation, stringWriter);

        } catch (JAXBException e) {
            log.warn("Cannot marshal configuration into string: " + e);
            return;

        }

        String sql = "INSERT INTO Configuration (monSeqID, configuration) " + "VALUES (?, ?)";

        jdbcTemplate.execute(sql, new AbstractLobCreatingPreparedStatementCallback(new DefaultLobHandler()) {
            protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException {
                ps.setString(1, serviceID);
                lobCreator.setClobAsString(ps, 2, stringWriter.toString());
            }
        });
    }

    // todo recreate persistence context here, invoked if service configuration changes (actually re-instantiates the PersistenceSQLAccess object)
    public void refresh() {

    }

    public List<String> getMonitoringSequencesIDs() {

        String sql = "SELECT ID from MonitoringSeq";
        RowMapper<String> rowMapper = new RowMapper<String>() {
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                String id = rs.getString(1);
                return id;

            }
        };

        //get last space
        List<String> strings = jdbcTemplate.query(sql, rowMapper);
        if (strings.isEmpty()) {
            return new ArrayList<String>();
        } else {
            return strings;
        }
    }

    public List<Event> getEvents(final String serviceID) {

        String sql = "SELECT id, event from Events where monSeqID=?";
        RowMapper<Event> rowMapper = new RowMapper<Event>() {
            public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
                Event event = new Event().withId(rs.getString(1)).withEvent(rs.getString(2)).withServiceID(serviceID);
                return event;
            }
        };

        //get last events
        List<Event> strings = jdbcTemplate.query(sql, rowMapper, serviceID);
        if (strings.isEmpty()) {
            return new ArrayList<Event>();
        } else {
            return strings;
        }
    }

    public List<Event> getUnreadEvents(final String serviceID) {

        String sql = "SELECT id, event from Events where monSeqID=? and flag='false'";
        RowMapper<Event> rowMapper = new RowMapper<Event>() {
            public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
                Event event = new Event().withId(rs.getString(1)).withEvent(rs.getString(2)).withServiceID(serviceID);
                return event;
            }
        };

        //get last events
        List<Event> strings = jdbcTemplate.query(sql, rowMapper, serviceID);
        if (strings.isEmpty()) {
            return new ArrayList<Event>();
        } else {
            return strings;
        }
    }

    public List<Event> getEvents(final String serviceID, String eventID) {

        String sql = "SELECT id, event from Events where monSeqID=? and ID>=?";
        RowMapper<Event> rowMapper = new RowMapper<Event>() {
            public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
                Event event = new Event().withId(rs.getString(1)).withEvent(rs.getString(2)).withServiceID(serviceID);
                return event;
            }
        };

        //get last events
        List<Event> strings = jdbcTemplate.query(sql, rowMapper, serviceID);
        if (strings.isEmpty()) {
            return new ArrayList<Event>();
        } else {
            return strings;
        }
    }

    public void writeEvents(String serviceID, List<Event> events) {
        //add new entry
        String sql = "INSERT INTO Events (monSeqID, event, flag) "
                + "VALUES";
        for (int i = 0; i < events.size(); i++) {
            String event = events.get(i).getEvent();
            sql += " ('" + serviceID + "','" + event + "', 'false')";
            if (i < events.size() - 1) {
                sql += ",";
            }
        }

        log.info("Executing " + sql);
        jdbcTemplate.update(sql);
    }

    public void markEventsAsRead(String serviceID, List<Event> events) {
        //add new entry
//        UPDATE table_name
//SET column1=value1,column2=value2,...
//WHERE some_column
        if (events.size() > 0) {
            String sql = "UPDATE Events SET flag='true' where monSeqID=? and ID IN (";

            for (int i = 0; i < events.size(); i++) {

                sql += "" + events.get(i).getId() + "";
                if (i < events.size() - 1) {
                    sql += ",";
                }
            }

            sql += ")";

            jdbcTemplate.update(sql, serviceID);
        }
    }

    public PersistenceSQLAccess withDataSource(final DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public PersistenceSQLAccess withJdbcTemplate(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        return this;
    }

    private ServiceMonitoringSnapshot mapToSnapshot(ResultSet rs) throws SQLException {
        int sTimestamp = rs.getInt(1);
        String timestamp = rs.getString(2);
        Object data = rs.getObject(3);

        //if array of bytes as mysql returns
        if (data instanceof byte[]) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) data);
                ObjectInput in = new ObjectInputStream(bis);
                ServiceMonitoringSnapshot snapshot = (ServiceMonitoringSnapshot) in.readObject();
                snapshot.setTimestampID(sTimestamp);
                snapshot.setTimestamp(timestamp);
                return snapshot;
            } catch (ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new ServiceMonitoringSnapshot();
            } catch (IOException ex) {
                log.info(ex.getMessage(), ex);
                return new ServiceMonitoringSnapshot();
            }
        } else {
            //can convert and return with H2 and HyperSQL adapters
            ServiceMonitoringSnapshot snapshot = (ServiceMonitoringSnapshot) rs.getObject(3);
            snapshot.setTimestampID(sTimestamp);
            snapshot.setTimestamp(timestamp);
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
                ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) data);
                ObjectInput in = new ObjectInputStream(bis);
                ElasticitySpace space = (ElasticitySpace) in.readObject();
                space.setStartTimestampID(startTimestamp);
                space.setEndTimestampID(endTimestamp);
                return space;
            } catch (ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new ElasticitySpace();
            } catch (IOException ex) {
                log.info(ex.getMessage(), ex);
                return new ElasticitySpace();
            }
        } else {
            //can convert and space with H2 and HyperSQL adapters
            ElasticitySpace space = (ElasticitySpace) rs.getObject(3);
            space.setStartTimestampID(startTimestamp);
            space.setEndTimestampID(endTimestamp);
            return space;
        }
    }

    private LightweightEncounterRateElasticityPathway mapToPathway(ResultSet rs) throws SQLException {
        Object data = rs.getObject(1);

        //if array of bytes as mysql returns
        if (data instanceof byte[]) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) data);
                ObjectInput in = new ObjectInputStream(bis);
                LightweightEncounterRateElasticityPathway snapshot = (LightweightEncounterRateElasticityPathway) in.readObject();
                return snapshot;
            } catch (ClassNotFoundException ex) {
                log.info(ex.getMessage(), ex);
                return new LightweightEncounterRateElasticityPathway(1);
            } catch (IOException ex) {
                log.info(ex.getMessage(), ex);
                return new LightweightEncounterRateElasticityPathway(1);
            }
        } else {
            //can convert and return with H2 and HyperSQL adapters
            LightweightEncounterRateElasticityPathway snapshot = (LightweightEncounterRateElasticityPathway) rs.getObject(3);

            return snapshot;
        }
    }

    

}
