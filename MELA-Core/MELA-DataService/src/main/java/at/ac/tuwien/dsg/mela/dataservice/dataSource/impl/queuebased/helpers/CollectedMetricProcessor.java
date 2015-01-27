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
package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects.CollectedMetricValue;
import at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.MetricAccuracyAnalysis;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidationTest;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

/**
 * Validates and computes accuracy of collected metric
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class CollectedMetricProcessor implements Runnable {

    //what value to process
    private CollectedMetricValue valueToProcess;

    //array of tests to be perform to determine value validity
    private MetricValidator metricValidator;

    private MetricAccuracyAnalysis accuracyAnalysis;

    //where to put the value
    private ServiceMonitoringSnapshot currentMonitoringSnapshot;

    public CollectedMetricProcessor(CollectedMetricValue valueToProcess, MetricValidator metricValidator, MetricAccuracyAnalysis accuracyAnalysis, ServiceMonitoringSnapshot currentMonitoringSnapshot) {
        this.valueToProcess = valueToProcess;
        this.metricValidator = metricValidator;
        this.accuracyAnalysis = accuracyAnalysis;
        this.currentMonitoringSnapshot = currentMonitoringSnapshot;
    }

    @Override
    public void run() {
        boolean valid = false;
        
        if (metricValidator.isValid(valueToProcess)) {
            valid = true;
        } else {
            //logging why is it invalid
            for (MetricValidationTest entry : metricValidator.isValidDetailedAnalaysis(valueToProcess)) {
                Logger.getLogger(CollectedMetricProcessor.class.getName()).info(entry.getHumanReadableDescription() + " failed for metric " + valueToProcess.toString());
            }
        }

        if (valid) {
            
            MonitoredElement.MonitoredElementLevel level = MonitoredElement.MonitoredElementLevel.valueOf(
                    valueToProcess.getMonitoredElementLevel());

            Metric metric = new Metric(valueToProcess.getName());

            currentMonitoringSnapshot.addMonitoredData(level, new MonitoredElement(valueToProcess.getMonitoredElementID()), metric,
                    new MetricValue(valueToProcess.getValue())
            );

            //see what I compute and if something in terms of data accuracy/staleness/etc
            Double accuracy = accuracyAnalysis.computeAccuracy(valueToProcess);

        }

    }

}
