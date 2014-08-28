/*
 * Copyright 2014 daniel-tuwien.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.utils;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class Configuration {

    private static final Properties configuration = new Properties();

    static {

        String date = new Date().toString();
        date = date.replace(" ", "_");
        date = date.replace(":", "_");
        System.getProperties().put("recording_date", date);

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            // ClassLoader classLoader = Configuration.class.getClassLoader();
            InputStream stream = ResourceLoader.getPropertiesStream();
            configuration.load(stream);
            stream.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    //
    // private static PrintStream createOutLoggingProxy(final PrintStream
    // realPrintStream, final Logger l) {
    // return new PrintStream(realPrintStream) {
    // public void print(final String string) {
    // l.info(string);
    // }
    // };
    // }
    //
    // private static PrintStream createErrLoggingProxy(final PrintStream
    // realPrintStream, final Logger l) {
    // return new PrintStream(realPrintStream) {
    // public void print(final String string) {
    // l.info(string);
    // }
    // };
    // }
    public static int getDataPoolingInterval() {
        if (configuration.containsKey("DATA_COLLECTION_INTERVAL_IN_SECONDS")) {
            return Integer.parseInt(configuration.getProperty("DATA_COLLECTION_INTERVAL_IN_SECONDS"));
        } else {
            return 5; // default 5 seconds
        }
    }

    public static int getDataAggregationWindows() {
        if (configuration.containsKey("DATA_AGGREGATION_WINDOWS")) {
            return Integer.parseInt(configuration.getProperty("DATA_AGGREGATION_WINDOWS"));
        } else {
            return 2; // default 2 frames
        }
    }

    public static void setProperty(String property, Object value) {
        configuration.put(property, value);
    }

    public static String getSecurityCertificatePath() {
        return configuration.getProperty("PEM_CERT_PATH");
    }

    // public static String getGangliaPort() {
    // return configuration.getProperty("DATA_SOURCE.PORT");
    // }
    //
    // public static String getAccessMachineIP() {
    // return configuration.getProperty("DATA_SOURCE.IP");
    // }
    public static String getMonitoredElementIDMetricName() {
        return "serviceUnitID";
    }

    public static String getAccessUserName() {
        return configuration.getProperty("ACCESS_MACHINE_USER_NAME");
    }

    public static String getDefaultMonitoringDataAccessMethod() {
        return configuration.getProperty("DATA_SOURCE.TYPE");
    }

    public static String getStoredMonitoringSequenceID() {
        return configuration.getProperty("MONITORING_SEQ_ID");
    }

    //
    // public static int getDataServicePort() {
    // if (configuration.containsKey("MELA_DATA_SERVICE.PORT")) {
    // return
    // Integer.parseInt(configuration.getProperty("MELA_DATA_SERVICE.PORT"));
    // } else {
    // return 9123; //default 2 frames
    // }
    // }
    public static String getDataServiceIP() {
        if (configuration.containsKey("MELA_DATA_SERVICE.IP")) {
            return configuration.getProperty("MELA_DATA_SERVICE.IP");
        } else {
            return "localhost";
        }
    }

    public static String getOperationMode() {
        if (configuration.containsKey("OPEATION_MODE")) {
            return configuration.getProperty("OPEATION_MODE");
        } else {
            return "monitoring";
        }
    }

    public static int getDataServicePort() {
        if (configuration.containsKey("MELA_DATA_SERVICE.DATA_PORT")) {
            return Integer.parseInt(configuration.getProperty("MELA_DATA_SERVICE.DATA_PORT"));
        } else {
            return 9123;
        }
    }

    public static int getDataServiceConfigurationPort() {
        if (configuration.containsKey("MELA_DATA_SERVICE.CONFIGURATION_PORT")) {
            return Integer.parseInt(configuration.getProperty("MELA_DATA_SERVICE.CONFIGURATION_PORT"));
        } else {
            return 9124;
        }
    }

    public static Object getValue(String key) {
        if (configuration.containsKey(key)) {
            return configuration.getProperty(key);
        } else {
            return null;
        }
    }

    public static String getDatabaseFileLocation() {
        if (configuration.containsKey("DATA_BASE_LOCATION_PATH")) {
            return configuration.getProperty("DATA_BASE_LOCATION_PATH");
        } else {
            return ".";
        }
    }

    /**
     * Assumes it returns " manual | auto | guided" Manual means no structure
     * detection. Auto means auto-structure detection in 1 Service Unit 1
     * Service Topology Guided means that a metric indicating Service Unit is
     * present in monitored data
     *
     * If starting with Auto/Guided but someone invokes "setServiceStructure" on
     * mela AnalysysService, the data access mechanism will be changed to manual
     *
     * @return
     */
    public static String getServiceStructureDetectionMechanism() {
        if (configuration.containsKey("SERVICE_STRUCTURE_DETECTION")) {
            return configuration.getProperty("SERVICE_STRUCTURE_DETECTION");
        } else {
            return "auto";
        }
    }

    public static boolean continuousOperation() {
        if (configuration.containsKey("OPERATION_MODE")) {
            return configuration.getProperty("OPERATION_MODE").equalsIgnoreCase("continuous");
        } else {
            return false;
        }
    }
}
