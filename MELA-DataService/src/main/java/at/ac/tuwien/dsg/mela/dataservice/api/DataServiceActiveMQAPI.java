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
package at.ac.tuwien.dsg.mela.dataservice.api;

import java.io.StringReader;
import org.apache.log4j.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ActionXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.DataCollectionService;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;
import org.apache.log4j.Level;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
public class DataServiceActiveMQAPI implements Runnable {

    private DataCollectionService collectionService;

    public DataServiceActiveMQAPI(DataCollectionService collectionService) {
        super();
        this.collectionService = collectionService;
    }
    public static final String SUBMIT_CONFIGURATION_COMMAND = "SubmitConfig";
    public static final String SUBMIT_COMPOSITION_RULES = "SubmitCompositionRules";
    public static final String SUBMIT_REQUIREMENTS = "SubmitRequirements";
    public static final String UPDATE_SERVICE_STRUCTURE = "UpdateServiceStructure";
    public static final String SET_SERVICE_STRUCTURE = "SetServiceStructure";
    public static final String ADD_EXECUTING_ACTION = "AddExecutingAction";
    public static final String REMOVE_EXECUTING_ACTION = "RemoveExecutingAction";
    private BrokerService broker;

    public class CommandConsumer implements MessageListener {

        public synchronized void onException(JMSException ex) {
            System.out.println("JMS Exception occured.  Shutting down client.");
        }

        public void onMessage(Message message) {

            if (message instanceof MapMessage) {

                try {

                    MapMessage mapMessage = (MapMessage) message;
                    if (mapMessage.itemExists(SUBMIT_CONFIGURATION_COMMAND)) {
                        String cfg = (String) mapMessage.getObject(SUBMIT_CONFIGURATION_COMMAND);

                        JAXBContext jAXBContext = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);
                        ConfigurationXMLRepresentation repr = (ConfigurationXMLRepresentation) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.setConfiguration(repr);

                    } else if (mapMessage.itemExists(SUBMIT_COMPOSITION_RULES)) {
                        String cfg = (String) mapMessage.getObject(SUBMIT_COMPOSITION_RULES);

                        JAXBContext jAXBContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);
                        CompositionRulesConfiguration repr = (CompositionRulesConfiguration) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.setCompositionRulesConfiguration(repr);
                    }else if (mapMessage.itemExists(SET_SERVICE_STRUCTURE)) {
                        String cfg = (String) mapMessage.getObject(SET_SERVICE_STRUCTURE);

                        JAXBContext jAXBContext = JAXBContext.newInstance(MonitoredElement.class);
                        MonitoredElement repr = (MonitoredElement) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.setServiceConfiguration(repr);
                    }else if (mapMessage.itemExists(UPDATE_SERVICE_STRUCTURE)) {
                        String cfg = (String) mapMessage.getObject(UPDATE_SERVICE_STRUCTURE);

                        JAXBContext jAXBContext = JAXBContext.newInstance(MonitoredElement.class);
                        MonitoredElement repr = (MonitoredElement) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.updateServiceConfiguration(repr);
                    } else if (mapMessage.itemExists(ADD_EXECUTING_ACTION)) {
                        String cfg = (String) mapMessage.getObject(ADD_EXECUTING_ACTION);

                        JAXBContext jAXBContext = JAXBContext.newInstance(ActionXML.class);
                        ActionXML action = (ActionXML) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.addExecutingAction(action.getElement().getId(), action.getActions());
                    } else if (mapMessage.itemExists(REMOVE_EXECUTING_ACTION)) {
                        String cfg = (String) mapMessage.getObject(REMOVE_EXECUTING_ACTION);

                        JAXBContext jAXBContext = JAXBContext.newInstance(ActionXML.class);
                        ActionXML action = (ActionXML) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.removeExecutingAction(action.getElement().getId(), action.getActions());
                    } else if (mapMessage.itemExists(SUBMIT_REQUIREMENTS)) {
                        String cfg = (String) mapMessage.getObject(SUBMIT_REQUIREMENTS);

                        JAXBContext jAXBContext = JAXBContext.newInstance(Requirements.class);
                        Requirements requirements = (Requirements) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.setRequirements(requirements);
                    }

                } catch (JAXBException ex) {
                    Logger.getLogger(DataServiceActiveMQAPI.class.getName()).log(Level.ERROR, null, ex);
                } catch (JMSException ex) {
                    Logger.getLogger(CommandConsumer.class.getName()).log(Level.ERROR, null, ex);
                }
            } else {
                System.out.println("Unrecognized message: " + message);
            }

        }
    }

    public void run() {
        try {
            broker = new BrokerService();
            broker.setUseJmx(true);
            try {
                broker.addConnector(System.getProperty("ActiveMQProtocol", "tcp") + "://" + Configuration.getDataServiceIP() + ":"
                        + Configuration.getDataServiceConfigurationPort());
                broker.start();
            } catch (Exception e) {
                Logger.getLogger(CommandConsumer.class.getName()).log(Level.ERROR, null, e);
            }

            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(System.getProperty("ActiveMQProtocol", "tcp") + "://" + Configuration.getDataServiceIP()
                    + ":" + Configuration.getDataServiceConfigurationPort());

            // Create a Connection
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue("MELADataService.Config");

            // Create a MessageConsumer from the Session to the Topic or
            // Queue
            MessageConsumer consumer = session.createConsumer(destination);
            consumer.setMessageListener(new CommandConsumer());
        } catch (JMSException ex) {
            Logger.getLogger(DataServiceActiveMQAPI.class.getName()).log(Level.ERROR, null, ex);
        }

    }

    @Override
    protected void finalize() throws Throwable {
        broker.stop();
        super.finalize();
    }
}
