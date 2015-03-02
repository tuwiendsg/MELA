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
package at.ac.tuwien.dsg.mela.costeval.pricingscheme;

import at.ac.tuwien.dsg.quelle.cloudDescriptionParsers.impl.AmazonCloudJSONDescriptionParser;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import java.io.FileWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniel-tuwien
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"file:src/test/java/spring/test-context.xml"})
public class AmazonCloudDescriptionGenerationTest {

    static final Logger log = LoggerFactory.getLogger(AmazonCloudDescriptionGenerationTest.class);

    public AmazonCloudDescriptionGenerationTest() {
    }

    public static void main(String[] args) throws Exception {
        AmazonCloudJSONDescriptionParser parser = new AmazonCloudJSONDescriptionParser();
        CloudProvider provider = parser.getCloudProviderDescription();
        {
            JAXBContext elementContext = JAXBContext.newInstance(CloudProvider.class);
            //persist structure
            Marshaller m = elementContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(provider, new FileWriter("src/test/resources/AMAZON_cloudServicesSpecification.xml"));
        }
    }
}
