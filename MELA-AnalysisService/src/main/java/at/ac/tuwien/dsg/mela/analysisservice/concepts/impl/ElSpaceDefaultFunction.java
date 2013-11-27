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
package at.ac.tuwien.dsg.mela.analysisservice.concepts.impl;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.analysisservice.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.analysisservice.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
 
/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 *
 * Returns the maximum and minimum values for all monitored metrics, given a set of user requirements
 * Somewhat follows a decorator pattern, I.E. decorates an elasticity space with boundary
 */
public class ElSpaceDefaultFunction extends ElasticitySpaceFunction {


    public ElSpaceDefaultFunction(MonitoredElement service) {
        super(service);
    }



    @Override
    public void trainElasticitySpace(Collection<ServiceMonitoringSnapshot> monitoringData) {

        for (ServiceMonitoringSnapshot serviceMonitoringSnapshot : monitoringData) {
            trainElasticitySpace(serviceMonitoringSnapshot);
        }
    }

    @Override
    public void trainElasticitySpace(ServiceMonitoringSnapshot monitoringData) {
        ServiceMonitoringSnapshot upperBoundary = elasticitySpace.getElasticitySpaceBoundary().getUpperBoundary();
        ServiceMonitoringSnapshot lowerBoundary = elasticitySpace.getElasticitySpaceBoundary().getLowerBoundary();

        AnalysisReport report = new InstantMonitoringDataAnalysisEngine().analyzeRequirements(monitoringData, requirements);
        
        //TODO: adding data to keep history, but it can't be left like this, since we can't hold all monitoring data in memory
        //currently it is used by the elasticity signature. All monitoring data in future must be retrieved from monitoring storage
        elasticitySpace.addMonitoringEntry(report, monitoringData);

        //only update boundaries if the analysis report does not contain ANY requirement violation, I.E. the service is in elastic behavior
        if(!report.isClean()){
            return;
        }

        List<MonitoredElement> processingList = new ArrayList<MonitoredElement>();
        processingList.add(serviceStructure);

        //DFS traversal
        while (!processingList.isEmpty()) {
            MonitoredElement element = processingList.remove(processingList.size() - 1);

            {
                //extract limits at service element level
                MonitoredElementMonitoringSnapshot elementData = monitoringData.getMonitoredData(element);
                MonitoredElementMonitoringSnapshot upperBoundaryForElement = upperBoundary.getMonitoredData(element);
                MonitoredElementMonitoringSnapshot lowerBoundaryForElement = lowerBoundary.getMonitoredData(element);

                for (Metric metric : elementData.getMetrics()) {
                    MetricValue elementValue = elementData.getMetricValue(metric);

                    //upperBoundary update
                    {
                        //if boundary has value
                        if (upperBoundaryForElement.containsMetric(metric)) {
                            MetricValue boundaryValue = upperBoundaryForElement.getMetricValue(metric);

                            //if monitored element  > boundary, update boundary
                            if (elementValue.compareTo(boundaryValue) > 0) {
                                upperBoundaryForElement.putMetric(metric, elementValue.clone());
                            }
                        } else {
                            //if the boundary is empty, insert first value
                            upperBoundaryForElement.putMetric(metric, elementValue.clone());
                        }
                    }

                    //lowerBoundary update
                    {
                        //if boundary has value
                        if (lowerBoundaryForElement.containsMetric(metric)) {

                            MetricValue boundaryValue = lowerBoundaryForElement.getMetricValue(metric);

                            //if monitored element  > boundary, update boundary
                            if (elementValue.compareTo(boundaryValue) < 0) {
                                lowerBoundaryForElement.putMetric(metric, elementValue.clone());
                            }
                        } else {
                            //if the boundary is empty, insert first value
                            lowerBoundaryForElement.putMetric(metric, elementValue.clone());
                        }
                    }

                }
            }

            //if SERVICE_UNIT level, get All monitored VMs for this unit, and extract a unique boundary from all
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {

                //each boundary has only one VM boundary per service element
                MonitoredElement genericVM = element.getContainedElements().iterator().next();
                MonitoredElementMonitoringSnapshot upperBoundaryForGenericVM = upperBoundary.getMonitoredData(genericVM);
                MonitoredElementMonitoringSnapshot lowerBoundaryForGenericVM = lowerBoundary.getMonitoredData(genericVM);

                //retrieve the service element from the monitoring data, because that is the one which knows which VM to which service unit belongs to
                MonitoredElement monitoredServiceUnit = monitoringData.getMonitoredData(element).getMonitoredElement();

                //go trough all monitored VMs for the monitoredServiceUnit and update same genericVM boundaries
                for (MonitoredElement vm : monitoredServiceUnit.getContainedElements()) {
                    MonitoredElementMonitoringSnapshot vmMonitoredData = monitoringData.getMonitoredData(vm);

                    for (Metric metric : vmMonitoredData.getMetrics()) {
                        MetricValue elementValue = vmMonitoredData.getMetricValue(metric);

                        //upperBoundary update
                        {
                            //if boundary has value
                            if (upperBoundaryForGenericVM.containsMetric(metric)) {
                                MetricValue boundaryValue = upperBoundaryForGenericVM.getMetricValue(metric);

                                //if monitored element  > boundary, update boundary
                                if (elementValue.compareTo(boundaryValue) > 0) {
                                    upperBoundaryForGenericVM.putMetric(metric, elementValue.clone());
                                }
                            } else {
                                //if the boundary is empty, insert first value
                                upperBoundaryForGenericVM.putMetric(metric, elementValue.clone());
                            }
                        }

                        //lowerBoundary update
                        {
                            //if boundary has value
                            if (lowerBoundaryForGenericVM.containsMetric(metric)) {

                                MetricValue boundaryValue = lowerBoundaryForGenericVM.getMetricValue(metric);

                                //if monitored element  > boundary, update boundary
                                if (elementValue.compareTo(boundaryValue) < 0) {
                                    lowerBoundaryForGenericVM.putMetric(metric, elementValue.clone());
                                }
                            } else {
                                //if the boundary is empty, insert first value
                                lowerBoundaryForGenericVM.putMetric(metric, elementValue.clone());
                            }
                        }

                    }
                }


            }else{
                //only also process children if ! SERVICE_UNIT
                processingList.addAll(element.getContainedElements());
            }
        }


    }



}
