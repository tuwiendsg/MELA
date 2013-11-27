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

package at.ac.tuwien.dsg.mela.analysisservice.apis.impl;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.analysisservice.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.analysisservice.apis.MonitoringServiceAPI;
import at.ac.tuwien.dsg.mela.analysisservice.control.SystemControl;
import at.ac.tuwien.dsg.mela.analysisservice.control.SystemControlFactory;



/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 *
 * Each API method operates on fresh monitoring data. A sequence of getRawMonitoringData() and a getAggregatedMonitoringData() calls
 * will operate on potentially different monitoring data
 */
public class MonitoringServiceManager implements MonitoringServiceAPI {

    private SystemControl systemControl;

    public MonitoringServiceManager(SystemControlFactory systemControlFactory) {
        this.systemControl = systemControlFactory.getSystemControlInstance();
    }

    
    public ServiceMonitoringSnapshot getRawMonitoringData() {
        return systemControl.getRawMonitoringData();
    }

    
    public ServiceMonitoringSnapshot getAggregatedMonitoringData() {
        return systemControl.getLatestMonitoringData();
    }

    
    public AnalysisReport getRequirementsAnalysisReport() {
        return systemControl.analyzeLatestMonitoringData();
    }
}
