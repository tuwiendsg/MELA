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

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class PersistenceSQLAccess {

    private static final String AGGREGATED_DATA_TABLE_NAME = "AggregatedData";
    private String username;
    private String password;
    //this is meant as a fisrst step towards implementing a separation process
    //for ensuring a query does not fail because another large query destroyed the connection
    //used only for retrieving large amounts of data, and thus si prone to failure
    //use in retrieving data for computing the elasticity space and pathway
    private Connection largeDataManagementConnection;
    //used for everything else
    private Connection connection;
    private String monitoringSequenceID;
    private String dataServiceIP;
    private int dataServicePort;
    private PreparedStatement insertConfigurationPreparedStatement;
    private PreparedStatement getMonitoringEntryPreparedStatement;
    private PreparedStatement getEntriesCountPreparedStatement;
    private PreparedStatement getMetricsForElement;
    private PreparedStatement getAllAggregatedDataStatement;
    private PreparedStatement getAggregatedDataStatementFromTimestamp;
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
    private PreparedStatement deleteElasticitySpacePreparedStatement;
    private PreparedStatement insertElasticityPathwayPreparedStatement;
    private PreparedStatement deleteElasticityPathwayPreparedStatement;

    public PersistenceSQLAccess(String username, String password, String dataServiceIP, int dataServicePort, String monitoringSequenceID) {

        this.monitoringSequenceID = monitoringSequenceID;
        this.username = username;
        this.password = password;
        this.dataServiceIP = dataServiceIP;
        this.dataServicePort = dataServicePort;
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
                            "jdbc:hsqldb:hsql://" + dataServiceIP + ":" + dataServicePort + "/melaDataServiceDB",
                            username, password);
                    largeDataManagementConnection = DriverManager.getConnection(
                            "jdbc:hsqldb:hsql://" + dataServiceIP + ":" + dataServicePort + "/melaDataServiceDB",
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
                getMonitoringEntryPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT MAX(ID) from " + AGGREGATED_DATA_TABLE_NAME + " WHERE monSeqID=?;";
                getEntriesCountPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
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

                String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where " + "ID = (SELECT MAX(ID) from " + AGGREGATED_DATA_TABLE_NAME
                        + " where monSeqID=?);";
                getLastAggregatedDataStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT timestampID, elasticitySpace from ElasticitySpace where monSeqID=?;";
                getLastElasticitySpaceStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }
        {
            try {
                String sql = "SELECT elasticityPathway from ElasticityPathway where monSeqID=?;";
                getLastElasticityPathwayStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=?;";
                getAllAggregatedDataStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=? and timestampID > ?;";
                getAggregatedDataStatementFromTimestamp = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into Timestamp (monSeqID, timestamp) VALUES ( (SELECT ID from MonitoringSeq where id='" + monitoringSequenceID + "'), ?)";
                insertIntoTimestamp = largeDataManagementConnection.prepareStatement(sql);
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
                insertRawMonitoringData = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into ElasticitySpace (monSeqID, timestampID, elasticitySpace) " + "VALUES " + "( (select ID from MonitoringSeq where id='"
                        + monitoringSequenceID + "')" + ", ? " + ", ? )";

                insertElasticitySpacePreparedStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into ElasticityPathway (monSeqID, timestampID, elasticityPathway) " + "VALUES "
                        + "( (select ID from MonitoringSeq where id='" + monitoringSequenceID + "')"
                        + ", (select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='" + monitoringSequenceID + "')"
                        + " AND timestamp= ? )" + ", ?)";
                insertElasticityPathwayPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "delete from ElasticitySpace where monseqid= ?;";
                deleteElasticitySpacePreparedStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "delete from ElasticityPathway where monseqid= ?;";
                deleteElasticityPathwayPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
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

    /**
     * Tests if the connection is still open, otherwise creates a new one
     *
     * @return
     */
    private Connection refreshConnection(Connection connection) {

        //check if it can respond in 1 second
        try {

            boolean isValid = connection.isValid(1000);

            if (!isValid) {
                connection = null;

                // if the SQL connection fails, try to reconnect, as the
                // MELA_DataService might not be running.
                // BUSY wait used
                while (connection == null) {
                    try {
                        connection = DriverManager.getConnection(
                                "jdbc:hsqldb:hsql://" + dataServiceIP + ":" + dataServicePort + "/melaDataServiceDB",
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

        } catch (SQLException e) {
            connection = null;

            // if the SQL connection fails, try to reconnect, as the
            // MELA_DataService might not be running.
            // BUSY wait used
            while (connection == null) {
                try {
                    connection = DriverManager.getConnection(
                            "jdbc:hsqldb:hsql://" + dataServiceIP + ":" + dataServicePort + "/melaDataServiceDB",
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
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

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
                largeDataManagementConnection.commit();
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
    public void writeElasticitySpace(ElasticitySpace elasticitySpace) {
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence

        try {
            //delete previous entry
            deleteElasticitySpacePreparedStatement.setString(1, elasticitySpace.getService().getId());
            deleteElasticitySpacePreparedStatement.executeUpdate();
            //add new entry
            insertElasticitySpacePreparedStatement.setInt(1, elasticitySpace.getTimestampID());
            insertElasticitySpacePreparedStatement.setObject(2, elasticitySpace);
            insertElasticitySpacePreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                largeDataManagementConnection.commit();
                // / connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
            }
        }
    }

    public void writeElasticityPathway(String timestamp, LightweightEncounterRateElasticityPathway elasticityPathway) {
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence
        try {
            //delete previous entry
            deleteElasticityPathwayPreparedStatement.setString(1, monitoringSequenceID);
            deleteElasticityPathwayPreparedStatement.executeUpdate();
            //add new entry
            insertElasticityPathwayPreparedStatement.setString(1, timestamp);
            insertElasticitySpacePreparedStatement.setObject(2, elasticityPathway);
            insertElasticitySpacePreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                largeDataManagementConnection.commit();
                // / connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
            }
        }
    }

    public ElasticitySpace extractLatestElasticitySpace() {

        ElasticitySpace space = null;

        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);
        try {
            getLastElasticitySpaceStatement.setString(1, monitoringSequenceID);

            ResultSet resultSet = getLastElasticitySpaceStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    int timestamp = resultSet.getInt(1);
                    space = (ElasticitySpace) resultSet.getObject(2);
                    space.setTimestampID(timestamp);
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

        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);
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
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

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
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

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

        connection = refreshConnection(connection);
        try {
            getLastAggregatedDataStatement.setString(1, monitoringSequenceID);

            ResultSet resultSet = getLastAggregatedDataStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    int sTimestamp = resultSet.getInt(1);
                    monitoringSnapshot = (ServiceMonitoringSnapshot) resultSet.getObject(2);
                    monitoringSnapshot.setTimestampID(sTimestamp);
                    break;
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            return monitoringSnapshot;
        }
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(int timestamp) {

        List<ServiceMonitoringSnapshot> monitoringSnapshots = new ArrayList<ServiceMonitoringSnapshot>();

        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);
        try {
            getAggregatedDataStatementFromTimestamp.setString(1, monitoringSequenceID);
            getAggregatedDataStatementFromTimestamp.setInt(2, timestamp);
            ResultSet resultSet = getAggregatedDataStatementFromTimestamp.executeQuery();
            if (resultSet != null) {
                while (resultSet.next()) {
                    int sTimestamp = resultSet.getInt(1);
                    ServiceMonitoringSnapshot s = (ServiceMonitoringSnapshot) resultSet.getObject(2);
                    s.setTimestampID(sTimestamp);
                    monitoringSnapshots.add(s);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            return monitoringSnapshots;
        }
    }

    /**
     *
     * @param startIndex from which monitored entry ID to start extracting
     * @param count max number of elements to return
     * @return returns maximum count elements
     */
    public List<ServiceMonitoringSnapshot> extractMonitoringData() {
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

        List<ServiceMonitoringSnapshot> monitoringSnapshots = new ArrayList<ServiceMonitoringSnapshot>();
        try {
            getAllAggregatedDataStatement.setString(1, monitoringSequenceID);
            ResultSet resultSet = getAllAggregatedDataStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    int sTimestamp = resultSet.getInt(1);
                    ServiceMonitoringSnapshot s = (ServiceMonitoringSnapshot) resultSet.getObject(2);
                    s.setTimestampID(sTimestamp);
                    monitoringSnapshots.add(s);
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

        connection = refreshConnection(connection);

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
    public static ConfigurationXMLRepresentation getLatestConfiguration(String username, String password, String dataServiceIP, int dataServicePort) {
        Connection c = null;
        // if the SQL connection fails, try to reconnect, as the
        // MELA_DataService might not be running.
        // BUSY wait used
        do {
            try {
                c = DriverManager.getConnection("jdbc:hsqldb:hsql://" + dataServiceIP + ":" + dataServicePort
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
        ConfigurationXMLRepresentation configurationXMLRepresentation = null;

        try {
            ResultSet resultSet = c.createStatement().executeQuery(sql);
            if (resultSet != null) {
                while (resultSet.next()) {
                    Reader repr = resultSet.getClob(1).getCharacterStream();
                    JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
                    configurationXMLRepresentation = (ConfigurationXMLRepresentation) context.createUnmarshaller().unmarshal(repr);
                }
            }

        } catch (Exception ex) {
            Logger.getLogger(PersistenceSQLAccess.class).log(Level.ERROR, ex);
        }

        if (configurationXMLRepresentation == null) {
            return ConfigurationXMLRepresentation.createDefaultConfiguration();
        } else {
            return configurationXMLRepresentation;
        }
    }

    /**
     *
     * @param configurationXMLRepresentation the used MELA configuration to be
     * persisted in XML and reused
     */
    public void writeConfiguration(ConfigurationXMLRepresentation configurationXMLRepresentation) {
        connection = refreshConnection(connection);

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
        try {
            connection.close();
        } catch (SQLException ex) {
             Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
        }
        try {
            largeDataManagementConnection.close();
        } catch (SQLException ex) {
            Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, ex);
        }
    }
}
