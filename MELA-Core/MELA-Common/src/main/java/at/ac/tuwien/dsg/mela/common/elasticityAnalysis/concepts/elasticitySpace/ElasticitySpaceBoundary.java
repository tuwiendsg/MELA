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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import java.io.Serializable;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
public class ElasticitySpaceBoundary implements Serializable {

    private ServiceMonitoringSnapshot upperBoundary;
    private ServiceMonitoringSnapshot lowerBoundary;

    public ElasticitySpaceBoundary(ServiceMonitoringSnapshot upperBoundary, ServiceMonitoringSnapshot lowerBoundary) {
        this.upperBoundary = upperBoundary;
        this.lowerBoundary = lowerBoundary;
    }

    public ElasticitySpaceBoundary() {
        upperBoundary = new ServiceMonitoringSnapshot();
        lowerBoundary = new ServiceMonitoringSnapshot();
    }

    public ServiceMonitoringSnapshot getUpperBoundary() {
        return upperBoundary;
    }

    public ServiceMonitoringSnapshot getLowerBoundary() {
        return lowerBoundary;
    }

    public boolean containsUpperBoundary(MonitoredElement element, Metric metric) {
        MonitoredElementMonitoringSnapshot elementData = upperBoundary.getMonitoredData(element);
        return (elementData != null) && elementData.containsMetric(metric);
    }

    public boolean containsLowerBoundary(MonitoredElement element, Metric metric) {
        MonitoredElementMonitoringSnapshot elementData = lowerBoundary.getMonitoredData(element);
        return (elementData != null) && elementData.containsMetric(metric);
    }

    public MetricValue getUpperBoundary(MonitoredElement element, Metric metric) {
        if (containsUpperBoundary(element, metric)) {
            return upperBoundary.getMonitoredData(element).getMetricValue(metric);
        } else {
            return null;
        }
    }

    public MetricValue getLowerBoundary(MonitoredElement element, Metric metric) {
        if (containsLowerBoundary(element, metric)) {
            return lowerBoundary.getMonitoredData(element).getMetricValue(metric);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "ElasticitySpaceBoundary{"
                + "\n upperBoundary=" + upperBoundary
                + ",\n\t lowerBoundary=" + lowerBoundary
                + '}';
    }
}
