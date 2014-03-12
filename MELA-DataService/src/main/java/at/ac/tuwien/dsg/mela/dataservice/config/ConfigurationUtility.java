package at.ac.tuwien.dsg.mela.dataservice.config;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import java.io.InputStream;

/**
 * Created by omoser on 1/24/14.
 */

@Component
public class ConfigurationUtility {

    static final Logger log = LoggerFactory.getLogger(ConfigurationUtility.class);

    @Autowired
    ApplicationContext context;

    @Value("${MELA_CONFIG_DIR}")
    private String configDir;

    public ConfigurationXMLRepresentation createDefaultConfiguration() {
        ConfigurationXMLRepresentation configurationXMLRepresentation = new ConfigurationXMLRepresentation();

        // create service with 1 topology and 1 service unit having * (all) VMs
        MonitoredElement service = new MonitoredElement()
                .withId("Service")
                .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        MonitoredElement topology = new MonitoredElement()
                .withId("ServiceTopology")
                .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);

        service.addElement(topology);

        MonitoredElement serviceUnit = new MonitoredElement()
                .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                .withId("ServiceUnit");

        topology.addElement(serviceUnit);

        service = unmarshalFragment(MonitoredElement.class, "file://" + configDir + "/default/structure.xml");
        CompositionRulesConfiguration compositionRulesConfiguration = unmarshalFragment(CompositionRulesConfiguration.class, "file://" + configDir + "/default/compositionRules.xml");
        Requirements requirements = unmarshalFragment(Requirements.class, "file://" + configDir + "/default/requirements.xml");

        configurationXMLRepresentation.setServiceConfiguration(service);
        configurationXMLRepresentation.setCompositionRulesConfiguration(compositionRulesConfiguration);
        configurationXMLRepresentation.setRequirements(requirements);

        return configurationXMLRepresentation;
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshalFragment(Class<T> fragmentType, String filename) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(fragmentType);
            InputStream fileStream = context.getResource(filename).getInputStream();
            return (T) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        } catch (Exception ex) {
            log.error("Cannot unmarshall ServiceStructure: {}", ex.getMessage());
            // todo shouldn't we throw an exception in this case?
            return null;
        }

    }
}
