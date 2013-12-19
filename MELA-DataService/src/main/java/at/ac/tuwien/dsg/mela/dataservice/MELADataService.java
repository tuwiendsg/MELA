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

import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.apache.log4j.Logger;

import org.apache.log4j.Level;
import org.hsqldb.Server;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class MELADataService {

    private Server server;

    public void startServer() {
        {
            server = new Server();
            server.setDaemon(true);
            server.setDatabaseName(0, "melaDataServiceDB");
            server.setDatabasePath(0, "file:" + Configuration.getDatabaseFileLocation() + "/melaDataServiceDB" + ";user=mela;password=mela");
            server.setPort(Configuration.getDataServicePort());
            server.setAddress(Configuration.getDataServiceIP());

            Thread thread = new Thread() {
                public void run() {
                    server.start();
                    Logger.getLogger(this.getClass()).log(Level.INFO, "SQL Server started");
                }
            };
            thread.start();

            // int hyperSQLPort = 1234;
            //
            // do {
            // try {
            //
            // Socket sock = new Socket("localhost", hyperSQLPort);
            // sock.close();
            // hyperSQLPort++;
            //
            // } catch (Exception e) {
            // // if exception, then port is not open

            // Configuration.setProperty("HYPERSQL.PORT", hyperSQLPort);
            // break;
            // }
            // } while (true);

        }

    }

    public void createInitialStructure() {
        // if database empty, create initial database structure
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        Connection connection = null;
        // instantiate connection first
        {

            // if the SQL connection fails, try to reconnect, as the
            // MELA_DataService might not be running.
            // BUSY wait used
            while (connection == null) {
                try {
                    connection = DriverManager.getConnection(
                            "jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB", "mela",
                            "mela");
                } catch (SQLException ex) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
                    Logger.getLogger(this.getClass()).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MELADataService.class.getName()).log(Level.ERROR, null, ex);
                }
            }
        }

        try {
            connection.createStatement().execute("drop table IF EXISTS ElasticityPathway;");
            connection.createStatement().execute("drop table IF EXISTS ElasticitySpace;");
            connection.createStatement().execute("drop table IF EXISTS AggregatedData;");
            connection.createStatement().execute("drop table IF EXISTS Configuration;");
            connection.createStatement().execute("drop table IF EXISTS RawCollectedData;");
            connection.createStatement().execute("drop table IF EXISTS Timestamp;");
            connection.createStatement().execute("drop table IF EXISTS MonitoringSeq;");

            connection.createStatement().execute("create table MonitoringSeq (ID VARCHAR(200) PRIMARY KEY);");
            connection
                    .createStatement()
                    .execute(
                    "create table Timestamp (ID int IDENTITY, monSeqID VARCHAR(200), timestamp VARCHAR(200), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID) );");
            connection
                    .createStatement()
                    .execute(
                    "create table RawCollectedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, metricName VARCHAR(100), metricUnit VARCHAR(100), metrictype VARCHAR(20), value VARCHAR(50),  monitoredElementID VARCHAR (50), monitoredElementLevel VARCHAR (50), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID));");
            // this creates a table used to store on rows the
            // ServiceStructure,
            // CompositionRules, and Requirements
            connection.createStatement().execute("create table Configuration (ID int IDENTITY, configuration LONGVARCHAR);");
            connection
                    .createStatement()
                    .execute(
                    "create table AggregatedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, data OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );");
            connection
                    .createStatement()
                    .execute(
                    "create table ElasticitySpace (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticitySpace OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );");
            connection
                    .createStatement()
                    .execute(
                    "create table ElasticityPathway (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticityPathway OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );");

            connection.commit();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
    }

    public void createInitialStructureIfItDoesNotExist() {
        // if database empty, create initial database structure
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }

        Connection connection = null;
        // instantiate connection first
        {

            // if the SQL connection fails, try to reconnect, as the
            // MELA_DataService might not be running.
            // BUSY wait used
            while (connection == null) {
                try {
                    connection = DriverManager.getConnection(
                            "jdbc:hsqldb:hsql://" + Configuration.getDataServiceIP() + ":" + Configuration.getDataServicePort() + "/melaDataServiceDB", "mela",
                            "mela");
                } catch (SQLException ex) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
                    Logger.getLogger(this.getClass()).log(Level.WARN, "Could not connect to sql data end. Retrying in 1 second");
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MELADataService.class.getName()).log(Level.ERROR, null, ex);
                }
            }
        }

        try {

            connection.createStatement().execute("create table IF NOT EXISTS MonitoringSeq (ID VARCHAR(200) PRIMARY KEY);");
            connection
                    .createStatement()
                    .execute(
                    "create table IF NOT EXISTS Timestamp (ID int IDENTITY, monSeqID VARCHAR(200), timestamp VARCHAR(200), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID) );");
            connection
                    .createStatement()
                    .execute(
                    "create table IF NOT EXISTS RawCollectedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, metricName VARCHAR(100), metricUnit VARCHAR(100), metrictype VARCHAR(20), value VARCHAR(50),  monitoredElementID VARCHAR (50), monitoredElementLevel VARCHAR (50), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID));");
            // this creates a table used to store on rows the
            // ServiceStructure,
            // CompositionRules, and Requirements
            connection.createStatement().execute("create table IF NOT EXISTS Configuration (ID int IDENTITY, configuration LONGVARCHAR);");
            connection
                    .createStatement()
                    .execute(
                    "create table IF NOT EXISTS AggregatedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, data OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );");
            connection
                    .createStatement()
                    .execute(
                    "create table IF NOT EXISTS ElasticitySpace (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticitySpace OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );");
            connection
                    .createStatement()
                    .execute(
                    "create table IF NOT EXISTS ElasticityPathway (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int,elasticityPathway OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID));");

            connection.commit();
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
        }
    }

    public void stopServer() {
        server.stop();
    }
}
