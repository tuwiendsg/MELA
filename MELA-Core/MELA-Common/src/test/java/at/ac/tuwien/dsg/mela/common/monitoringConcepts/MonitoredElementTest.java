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
package at.ac.tuwien.dsg.mela.common.monitoringConcepts;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import junit.framework.TestCase;

/**
 *
 * @author daniel-tuwien
 */
public class MonitoredElementTest extends TestCase {

    public MonitoredElementTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getCloudOfferedServices method, of class MonitoredElement.
     */
    public void testGetCloudOfferedServices() {
        try {
            MonitoredElement service = new MonitoredElement("Service")
                    .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE)
                    .withCloudOfferedService(new UsedCloudOfferedService()
                            .withCloudProviderID(UUID.randomUUID())
                            .withCloudProviderName("DSG@OpenStack")
                            .withId(UUID.randomUUID())
                            .withName("Network"))
                    .withContainedElement(new MonitoredElement("Service_Topology_1")
                            .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY)
                            .withCloudOfferedService(new UsedCloudOfferedService()
                                    .withCloudProviderID(UUID.randomUUID())
                                    .withCloudProviderName("DSG@OpenStack")
                                    .withId(UUID.randomUUID())
                                    .withName("Backup"))
                            .withContainedElement(new MonitoredElement("Service_Unit_1")
                                    .withLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)
                                    .withContainedElement(new MonitoredElement("Service_unit_1_Instance_1")
                                            .withLevel(MonitoredElement.MonitoredElementLevel.VM)
                                            .withCloudOfferedService(new UsedCloudOfferedService()
                                                    .withCloudProviderID(UUID.randomUUID())
                                                    .withCloudProviderName("DSG@OpenStack")
                                                    .withId(UUID.randomUUID())
                                                    .withName("VM"))
                                            .withCloudOfferedService(new UsedCloudOfferedService()
                                                    .withCloudProviderID(UUID.randomUUID())
                                                    .withCloudProviderName("DSG@OpenStack")
                                                    .withId(UUID.randomUUID())
                                                    .withName("Storage"))
                                    )
                            )
                    );

            JAXBContext jc = JAXBContext.newInstance(MonitoredElement.class);

            jc.generateSchema(new SchemaOutputResolver() {

                @Override
                public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                    StreamResult result = new StreamResult(new FileOutputStream(new File("./MonitoredElement.xsd")));
                    result.setSystemId(suggestedFileName);
                    return result;
                }

            });

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(service, new FileOutputStream(new File("./Example_MonitoredElement.xml")));

        } catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

}
