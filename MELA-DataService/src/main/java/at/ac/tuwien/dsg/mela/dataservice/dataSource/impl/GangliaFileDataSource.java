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

package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl;
 
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities.ClusterInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataAccess.DataSourceI;
import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import java.io.InputStream;
import java.util.Iterator;
import org.yaml.snakeyaml.Yaml;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class GangliaFileDataSource implements DataSourceI {
    private String file;
    private Yaml yaml = new Yaml();
    private Iterator<Object> monitoredDataIterator;

    public GangliaFileDataSource(String file) {
        this.file = file;
    }


    
    public ClusterInfo getMonitoringData() throws DataAccessException {
        if(monitoredDataIterator == null){
        	InputStream is = this.getClass().getResourceAsStream(file);
            monitoredDataIterator = yaml.loadAll(is).iterator();
            for(int i = 0; i < 35; i++){
                if(monitoredDataIterator.hasNext()){
                    monitoredDataIterator.next();
                }
            }
        }
        if(monitoredDataIterator.hasNext()){
            return (ClusterInfo) monitoredDataIterator.next();
        }else{
            return null;
        }
    }


}
