/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group E184
 *
 * This work was partially supported by the European Commission in terms of the CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
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
import java.sql.SQLException;
import org.apache.log4j.Level;
import org.hsqldb.Server;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class MELADataService {
    private Server server;
    
    {
        server = new Server();
        server.setDaemon(true);
        server.setDatabaseName(0, "melaDataServiceDB");
        server.setDatabasePath(0, "file:"+ Configuration.getDatabaseFileLocation() + "/melaDataServiceDB" +";user=mela;password=mela");
        server.setPort(Configuration.getDataServicePort());
        server.setAddress(Configuration.getDataServiceIP());
        
        //username and password : mela mela
        //TODO: add databases for el pathway and space and service types
//        server.setDatabaseName(1, "melaDataServiceDB");
    }
    
    public void startServer(){
        server.start();
        Configuration.getLogger(this.getClass()).log(Level.INFO, "SQL Server started");
        new AggregatedMonitoringDataSQLAccess("mela", "mela").createDatabaseStructure();
        
    }
    
    public void stopServer(){
        server.stop();
    }
    
   
}
