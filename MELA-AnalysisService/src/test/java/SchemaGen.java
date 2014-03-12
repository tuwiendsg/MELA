/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group E184
 *
 * This work was partially supported by the European Commission in terms of the CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class SchemaGen {

    public static void main(String[] args) throws JAXBException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);
        SchemaOutputResolver sor = new MySchemaOutputResolver();
        jaxbContext.generateSchema(sor);
    }
    
    static class MySchemaOutputResolver extends SchemaOutputResolver {

    public Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
        File file = new File(suggestedFileName);
        StreamResult result = new StreamResult(file);
        result.setSystemId(file.toURI().toURL().toString());
        System.out.println(file.getAbsolutePath());
        return result;
    }

}
}
