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
package at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts;

import javax.xml.bind.annotation.*;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at Class represents
 * monitoring information collected for a MonitoredElement
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementData")
public class MonitoredElementData {

    @XmlElement(name = "MonitoredElement", required = true)
    private MonitoredElement monitoredElement;

    @XmlElement(name = "Metric")
    List<CollectedMetricValue> metrics;

    {
        metrics = new ArrayList<CollectedMetricValue>();
    }

    public Collection<CollectedMetricValue> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<CollectedMetricValue> metrics) {
        this.metrics = metrics;
    }

    public void addMetrics(List<CollectedMetricValue> metrics) {
        this.metrics.addAll(metrics);
    }

    public void addMetric(CollectedMetricValue metric) {
        this.metrics.add(metric);
    }

    public MonitoredElement getMonitoredElement() {
        return monitoredElement;
    }

    public void setMonitoredElement(MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
    }

    /**
     * @param name name to search for. All Metrics that CONTAIN the supplied
     * name will be returned
     * @return
     */
    public List<CollectedMetricValue> searchMetricsByName(String name) {
        List<CollectedMetricValue> metrics = new ArrayList<CollectedMetricValue>();
        for (CollectedMetricValue metricInfo : this.metrics) {
            if (metricInfo.getName().contains(name)) {
                metrics.add(metricInfo);
            }
        }
        return metrics;
    }

    @Override
    public String toString() {
        String info = "MonitoredElement: " + monitoredElement.getId() + ", metrics=";

        for (CollectedMetricValue metricInfo : metrics) {
            info += "\n\t " + metricInfo.toString();
        }
        info += '}';
        return info;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((monitoredElement == null) ? 0 : monitoredElement.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MonitoredElementData other = (MonitoredElementData) obj;
        if (monitoredElement == null) {
            if (other.monitoredElement != null) {
                return false;
            }
        } else if (!monitoredElement.equals(other.monitoredElement)) {
            return false;
        }
        return true;
    }

    public MonitoredElementData withMonitoredElement(final MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
        return this;
    }

    public MonitoredElementData withMetrics(final List<CollectedMetricValue> metrics) {
        this.metrics = metrics;
        return this;
    }

    public void withMetricInfo(CollectedMetricValue info) {

        if (metrics.contains(info)) {
            CollectedMetricValue metricInfo = metrics.get(metrics.indexOf(info));
            metricInfo.setValue(info.getValue());
        } else {
            metrics.add(info);
        }
    }

}
