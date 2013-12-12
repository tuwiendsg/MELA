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
package at.ac.tuwien.dsg.mela.dataservice.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 * Class which acts as entry point towards all configuration options that need
 * to be added to the MELA-DataService
 */
public class Configuration {

    private static final Properties configuration = new Properties();
    static Logger logger;

    static {

        String date = new Date().toString();
        date = date.replace(" ", "_");
        date = date.replace(":", "_");
        System.getProperties().put("recording_date", date);

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//            ClassLoader classLoader = Configuration.class.getClassLoader();
            InputStream propertiesStream = Configuration.class.getResourceAsStream("/dataServiceConfig/Config.properties");

            configuration.load(propertiesStream);

            InputStream log4jStream = Configuration.class.getResourceAsStream("/dataServiceConfig/Log4j.properties");

            if (log4jStream != null) {
                PropertyConfigurator.configure(log4jStream);
                logger = Logger.getLogger("rootLogger");
            } else {
                logger = Logger.getLogger("rootLogger");
            }


        } catch (Exception ex) {
            ex.printStackTrace();
            logger = Logger.getLogger("rootLogger");
        }

    }
//
//    private static PrintStream createOutLoggingProxy(final PrintStream realPrintStream, final Logger l) {
//        return new PrintStream(realPrintStream) {
//            public void print(final String string) {
//                l.info(string);
//            }
//        };
//    }
//
//    private static PrintStream createErrLoggingProxy(final PrintStream realPrintStream, final Logger l) {
//        return new PrintStream(realPrintStream) {
//            public void print(final String string) {
//                l.info(string);
//            }
//        };
//    }
    
    public static int getDataPoolingInterval() {
        if (configuration.containsKey("DATA_COLLECTION_INTERVAL_IN_SECONDS")) {
            return Integer.parseInt(configuration.getProperty("DATA_COLLECTION_INTERVAL_IN_SECONDS"));
        } else {
            return 5; //default 5 seconds
        }
    }

    public static int getDataAggregationWindows() {
        if (configuration.containsKey("DATA_AGGREGATION_WINDOWS")) {
            return Integer.parseInt(configuration.getProperty("DATA_AGGREGATION_WINDOWS"));
        } else {
            return 2; //default 2 frames
        }
    }
    
    public static void setProperty(String property, Object value){
    	configuration.put(property, value);
    }

    public static String getSecurityCertificatePath() {
        return configuration.getProperty("PEM_CERT_PATH");
    }

    public static String getGangliaPort() {
        return configuration.getProperty("DATA_SOURCE.PORT");
    }

    public static String getAccessMachineIP() {
        return configuration.getProperty("DATA_SOURCE.IP");
    }

    public static String getMonitoredElementIDMetricName() {
        return "serviceUnitID";
    }

    public static Logger getLogger(Class loggerClass) {
        return Logger.getLogger(loggerClass);
    }

    public static String getAccessUserName() {
        return configuration.getProperty("ACCESS_MACHINE_USER_NAME");
    }

    public static String getMonitoringDataAccessMethod() {
        return configuration.getProperty("DATA_SOURCE.TYPE");
    }

    public static String getStoredMonitoringSequenceID() {
        return configuration.getProperty("MONITORING_SEQ_ID");
    }
//
//    public static int getDataServicePort() {
//        if (configuration.containsKey("MELA_DATA_SERVICE.PORT")) {
//            return Integer.parseInt(configuration.getProperty("MELA_DATA_SERVICE.PORT"));
//        } else {
//            return 9123; //default 2 frames
//        }
//    }

    public static String getDataServiceIP() {
        if (configuration.containsKey("MELA_DATA_SERVICE.IP")) {
            return configuration.getProperty("MELA_DATA_SERVICE.IP");
        } else {
            return "localhost";
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

	public static boolean automatedStructureDetection() {
		if (configuration.containsKey("SERVICE_STRUCTURE_DETECTION")) {
            return configuration.getProperty("SERVICE_STRUCTURE_DETECTION").equalsIgnoreCase("AUTOMATIC");
        } else {
            return false;
        }
	}
}
