package at.ac.tuwien.dsg.mela.dataservice.api;

import static at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands.ADD_EXECUTING_ACTION;
import static at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands.REMOVE_EXECUTING_ACTION;
import static at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands.SET_SERVICE_STRUCTURE;
import static at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands.SUBMIT_COMPOSITION_RULES;
import static at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands.SUBMIT_CONFIGURATION_COMMAND;
import static at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands.SUBMIT_REQUIREMENTS;
import static at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands.UPDATE_SERVICE_STRUCTURE;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ActionXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.DataCollectionService;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;

/**
 * Created by omoser on 1/17/14.
 */
@Component
public class CommandConsumer implements MessageListener {

    static final Logger log = LoggerFactory.getLogger(CommandConsumer.class);
  

    @Autowired
    private DataCollectionService collectionService;

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
                        ActionXML actions = (ActionXML) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.addExecutingActions(actions.getActions());
                    } else if (mapMessage.itemExists(REMOVE_EXECUTING_ACTION)) {
                        String cfg = (String) mapMessage.getObject(REMOVE_EXECUTING_ACTION);

                        JAXBContext jAXBContext = JAXBContext.newInstance(ActionXML.class);
                        ActionXML actions = (ActionXML) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.removeExecutingActions(actions.getActions());
                    } else if (mapMessage.itemExists(SUBMIT_REQUIREMENTS)) {
                        String cfg = (String) mapMessage.getObject(SUBMIT_REQUIREMENTS);

                        JAXBContext jAXBContext = JAXBContext.newInstance(Requirements.class);
                        Requirements requirements = (Requirements) jAXBContext.createUnmarshaller().unmarshal(new StringReader(cfg));

                        collectionService.setRequirements(requirements);
                    }

                } catch (JAXBException ex) {
                    log.error( ex.getMessage(), ex);
                } catch (JMSException ex) {
                     log.error( ex.getMessage(), ex);
                }
            } else {
                System.out.println("Unrecognized message: " + message);
            }

        }
}
