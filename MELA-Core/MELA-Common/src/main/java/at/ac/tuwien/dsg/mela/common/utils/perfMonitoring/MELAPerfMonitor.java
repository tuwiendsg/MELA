/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.common.utils.perfMonitoring;

import org.slf4j.Logger;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class MELAPerfMonitor {

    public static void logMemoryUsage(Logger logger) {
        int mb = 1024 * 1024;

        // get Runtime instance
        Runtime instance = Runtime.getRuntime();

        // available memory
        logger.info("Total Memory: " + instance.totalMemory() / mb);

        // free memory
        logger.info("Free Memory: " + instance.freeMemory() / mb);

        // used memory
        logger.info("Used Memory: "
                + (instance.totalMemory() - instance.freeMemory()) / mb);

        // Maximum available memory
        logger.info("Max Memory: " + instance.maxMemory() / mb);
    }
}
