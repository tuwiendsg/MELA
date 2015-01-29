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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractPushDataSource;
import at.ac.tuwien.dsg.mela.dataservice.DataCollectionService;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.MetricCollectionConsumer;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects.NumericalCollectedMetricValue;
import at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.MetricAccuracyAnalysis;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidationTest;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidator;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 *
 * The purpose of this class is to help in asynhcronous collection of monitored
 * metrics.
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class QueueBasedDataSource extends AbstractPushDataSource {

    private String brokerURL = "tcp://localhost:9124";

    private String QUEUE_NAME = "metrics_queue";

    private Session session;
    private javax.jms.Connection connection = null;

    private MessageConsumer consumer;

    //array of tests to be perform to determine value validity
    private MetricValidator metricValidator;

    private MetricAccuracyAnalysis accuracyAnalysis;

    //where to put the value
    private Logger logger;

    @Autowired
    private ApplicationContext context;

    private long rateAtWhichDataShouldBeRead = 1000;

    @Override
    public Long getRateAtWhichDataShouldBeRead() {
        return rateAtWhichDataShouldBeRead;
    }

    {
        logger = LoggerFactory.getLogger(QueueBasedDataSource.class);
    }

    {
        metricValidator = new MetricValidator();
    }

    {
        accuracyAnalysis = new MetricAccuracyAnalysis() {

            @Override
            public Double computeAccuracy(NumericalCollectedMetricValue metricValue) {
                return 100.0d;
            }
        };
    }

    @PostConstruct
    public void init() {
        startListeningToData();
    }

    @PreDestroy
    public void destroy() {
        stopListeningToData();
    }

    @Override
    public void startListeningToData() {
        //as monitoring data is collected asynhcronously, 
        //we use a message queue to put the collected processed metrics
        if (initiateMetricsQueue()) {
            //add metric validators 
            Map<String, MetricValidationTest> metricValidationTests = context.getBeansOfType(MetricValidationTest.class);

            for (String testName : metricValidationTests.keySet()) {
                MetricValidationTest test = metricValidationTests.get(testName);
                logger.debug("Found MetricValidationTest '{}': {}", testName, test);
                metricValidator.addValidationTest(test);
            }

            try {
                consumer.setMessageListener(new MetricCollectionConsumer(metricValidator, accuracyAnalysis, freshestData));

            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        } else {
            logger.error("Could not get connection to queue " + QUEUE_NAME + " at broker" + brokerURL);
        }

    }

    @Override
    public void stopListeningToData() {
        try {
            consumer.close();
        } catch (JMSException ex) {
            logger.error(ex.getMessage(), ex);
        }
        try {
            session.close();
        } catch (JMSException ex) {
            java.util.logging.Logger.getLogger(QueueBasedDataSource.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private boolean initiateMetricsQueue() {
        ActiveMQConnectionFactory connectionFactory;

        try {
            connectionFactory = new ActiveMQConnectionFactory(brokerURL);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        ;

        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }
        Destination destination;
        try {
            destination = session.createQueue(QUEUE_NAME);
            logger.info("Created destination " + destination.toString());
        } catch (JMSException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }

        try {
            consumer = session.createConsumer(destination);
            logger.info("Created consumer " + consumer.toString());
        } catch (JMSException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }

        return true;

    }

    public String getBrokerURL() {
        return brokerURL;
    }

    public String getQUEUE_NAME() {
        return QUEUE_NAME;
    }

    public void setBrokerURL(String brokerURL) {
        this.brokerURL = brokerURL;
    }

    public void setQUEUE_NAME(String QUEUE_NAME) {
        this.QUEUE_NAME = QUEUE_NAME;
    }

    public void setRateAtWhichDataShouldBeRead(long rateAtWhichDataShouldBeRead) {
        this.rateAtWhichDataShouldBeRead = rateAtWhichDataShouldBeRead;
    }

}
