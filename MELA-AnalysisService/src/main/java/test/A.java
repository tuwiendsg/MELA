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
package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.yaml.snakeyaml.reader.StreamReader;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class A {

    public static void main(String[] args) throws IOException {
        URL url = new URL("http://83.212.117.112/MELA-AnalysisService-0.1-SNAPSHOT/REST_WS/monitoringdataXML");
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Accept", "application/xml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String l = "";
        while ((l = reader.readLine()) != null) {
            System.out.println(l);
        }
    }
}
