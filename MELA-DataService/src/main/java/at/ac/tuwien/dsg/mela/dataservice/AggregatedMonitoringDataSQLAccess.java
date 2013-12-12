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
package at.ac.tuwien.dsg.mela.dataservice;

import at.ac.tuwien.dsg.mela.common.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import java.io.Reader;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Clob;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;

import org.apache.log4j.Level;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class AggregatedMonitoringDataSQLAccess {

    private static final String AGGREGATED_DATA_TABLE_NAME = "AggregatedData";
    private String username;
    private String password;
    private Connection connection;
    private String monitoringSequenceID;
    private PreparedStatement insertMonitoringEntryPreparedStatement;
    private PreparedStatement insertConfigurationPreparedStatement;
    private PreparedStatement getMonitoringEntryPreparedStatement;
    private PreparedStatement getEntriesCountPreparedStatement;
    private PreparedStatement getLastAggregatedDataStatement;
    private PreparedStatement getAllAggregatedDataStatement;

    public AggregatedMonitoringDataSQLAccess(String username, String password, String monitoringSequenceID) {

        this.monitoringSequenceID = monitoringSequenceID;
        this.username = username;
        this.password = password;

        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (Exception ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        //instantiate connection first
        {

            //if the SQL connection fails, try to reconnect, as the MELA_DataService might not be running.
            //BUSY wait used
            while (connection == null) {
                try {
                    connection = DriverManager.getConnection("jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB", username, password);
                } catch (SQLException ex) {
                    Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
            }
        }

        //ServiceID is used in retrieving historical monitoring data (if needed)
        try {
            Statement addSeqStmt = connection.createStatement();
            addSeqStmt.executeUpdate("insert into MonitoringSeq (ID) VALUES ('" + monitoringSequenceID + "')");
            addSeqStmt.close();
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }


        //prepare statements
        {
            try {
                String sql = "INSERT INTO " + AGGREGATED_DATA_TABLE_NAME + " (data, monSeqID) "
                        + "VALUES (?, ?)";
                insertMonitoringEntryPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "INSERT INTO Configuration (configuration) "
                        + "VALUES (?)";
                insertConfigurationPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
                return;
            }
        }



        {
            try {
                String sql = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where "
                        + "ID > (?) AND ID < (?) AND monSeqID=(?);";
                getMonitoringEntryPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "SELECT MAX(ID) from " + AGGREGATED_DATA_TABLE_NAME + " WHERE monSeqID=?;";
                getEntriesCountPreparedStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }


        {
            try {

                String sql = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where " + "ID = (SELECT MAX(ID) from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=?);";
                getLastAggregatedDataStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {

                String sql = "SELECT data from " + AGGREGATED_DATA_TABLE_NAME + " where monSeqID=?;";
                getAllAggregatedDataStatement = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

    }

    private Connection getConnection() {
        try {
            //check if connection is open. if not, it gives exception
            String catalog = connection.getCatalog();
        } catch (SQLException e) {

            connection = null;

            //if the SQL connection fails, try to reconnect, as the MELA_DataService might not be running.
            //BUSY wait used
            while (connection == null) {
                try {
                    connection = DriverManager.getConnection("jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB", username, password);
                } catch (SQLException ex) {
                    Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
            }

        }
        return connection;
    }

    public void writeMonitoringData(ServiceMonitoringSnapshot monitoringSnapshot) {
        connection = getConnection();


        //if the firstMonitoringSequenceTimestamp is null, insert new monitoring sequence


        try {
            insertMonitoringEntryPreparedStatement.setObject(1, monitoringSnapshot);
            insertMonitoringEntryPreparedStatement.setString(2, monitoringSequenceID);
            insertMonitoringEntryPreparedStatement.executeUpdate();
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                connection.commit();
                /// connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
    }

    //gets the maximum ID encountered
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
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);

            return 0;
        } finally {
//            try {
            /// connection.close();
//            } catch (SQLException ex) {
//                Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//            }
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
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
//            try {
//               /// connection.close();
//            } catch (SQLException ex) {
//                Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//            }
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
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
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
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
//            try {
//               /// connection.close();
//            } catch (SQLException ex) {
//                Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//            }
            return monitoringSnapshots;
        }
    }

    public static ConfigurationXMLRepresentation getLatestConfiguration(String username, String password) {
        Connection c = null;
        //if the SQL connection fails, try to reconnect, as the MELA_DataService might not be running.
        //BUSY wait used
        do {
            try {
                c = DriverManager.getConnection("jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB", username, password);
            } catch (SQLException ex) {
                Configuration.getLogger(AggregatedMonitoringDataSQLAccess.class).log(Level.ERROR, ex);
                Configuration.getLogger(AggregatedMonitoringDataSQLAccess.class).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        } while (c == null);

        String sql = "SELECT configuration from Configuration where ID=(Select max(ID) from Configuration);";
        ConfigurationXMLRepresentation configurationXMLRepresentation = new ConfigurationXMLRepresentation();

        try {
            ResultSet resultSet = c.createStatement().executeQuery(sql);
            if (resultSet != null) {
                while (resultSet.next()) {
                    Reader repr = resultSet.getClob(1).getCharacterStream();
                    JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
                    configurationXMLRepresentation = (ConfigurationXMLRepresentation) context.createUnmarshaller().unmarshal(repr);
                }
            }

        } catch (SQLException ex) {
            Configuration.getLogger(AggregatedMonitoringDataSQLAccess.class).log(Level.ERROR, ex);
        } finally {
            return configurationXMLRepresentation;
        }
    }

    public void writeConfig(ConfigurationXMLRepresentation configurationXMLRepresentation) {
        connection = getConnection();

        //if the firstMonitoringSequenceTimestamp is null, insert new monitoring sequence
        try {
            JAXBContext context = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
            StringWriter stringWriter = new StringWriter();
            context.createMarshaller().marshal(configurationXMLRepresentation, stringWriter);
            Clob clob = connection.createClob();
            clob.setString(1, stringWriter.getBuffer().toString());

            insertConfigurationPreparedStatement.setClob(1, clob);
            insertConfigurationPreparedStatement.executeUpdate();
        } catch (Exception ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        } finally {
            try {
                connection.commit();
                /// connection.close();
            } catch (SQLException ex) {
                Logger.getLogger(AggregatedMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
    }

//   
//        
//        
//        try {
// 
//            prstGetById.setInt(1, id);
// 
//            ResultSet rs = queryExecute(prstGetById);
// 
//            if(rs == null)
// 
//                return null;
// 
//            if(!rs.next())
// 
//                return null;
// 
//            GetSetId obj = (GetSetId) rs.getObject(1);  // get next object in column 1
// 
//            obj.setId(id);
// 
//            return obj;
// 
//        }
//
//    }
    public void closeConnection() throws SQLException {
        connection.close();
    }
}
