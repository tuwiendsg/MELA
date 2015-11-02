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
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.report.AnalysisReport;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.io.Serializable;

import java.util.*;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
public class ElasticitySpace implements Serializable {

    /**
     * The space structure seems a bit odd, but it holds also analysis reports
     * which work on monitored snapshots
     */
    /**
     * Currently we see the space recorded in time as a list of entries saying
     * AnalysisReport (violated, not) and monitored snapshot
     */
    private List<ElasticitySpaceEntry> spaceEntries;
    private ElasticitySpaceBoundary elasticitySpaceBoundary;
    private MonitoredElement service;
    //stored monitoring data for easy access -> for a service element, the recorded data for the space dimensions
    private Map<MonitoredElement, Map<Metric, List<MetricValue>>> monitoringData;

    //used to referenciate the timestamp at which this space was computed
    //used in space caching
    private int endTimestampID;
    private int startTimestampID;

    {
        spaceEntries = new ArrayList<ElasticitySpaceEntry>();
        elasticitySpaceBoundary = new ElasticitySpaceBoundary();
        monitoringData = new LinkedHashMap<MonitoredElement, Map<Metric, List<MetricValue>>>();
    }

    public ElasticitySpace(MonitoredElement service) {
        if (service != null) {
            this.service = service;

            //create the monitoring data structure
            List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
            queue.add(this.service);
            while (!queue.isEmpty()) {
                MonitoredElement element = queue.remove(0);
                queue.addAll(element.getContainedElements());
                monitoringData.put(element, new HashMap<Metric, List<MetricValue>>());
            }
        }
    }

    public ElasticitySpace() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public ElasticitySpaceBoundary getElasticitySpaceBoundary() {
        return elasticitySpaceBoundary;
    }

    public void setElasticitySpaceBoundary(ElasticitySpaceBoundary elasticitySpaceBoundary) {
        this.elasticitySpaceBoundary = elasticitySpaceBoundary;
    }

    public void addMonitoringEntry(AnalysisReport analysisReport, ServiceMonitoringSnapshot serviceMonitoringSnapshot) {
        //populate the monitoring data structure
        List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
        queue.add(this.service);
        while (!queue.isEmpty()) {
            MonitoredElement element = queue.remove(0);
            queue.addAll(element.getContainedElements());
            MonitoredElementMonitoringSnapshot snapshot = serviceMonitoringSnapshot.getMonitoredData(element);
            Map<Metric, List<MetricValue>> data = monitoringData.get(element);
            for (Metric metric : snapshot.getMetrics()) {
                try{
                if (Double.isNaN(((Number) snapshot.getMetricValue(metric).getValue()).doubleValue()) || snapshot.getMetricValue(metric).getValueRepresentation().contains("NaN")) {
                    continue;
                }
                if (data.containsKey(metric)) {
                    data.get(metric).add(snapshot.getMetricValue(metric));
                } else {
                    List<MetricValue> values = new ArrayList<MetricValue>();
                    values.add(snapshot.getMetricValue(metric));
                    data.put(metric, values);
                }
                }catch(java.lang.ClassCastException e){
                    System.out.println(e.toString());
                    System.out.println(snapshot.getMetricValue(metric).getValue());
                    
                }
            }
        }

        spaceEntries.add(new ElasticitySpaceEntry(analysisReport, serviceMonitoringSnapshot));
    }

    public int getEndTimestampID() {
        return endTimestampID;
    }

    public void setEndTimestampID(int endTimestampID) {
        this.endTimestampID = endTimestampID;
    }

    public int getStartTimestampID() {
        return startTimestampID;
    }

    public void setStartTimestampID(int startTimestampID) {
        this.startTimestampID = startTimestampID;
    }

    public List<ElasticitySpaceEntry> getSpaceEntries() {
        return spaceEntries;
    }

    public Map<MonitoredElement, Map<Metric, List<MetricValue>>> getMonitoringData() {
        return monitoringData;
    }

    public Map<Metric, List<MetricValue>> getMonitoredDataForService(MonitoredElement service) {
        return monitoringData.get(service);
    }

    //used to add more data at once, if we want to get a space from a historical data
    public void addMonitoringData(ServiceMonitoringSnapshot serviceMonitoringSnapshot) {
        //populate the monitoring data structure
        List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
        queue.add(this.service);
        while (!queue.isEmpty()) {
            MonitoredElement element = queue.remove(0);

            queue.addAll(element.getContainedElements());
            //VM monitored elements should NOT be kept in space
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.VM)) {
                continue;
            }

            MonitoredElementMonitoringSnapshot snapshot = serviceMonitoringSnapshot.getMonitoredData(element);
            Map<Metric, List<MetricValue>> data = monitoringData.get(element);
            for (Metric metric : snapshot.getMetrics()) {
                if (data.containsKey(metric)) {
                    data.get(metric).add(snapshot.getMetricValue(metric));
                } else {
                    List<MetricValue> values = new ArrayList<MetricValue>();
                    values.add(snapshot.getMetricValue(metric));
                    data.put(metric, values);
                }
            }
        }

    }

    /**
     *
     * @param service
     * @param metric
     * @return [] on position 0 is lower value, on position 1 is upper value.
     * Need to rewrite this to return correct
     */
    public MetricValue[] getSpaceBoundaryForMetric(MonitoredElement service, Metric metric) {
        MonitoredElementMonitoringSnapshot upperBoundary = elasticitySpaceBoundary.getUpperBoundary().getMonitoredData(service);
        MonitoredElementMonitoringSnapshot lowerBoundary = elasticitySpaceBoundary.getLowerBoundary().getMonitoredData(service);
        if (upperBoundary != null && lowerBoundary != null) {
            MetricValue upperValue = upperBoundary.getMetricValue(metric);
            MetricValue lowerValue = lowerBoundary.getMetricValue(metric);
            return new MetricValue[]{lowerValue, upperValue};
        } else {
            return null;
        }
    }

    public MonitoredElement getService() {
        return service;
    }

    public class ElasticitySpaceEntry implements Serializable {

        private AnalysisReport analysisReport;
        private ServiceMonitoringSnapshot serviceMonitoringSnapshot;

        public ElasticitySpaceEntry(AnalysisReport analysisReport, ServiceMonitoringSnapshot serviceMonitoringSnapshot) {
            this.analysisReport = analysisReport;
            this.serviceMonitoringSnapshot = serviceMonitoringSnapshot;
        }

        public AnalysisReport getAnalysisReport() {
            return analysisReport;
        }

        public ServiceMonitoringSnapshot getServiceMonitoringSnapshot() {
            return serviceMonitoringSnapshot;
        }
    }

    @Override
    public String toString() {
        return "ElasticitySpace for " + service.getId();
    }

    /**
     * resets to initial state after construction the object
     */
    public void reset() {

        spaceEntries.clear();
        elasticitySpaceBoundary = new ElasticitySpaceBoundary();
        monitoringData.clear();

        //create the monitoring data structure
        List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
        queue.add(this.service);
        while (!queue.isEmpty()) {
            MonitoredElement element = queue.remove(0);
            queue.addAll(element.getContainedElements());
            monitoringData.put(element, new HashMap<Metric, List<MetricValue>>());
        }
    }

    public ElasticitySpace withSpaceEntries(final List<ElasticitySpaceEntry> spaceEntries) {
        this.spaceEntries = spaceEntries;
        return this;
    }

    public ElasticitySpace withElasticitySpaceBoundary(final ElasticitySpaceBoundary elasticitySpaceBoundary) {
        this.elasticitySpaceBoundary = elasticitySpaceBoundary;
        return this;
    }

    public ElasticitySpace withService(final MonitoredElement service) {
        this.service = service;
        return this;
    }

    public ElasticitySpace withMonitoringData(final Map<MonitoredElement, Map<Metric, List<MetricValue>>> monitoringData) {
        this.monitoringData = monitoringData;
        return this;
    }

    public ElasticitySpace withEndTimestampID(final int endTimestampID) {
        this.endTimestampID = endTimestampID;
        return this;
    }

    public ElasticitySpace withStartTimestampID(final int startTimestampID) {
        this.startTimestampID = startTimestampID;
        return this;
    }
}
