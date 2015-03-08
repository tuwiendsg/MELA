/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testCost;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.costeval.model.CloudServicesSpecification;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import junit.framework.TestCase;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class SimpleCostDescriptionTest extends TestCase {

    static final Logger log = LoggerFactory.getLogger(SimpleCostDescriptionTest.class);

    public void testEcosystemDescription() throws IOException, JAXBException {
        CloudProvider provider = new CloudProvider("Amazon");
        provider.setUuid(UUID.fromString("251ed7c7-aa4d-49d4-b42b-7efefd970d6b"));

//        UsedCloudOfferedService cfg .put(new Metric("disk", "GB", Metric.MetricType.RESOURCE), new MetricValue(1024));

        String melaIP = "localhost";
        Integer melaPort = 8080;
        String melaURL = "/MELA/REST_WS";
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpHost endpoint = new HttpHost(melaIP, melaPort);

        CloudServicesSpecification cloudServicesSpecification = new CloudServicesSpecification();
        cloudServicesSpecification.addCloudProvider(provider);

        {
            CloudOfferedService unit = new CloudOfferedService("IaaS", "VM", "m1.small");
            unit.withUuid(UUID.fromString("38400000-8cf0-11bd-b23e-000000000000"));

            //VM COST
            {
                CostFunction vmCost = new CostFunction(unit.getName() + "_cost");
                CostElement vmCostElement = new CostElement("vmCost", new Metric("instance", "#/m", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                vmCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.12);
                vmCost.addCostElement(vmCostElement);
                unit.addCostFunction(vmCost);
            }

            provider.addCloudOfferedService(unit);

        }

        {
            CloudOfferedService unit = new CloudOfferedService("IaaS", "VM", "m1.medium");
            unit.withUuid(UUID.fromString("38400000-8cf0-11bd-b23e-000000000001"));
            //VM COST
            {
                CostFunction vmCost = new CostFunction(unit.getName() + "_cost");
                CostElement vmCostElement = new CostElement("vmCost", new Metric("instance", "#/m", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                vmCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.24);
                vmCost.addCostElement(vmCostElement);
                unit.addCostFunction(vmCost);
            }

            provider.addCloudOfferedService(unit);

        }

        {
            CloudOfferedService unit = new CloudOfferedService("IaaS", "VM", "m1.large");
            unit.withUuid(UUID.fromString("38400000-8cf0-11bd-b23e-000000000002"));
            //VM COST
            {
                CostFunction vmCost = new CostFunction(unit.getName() + "_cost");
                CostElement vmCostElement = new CostElement("vmCost", new Metric("instance", "#/m", Metric.MetricType.COST), CostElement.Type.PERIODIC);
                vmCostElement.addCostInterval(new MetricValue(Double.POSITIVE_INFINITY), 0.48);
                vmCost.addCostElement(vmCostElement);
                unit.addCostFunction(vmCost);
            }

            provider.addCloudOfferedService(unit);

        }
        {
            CloudOfferedService unit = new CloudOfferedService("MaaS", "Network", "network");
            unit.withUuid(UUID.fromString("38400000-8cf0-11bd-b23e-000000000003"));
            //Data Transfer
            {
                CostFunction cost = new CostFunction(unit.getName() + "_cost");
                CostElement costElement = new CostElement("dataTransferCost", new Metric("dataTransfer", "MB/s", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
                costElement.addCostInterval(new MetricValue(1024), 0.03 / 1024);
                costElement.addCostInterval(new MetricValue(10 * 1024 * 1024), 0.01 / 1024);
                cost.addCostElement(costElement);
                unit.addCostFunction(cost);
            }
            provider.addCloudOfferedService(unit);
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

//                URI putDeploymentStructureURL = UriBuilder.fromPath(melaURL + "/cloudofferedservice/pricingscheme").build();
//                HttpPut putDeployment = new HttpPut(putDeploymentStructureURL);
//
//                StringEntity entity = new StringEntity(sw.getBuffer().toString());
//
//                entity.setContentType("application/xml");
//                entity.setChunked(true);
//
//                putDeployment.setEntity(entity);
//
//                log.info("Executing request " + putDeployment.getRequestLine());
//                HttpResponse response = httpClient.execute(endpoint, putDeployment);
//                HttpEntity resEntity = response.getEntity();
//
//                System.out.println("----------------------------------------");
//                System.out.println(response.getStatusLine());
//                if (response.getStatusLine().getStatusCode() == 200) {
//
//                }
//                if (resEntity != null) {
//                    System.out.println("Response content length: " + resEntity.getContentLength());
//                    System.out.println("Chunked?: " + resEntity.isChunked());
//                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                fail(e.getMessage());
            }

        }
    }
}
