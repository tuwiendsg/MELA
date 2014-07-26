/*
 * Copyright 2014 daniel-tuwien.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ElasticityDependencyCoefficient")
public class ElasticityDependencyCoefficient implements Serializable {

    /**
     * Element to which this metric belongs to
     */
    @XmlElement(name = "MonitoredElement", required = true)
    private MonitoredElement monitoredElement;

    /**
     * Metric which is dependent on other
     */
    @XmlElement(name = "Metric", required = true)
    private Metric metric;

    @XmlElement(name = "Coefficient", required = true)
    private double coefficient;

    @XmlElement(name = "StdError", required = true)
    private double stdError;

    @XmlElement(name = "MetricValue", required = true)
    private List<MetricValue> metricValues;

    /**
     * Lag of coefficient from dependent. So a lag of 2 means coefficient is in
     * front with 2, of -2 means is behind with 2
     */
    @XmlElement(name = "Lag", required = true)
    private int lag;

    public ElasticityDependencyCoefficient() {
    }

    public ElasticityDependencyCoefficient(MonitoredElement monitoredElement, Metric metric, double coefficient, double stdError, int lag) {
        this.monitoredElement = monitoredElement;
        this.metric = metric;
        this.coefficient = coefficient;
        this.stdError = stdError;
        this.lag = lag;
    }

    public MonitoredElement getMonitoredElement() {
        return monitoredElement;
    }

    public void setMonitoredElement(MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }

    public double getStdError() {
        return stdError;
    }

    public void setStdError(double stdError) {
        this.stdError = stdError;
    }

    public int getLag() {
        return lag;
    }

    public void setLag(int lag) {
        this.lag = lag;
    }

    public List<MetricValue> getMetricValues() {
        return metricValues;
    }

    public void setMetricValues(List<MetricValue> metricValues) {
        this.metricValues = metricValues;
    }

    public ElasticityDependencyCoefficient withMonitoredElement(final MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
        return this;
    }

    public ElasticityDependencyCoefficient withMetric(final Metric metric) {
        this.metric = metric;
        return this;
    }

    public ElasticityDependencyCoefficient withCoefficient(final double coefficient) {
        this.coefficient = coefficient;
        return this;
    }

    public ElasticityDependencyCoefficient withStdError(final double stdError) {
        this.stdError = stdError;
        return this;
    }

    public ElasticityDependencyCoefficient withMetricValues(final List<MetricValue> metricValues) {
        this.metricValues = metricValues;
        return this;
    }

    public ElasticityDependencyCoefficient withLag(final int lag) {
        this.lag = lag;
        return this;
    }

}
