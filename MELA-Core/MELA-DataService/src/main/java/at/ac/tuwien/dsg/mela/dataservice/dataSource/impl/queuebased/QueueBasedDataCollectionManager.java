/*
 *  Copyright 2015 Technische Universitat Wien (TUW), Distributed Systems Group E184
 * 
 *  This work was partially supported by the European Commission in terms of the 
 *  CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metrics;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.MetricCollectionConsumer;
import at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.MetricAccuracyAnalysis;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidator;
import com.rabbitmq.client.Channel;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 *
 * The purpose of this class is to help in asynhcronous collection of monitored
 * metrics.
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class QueueBasedDataCollectionManager {

    // Map< MonitoredElementLevel, Map<MonitoredElementID, List<Metrics>>
    private Map<String, Map<String, List<Metric>>> metricsToCollect;

    private final String DEFAULT_MONITORED_ELEMENT_ID = "";

    private String brokerURL = "tcp://localhost:9124";

    private String QUEUE_NAME = "metrics_queeu_";

    private Channel channel;

    //array of tests to be perform to determine value validity
    private MetricValidator metricValidator;

    private MetricAccuracyAnalysis accuracyAnalysis;

    //where to put the value
    private ServiceMonitoringSnapshot currentMonitoringSnapshot;

    private Logger logger;

    {
        QUEUE_NAME += UUID.randomUUID();
    }

    {
        metricsToCollect = new HashMap<>();
    }

    {
        logger = Logger.getLogger(QueueBasedDataCollectionManager.class.getName());
    }

    /**
     * Each metric has its level and monitored element ID, so based on this
     * information it must be collected using specific monitoring data
     * collection things (not implemented yet)
     *
     * @param metrics
     */
    public void specifyMetricsToCollect(Metrics metrics) {
        for (Metric m : metrics.getMetrics()) {
            Map<String, List<Metric>> metricsToCollectForLevel = null;

            if (metricsToCollect.containsKey(m.getMonitoredElementLevel())) {
                metricsToCollectForLevel = metricsToCollect.get(m.getMonitoredElementLevel());
            } else {
                metricsToCollectForLevel = new HashMap<>();
                metricsToCollect.put(m.getMonitoredElementLevel(), metricsToCollectForLevel);
            }

            //if no ID => metrics to be collected for ALL instances from same LEVEL
            //so we use default ID
            String monitoredElementID = (m.getMonitoredElementID() == null || m.getMonitoredElementID().length() == 0)
                    ? DEFAULT_MONITORED_ELEMENT_ID : m.getMonitoredElementID();

            List<Metric> metricsToCollectForLevelAndID = null;

            if (metricsToCollectForLevel.containsKey(monitoredElementID)) {
                metricsToCollectForLevelAndID = metricsToCollectForLevel.get(monitoredElementID);
            } else {
                metricsToCollectForLevelAndID = new ArrayList<>();
                metricsToCollectForLevel.put(monitoredElementID, metricsToCollectForLevelAndID);
            }

            metricsToCollectForLevelAndID.add(m);

        }
    }

    /**
     * 
     * @return where the processed metrics will be stored
     */
    public ServiceMonitoringSnapshot startMetricCollection() {
        //as monitoring data is collected asynhcronously, 
        //we use a message queue to put the collected processed metrics
        initiateMetricsQueue();
        try {
            //our consumer VALIDATES the metric, and then stores it in the currentMonitoringSnapshot
            channel.basicConsume(QUEUE_NAME, true, new MetricCollectionConsumer(100, channel, QUEUE_NAME, metricValidator, accuracyAnalysis, currentMonitoringSnapshot));
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        
        return currentMonitoringSnapshot;
    }

    public void stopMetricCollection() {
        try {
            channel.abort();
            channel.close();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }

    }

    private boolean initiateMetricsQueue() {
        ConnectionFactory factory = new ConnectionFactory();
        Connection conn = null;

        try {
            factory.setUri(brokerURL);
        } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        try {
            conn = factory.newConnection();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        ;
        try {
            channel = conn.createChannel();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        try {
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        return true;

    }

    private void stopQueue() {
        ConnectionFactory factory = new ConnectionFactory();
        Connection conn = null;

        try {
            factory.setUri(brokerURL);
        } catch (URISyntaxException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        } catch (NoSuchAlgorithmException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        } catch (KeyManagementException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }
        try {
            conn = factory.newConnection();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }
        Channel channel;
        try {
            channel = conn.createChannel();
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return;
        }
        try {
            channel.queueDelete(QUEUE_NAME);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        }
    }
}
