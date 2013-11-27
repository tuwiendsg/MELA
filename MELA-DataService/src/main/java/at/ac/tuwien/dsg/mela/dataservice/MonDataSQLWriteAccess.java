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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.ClusterInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.HostInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.MetricInfo;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;
import org.apache.log4j.Level;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class MonDataSQLWriteAccess {

    private Connection connection;
    private String firstMonitoringSequenceTimestamp;

    public MonDataSQLWriteAccess(String username, String password) {

        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (Exception ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        //if the SQL connection fails, try to reconnect, as the MELA_DataService might not be running.
        //BUSY wait used
        while (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:hsqldb:hsql://"+Configuration.getDataServiceIP()+":" + Configuration.getDataServicePort() + "/melaDataServiceDB", username, password);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
                Configuration.getLogger(this.getClass()).log(Level.WARN, "Could not conenct to sql data end. Retrying in 1 second");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MonDataSQLWriteAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }
    }

    public void writeMonitoringData(ClusterInfo gangliaClusterInfo) {
        //if the firstMonitoringSequenceTimestamp is null, insert new monitoring sequence
        if (firstMonitoringSequenceTimestamp == null) {
            try {
                firstMonitoringSequenceTimestamp = gangliaClusterInfo.getLocaltime();
                Statement addSeqStmt = connection.createStatement();
                addSeqStmt.executeUpdate("insert into MonitoringSeq (timestamp) VALUES (" + firstMonitoringSequenceTimestamp + ")");
                addSeqStmt.close();
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        Statement insertValuesStmt = null;
        try {
            insertValuesStmt = connection.createStatement();
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            return;
        }

        //insert timestamp value
        String timestamp = gangliaClusterInfo.getLocaltime();
        try {
            insertValuesStmt.executeQuery("insert into Timestamp (monSeqID, timestamp) VALUES ( (SELECT ID from MonitoringSeq where timestamp=" + firstMonitoringSequenceTimestamp + "), " + timestamp + ")");
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
 

        //for all monitored metrics insert in the metric values 
        for (HostInfo gangliaHostInfo : gangliaClusterInfo.getHostsInfo()) {
            String vmIP = gangliaHostInfo.getIp();
            for (MetricInfo gangliaMetricInfo : gangliaHostInfo.getMetrics()) {
                String insertMetricValue = "insert into MetricValue (monSeqID, timestampID, metricName, metricUnit, metrictype, value, vmIP) "
                        + "VALUES "
                        + "( (select ID from MonitoringSeq where timestamp=" + firstMonitoringSequenceTimestamp + ")"
                        + ", ( select ID from Timestamp where monseqid=(select ID from MonitoringSeq where timestamp=" + firstMonitoringSequenceTimestamp + ")" + " AND timestamp=" + timestamp + ")"
                        + ",'" + gangliaMetricInfo.getName() + "'"
                        + ",'" + gangliaMetricInfo.getUnits() + "'"
                        + ",'" + gangliaMetricInfo.getType() + "'"
                        + ",'" + gangliaMetricInfo.getValue() + "'"
                        + ",'" + vmIP + "'"
                        + ")";
                try {
                    insertValuesStmt.addBatch(insertMetricValue);
                } catch (SQLException ex) {
                    Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
                }
            }
        }
        try {
            insertValuesStmt.executeBatch();
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
        try {
            insertValuesStmt.close();
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
    }

    public void closeConnection() throws SQLException {
        connection.close();
    }
//    public static void main(String[] args){
//        MonDataSQLWriteAccess access = new MonDataSQLWriteAccess("mela", "mela");
//    }
}
