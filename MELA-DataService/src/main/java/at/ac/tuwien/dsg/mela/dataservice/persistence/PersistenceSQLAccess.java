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

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.MonitoredElementElasticityDependencies;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationUtility;
import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import java.io.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class PersistenceSQLAccess {

    private static final String AGGREGATED_DATA_TABLE_NAME = "AggregatedData";
    static final Logger log = LoggerFactory.getLogger(PersistenceSQLAccess.class);

    @Value("#{dataSource}")
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConfigurationUtility configurationUtility;

    public PersistenceSQLAccess() {
    }

    @PostConstruct
    public void init() {
        log.debug("Creating new JdbcTemplate with datasource {}", dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void writeMonitoringSequenceId(String sequenceId) {

        String checkIfExistsSql = "select count(1) from MonitoringSeq where ID=?";
        long result = jdbcTemplate.queryForObject(checkIfExistsSql, Long.class, sequenceId);
        if (result < 1) {
            log.debug("Inserting sequenceId into MontoringSeq");
            String sql = "insert into MonitoringSeq (ID) VALUES (?)";
            jdbcTemplate.update(sql, sequenceId);

        }
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
        String sql = "INSERT INTO " + AGGREGATED_DATA_TABLE_NAME + " (data, monSeqID, timestampID) "
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
        ElasticitySpace space = jdbcTemplate.queryForObject(sql, rowMapper, monitoringSequenceID);

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

        jdbcTemplate.queryForObject(sql, rowMapper, monitoringSequenceID, timestampID);

        monitoringData.addMonitoredElementDatas(retrievedData.values());

        return monitoringData;
    }

    public MonitoredElementElasticityDependencies extractLatestElasticityDependencies(String monitoringSequenceID) {

        String sql = "SELECT timestampID, elasticitySpace from ElasticitySpace where monSeqID=?;";

        RowMapper<MonitoredElementElasticityDependencies> rowMapper = new RowMapper<MonitoredElementElasticityDependencies>() {
            public MonitoredElementElasticityDependencies mapRow(ResultSet rs, int rowNum) throws SQLException {
                MonitoredElementElasticityDependencies dependencies = null;
                try {
                    Reader repr = rs.getClob(1).getCharacterStream();
                    JAXBContext context = JAXBContext.newInstance(MonitoredElementElasticityDependencies.class);
                    dependencies = (MonitoredElementElasticityDependencies) context.createUnmarshaller().unmarshal(repr);

                } catch (JAXBException ex) {
                    java.util.logging.Logger.getLogger(PersistenceSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
                return dependencies;
            }
        };

        List<MonitoredElementElasticityDependencies> dependencies = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID);
        if (dependencies.isEmpty()) {
            return null;
        } else {
            return dependencies.get(0);
        }

    }

    public LightweightEncounterRateElasticityPathway extractLatestElasticityPathway(String monitoringSequenceID) {
        String sql = "SELECT elasticityPathway from ElasticityPathway where monSeqID=?;";
        RowMapper<LightweightEncounterRateElasticityPathway> rowMapper = new RowMapper<LightweightEncounterRateElasticityPathway>() {
            public LightweightEncounterRateElasticityPathway mapRow(ResultSet rs, int rowNum) throws SQLException {
                return (LightweightEncounterRateElasticityPathway) rs.getObject(1);
            }
        };

        return jdbcTemplate.queryForObject(sql, rowMapper, monitoringSequenceID);

    }

    /**
     * @param startIndex from which monitored entry ID to start extracting
     * @param count max number of elements to return
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractMonitoringData(int startIndex, int count, String monitoringSequenceID) {
        String sql = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where " + "ID > (?) AND ID < (?) AND monSeqID=(?);";
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

            minIimestampID = jdbcTemplate.queryForObject(getMinTimestampIDSQL, getMinTimestampRowMapper, monitoringSequenceID);
        }

        {
            String getMaxTimestampIDSQL = "SELECT MAX(id) from timestamp where monSeqID=?;";
            RowMapper<Integer> getMaxTimestampRowMapper = new RowMapper<Integer>() {
                public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getInt(1);
                }
            };

            maxIimestampID = jdbcTemplate.queryForObject(getMaxTimestampIDSQL, getMaxTimestampRowMapper, monitoringSequenceID);
        }

        int timestampIDToSelectFrom = (maxIimestampID - x) >= 0 ? maxIimestampID - x : minIimestampID;

        String getLastXAggregatedDataSQL = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where " + "ID > (?) AND ID < (?) AND monSeqID=(?);";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                return (ServiceMonitoringSnapshot) rs.getObject(1);
            }
        };

        return jdbcTemplate.query(getLastXAggregatedDataSQL, rowMapper, timestampIDToSelectFrom, maxIimestampID, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(String startTime, String endTime, String monitoringSequenceID) {

        String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where " + " timestampID >= (select ID from Timestamp where timestamp = ? ) "
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
        String sql = "SELECT timestampID, data from "
                + AGGREGATED_DATA_TABLE_NAME
                + " where " + "ID = (SELECT MAX(ID) from "
                + AGGREGATED_DATA_TABLE_NAME
                + " where monSeqID=?);";

        RowMapper<ServiceMonitoringSnapshot> rowMapper = new RowMapper<ServiceMonitoringSnapshot>() {
            public ServiceMonitoringSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                int sTimestamp = rs.getInt(1);
                ServiceMonitoringSnapshot monitoringSnapshot = (ServiceMonitoringSnapshot) rs.getObject(2);
                monitoringSnapshot.setTimestampID(sTimestamp);
                return monitoringSnapshot;
            }
        };

        return jdbcTemplate.queryForObject(sql, rowMapper, monitoringSequenceID); // todo does this do the expected thing?

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(int timestamp, String monitoringSequenceID) {
        String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=? and timestampID > ? LIMIT 1000;";
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
        String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=? LIMIT 1000;";
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

    public ConfigurationXMLRepresentation getLatestConfiguration() {
        String sql = "SELECT configuration from Configuration where ID=(Select max(ID) from Configuration)";
        ConfigurationXMLRepresentation configurationXMLRepresentation = null;
        try {
            RowMapper<String> rowMapper = new RowMapper<String>() {
                public String mapRow(ResultSet rs, int i) throws SQLException {
                    return new DefaultLobHandler().getClobAsString(rs, "configuration");
                }
            };

            List<String> configs = jdbcTemplate.query(sql, rowMapper);

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

        if (configurationXMLRepresentation == null) {
            return configurationUtility.createDefaultConfiguration();
        } else {
            return configurationXMLRepresentation;
        }
    }

    /**
     * @param configurationXMLRepresentation the used MELA configuration to be
     * persisted in XML and reused
     */
    public void writeConfiguration(final ConfigurationXMLRepresentation configurationXMLRepresentation) {
        final StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
            context.createMarshaller().marshal(configurationXMLRepresentation, stringWriter);

        } catch (JAXBException e) {
            log.warn("Cannot marshal configuration into string: " + e);
            return;

        }

        String sql = "INSERT INTO Configuration (configuration) " + "VALUES (?)";
        jdbcTemplate.execute(sql, new AbstractLobCreatingPreparedStatementCallback(new DefaultLobHandler()) {
            protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException {
                lobCreator.setClobAsString(ps, 1, stringWriter.toString());
            }
        });
    }

    // todo recreate persistence context here, invoked if service configuration changes (actually re-instantiates the PersistenceSQLAccess object)
    public void refresh() {

    }

}
