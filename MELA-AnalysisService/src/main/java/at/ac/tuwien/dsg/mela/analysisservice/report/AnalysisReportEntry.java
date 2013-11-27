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

import at.ac.tuwien.dsg.mela.common.requirements.Condition;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import java.util.*;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class AnalysisReportEntry {

    private Metric metric;
    private MetricValue metricValue;
    //<fulfilled = TRUE || FALSE, condition>
    private Map<Boolean, List<Condition>> metricConditions;
    private MonitoredElement MonitoredElement;
    private boolean clean = true;

    {
        metricConditions = new LinkedHashMap<Boolean, List<Condition>>();
        metricConditions.put(false, new ArrayList<Condition>());
        metricConditions.put(true, new ArrayList<Condition>());
    }

    private AnalysisReportEntry() {
    }

    public AnalysisReportEntry(Metric metric, MetricValue metricValue, Collection<Condition> metricCondition, MonitoredElement MonitoredElement) {
        this.metric = metric;
        this.metricValue = metricValue;
        for (Condition condition : metricCondition) {
            boolean respected = condition.isRespectedByValue(metricValue);
            if (!respected) {
                clean = false;
            }
            metricConditions.get(respected).add(condition);
        }
        this.MonitoredElement = MonitoredElement;
    }

    public Metric getMetric() {
        return metric;
    }

    public MetricValue getMetricValue() {
        return metricValue;
    }

    public Map<Boolean, List<Condition>> getMetricConditions() {
        return metricConditions;
    }

    public MonitoredElement getMonitoredElement() {
        return MonitoredElement;
    }

    public List<Condition> getFulfilledConditions() {
        return metricConditions.get(true);
    }

    public List<Condition> getUnfulfilledConditions() {
        return metricConditions.get(false);
    }

    public boolean isClean() {
        return clean;
    }
}
