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
package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.ClusterInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.HostInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.MetricInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataAccess.DataSourceI;
import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.Logger;
import org.apache.log4j.Level;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 * Used in replaying monitoring data
 *
 */
public class GangliaSQLDataSource implements DataSourceI {

    private String currentTimestampID;
    private Connection connection;
    private String monSeqID;

    public GangliaSQLDataSource(String monSeqID, String username, String password){
        this.monSeqID = monSeqID;

        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (Exception ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        //if the SQL connection fails, try to reconnect, as the MELA_DataService might not be running.
        //BUSY wait used
        while (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:hsqldb:hsql://"+Configuration.getDataServiceIP()+":"+Configuration.getDataServicePort()+"/melaDataServiceDB", username, password);
            } catch (SQLException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
                Configuration.getLogger(this.getClass()).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            }
        }

        Statement statement;
        try {
            statement = connection.createStatement();

            ResultSet getMinTimestampID = statement.executeQuery("select MIN(id) from Timestamp where monseqid = (SELECT ID FROM MONITORINGSEQ where TIMESTAMP='" + monSeqID + "')");

            if (!getMinTimestampID.next()) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, "Could not find monitored timestamps for monitoring sequence ID " + monSeqID);
                statement.close();
                return;
            }
            //register first monitoring snapshot
            currentTimestampID = getMinTimestampID.getString(1);
            statement.close();
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            Configuration.getLogger(this.getClass()).log(Level.WARN, "Could not get timestampID from the SQL Data End");
        }
        
    }

    public ClusterInfo getMonitoringData() throws DataAccessException {
        ClusterInfo clusterInfo = new ClusterInfo();

        Statement statement;
        try {
            statement = connection.createStatement();
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            throw new DataAccessException(ex.getMessage(), ex.getCause());
        }

        //get next timestamp ID, based on which will retrieve monitoring data
        ResultSet getMinTimestampID;
        try {
            getMinTimestampID = statement.executeQuery("select MIN(ID) from Timestamp where monseqid = (SELECT ID FROM MONITORINGSEQ where TIMESTAMP='" + monSeqID + "') AND id > " + currentTimestampID);

            if (!getMinTimestampID.next()) {
                Configuration.getLogger(this.getClass()).log(Level.ERROR, "Could not find monitored timestamps for monitoring sequence ID " + monSeqID);
                statement.close();
                return null;
            }
            //register next monitoring snapshot
            currentTimestampID = getMinTimestampID.getString(1);
        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            throw new DataAccessException(ex.getMessage(), ex.getCause());
        }

        //go trough all metrics belonging to the extracted snapshot ID and extract them in a map of VM IP, metric

        //key:VM, value : metric value
        Map<String, List<MetricInfo>> monitoringData = new LinkedHashMap<String, List<MetricInfo>>();


        try {
            //todo: maybe translate thhe statements in this class in Prepared Statement instances to improve performance
            ResultSet metricsQuery = statement.executeQuery("select * from metricvalue where "
                    + "monseqid=(SELECT ID FROM MONITORINGSEQ where TIMESTAMP='" + monSeqID + "') AND timestampid=" + currentTimestampID + "");

            while (metricsQuery.next()) {
                MetricInfo gangliaMetricInfo = new MetricInfo();
                gangliaMetricInfo.setName(metricsQuery.getString("metricname"));
                gangliaMetricInfo.setUnits(metricsQuery.getString("metricunit"));
                gangliaMetricInfo.setType(metricsQuery.getString("metricType"));
                gangliaMetricInfo.setValue(metricsQuery.getString("value"));

                //get the IP of the VM from which the metrics where collected
                //the assignment to Service Unit is done at a higher level, not here
                String vmIp = metricsQuery.getString("vmip");
                if (monitoringData.containsKey(vmIp)) {
                    monitoringData.get(vmIp).add(gangliaMetricInfo);
                } else {
                    List<MetricInfo> gangliaMetricInfos = new ArrayList<MetricInfo>();
                    gangliaMetricInfos.add(gangliaMetricInfo);
                    monitoringData.put(vmIp, gangliaMetricInfos);
                }
            }

            //after reading the data, go trough the map and create GangliaHostInfo instances
            for (String vmIP : monitoringData.keySet()) {
                HostInfo gangliaHostInfo = new HostInfo();
                gangliaHostInfo.setIp(vmIP);
                gangliaHostInfo.setMetrics(monitoringData.get(vmIP));
                clusterInfo.getHostsInfo().add(gangliaHostInfo);
            }


        } catch (SQLException ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            throw new DataAccessException(ex.getMessage(), ex.getCause());
        }
        return clusterInfo;
    }
}
