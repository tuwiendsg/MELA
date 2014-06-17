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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementElasticityDependency")
public class ElasticityDependencyElement implements Serializable {

    @XmlElement(name = "DependentMetric", required = true)
    private Metric dependentMetric;

    @XmlElement(name = "ElasticityDependencyCoefficient", required = false)
    private Collection<ElasticityDependencyCoefficient> coefficients;

    /**
     * As we currently extract linear dependencies, f(x) = interceptor_constant + coeff*var
     */
    @XmlElement(name = "Interceptor", required = true)
    private Double interceptor;

    /**
     * Can be used as an indicator about the confidence of the dependency
     */
    @XmlElement(name = "AdjustedR", required = true)
    private Double adjustedR;

    {
        coefficients = new ArrayList<ElasticityDependencyCoefficient>();
    }

    public ElasticityDependencyElement() {
    }

    public ElasticityDependencyElement(Metric dependentMetric, Double interceptor, Double adjustedR) {
        this.dependentMetric = dependentMetric;
        this.interceptor = interceptor;
        this.adjustedR = adjustedR;
    }

    public void addCoefficient(ElasticityDependencyCoefficient element) {
        coefficients.add(element);
    }

    public void removeCoefficient(ElasticityDependencyCoefficient element) {
        coefficients.remove(element);
    }

    public Double getInterceptor() {
        return interceptor;
    }

    public void setInterceptor(Double interceptor) {
        this.interceptor = interceptor;
    }

    public Collection<ElasticityDependencyCoefficient> getCoefficients() {
        return coefficients;
    }

    public void setCoefficients(Collection<ElasticityDependencyCoefficient> coefficients) {
        this.coefficients = coefficients;
    }

    public Metric getDependentMetric() {
        return dependentMetric;
    }

    public void setDependentMetric(Metric dependentMetric) {
        this.dependentMetric = dependentMetric;
    }

    public Double getAdjustedR() {
        return adjustedR;
    }

    public void setAdjustedR(Double adjustedR) {
        this.adjustedR = adjustedR;
    }

    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.dependentMetric != null ? this.dependentMetric.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ElasticityDependencyElement other = (ElasticityDependencyElement) obj;
        if (this.dependentMetric != other.dependentMetric && (this.dependentMetric == null || !this.dependentMetric.equals(other.dependentMetric))) {
            return false;
        }
        return true;
    }

}
