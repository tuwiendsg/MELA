package at.ac.tuwien.dsg.mela.analysisservice.utils.connectors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import at.ac.tuwien.dsg.mela.dataservice.config.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ActionXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.api.DataServiceActiveMQAPI;
import at.ac.tuwien.dsg.mela.dataservice.utils.Configuration;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class MelaDataServiceConfigurationAPIConnector {

    public MelaDataServiceConfigurationAPIConnector() {
    }

    public static void sendConfiguration(ConfigurationXMLRepresentation configurationXMLRepresentation) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(configurationXMLRepresentation, writer);

            sendMessage(DataServiceActiveMQAPI.SUBMIT_CONFIGURATION_COMMAND, writer.getBuffer().toString());
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.INFO, "Config submitted");
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }
    }

    public static void sendCompositionRules(CompositionRulesConfiguration compositionRulesConfiguration) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(compositionRulesConfiguration, writer);

            sendMessage(DataServiceActiveMQAPI.SUBMIT_COMPOSITION_RULES, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }
    }

    public static void sendRequirements(Requirements requirements) {

        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(Requirements.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(requirements, writer);

            sendMessage(DataServiceActiveMQAPI.SUBMIT_REQUIREMENTS, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }

    }

    public static void sendUpdatedServiceStructure(MonitoredElement serviceConfiguration) {

        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(MonitoredElement.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(serviceConfiguration, writer);

            sendMessage(DataServiceActiveMQAPI.UPDATE_SERVICE_STRUCTURE, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }

    }

    public static void addExecutingAction(String targetEntityID, String actionName) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(ActionXML.class);
            ActionXML action = new ActionXML();
            MonitoredElement element = new MonitoredElement(targetEntityID);
            action.setElement(element);
            action.addAction(actionName);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(action, writer);

            sendMessage(DataServiceActiveMQAPI.ADD_EXECUTING_ACTION, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }

    }

    public static void removeExecutingAction(String targetEntityID, String actionName) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(ActionXML.class);
            ActionXML action = new ActionXML();
            MonitoredElement element = new MonitoredElement(targetEntityID);
            action.setElement(element);
            action.addAction(actionName);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(action, writer);

            sendMessage(DataServiceActiveMQAPI.REMOVE_EXECUTING_ACTION, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }
    }

    public static void sendServiceStructure(MonitoredElement serviceConfiguration) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(MonitoredElement.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(serviceConfiguration, writer);

            sendMessage(DataServiceActiveMQAPI.SET_SERVICE_STRUCTURE, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }
    }

    private static void sendMessage(String key, String value) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(System.getProperty("ActiveMQProtocol", "tcp") + "://"
                + Configuration.getDataServiceIP() + ":" + Configuration.getDataServiceConfigurationPort());

        Connection connection = null;

        do {
            try {
                connection = connectionFactory.createConnection();
                connection.start();
            } catch (JMSException e) {
                Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, "Waiting for MELA-DataService to start");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
            }
        } while (connection == null);

        try {

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Destination destination = session.createQueue("MELADataService.Config");

            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            MapMessage message = session.createMapMessage();
            message.setObject(key, value);

            producer.send(message);
            session.close();
            connection.close();
        } catch (JMSException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }
    }

}
