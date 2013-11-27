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
package at.ac.tuwien.dsg.mela.analysisservice.report;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.analysisservice.utils.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Level;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class AnalysisReport {
    // stores requirement analysis information by LEVEL, then by MonitoredElement. Service Element also stores hierarchical info
    //list of  AnalysisReportEntry as there is 1 report for all conditions for each target METRIC
    private Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, List<AnalysisReportEntry>>> analysisReport;

    private boolean clean = true;


    public AnalysisReport(ServiceMonitoringSnapshot serviceMonitoringSnapshot, Requirements requirements) {

        analysisReport = new ConcurrentHashMap<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, List<AnalysisReportEntry>>>(serviceMonitoringSnapshot.getMonitoredData().keySet().size());

        List<Requirement> requirementList = requirements.getRequirements();

        for (Requirement requirement : requirementList) {
            MonitoredElement.MonitoredElementLevel targetLevel = requirement.getTargetMonitoredElementLevel();
            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> targetElements = serviceMonitoringSnapshot.getMonitoredData(targetLevel, requirement.getTargetMonitoredElementIDs());


            Metric targetMetric = requirement.getMetric();

            //for each element targeted by the restriction, find and evaluate the targeted metric.
            for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : targetElements.entrySet()) {

                List<AnalysisReportEntry> analysis = Collections.synchronizedList(new ArrayList<AnalysisReportEntry>());

                //get the value of the targeted metric
                MetricValue targetMetricValue = entry.getValue().getValueForMetric(targetMetric);
                if (targetMetricValue == null) {
                    Configuration.getLogger(this.getClass()).log(Level.WARN, "Metric " + targetMetric + "not found on " + entry.getKey());
                } else {
                    AnalysisReportEntry analysisReportEntry = new AnalysisReportEntry(targetMetric, targetMetricValue, requirement.getConditions(), entry.getKey());
                    if(!analysisReportEntry.isClean()){
                        clean = false;
                    }
                    analysis.add(analysisReportEntry);
                }

                if (analysisReport.containsKey(targetLevel)) {
                    Map<MonitoredElement, List<AnalysisReportEntry>> map = analysisReport.get(targetLevel);
                    if (map.containsKey(entry.getKey())) {
                        map.get(entry.getKey()).addAll(analysis);
                    } else {
                        map.put(entry.getKey(), analysis);
                    }
                } else {
                    Map<MonitoredElement, List<AnalysisReportEntry>> map = new ConcurrentHashMap<MonitoredElement, List<AnalysisReportEntry>>();
                    map.put(entry.getKey(), analysis);
                    analysisReport.put(targetLevel, map);
                }
            }
        }

    }


    public Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, List<AnalysisReportEntry>>> getAnalysisReport() {
        return analysisReport;
    }

    public boolean isClean() {
        return clean;
    }

    @Override
    public String toString() {
        String description = "AnalysisReport{";
        //traverse in DFS the tree

        for (Map.Entry<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement,  List<AnalysisReportEntry>>> entry : analysisReport.entrySet()) {

            String space = "";
            switch (entry.getKey()) {
                case SERVICE:
                    space = "";
                    break;
                case SERVICE_TOPOLOGY:
                    space = "\t";
                    break;
                case SERVICE_UNIT:
                    space = "\t\t";
                    break;
                case VM:
                    space = "\t\t\t";
                    break;

            }

            for (Map.Entry<MonitoredElement,  List<AnalysisReportEntry>> reportEntry : entry.getValue().entrySet()) {
                description += "\n" + space + reportEntry.getKey().getName() ;
                for(AnalysisReportEntry analysisReportEntry : reportEntry.getValue()){
                    description += "\n" + space + "\t"+ analysisReportEntry.getMetric() + " = " + analysisReportEntry.getMetricValue() + " unfulfilled " + analysisReportEntry.getUnfulfilledConditions() + " fulfilled " + analysisReportEntry.getFulfilledConditions();
                }
            }


        }

        return description;
    }

}
