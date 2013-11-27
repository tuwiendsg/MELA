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

import at.ac.tuwien.dsg.mela.dataservice.MonDataSQLWriteAccess;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.ClusterInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.MonitoringSystemInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataAccess.DataSourceI;
import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import org.apache.log4j.Level;
import org.yaml.snakeyaml.Yaml;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class LocalGangliaLiveDataSource implements DataSourceI {

    private Yaml yaml = new Yaml();
    private MonDataSQLWriteAccess dataSQLWriteAccess;

    {
        dataSQLWriteAccess = new MonDataSQLWriteAccess("mela", "mela");
    }

    public ClusterInfo getMonitoringData() throws DataAccessException {

        String cmd = "telnet " + Configuration.getAccessMachineIP() + " " + Configuration.getGangliaPort();
        String content = "";

        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;

            while ((line = reader.readLine()) != null) {

                //if ganglia does not respond
                if (line.contains("Unable to connect")) {
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "Unable to execute " + cmd);
                    return null;
                }
                if (line.contains("<") || line.endsWith("]>")) {
                    content += line + "\n";
                }
            }

            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            p.destroy();

            //if ganglia does not respond
            if (content == null || content.length() == 0) {
                Configuration.getLogger(this.getClass()).log(Level.WARN, "" + "Unable to execute " + cmd);
                return new ClusterInfo();
            }
        } catch (Exception ex) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, ex);
            return new ClusterInfo();
        }

        StringReader stringReader = new StringReader(content);
        try {
            JAXBContext jc = JAXBContext.newInstance(MonitoringSystemInfo.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            MonitoringSystemInfo info = (MonitoringSystemInfo) unmarshaller.unmarshal(stringReader);
            ClusterInfo gangliaClusterInfo = info.getClusters().iterator().next();
            stringReader.close();

            dataSQLWriteAccess.writeMonitoringData(gangliaClusterInfo);

            return gangliaClusterInfo;
        } catch (Exception e) {
            Configuration.getLogger(this.getClass()).log(Level.WARN, e.getMessage());
            return new ClusterInfo();
        }
    }

//    private void saveRawDataToFile(String file, GangliaClusterInfo gangliaClusterInfo) {
////        Configuration.getLogger(this.getClass()).log(Level.INFO,"Collected monitoring data at " + new Date());
//        try {
//            String elasticity = yaml.dump(gangliaClusterInfo);
//            //better to open close buffers as there are less chances I get the file in unstable state if I terminate the
//            //program execution abruptly
//            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));
//            bufferedWriter.newLine();
//            bufferedWriter.write("--- " + elasticity);
//            bufferedWriter.flush();
//            bufferedWriter.close();
//        } catch (Exception e) {
//            Configuration.getLogger(this.getClass()).log(Level.WARN, e.getMessage(), e);
//            e.printStackTrace();
//        }
//    }
    @Override
    protected void finalize() throws Throwable {
        dataSQLWriteAccess.closeConnection();
        super.finalize();
    }
}
