/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
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
package at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ElasticitySpaceDimension")
public class ElasticitySpaceDimensionXML {

    @XmlElement(name = "DimensionMetric")
    private Metric metric;
    @XmlElement(name = "RecordedValue")
    private Collection<MetricValue> metricValues;

    {
        metricValues = new ArrayList<MetricValue>();
    }
    @XmlElement(name = "UpperBoundary")
    private MetricValue upperBoundary;
    @XmlElement(name = "LowerBoundary")
    private MetricValue lowerBoundary;

    public ElasticitySpaceDimensionXML() {
    }

    public ElasticitySpaceDimensionXML(Metric metric, Collection<MetricValue> metricValues,
            MetricValue upperBoundary, MetricValue lowerBoundary) {
        this.metric = metric;
        this.metricValues = metricValues;
        this.upperBoundary = upperBoundary;
        this.lowerBoundary = lowerBoundary;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public Collection<MetricValue> getMetricValues() {
        return metricValues;
    }

    public void setMetricValues(Collection<MetricValue> metricValues) {
        this.metricValues = metricValues;
    }

    public void addMetricValues(Collection<MetricValue> metricValues) {
        this.metricValues.addAll(metricValues);
    }
    
     public void addMetricValue(MetricValue metricValue) {
        this.metricValues.add(metricValue);
    }

    public MetricValue getUpperBoundary() {
        return upperBoundary;
    }

    public void setUpperBoundary(MetricValue upperBoundary) {
        this.upperBoundary = upperBoundary;
    }

    public MetricValue getLowerBoundary() {
        return lowerBoundary;
    }

    public void setLowerBoundary(MetricValue lowerBoundary) {
        this.lowerBoundary = lowerBoundary;
    }
}
