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
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
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

                for (MetricInfo metricInfo : elementData.getMetrics()) {
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

    public void writeElasticitySpace(ElasticitySpace elasticitySpace, String monitoringSequenceID) {

        //delete previous entry
        String sql = "DELETE FROM ElasticitySpace WHERE monseqid=?";
        jdbcTemplate.update(sql, elasticitySpace.getService().getId());

        //add new entry
        sql = "INSERT INTO ElasticitySpace (monSeqID, timestampID, elasticitySpace) "
                + "VALUES "
                + "( (SELECT ID FROM MonitoringSeq WHERE id='"
                + monitoringSequenceID + "')" + ", ? " + ", ? )";

        jdbcTemplate.update(sql, elasticitySpace.getTimestampID(), elasticitySpace);

    }

    public void writeElasticityPathway(String timestamp, LightweightEncounterRateElasticityPathway elasticityPathway, String monitoringSequenceID) {
        String sql = "insert into ElasticitySpace (monSeqID, timestampID, elasticitySpace) " + "VALUES " + "( (select ID from MonitoringSeq where id='"
                + monitoringSequenceID + "')" + ", ? " + ", ? )";

        jdbcTemplate.update(sql, monitoringSequenceID);

        sql = "insert into ElasticityPathway (monSeqID, timestampID, elasticityPathway) " + "VALUES "
                + "( (select ID from MonitoringSeq where id='" + monitoringSequenceID + "')"
                + ", (select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='" + monitoringSequenceID + "')"
                + " AND timestamp= ? )" + ", ?)";

        jdbcTemplate.update(sql, timestamp, elasticityPathway);
    }

    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID) {
        String sql = "SELECT timestampID, elasticitySpace from ElasticitySpace where monSeqID=?;";
        RowMapper<ElasticitySpace> rowMapper = new RowMapper<ElasticitySpace>() {
            public ElasticitySpace mapRow(ResultSet rs, int rowNum) throws SQLException {
                int timestamp = rs.getInt(1);
                ElasticitySpace space = (ElasticitySpace) rs.getObject(2);
                space.setTimestampID(timestamp);
                return space;

            }
        };

        //get last space
        List<ElasticitySpace> space = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);
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
                    MetricInfo info = new MetricInfo();
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

                    MetricInfo info = new MetricInfo();
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
                return (LightweightEncounterRateElasticityPathway) rs.getObject(1);
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
        String sql = "SELECT data from AggregatedData where " + "ID > (?) AND ID < (?) AND monSeqID=(?);";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return (ServiceMonitoringSnapshot) rs.getObject(1);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, startIndex, startIndex + count, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractLastXMonitoringDataSnapshots(int x, String monitoringSequenceID) {

        int minIimestampID = 0;
        int maxIimestampID = 0;

        {
            String getMinTimestampIDSQL = "SELECT MIN(id) from timestamp where monSeqID=?;";
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
            String getMaxTimestampIDSQL = "SELECT MAX(id) from timestamp where monSeqID=?;";
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

        String getLastXAggregatedDataSQL = "SELECT data from AggregatedData where " + "ID > (?) AND ID < (?) AND monSeqID=(?);";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return (ServiceMonitoringSnapshot) rs.getObject(1);
            }
        };

        return jdbcTemplate.query(getLastXAggregatedDataSQL, rowMapper, timestampIDToSelectFrom, maxIimestampID, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(String startTime, String endTime, String monitoringSequenceID) {

        String sql = "SELECT timestampID, data from AggregatedData where " + " timestampID >= (select ID from Timestamp where timestamp = ? ) "
                + "AND timestampID <= (select ID from Timestamp where timestamp = ? ) AND monSeqID=?;";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return (ServiceMonitoringSnapshot) rs.getObject(1);
            }
        };

        return jdbcTemplate.query(sql, rowMapper, startTime, endTime, monitoringSequenceID);

    }

    /**
     * @return returns maximum count elements
     */
    public ServiceMonitoringSnapshot extractLatestMonitoringData(String monitoringSequenceID) {
        String sql = "SELECT timestampID, data from AggregatedData where " + "ID = (SELECT MAX(ID) from AggregatedData where monSeqID=?);";


        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                int sTimestamp = rs.getInt(1);
                ServiceMonitoringSnapshot monitoringSnapshot = (ServiceMonitoringSnapshot) rs.getObject(2);
                monitoringSnapshot.setTimestampID(sTimestamp);
                return monitoringSnapshot;
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
        String sql = "SELECT timestampID, data from AggregatedData where monSeqID=? and timestampID > ? LIMIT 1000;";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                int sTimestamp = rs.getInt(1);
                ServiceMonitoringSnapshot snapshot = (ServiceMonitoringSnapshot) rs.getObject(2);
                snapshot.setTimestampID(sTimestamp);
                return snapshot;
            }
        };

        return jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, timestamp);
    }

    /**
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractMonitoringData(String monitoringSequenceID) {
        String sql = "SELECT timestampID, data from AggregatedData where monSeqID=? LIMIT 1000;";
        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                int sTimestamp = rs.getInt(1);
                ServiceMonitoringSnapshot snapshot = (ServiceMonitoringSnapshot) rs.getObject(2);
                snapshot.setTimestampID(sTimestamp);
                return snapshot;
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
}
