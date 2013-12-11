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

import at.ac.tuwien.dsg.mela.common.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
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

    public static void sendConfiguration(
            ConfigurationXMLRepresentation configurationXMLRepresentation)
            throws JMSException {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(ConfigurationXMLRepresentation.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(configurationXMLRepresentation, writer);

            sendConfigMessage(DataServiceActiveMQAPI.SUBMIT_CONFIGURATION_COMMAND,
                    writer.getBuffer().toString());
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.INFO,"Config submitted");
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }
    }

    public static void sendCompositionRules(CompositionRulesConfiguration compositionRulesConfiguration)
            throws JMSException {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(compositionRulesConfiguration, writer);


            sendConfigMessage(DataServiceActiveMQAPI.SUBMIT_COMPOSITION_RULES, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }
    }

    public static void sendRequirements(Requirements requirements)
            throws JMSException {

        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(Requirements.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(requirements, writer);


            sendConfigMessage(DataServiceActiveMQAPI.SUBMIT_REQUIREMENTS, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }


    }

    public static void sendUpdatedServiceStructure(MonitoredElement serviceConfiguration) throws JMSException {

        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(MonitoredElement.class);

            StringWriter writer = new StringWriter();
            jAXBContext.createMarshaller().marshal(serviceConfiguration, writer);


            sendConfigMessage(DataServiceActiveMQAPI.UPDATE_SERVICE_STRUCTURE, writer.getBuffer().toString());
        } catch (JAXBException ex) {
            Logger.getLogger(MelaDataServiceConfigurationAPIConnector.class.getName()).log(Level.ERROR, null, ex);
        }

    }

    private static void sendConfigMessage(String key, String value) throws JMSException {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                System.getProperty("ActiveMQProtocol", "tcp") + "://"
                + Configuration.getDataServiceIP() + ":"
                + Configuration.getDataServiceConfigurationPort());

        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session session = connection.createSession(false,
                Session.AUTO_ACKNOWLEDGE);

        Destination destination = session.createQueue("MELADataService.Config");

        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        MapMessage message = session.createMapMessage();
        message.setObject(key, value);

        producer.send(message);
        session.close();
        connection.close();
    }
}
