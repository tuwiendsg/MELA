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

import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import at.ac.tuwien.dsg.mela.common.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import org.hsqldb.types.Type;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class PersistenceSQLAccess {

    private static final String AGGREGATED_DATA_TABLE_NAME = "AggregatedData";
    private String username;
    private String password;
    private Connection connection;
    private String monitoringSequenceID;
    private PreparedStatement insertConfigurationPreparedStatement;
    private PreparedStatement getMonitoringEntryPreparedStatement;
    private PreparedStatement getEntriesCountPreparedStatement;
    private PreparedStatement getMetricsForElement;
    private PreparedStatement getAllAggregatedDataStatement;
    private PreparedStatement insertIntoTimestamp;
    private PreparedStatement getLastAggregatedDataStatement;
    private PreparedStatement getLastElasticitySpaceStatement;
    private PreparedStatement getLastElasticityPathwayStatement;
    // used to insert in SQL data collected directly from data sources, WITHOUT
    // any structuring applied over it
    private PreparedStatement insertRawMonitoringData;
    // used to insert in SQL data WITH structuring and aggregation applied over
    // it
    private PreparedStatement insertAggregatedDataPreparedStatement;
    private PreparedStatement insertElasticitySpacePreparedStatement;
    private PreparedStatement insertElasticityPathwayPreparedStatement;

    public PersistenceSQLAccess(String username, String password, String monitoringSequenceID) {

        this.monitoringSequenceID = monitoringSequenceID;
        this.username = username;
        this.password = password;

        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        // instantiate connection first
        {

            // if the SQL connection fails, try to reconnect, as the
            // MELA_DataService might not be running.
            // BUSY wait used
            while (connection == null) {
                try {
                    connection = DriverManager.getConnection(
                            "jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB",
                            username, password);
                } catch (SQLException ex) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
                    Logger.getLogger(this.getClass()).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
                }
            }
        }

        // prepare statements
        {
            try {
                String sql = "INSERT INTO " + AGGREGATED_DATA_TABLE_NAME + " (data, monSeqID, timestampID) "
                        + "VALUES (?, ?, (SELECT ID from Timestamp where timestamp=? AND monSeqID=?))";
                insertAggregatedDataPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "INSERT INTO Configuration (configuration) " + "VALUES (?)";
                insertConfigurationPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
                return;
            }
        }

        {
            try {
                String sql = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where " + "ID > (?) AND ID < (?) AND monSeqID=(?);";
                getMonitoringEntryPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT MAX(ID) from " + AGGREGATED_DATA_TABLE_NAME + " WHERE monSeqID=?;";
                getEntriesCountPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT metricName, metricUnit, metrictype  from RawCollectedData where "
                        + "timestampID = (SELECT MAX(ID) from Timestamp where monSeqID=?)" + " AND monitoredElementID=? AND monitoredElementLevel=?;";
                getMetricsForElement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where " + "ID = (SELECT MAX(ID) from " + AGGREGATED_DATA_TABLE_NAME
                        + " where monSeqID=?);";
                getLastAggregatedDataStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT elasticitySpace from ElasticitySpace where " + "ID = (SELECT MAX(ID) from ElasticitySpace where monSeqID=?);";
                getLastElasticitySpaceStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }
        {
            try {
                String sql = "SELECT elasticityPathway from ElasticityPathway where " + "ID = (SELECT MAX(ID) from ElasticityPathway where monSeqID=?);";
                getLastElasticityPathwayStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=?;";
                getAllAggregatedDataStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into Timestamp (monSeqID, timestamp) VALUES ( (SELECT ID from MonitoringSeq where id='" + monitoringSequenceID + "'), ?)";
                insertIntoTimestamp = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into RawCollectedData (monSeqID, timestampID, metricName, metricUnit, metrictype, value, monitoredElementID, monitoredElementLevel) "
                        + "VALUES "
                        + "( (select ID from MonitoringSeq where id='"
                        + monitoringSequenceID
                        + "')"
                        + ", ( select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='"
                        + monitoringSequenceID
                        + "')"
                        + " AND timestamp=? )" + ",?,?,?,?,?,?)";
                insertRawMonitoringData = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into ElasticitySpace (monSeqID, timestampID, elasticitySpace) " + "VALUES " + "( (select ID from MonitoringSeq where id='"
                        + monitoringSequenceID + "')" + ", ( select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='"
                        + monitoringSequenceID + "')" + " AND timestamp= ? )" + ", ? )";
                
                insertElasticitySpacePreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into ElasticityPathway (monSeqID, timestampID, elasticitySpaceID, elasticityPathway) " + "VALUES "
                        + "( (select ID from MonitoringSeq where id='" + monitoringSequenceID + "')"
                        + ", (select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='" + monitoringSequenceID + "')"
                        + " AND timestamp= ? )" + ",(select ID from ElasticitySpace where monseqid=(select ID from MonitoringSeq where ID='"
                        + monitoringSequenceID + "' AND timestampID=( select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='"
                        + monitoringSequenceID + "')" + " AND timestamp= ? )) )" + ", ?)";
                insertElasticityPathwayPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        // ServiceID is used in retrieving historical monitoring data (if
        // needed)
        try {
            Statement addSeqStmt = connection.createStatement();
            addSeqStmt.executeUpdate("insert into MonitoringSeq (ID) VALUES ('" + monitoringSequenceID + "')");
            addSeqStmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

    }

    /**
     *
     * @param monitoringData MonitoringData objects collected from different
     * data sources
     */
    public void writeRawMonitoringData(String timestamp, Collection<MonitoringData> monitoringData) {

        for (MonitoringData data : monitoringData) {
            // for all monitored metrics insert in the metric values
            for (MonitoredElementData elementData : data.getMonitoredElementDatas()) {
                MonitoredElement element = elementData.getMonitoredElement();

                for (MetricInfo metricInfo : elementData.getMetrics()) {
                    try {
                        insertRawMonitoringData.setString(1, timestamp);
                        insertRawMonitoringData.setString(2, metricInfo.getName());
                        insertRawMonitoringData.setString(3, metricInfo.getUnits());
                        insertRawMonitoringData.setString(4, metricInfo.getType());
                        insertRawMonitoringData.setString(5, metricInfo.getValue());
                        insertRawMonitoringData.setString(6, element.getId());
                        insertRawMonitoringData.setString(7, element.getLevel().toString());
                        insertRawMonitoringData.executeUpdate();
                    } catch (SQLException ex) {
                        Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
                    }
                }
            }
        }

    }

    /**
     *
     * @param monSeqID currently ignored. Left for future extention
     */
    public void writeInTimestamp(String timestamp, String monSeqID) {
        try {
            insertIntoTimestamp.setString(1, timestamp);
            insertIntoTimestamp.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
    }

    private Connection getConnection() {
        try {
            // check if connection is open. if not, it gives exception
            String catalog = connection.getCatalog();
        } catch (SQLException e) {
            connection = null;

            // if the SQL connection fails, try to reconnect, as the
            // MELA_DataService might not be running.
            // BUSY wait used
            while (connection == null) {
                try {
                    connection = DriverManager.getConnection(
                            "jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB",
                            username, password);
                } catch (SQLException ex) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
                    Logger.getLogger(this.getClass()).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
                    connection = null;
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
                }
            }

        }
        return connection;
    }

    public void writeMonitoringData(String timestamp, ServiceMonitoringSnapshot monitoringSnapshot) {
        connection = getConnection();

        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence

        try {
            insertAggregatedDataPreparedStatement.setObject(1, monitoringSnapshot);
            insertAggregatedDataPreparedStatement.setString(2, monitoringSequenceID);
            insertAggregatedDataPreparedStatement.setString(3, timestamp);
            insertAggregatedDataPreparedStatement.setString(4, monitoringSequenceID);
            insertAggregatedDataPreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                connection.commit();
                // / connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
            }
        }
    }

    // {
    // try {
    // String sql =
    // "insert into ElasticitySpace (monSeqID, timestampID, elasticitySpace) " +
    // "VALUES " + "( (select ID from MonitoringSeq where id='"
    // + monitoringSequenceID + "')" +
    // ", ( select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='"
    // + monitoringSequenceID + "')" + " AND timestamp= :timestamp )" +
    // ", :elasticitySpace)";
    // insertElasticitySpacePreparedStatement =
    // connection.prepareStatement(sql);
    // } catch (SQLException ex) {
    // Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
    // }
    // }
    public void writeElasticitySpace(String timestamp, ElasticitySpace elasticitySpace) {
        connection = getConnection();

        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence

        try {
            insertElasticitySpacePreparedStatement.setString(1, timestamp);
            insertElasticitySpacePreparedStatement.setObject(2, elasticitySpace);
            insertElasticitySpacePreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                connection.commit();
                // / connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
            }
        }
    }

    public void writeElasticityPathway(String timestamp, LightweightEncounterRateElasticityPathway elasticityPathway) {
        connection = getConnection();

        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence

        try {
            insertElasticityPathwayPreparedStatement.setString(1, timestamp);
            insertElasticityPathwayPreparedStatement.setString(1, timestamp);
            insertElasticitySpacePreparedStatement.setObject(3, elasticityPathway);
            insertElasticitySpacePreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                connection.commit();
                // / connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
            }
        }
    }

    public ElasticitySpace extractLatestElasticitySpace() {

        ElasticitySpace space = null;

        connection = getConnection();
        try {
            getLastElasticitySpaceStatement.setString(1, monitoringSequenceID);

            ResultSet resultSet = getLastElasticitySpaceStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    space = (ElasticitySpace) resultSet.getObject(1);
                    break;
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            return space;
        }
    }

    public LightweightEncounterRateElasticityPathway extractLatestElasticityPathway() {

        LightweightEncounterRateElasticityPathway pathway = new LightweightEncounterRateElasticityPathway(1);

        connection = getConnection();
        try {
            getLastElasticityPathwayStatement.setString(1, monitoringSequenceID);

            ResultSet resultSet = getLastElasticityPathwayStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    pathway = (LightweightEncounterRateElasticityPathway) resultSet.getObject(1);
                    break;
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            return pathway;
        }
    }

    // gets the maximum ID encountered
    public int getRecordsCount() {
        connection = getConnection();

        try {
            getEntriesCountPreparedStatement.setString(1, monitoringSequenceID);
            ResultSet resultSet = getEntriesCountPreparedStatement.executeQuery();
            if (resultSet != null) {
                resultSet.next();
                return resultSet.getInt(1);
            } else {
                return 0;
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);

            return 0;
        } finally {
            // try {
            // / connection.close();
            // } catch (SQLException ex) {
            // Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(Level.ERROR,
            // null, ex);
            // }
        }
    }

    /**
     *
     * @param startIndex from which monitored entry ID to start extracting
     * @param count max number of elements to return
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractMonitoringData(int startIndex, int count) {
        connection = getConnection();

        List<ServiceMonitoringSnapshot> monitoringSnapshots = new ArrayList<ServiceMonitoringSnapshot>();
        try {
            getMonitoringEntryPreparedStatement.setInt(1, startIndex);
            getMonitoringEntryPreparedStatement.setInt(2, startIndex + count);
            getMonitoringEntryPreparedStatement.setString(3, monitoringSequenceID);

            ResultSet resultSet = getMonitoringEntryPreparedStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    ServiceMonitoringSnapshot monitoringSnapshot = (ServiceMonitoringSnapshot) resultSet.getObject(1);
                    monitoringSnapshots.add(monitoringSnapshot);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            // try {
            // /// connection.close();
            // } catch (SQLException ex) {
            // Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(Level.ERROR,
            // null, ex);
            // }
            return monitoringSnapshots;
        }
    }

    /**
     *
     * @param startIndex from which monitored entry ID to start extracting
     * @param count max number of elements to return
     * @return returns maximum count elements
     */
    public ServiceMonitoringSnapshot extractLatestMonitoringData() {

        ServiceMonitoringSnapshot monitoringSnapshot = new ServiceMonitoringSnapshot();

        connection = getConnection();
        try {
            getLastAggregatedDataStatement.setString(1, monitoringSequenceID);

            ResultSet resultSet = getLastAggregatedDataStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    monitoringSnapshot = (ServiceMonitoringSnapshot) resultSet.getObject(1);
                    break;
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            return monitoringSnapshot;
        }
    }

    /**
     *
     * @param startIndex from which monitored entry ID to start extracting
     * @param count max number of elements to return
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractMonitoringData() {
        connection = getConnection();

        List<ServiceMonitoringSnapshot> monitoringSnapshots = new ArrayList<ServiceMonitoringSnapshot>();
        try {
            getAllAggregatedDataStatement.setString(1, monitoringSequenceID);
            ResultSet resultSet = getAllAggregatedDataStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    ServiceMonitoringSnapshot monitoringSnapshot = (ServiceMonitoringSnapshot) resultSet.getObject(1);
                    monitoringSnapshots.add(monitoringSnapshot);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            // try {
            // /// connection.close();
            // } catch (SQLException ex) {
            // Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(Level.ERROR,
            // null, ex);
            // }
            return monitoringSnapshots;
        }
    }

    public List<Metric> getAvailableMetrics(MonitoredElement monitoredElement) {

        List<Metric> metrics = new ArrayList<Metric>();
        try {
            getMetricsForElement.setString(1, monitoringSequenceID);
            getMetricsForElement.setString(2, monitoredElement.getId());
            getMetricsForElement.setString(3, monitoredElement.getLevel().toString());

            ResultSet resultSet = getMetricsForElement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    String metricName = resultSet.getString("metricName");
                    String metricUnit = resultSet.getString("metricUnit");
                    // currently NOT used
                    String metricType = resultSet.getString("metrictype");

                    Metric metric = new Metric(metricName, metricUnit);
                    metrics.add(metric);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            // try {
            // /// connection.close();
            // } catch (SQLException ex) {
            // Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(Level.ERROR,
            // null, ex);
            // }
            return metrics;
        }

    }

    /**
     *
     * @param username mela SQL username
     * @param password mela SQL password
     * @return the latest MELA configuration
     */
    public static ConfigurationXMLRepresentation getLatestConfiguration(String username, String password) {
        Connection c = null;
        // if the SQL connection fails, try to reconnect, as the
        // MELA_DataService might not be running.
        // BUSY wait used
        do {
            try {
                c = DriverManager.getConnection("jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort()
                        + "/melaDataServiceDB", username, password);
            } catch (SQLException ex) {
                Logger.getLogger(PersistenceSQLAccess.class).log(Level.ERROR, ex);
                Logger.getLogger(PersistenceSQLAccess.class).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
            }
        } while (c == null);

        String sql = "SELECT configuration from Configuration where ID=(Select max(ID) from Configuration);";
        ConfigurationXMLRepresentation LoggerXMLRepresentation = new ConfigurationXMLRepresentation();

        try {
            ResultSet resultSet = c.createStatement().executeQuery(sql);
            if (resultSet != null) {
                while (resultSet.next()) {
                    Reader repr = resultSet.getClob(1).getCharacterStream();
                    JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
                    LoggerXMLRepresentation = (ConfigurationXMLRepresentation) context.createUnmarshaller().unmarshal(repr);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(PersistenceSQLAccess.class).log(Level.ERROR, ex);
        } finally {
            return LoggerXMLRepresentation;
        }
    }

    /**
     *
     * @param configurationXMLRepresentation the used MELA configuration to be
     * persisted in XML and reused
     */
    public void writeConfiguration(ConfigurationXMLRepresentation configurationXMLRepresentation) {
        connection = getConnection();

        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence
        try {
            JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
            StringWriter stringWriter = new StringWriter();
            context.createMarshaller().marshal(configurationXMLRepresentation, stringWriter);
            Clob clob = connection.createClob();
            clob.setString(1, stringWriter.getBuffer().toString());

            insertConfigurationPreparedStatement.setClob(1, clob);
            insertConfigurationPreparedStatement.executeUpdate();
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                connection.commit();
                // / connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
            }
        }
    }

    //
    //
    //
    // try {
    //
    // prstGetById.setInt(1, id);
    //
    // ResultSet rs = queryExecute(prstGetById);
    //
    // if(rs == null)
    //
    // return null;
    //
    // if(!rs.next())
    //
    // return null;
    //
    // GetSetId obj = (GetSetId) rs.getObject(1); // get next object in column 1
    //
    // obj.setId(id);
    //
    // return obj;
    //
    // }
    //
    // }
    public void closeConnection() throws SQLException {
        connection.close();
    }
}
