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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.ClusterInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.MonitoringSystemInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataAccess.DataSourceI;
import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import com.jcraft.jsch.UserInfo;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import org.apache.log4j.Level;
import org.yaml.snakeyaml.Yaml;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
public class RemoteGangliaLiveDataSource implements DataSourceI {

    private Yaml yaml = new Yaml();
    private MonDataSQLWriteAccess dataSQLWriteAccess;

    {
        dataSQLWriteAccess = new MonDataSQLWriteAccess("mela", "mela");
    }

    private byte[] readFile(String file) throws IOException {
        // Open file
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(file);

        try {
            // Get and check length
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } finally {
            is.close();
        }
    }
    Session session;

    private String executeSSH(String userName, String rootIPAddress, String securityCertificatePath, String gangliaPort, String command) throws JSchException {
        if (session == null) {
            JSch jSch = new JSch();

            byte[] prvkey = null;
            try {
                prvkey = readFile(securityCertificatePath);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } // Private key must be byte array
            final byte[] emptyPassPhrase = new byte[0]; // Empty passphrase for now, get real passphrase from MyUserInfo

            jSch.addIdentity(
                    Configuration.getAccessUserName(), // String userName
                    prvkey, // byte[] privateKey
                    null, // byte[] publicKey //maybe generate a public key and try with it
                    emptyPassPhrase // byte[] passPhrase
                    );

            session = jSch.getSession(userName, rootIPAddress, 22);
            session.setConfig("StrictHostKeyChecking", "no"); //         Session session = jSch.getSession("ubuntu", rootIPAddress, 22);

            
            UserInfo ui = new UserInfo() {
                public String getPassphrase() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                public String getPassword() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                public boolean promptPassword(String string) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                public boolean promptPassphrase(String string) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                public boolean promptYesNo(String string) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                public void showMessage(String string) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            }; // MyUserInfo implements UserInfo
            session.setUserInfo(ui);
            session.connect();
        }
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.connect();
        InputStream stdout = null;
        try {
            stdout = channel.getInputStream();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }


        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        String line = null;
        String content = "";

        try {
            while ((line = reader.readLine()) != null) {
                //if ganglia does not respond
                if (line.contains("Unable to connect")) {
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "" + rootIPAddress + " does not respond to monitoring request");
                    return null;
                }
                if (line.contains("<") || line.endsWith("]>")) {
                    content += line + "\n";
                }
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        channel.disconnect();
        return content;

    }

//  String command = "telnet localhost " + gangliaPort;
//
//	String content= "";
//	try {
//		content = execute(rootIPAddress, securityCertificatePath, gangliaPort, command);
//	} catch (JSchException e1) {
//		// TODO Auto-generated catch block
//		e1.printStackTrace();
//	}
//
//	StringReader stringReader = new StringReader(content);
//
    public ClusterInfo getMonitoringData() throws DataAccessException {

        String cmd = "telnet localhost " + Configuration.getGangliaPort();


        String content = "";
        try {
            content = executeSSH(Configuration.getAccessUserName(), Configuration.getAccessMachineIP(), Configuration.getSecurityCertificatePath(), Configuration.getGangliaPort(), cmd);
        } catch (JSchException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        StringReader stringReader = new StringReader(content);
        //String cmd = Configuration.getMonitoringCommand();

        //Process p = Runtime.getRuntime().exec(cmd);
//    	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//        String line = null;
//        String content = "";
//        while ((line = reader.readLine()) != null) {
//
//            //if ganglia does not respond
//            if (line.contains("Unable to connect")) {
//                Configuration.getLogger(this.getClass()).log(Level.WARN, "Unable to execute " + cmd);
//                return null;
//            }
//            if (line.contains("<") || line.endsWith("]>")) {
//                content += line + "\n";
//            }
//        }
//
//        p.getInputStream().close();
//        p.getErrorStream().close();
//        p.getOutputStream().close();
//        p.destroy();
//
//        //if ganglia does not respond
//        if (content == null || content.length() == 0) {
//            Configuration.getLogger(this.getClass()).log(Level.WARN, "" + "Unable to execute " + cmd);
//            return null;
//        }
//
//        StringReader stringReader = new StringReader(content);
        try {
            JAXBContext jc = JAXBContext.newInstance(MonitoringSystemInfo.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            MonitoringSystemInfo info = (MonitoringSystemInfo) unmarshaller.unmarshal(stringReader);
            ClusterInfo gangliaClusterInfo = info.getClusters().iterator().next();
            stringReader.close();

            dataSQLWriteAccess.writeMonitoringData(gangliaClusterInfo);

            return gangliaClusterInfo;
        } catch (Exception e) {
            session = null;
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
