package at.ac.tuwien.dsg.mela.costeval.reporting;

import at.ac.tuwien.dsg.mela.common.configuration.ConfigurationCommands;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ActionXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Action;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngine;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringWriter;
import javax.jms.ObjectMessage;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects.NumericalCollectedMetricValue;

@Component
public class CostReportingAsMetricsToMelaDataService {

    static final Logger log = LoggerFactory.getLogger(CostReportingAsMetricsToMelaDataService.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    public void sendElementCostMetrics(ServiceMonitoringSnapshot costEnrichedSnapshot) {
        for (MonitoredElement.MonitoredElementLevel level : costEnrichedSnapshot.getMonitoredData().keySet()) {
            for (MonitoredElementMonitoringSnapshot snapshot : costEnrichedSnapshot.getMonitoredData().get(level).values()) {
                //do not send also cost of used cloud offered services, as that is not currently usefull for mela data service
                if (!snapshot.getMonitoredElement().getLevel().equals(MonitoredElement.MonitoredElementLevel.CLOUD_OFFERED_SERVICE)) {
                    if (snapshot.containsMetric(CostEvalEngine.ELEMENT_COST_METRIC)) {
                        MetricValue value = snapshot.getMetricValue(CostEvalEngine.ELEMENT_COST_METRIC);
                        NumericalCollectedMetricValue costMetric = new NumericalCollectedMetricValue()
                                .withName("cost")
                                .withType(CostEvalEngine.ELEMENT_COST_METRIC.getType().toString())
                                .withUnits(CostEvalEngine.ELEMENT_COST_METRIC.getMeasurementUnit())
                                .withValue(Double.parseDouble(value.getValueRepresentation())) //double
                                .withMonitoredElementLevel(snapshot.getMonitoredElement().getLevel().toString())
                                .withMonitoredElementID(snapshot.getMonitoredElement().getId())
                                .withTimeSinceCollection(0l);
                        sendMessage(costMetric);

                    }
                }
            }
        }
    }

    private void sendMessage(final NumericalCollectedMetricValue costMetric) {
        jmsTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                ObjectMessage message = session.createObjectMessage();
                message.setObject(costMetric);
                return message;
            }
        });
    }

}
