/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testCost;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.costeval.model.CloudServicesSpecification;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import junit.framework.TestCase;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class CostCompositionTest extends TestCase {

    static final Logger log = LoggerFactory.getLogger(CostCompositionTest.class);

    public void testX() throws IOException, JAXBException {
        MonitoredElement element = new MonitoredElement().withId("10.0.0.1").withLevel(MonitoredElement.MonitoredElementLevel.VM);
        UsedCloudOfferedService cfg = new UsedCloudOfferedService(1l).withName("m1.small");
        cfg.getQualityProperties().put(new Metric("cpu", "cores", Metric.MetricType.QUALITY), new MetricValue(2));
        cfg.getQualityProperties().put(new Metric("i/o", "level", Metric.MetricType.QUALITY), new MetricValue("high"));

        cfg.getResourceProperties().put(new Metric("RAM", "GB", Metric.MetricType.RESOURCE), new MetricValue(20));
        cfg.getResourceProperties().put(new Metric("disk", "GB", Metric.MetricType.RESOURCE), new MetricValue(1024));

        element.addUsedCloudOfferedService(cfg);

        String rSYBL_BASE_IP = "localhost";
        Integer rSYBL_BASE_PORT = 8081;
        String rSYBL_BASE_URL = "/MELA/REST_WS";
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpHost endpoint = new HttpHost(rSYBL_BASE_IP, rSYBL_BASE_PORT);

        CloudServicesSpecification cloudServicesSpecification = new CloudServicesSpecification();

        {
            ServiceUnit unit = new ServiceUnit("IaaS", "VM", "m1.small");

            //VM COST
            {
                CostFunction vmCost = new CostFunction();
                CostElement vmCostElement = new CostElement("vmCost", new Metric("numberOfVMs", "vms/m", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                vmCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.12);
                vmCost.addCostElement(vmCostElement);
                unit.addCostFunction(vmCost);
            }

            cloudServicesSpecification.addServiceUnit(unit);

        }

        {
            ServiceUnit unit = new ServiceUnit("IaaS", "VM", "m1.medium");

            //VM COST
            {
                CostFunction vmCost = new CostFunction();
                CostElement vmCostElement = new CostElement("vmCost", new Metric("numberOfVMs", "vms/m", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                vmCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.24);
                vmCost.addCostElement(vmCostElement);
                unit.addCostFunction(vmCost);
            }

            cloudServicesSpecification.addServiceUnit(unit);

        }

        {
            ServiceUnit unit = new ServiceUnit("IaaS", "VM", "m1.large");

            //VM COST
            {
                CostFunction vmCost = new CostFunction();
                CostElement vmCostElement = new CostElement("vmCost", new Metric("numberOfVMs", "vms/m", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                vmCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.48);
                vmCost.addCostElement(vmCostElement);
                unit.addCostFunction(vmCost);
            }

            cloudServicesSpecification.addServiceUnit(unit);

        }
        {
            ServiceUnit unit = new ServiceUnit("MaaS", "Network", "network");

            //Data Transfer
            {
                CostFunction cost = new CostFunction();
                CostElement costElement = new CostElement("dataTransferCost", new Metric("dataTransfer", "MB/s", Metric.MetricType.COST), CostElement.Type.USAGE);
                costElement.addCostInterval(new MetricValue(1024), 0.01 / 1024);
                costElement.addCostInterval(new MetricValue(10 * 1024 * 1024), 0.01 / 1024);
                cost.addCostElement(costElement);
                unit.addCostFunction(cost);
            }
            cloudServicesSpecification.addServiceUnit(unit);
        }

        {

            try {

                JAXBContext jAXBContext = JAXBContext.newInstance(CloudServicesSpecification.class);
                Marshaller marshaller = jAXBContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                StringWriter sw = new StringWriter();

                marshaller.marshal(cloudServicesSpecification, sw);
                log.info(sw.toString());

                BufferedWriter writer = new BufferedWriter(new FileWriter("./cloud_pricing_scheme.xml"));
                marshaller.marshal(cloudServicesSpecification, writer);
                writer.flush();
                writer.close();

                URI putDeploymentStructureURL = UriBuilder.fromPath(rSYBL_BASE_URL + "/cloudofferedservice/pricingscheme").build();
                HttpPut putDeployment = new HttpPut(putDeploymentStructureURL);

                StringEntity entity = new StringEntity(sw.getBuffer().toString());

                entity.setContentType("application/xml");
                entity.setChunked(true);

                putDeployment.setEntity(entity);

                log.info("Executing request " + putDeployment.getRequestLine());
                HttpResponse response = httpClient.execute(endpoint, putDeployment);
                HttpEntity resEntity = response.getEntity();

                System.out.println("----------------------------------------");
                System.out.println(response.getStatusLine());
                if (response.getStatusLine().getStatusCode() == 200) {

                }
                if (resEntity != null) {
                    System.out.println("Response content length: " + resEntity.getContentLength());
                    System.out.println("Chunked?: " + resEntity.isChunked());
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        }
    }
}
