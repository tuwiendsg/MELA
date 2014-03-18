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
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredEntry;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBException;

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
    private PreparedStatement getRawDataStatement;

    private PreparedStatement getAggregatedDataStatementFromTimestamp;
    private PreparedStatement insertIntoTimestamp;
    private PreparedStatement getFromTimestamp;
    private PreparedStatement getMinTimestamp;
    private PreparedStatement getMaxTimestamp;

    private PreparedStatement getLastAggregatedDataStatement;
    private PreparedStatement getLastAggregatedDataStatementBetweenTime;

    private PreparedStatement getLastElasticitySpaceStatement;

    private PreparedStatement getLastElasticityPathwayStatement;
    // used to insert in SQL data collected directly from data sources, WITHOUT
    // any structuring applied over it
    private PreparedStatement insertRawMonitoringData;
    private PreparedStatement getLastRawMonitoringData;
    // used to insert in SQL data WITH structuring and aggregation applied over
    // it
    private PreparedStatement insertAggregatedDataPreparedStatement;

    private PreparedStatement insertElasticitySpacePreparedStatement;
    private PreparedStatement deleteElasticitySpacePreparedStatement;

    private PreparedStatement insertElasticityPathwayPreparedStatement;
    private PreparedStatement deleteElasticityPathwayPreparedStatement;

    private PreparedStatement insertElasticityDependenciesPreparedStatement;
    private PreparedStatement deleteElasticityDependenciesPreparedStatement;
    private PreparedStatement getLastElasticityDependenciesPreparedStatement;

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
                String sql = "SELECT serviceStructure from timestamp where " + "ID =? AND monSeqID=?;";
                getFromTimestamp = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT MIN(id) from timestamp where monSeqID=?;";
                getMinTimestamp = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT MAX(id) from timestamp where monSeqID=?;";
                getMaxTimestamp = largeDataManagementConnection.prepareStatement(sql);
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
                        + " where monSeqID=?) LIMIT 1000;";
                getLastAggregatedDataStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where " + " timestampID >= (select ID from Timestamp where timestamp = ? ) "
                        + "AND timestampID <= (select ID from Timestamp where timestamp = ? ) AND monSeqID=?;";
                getLastAggregatedDataStatementBetweenTime = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT metricName, metricUnit, metrictype, value, monitoredElementID, monitoredElementLevel from RawCollectedData where timestampID = (SELECT MAX(ID) from timestamp where monSeqID=?);";
                getLastRawMonitoringData = connection.prepareStatement(sql);
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

                String sql = "SELECT timestampID, data from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=? and timestampID > ?; ";
                getAggregatedDataStatementFromTimestamp = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT timestampID, metricName, metricUnit,metricType, value, monitoredElementID from RAWCOLLECTEDDATA where monSeqID=? and timestampID = ?; ";
                getRawDataStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into Timestamp (monSeqID, timestamp, serviceStructure) VALUES ( (SELECT ID from MonitoringSeq where id='" + monitoringSequenceID + "'), ?,?)";
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

        {
            try {
                String sql = "delete from ElasticityDependency where monseqid= ?;";
                deleteElasticityDependenciesPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into ElasticityDependency (monSeqID, timestampID, elasticityDependency) " + "VALUES "
                        + "( (select ID from MonitoringSeq where id='" + monitoringSequenceID + "')"
                        + ", ? , ?)";
                insertElasticityDependenciesPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "delete from ElasticityDependency where monseqid= ?;";
                deleteElasticityDependenciesPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
            } catch (SQLException ex) {
                Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT elasticityDependency from ElasticityDependency where monSeqID=?;";
                getLastElasticityDependenciesPreparedStatement = largeDataManagementConnection.prepareStatement(sql);
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
    public void writeInTimestamp(String timestamp, String monSeqID, MonitoredElement serviceStructure) {

        StringWriter stringWriter = new StringWriter();
        JAXBContext jAXBContext;
        try {
            jAXBContext = JAXBContext.newInstance(MonitoredElement.class);
            jAXBContext.createMarshaller().marshal(serviceStructure, stringWriter);
        } catch (JAXBException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        String description = stringWriter.toString();

        try {
            insertIntoTimestamp.setString(1, timestamp);
            insertIntoTimestamp.setString(2, description);
            insertIntoTimestamp.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
    }

    public MonitoredElement getServiceStructure(String timestampID, String monSeqID) {
        MonitoredElement element = null;
        try {
            getFromTimestamp.setString(1, timestampID);
            getFromTimestamp.setString(2, monSeqID);

            ResultSet resultSet = getFromTimestamp.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    try {

                        JAXBContext context = JAXBContext.newInstance(MonitoredElement.class);
                        Reader repr = resultSet.getClob(1).getCharacterStream();
                        element = (MonitoredElement) context.createUnmarshaller().unmarshal(repr);
                        break;

                    } catch (JAXBException ex) {
                        Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
                    }

                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
        return element;
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

            try {
                largeDataManagementConnection.commit();
                // / connection.close();
            } catch (SQLException e) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, e);
            }
        }

        //write last configuration
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

            try {
                largeDataManagementConnection.commit();
                // / connection.close();
            } catch (SQLException e) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, e);
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

            try {
                largeDataManagementConnection.commit();
                // / connection.close();
            } catch (SQLException e) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, e);
            }
        }
    }

    public void writeElasticityDependencies(String timestamp, MonitoredElementElasticityDependencies dependencies) {
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

        // if the firstMonitoringSequenceTimestamp is null, insert new
        // monitoring sequence
        try {
            //delete previous entry
            deleteElasticityDependenciesPreparedStatement.setString(1, monitoringSequenceID);
            deleteElasticityDependenciesPreparedStatement.executeUpdate();
            //add new entry
            //insert elasticity dependencies as XML
            JAXBContext context = JAXBContext.newInstance(MonitoredElementElasticityDependencies.class);
            StringWriter stringWriter = new StringWriter();
            context.createMarshaller().marshal(dependencies, stringWriter);
            Clob clob = connection.createClob();
            clob.setString(1, stringWriter.getBuffer().toString());

            insertElasticityDependenciesPreparedStatement.setString(1, timestamp);
            insertElasticityDependenciesPreparedStatement.setClob(2, clob);
            insertElasticityDependenciesPreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);

            try {
                largeDataManagementConnection.commit();
                // / connection.close();
            } catch (SQLException e) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, e);
            }
        } catch (JAXBException ex) {
            java.util.logging.Logger.getLogger(PersistenceSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    /**
     * Computes space on demand first. Then returns
     *
     * @return
     */
    public ElasticitySpace extractLatestElasticitySpace() {

        ElasticitySpace space = null;

        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);
        ConfigurationXMLRepresentation cfg = PersistenceSQLAccess.getLatestConfiguration(username, password, dataServiceIP, dataServicePort);

        if (cfg == null) {
            Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, "Retrieved empty configuration.");
            return null;
        } else if (cfg.getRequirements() == null) {
            Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, "Retrieved configuration does not contain Requirements.");
            return null;
        } else if (cfg.getServiceConfiguration() == null) {
            Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, "Retrieved configuration does not contain Service Configuration.");
            return null;
        }
        Requirements requirements = cfg.getRequirements();

        Logger.getLogger(this.getClass()).log(Level.INFO, " reqs  " + requirements);

        MonitoredElement serviceConfiguration = cfg.getServiceConfiguration();
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
        }

        //if space == null, compute it 
        if (space == null) {
            //if space is null, compute it from all aggregated monitored data recorded so far
            List<ServiceMonitoringSnapshot> dataFromTimestamp = this.extractMonitoringData();

            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
            fct.setRequirements(requirements);
            fct.trainElasticitySpace(dataFromTimestamp);
            space = fct.getElasticitySpace();

            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
            space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

            //persist cached space
            this.writeElasticitySpace(space);
        } else {
            //else read max 1000 monitoring data records at a time, train space, and repeat as needed

            //if space is not null, update it with new data
            List<ServiceMonitoringSnapshot> dataFromTimestamp = null;

            //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
            do {
                dataFromTimestamp = this.extractMonitoringData(space.getTimestampID());
                //check if new data has been collected between elasticity space querries
                if (!dataFromTimestamp.isEmpty()) {
                    ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
                    fct.setRequirements(requirements);
                    fct.trainElasticitySpace(space, dataFromTimestamp, requirements);
                    //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                    space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

                    //persist cached space
                    this.writeElasticitySpace(space);
                }

            } while (!dataFromTimestamp.isEmpty());

        }
        //nothind cached, so just extract
        return space;
    }

//    public int getMinTimestampID(String monSeqID) {
//
//        String id = 
//
//        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);
//        try {
//            getLastElasticityPathwayStatement.setString(1, monitoringSequenceID);
//
//            ResultSet resultSet = getLastElasticityPathwayStatement.executeQuery();
//            if (resultSet != null) {
//
//                while (resultSet.next()) {
//                    pathway = (LightweightEncounterRateElasticityPathway) resultSet.getObject(1);
//                    break;
//                }
//            }
//
//        } catch (SQLException ex) {
//            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
//        }
//        return pathway;
//    }
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
        }
        return pathway;
    }

    public MonitoringData getRawMonitoringData(String monitoringSequenceID, String timestampID) {
        MonitoringData monitoringData = new MonitoringData();

        Map<String, MonitoredElementData> retrievedData = new HashMap<String, MonitoredElementData>();

        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);
        try {
            getRawDataStatement.setString(1, monitoringSequenceID);
            getRawDataStatement.setString(2, timestampID);

            ResultSet resultSet = getRawDataStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
//                    timestampID, metricName, metricUnit,metricType, value, monitoredElementID
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
                    }

                }

            }
            monitoringData.addMonitoredElementDatas(retrievedData.values());

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
        return monitoringData;
    }

    public MonitoredElementElasticityDependencies extractLatestElasticityDependencies() {

        MonitoredElementElasticityDependencies dependencies = null;

        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);
        try {
            getLastElasticityDependenciesPreparedStatement.setString(1, monitoringSequenceID);

            ResultSet resultSet = getLastElasticityDependenciesPreparedStatement.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    Reader repr = resultSet.getClob(1).getCharacterStream();
                    JAXBContext context = JAXBContext.newInstance(MonitoredElementElasticityDependencies.class);
                    dependencies = (MonitoredElementElasticityDependencies) context.createUnmarshaller().unmarshal(repr);
                    break;
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        } catch (JAXBException ex) {
            java.util.logging.Logger.getLogger(PersistenceSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return dependencies;
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
        }
        return monitoringSnapshots;
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(String startTime, String endTime) {
        largeDataManagementConnection = refreshConnection(largeDataManagementConnection);

        List<ServiceMonitoringSnapshot> monitoringSnapshots = new ArrayList<ServiceMonitoringSnapshot>();
        try {
            getLastAggregatedDataStatementBetweenTime.setString(1, startTime);
            getLastAggregatedDataStatementBetweenTime.setString(2, endTime);
            getLastAggregatedDataStatementBetweenTime.setString(3, monitoringSequenceID);

            ResultSet resultSet = getLastAggregatedDataStatementBetweenTime.executeQuery();
            if (resultSet != null) {

                while (resultSet.next()) {
                    int sTimestamp = resultSet.getInt(1);
                    ServiceMonitoringSnapshot monitoringSnapshot = (ServiceMonitoringSnapshot) resultSet.getObject(2);
                    monitoringSnapshot.setTimestampID(sTimestamp);
                    monitoringSnapshots.add(monitoringSnapshot);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
        return monitoringSnapshots;
    }

    /**
     *
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
        }
        return monitoringSnapshot;
    }

    /**
     *
     * @return returns maximum count elements
     */
    public Map<MonitoredElement, List<MonitoredEntry>> extractLatestRawMonitoringData() {

        Map<MonitoredElement, List<MonitoredEntry>> rawData = new HashMap<MonitoredElement, List<MonitoredEntry>>();

        connection = refreshConnection(connection);
        try {
            getLastRawMonitoringData.setString(1, monitoringSequenceID);

            ResultSet resultSet = getLastRawMonitoringData.executeQuery();
            if (resultSet != null) {

//  metricName, metricUnit, metrictype, value, monitoredElementID, monitoredElementLevel
                while (resultSet.next()) {
                    String metricName = resultSet.getString("metricName");
                    String value = resultSet.getString("value");
                    String monitoredElementID = resultSet.getString("monitoredElementID");
                    MonitoredElement monitoredElement = new MonitoredElement(monitoredElementID);
                    MonitoredEntry entry = new MonitoredEntry(new Metric(metricName), new MetricValue(value));

                    if (rawData.containsKey(monitoredElement)) {
                        rawData.get(monitoredElement).add(entry);
                    } else {
                        List<MonitoredEntry> entrys = new ArrayList<MonitoredEntry>();

                        entrys.add(entry);
                        rawData.put(monitoredElement, entrys);
                    }
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
        return rawData;
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
        }
        return monitoringSnapshots;
    }

    /**
     *
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
        }
        // try {
        // /// connection.close();
        // } catch (SQLException ex) {
        // Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(Level.ERROR,
        // null, ex);
        // }
        return monitoringSnapshots;
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
        }
        // try {
        // /// connection.close();
        // } catch (SQLException ex) {
        // Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(Level.ERROR,
        // null, ex);
        // }
        return metrics;

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

            try {
                connection.commit();
                // / connection.close();
            } catch (SQLException e) {
                Logger.getLogger(PersistenceSQLAccess.class.getName()).log(Level.ERROR, null, e);
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
