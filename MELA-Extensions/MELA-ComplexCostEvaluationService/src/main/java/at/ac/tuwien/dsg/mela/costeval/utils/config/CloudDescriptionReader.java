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
package at.ac.tuwien.dsg.mela.costeval.utils.config;

import at.ac.tuwien.dsg.quelle.cloudDescriptionParsers.impl.CloudFileDescriptionParser;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class CloudDescriptionReader {

    @Autowired
    private ApplicationContext context;

    static final Logger logger = LoggerFactory.getLogger(CloudDescriptionReader.class);
    
     static final  String cloudDescriptionsPath = "./config/default/";
    

    public CloudDescriptionReader() {

        
//        
//        AutowireCapableBeanFactory bf = context.getAutowireCapableBeanFactory();
//
//        File folder = new File(cloudDescriptionsPath);
//        for (File cloudDescriptionFile : folder.listFiles()) {
//            CloudFileDescriptionParser cloudFileDescriptionParser = new CloudFileDescriptionParser();
//            cloudFileDescriptionParser.setDescriptionFile(cloudDescriptionFile.getPath());
//
//            bf.initializeBean(cloudFileDescriptionParser, cloudDescriptionFile.getName().split(".")[0]);
//
//        }

    }

}
