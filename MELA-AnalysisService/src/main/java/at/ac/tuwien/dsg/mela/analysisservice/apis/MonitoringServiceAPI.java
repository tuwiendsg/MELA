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

package at.ac.tuwien.dsg.mela.analysisservice.apis;

import at.ac.tuwien.dsg.mela.analysisservice.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public interface MonitoringServiceAPI {
    /**
     *
     * @return gets directly collected monitoring data
     */
    public ServiceMonitoringSnapshot getRawMonitoringData();

    /**
     *
     * @return monitoring data over which the composition rules have been applied
     */
    public ServiceMonitoringSnapshot getAggregatedMonitoringData();


    /**
     *
     * @return a report analyzing the serviceStructure requirements over the aggregated monitoring data
     */
    public AnalysisReport getRequirementsAnalysisReport();

}
