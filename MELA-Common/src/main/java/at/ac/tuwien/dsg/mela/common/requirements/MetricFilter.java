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
package at.ac.tuwien.dsg.mela.common.requirements;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 * 
 * FILTERS will be applied SEQUENTIALLY
 */


@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MetricFilter")
public class MetricFilter  {
    @XmlAttribute(name = "id", required = true)
    private String id;

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "TargetMonitoredElementLevel", required = true)
    private MonitoredElement.MonitoredElementLevel level;

    @XmlElement(name = "TargetMonitoredElementIDs", required = false)
    private Collection<String> targetMonitoredElementIDs;

    @XmlElement(name = "MetricToMonitor", required = false)
    private Collection<Metric> metrics;

    {
        metrics = new ArrayList<Metric>();
        targetMonitoredElementIDs = new ArrayList<String>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MonitoredElement.MonitoredElementLevel getLevel() {
        return level;
    }

    public void setLevel(MonitoredElement.MonitoredElementLevel level) {
        this.level = level;
    }

    public Collection<String> getTargetMonitoredElementIDs() {
        return targetMonitoredElementIDs;
    }

    public void setTargetMonitoredElementIDs(Collection<String> targetMonitoredElementIDs) {
        this.targetMonitoredElementIDs = targetMonitoredElementIDs;
    }

    public Collection<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(Collection<Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetricFilter that = (MetricFilter) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (level != that.level) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (level != null ? level.hashCode() : 0);
        return result;
    }
}
