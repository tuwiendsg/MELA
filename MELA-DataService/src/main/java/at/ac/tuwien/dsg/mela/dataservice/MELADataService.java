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

import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Level;
import org.hsqldb.Server;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
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

            server.setAddress(Configuration.getDataServiceIP());

//        int hyperSQLPort = 1234;
//
//		do {
//			try {
//
//				Socket sock = new Socket("localhost", hyperSQLPort);
//				sock.close();
//				hyperSQLPort++;
//
//			} catch (Exception e) {
//				// if exception, then port is not open
            server.setPort(Configuration.getDataServicePort());
//				Configuration.setProperty("HYPERSQL.PORT", hyperSQLPort);
//				break;
//			}
//		} while (true);

        }

        Thread thread = new Thread() {
            public void run() {
                server.start();
                Configuration.getLogger(this.getClass()).log(Level.INFO, "SQL Server started");
                new AggregatedMonitoringDataSQLAccess("mela", "mela").createDatabaseStructure();
            }
        };
        thread.start();
    }

    public void stopServer() {
        server.stop();
    }
}
