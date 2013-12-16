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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.apache.log4j.Level;

import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class RawMonitoringDataSQLAccess {

    private Connection connection;
    //for example, Service ID, or timestamp
    private String monitoringSequenceID;
    private PreparedStatement insertIntoTimestamp;
    private PreparedStatement insertMonitoringData;

    public RawMonitoringDataSQLAccess(String username, String password, String monitoringSequenceID) {

        this.monitoringSequenceID = monitoringSequenceID;
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (Exception ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        //if the SQL connection fails, try to reconnect, as the MELA_DataService might not be running.
        //BUSY wait used
        while (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB", username, password);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
                Configuration.getLogger(this.getClass()).log(Level.WARN, "Could not conenct to sql data end. Retrying in 1 second");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(RawMonitoringDataSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }

 

        {
            try {
                String sql = "insert into Timestamp (monSeqID, timestamp) VALUES ( (SELECT ID from MonitoringSeq where id='" + monitoringSequenceID + "'), ?)";
                insertIntoTimestamp = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        {
            try {
                String sql = "insert into MetricValue (monSeqID, timestampID, metricName, metricUnit, metrictype, value, vmIP) "
                        + "VALUES "
                        + "( (select ID from MonitoringSeq where id='" + monitoringSequenceID + "')"
                        + ", ( select ID from Timestamp where monseqid=(select ID from MonitoringSeq where ID='" + monitoringSequenceID + "')" + " AND timestamp=? )"
                        + ",?,?,?,?,?)";
                insertMonitoringData = connection.prepareStatement(sql);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

    }

    //TODO: rewrite to support multiple data source on multiple levels
//    public void writeMonitoringData(ClusterInfo gangliaClusterInfo) {
//         
//        //insert timestamp value
//        String timestamp = gangliaClusterInfo.getLocaltime();
//        try {
//            insertIntoTimestamp.setString(1, timestamp);
//            insertIntoTimestamp.executeUpdate();
//        } catch (SQLException ex) {
//            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
//        }
//
//
//        //for all monitored metrics insert in the metric values 
//        for (MonitoredElementData gangliaHostInfo : gangliaClusterInfo.getHostsInfo()) {
//            String vmIP = gangliaHostInfo.getIp();
//            for (MetricInfo gangliaMetricInfo : gangliaHostInfo.getMetrics()) {
//                try {
//                    insertMonitoringData.setString(1, timestamp);
//                    insertMonitoringData.setString(2, gangliaMetricInfo.getName());
//                    insertMonitoringData.setString(3, gangliaMetricInfo.getUnits());
//                    insertMonitoringData.setString(4, gangliaMetricInfo.getType());
//                    insertMonitoringData.setString(5, gangliaMetricInfo.getValue());
//                    insertMonitoringData.setString(6, vmIP);
//
//                    insertMonitoringData.executeUpdate();
//                } catch (SQLException ex) {
//                    Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
//                }
//            }
//        }
//         
//    }

    public void closeConnection() throws SQLException {
        connection.close();
    }
//    public static void main(String[] args){
//        MonDataSQLWriteAccess access = new MonDataSQLWriteAccess("mela", "mela");
//    }
}
