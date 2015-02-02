/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package outputXML;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import junit.framework.TestCase;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class MonitoredElOutputTest extends TestCase {

    public void testX() throws IOException, JAXBException {
        MonitoredElement element = new MonitoredElement().withId("10.0.0.1").withLevel(MonitoredElement.MonitoredElementLevel.VM);
        
        CloudProvider provider = new CloudProvider("Amazon");
        
        UsedCloudOfferedService cfg = new UsedCloudOfferedService(provider.getUuid(),provider.getName(),UUID.randomUUID(),"m1.small");
        cfg.getQualityProperties().put(new Metric("cpu", "cores", Metric.MetricType.QUALITY), new MetricValue(2));
        cfg.getQualityProperties().put(new Metric("i/o", "level", Metric.MetricType.QUALITY), new MetricValue("high"));
        
        cfg.getResourceProperties().put(new Metric("RAM", "GB", Metric.MetricType.RESOURCE), new MetricValue(20));
        cfg.getResourceProperties().put(new Metric("disk", "GB", Metric.MetricType.RESOURCE), new MetricValue(1024));
        
        element.addUsedCloudOfferedService(cfg);

        JAXBContext aXBContext = JAXBContext.newInstance(MonitoredElement.class);
        Marshaller marshaller = aXBContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(element, new File("./MonitoredElementStruct.xml"));
        
    }
}
