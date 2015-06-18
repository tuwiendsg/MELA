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
package at.ac.tuwien.dsg.quelle.cloudServicesModel.util.writers;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class CloudProviderYAMLWriter {

    private CloudProviderYAMLWriter() {
    }

    
    
    public static void writeYAML(CloudProvider cloudProvider, String file) throws IOException {
         
        Yaml yaml = new Yaml();
        String output = yaml.dump(cloudProvider);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(output);
        writer.flush();
        writer.close();
    }
}