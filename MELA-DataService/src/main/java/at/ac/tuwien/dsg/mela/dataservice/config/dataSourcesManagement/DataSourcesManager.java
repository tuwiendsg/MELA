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
package at.ac.tuwien.dsg.mela.dataservice.config.dataSourcesManagement;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import java.io.FileInputStream;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class DataSourcesManager {

    public static DataSourceConfigs readDataSourcesConfiguration() {
        DataSourceConfigs configs = new DataSourceConfigs();

        try {
            JAXBContext context = JAXBContext
                    .newInstance(DataSourceConfigs.class);
            InputStream dataSourcesStream = new FileInputStream("./dataServiceConfig/dataSources.conf");

            Object unmarshalled = context.createUnmarshaller().unmarshal(
                    dataSourcesStream);

            configs = (DataSourceConfigs) unmarshalled;
            dataSourcesStream.close();
        } catch (Exception e) {

            Logger.getLogger(DataSourcesManager.class)
                    .log(Level.ERROR, null, e);
            return configs;
        }

        return configs;
    }
}
