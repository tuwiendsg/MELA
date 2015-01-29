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

import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.CollectedMetricValue;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects.NumericalCollectedMetricValue;
import at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.MetricAccuracyAnalysis;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidationTest;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidator;
import org.apache.log4j.Logger;

/**
 * Validates and computes accuracy of collected metric
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class CollectedMetricProcessor implements Runnable {

    //what value to process
    private NumericalCollectedMetricValue valueToProcess;

    //array of tests to be perform to determine value validity
    private MetricValidator metricValidator;

    private MetricAccuracyAnalysis accuracyAnalysis;

    //where to put the value
    private MonitoringData currentMonitoringSnapshot;

    public CollectedMetricProcessor(NumericalCollectedMetricValue valueToProcess, MetricValidator metricValidator, MetricAccuracyAnalysis accuracyAnalysis, MonitoringData currentMonitoringSnapshot) {
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
            for (MetricValidationTest entry : metricValidator.isValidDetailedAnalysis(valueToProcess)) {
                Logger.getLogger(CollectedMetricProcessor.class.getName()).info(entry.getHumanReadableDescription() + " failed for metric " + valueToProcess.toString());
            }
        }

        if (valid) {

//            MonitoredElement.MonitoredElementLevel level = MonitoredElement.MonitoredElementLevel.valueOf(
//                    valueToProcess.getMonitoredElementLevel());
//            CollectedMetricValue collectedMetricValue = new CollectedMetricValue()
//                    .withMonitoredElementLevel(valueToProcess.getMonitorjavaedElementLevel())
//                    .withMonitoredElementID(valueToProcess.getMonitoredElementID())
//                    .withName(valueToProcess.getName())
//                    .withType(valueToProcess.getType())
//                    .withUnits(valueToProcess.getUnits())
//                    .withValue(valueToProcess.getValue().toString());
            currentMonitoringSnapshot.withMetricInfo(valueToProcess.toCollectedMetricValue());

            //see what I compute and if something in terms of data accuracy/staleness/etc
            Double accuracy = accuracyAnalysis.computeAccuracy(valueToProcess);

        }

    }

}
