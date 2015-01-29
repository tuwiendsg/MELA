/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection;

import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import java.util.List;
import java.util.Map;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
public abstract class AbstractDataSource {

//    protected Map<String, Map<String, List<Metric>>> metricsToCollect;
//
//    public final void updateMetricsToCollect(Map<String, Map<String, List<Metric>>> metricsToCollect) {
//        this.metricsToCollect = metricsToCollect;
//    }
//
//    public final Map<String, Map<String, List<Metric>>> getMetricsToCollect() {
//        return metricsToCollect;
//    }
    public abstract MonitoringData getMonitoringData() throws DataAccessException;
    
    /**
     * In milliseconds, the rate at which collected data so far should be inspected.
     * Even if this is a push-based data source, it would still collect all the pushed data in a cache, that can be read
     * @return 
     */
    public abstract Long getRateAtWhichDataShouldBeRead();

}
