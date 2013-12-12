/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
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
package performance;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.ClusterInfo;
import at.ac.tuwien.dsg.mela.dataservice.MonDataSQLWriteAccess;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class LoadMonitoredDataInSQL {

    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws JAXBException, FileNotFoundException, SQLException {
        Yaml yaml = new Yaml();
        Iterable<Object> objects = yaml.loadAll(new FileReader(new File("/home/daniel-tuwien/Documents/DSG_SVN/papers/IJBDI_cloud_com_extended/figures/experiments/MELA/monitoringSat_Jun_29_20_01_04_CEST_2013")));
//        List<ClusterInfo> clusterInfos = new ArrayList<ClusterInfo>();
        MonDataSQLWriteAccess access = new MonDataSQLWriteAccess("mela", "mela");
        
        for (Object o : objects) {
//            clusterInfos.add((ClusterInfo) o);
            access.writeMonitoringData((ClusterInfo) o);
//            System.out.println((ClusterInfo) o);
        }
        
        
        access.closeConnection();
        
        

    }
}
