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
package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects.NumericalCollectedMetricValue;
import at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.MetricAccuracyAnalysis;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
//public class MetricCollectionConsumer extends DefaultConsumer {
//
//    private Channel channel;
//
//    //array of tests to be perform to determine value validity
//    private MetricValidator metricValidator;
//
//    private MetricAccuracyAnalysis accuracyAnalysis;
//
//    //where to put the value
//    private MonitoringData currentMonitoringSnapshot;
//
//    private ExecutorService executorService;
//
//    {
//        executorService = Executors.newCachedThreadPool();
//    }
//
//    public MetricCollectionConsumer(int prefetch, Channel channel,
//            String queue, MetricValidator metricValidator, MetricAccuracyAnalysis accuracyAnalysis,
//            MonitoringData currentMonitoringSnapshot) throws Exception {
//        super(channel);
//        this.metricValidator = metricValidator;
//        this.accuracyAnalysis = accuracyAnalysis;
//        this.currentMonitoringSnapshot = currentMonitoringSnapshot;
//        channel.basicQos(prefetch);
//        channel.basicConsume(queue, false, this);
//    }
//
//    @Override
//    public void handleDelivery(String consumerTag,
//            Envelope envelope,
//            AMQP.BasicProperties properties,
//            byte[] body) throws IOException {
//
//        try {
////            CollectedMetricValue collectedMetricValue;
////            ByteArrayInputStream bis = new ByteArrayInputStream(body);
////            ObjectInput in = new ObjectInputStream(bis);
////            collectedMetricValue = (CollectedMetricValue) in.readObject();
//            NumericalCollectedMetricValue collectedMetricValue = (NumericalCollectedMetricValue) SerializationUtils.deserialize(body);
//
//            //task to process metric goes here
//            executorService.submit(new CollectedMetricProcessor(collectedMetricValue, metricValidator, accuracyAnalysis, currentMonitoringSnapshot));
//        } catch (Exception ex) {
//            Logger.getLogger(MetricCollectionConsumer.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//}
public class MetricCollectionConsumer implements MessageListener {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(MetricCollectionConsumer.class);

    //array of tests to be perform to determine value validity
    private MetricValidator metricValidator;

    private MetricAccuracyAnalysis accuracyAnalysis;

    //where to put the value
    private MonitoringData currentMonitoringSnapshot;

    private ExecutorService executorService;

    {
        executorService = Executors.newCachedThreadPool();
    }

    public MetricCollectionConsumer(MetricValidator metricValidator, MetricAccuracyAnalysis accuracyAnalysis,
            MonitoringData currentMonitoringSnapshot) throws Exception {
        this.metricValidator = metricValidator;
        this.accuracyAnalysis = accuracyAnalysis;
        this.currentMonitoringSnapshot = currentMonitoringSnapshot;
    }

    public void onMessage(Message message) {

        if (message instanceof ObjectMessage) {

            try {

                ObjectMessage objectMessage = (ObjectMessage) message;
                NumericalCollectedMetricValue collectedMetricValue = (NumericalCollectedMetricValue) objectMessage.getObject();;
                //task to process metric goes here
                executorService.submit(new CollectedMetricProcessor(collectedMetricValue, metricValidator, accuracyAnalysis, currentMonitoringSnapshot));
            } catch (JMSException ex) {
                log.error(ex.getMessage(), ex);
            }
        } else {
            log.error("Unrecognized message: " + message);
        }

    }
}
