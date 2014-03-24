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
package at.ac.tuwien.dsg.mela.analysisservice.reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */

@Component
public class PerformanceReport {

    private Map<String, List<String>> reportEntries = new HashMap<String, List<String>>();

    public PerformanceReport() {
    }

    public PerformanceReport(String... reportFields) {
        for (String entry : reportFields) {
            reportEntries.put(entry, new ArrayList<String>());
        }
    }

    public void addReportEntry(String key, String value) {
        if (reportEntries.containsKey(key)) {
            reportEntries.get(key).add(value);
        } else {
            Logger.getLogger(PerformanceReport.class).log(Level.WARN, "Key " + key + " not found in report");
        }
    }

    public Map<String, List<String>> getReportEntries() {
        return reportEntries;
    }

    public void writeToCSVFile(String file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(file)));
        Set<String> columns = reportEntries.keySet();

        //write columns
        {
            String columnsLine = "";
            for (String column : columns) {
                columnsLine += column + ",";
            }
            writer.write(columnsLine);
            writer.newLine();
            writer.flush();
        }

        //write values
        {
            int maxIndex = reportEntries.values().iterator().next().size();
            for (int i = 0; i < maxIndex; i++) {
                String valuesLine = "";
                for (String column : columns) {
                    valuesLine += reportEntries.get(column).get(i) + ",";
                }
                writer.write(valuesLine);
                writer.newLine();
                writer.flush();
            }
        }

        writer.flush();
        writer.close();

    }
}
