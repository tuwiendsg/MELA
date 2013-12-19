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
package at.ac.tuwien.dsg.mela.dataservice.utils;

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 * opens input streams to configuration files
 */
public class ResourceLoader {

    private ResourceLoader() {
    }

    public static InputStream getDataServicePropertiesStream() throws FileNotFoundException {
        return new FileInputStream("./dataServiceConfig/Config.properties");
    }

    public static InputStream getDefaultServiceStructureStream() throws FileNotFoundException {
        return new FileInputStream("./dataServiceConfig/default/structure.xml");
    }

    public static InputStream getDefaultMetricCompositionRulesStream() throws FileNotFoundException, FileNotFoundException {
        return new FileInputStream("./dataServiceConfig/default/compositionRules.xml");
    }

    public static InputStream getDefaultRequirementsStream() throws FileNotFoundException {
        return new FileInputStream("./dataServiceConfig/default/requirements.xml");
    }
    
    public static InputStream getLog4JConfigurationStream() throws FileNotFoundException {
        return new FileInputStream("./dataServiceConfig/Log4j.properties");
    }
    
    
    
}
