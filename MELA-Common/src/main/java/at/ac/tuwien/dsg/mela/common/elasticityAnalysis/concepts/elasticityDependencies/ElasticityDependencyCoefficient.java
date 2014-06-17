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
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.io.Serializable;
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

    public ElasticityDependencyCoefficient() {
    }

    public ElasticityDependencyCoefficient(MonitoredElement monitoredElement, Metric metric, double coefficient, double stdError) {
        this.monitoredElement = monitoredElement;
        this.metric = metric;
        this.coefficient = coefficient;
        this.stdError = stdError;
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

}
