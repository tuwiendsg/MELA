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
package at.ac.tuwien.dsg.mela.common.monitoringConcepts;

import javax.xml.bind.annotation.*;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at * Used to
 * marshall in XML Maps
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredEntry")
public class MonitoredEntry {

    @XmlElement(name = "metric")
    private Metric metric;
    @XmlElement(name = "value")
    private MetricValue value;

    public MonitoredEntry() {
    }

    public MonitoredEntry(Metric metric, MetricValue value) {
        this.metric = metric;
        this.value = value;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public MetricValue getValue() {
        return value;
    }

    public void setValue(MetricValue value) {
        this.value = value;
    }

    public MonitoredEntry withMetric(final Metric metric) {
        this.metric = metric;
        return this;
    }

    public MonitoredEntry withValue(final MetricValue value) {
        this.value = value;
        return this;
    }

}
