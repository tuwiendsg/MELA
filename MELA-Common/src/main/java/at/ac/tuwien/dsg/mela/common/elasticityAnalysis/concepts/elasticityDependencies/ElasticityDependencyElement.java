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

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.xmlAdapters.ElasticityDependencyStatisticsXMLAdapter;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementElasticityDependency")
public class ElasticityDependencyElement implements Serializable {

    /**
     * Element to which this metric belongs to
     */
    @XmlElement(name = "MonitoredElement", required = true)
    private MonitoredElement monitoredElement;

    @XmlElement(name = "DependentMetric", required = true)
    private Metric dependentMetric;

    @XmlElement(name = "MetricValue", required = true)
    private List<MetricValue> dependentMetricValues;

    @XmlElement(name = "ElasticityDependencyCoefficient", required = false)
    private Collection<ElasticityDependencyCoefficient> coefficients;

    /**
     * As we currently extract linear dependencies, f(x) = interceptor_constant
     * + coeff*var
     */
    @XmlElement(name = "Interceptor", required = true)
    private Double interceptor;

    public static final String ADJUSTED_R = "adjustedR";
    public static final String ESTIMATION_ERROR_MIN = "minEstimationError";
    public static final String ESTIMATION_ERROR_MAX = "maxEstimationError";
    public static final String ESTIMATION_ERROR_AVERAGE = "avgEstimationError";
    public static final String ESTIMATION_ERROR_STD_DEVIATION = "absoluteStandardDeviation";

    @XmlJavaTypeAdapter(ElasticityDependencyStatisticsXMLAdapter.class)
    private Map<String, Double> statistics;

    {
        statistics = new ConcurrentHashMap<String, Double>();
        coefficients = new ArrayList<ElasticityDependencyCoefficient>();
    }

    public ElasticityDependencyElement() {
    }

    public ElasticityDependencyElement(MonitoredElement monitoredElement, Metric dependentMetric, Double interceptor) {
        this.monitoredElement = monitoredElement;
        this.dependentMetric = dependentMetric;
        this.interceptor = interceptor;
    }

    public List<MetricValue> getDependentMetricValues() {
        return dependentMetricValues;
    }

    public void setDependentMetricValues(List<MetricValue> dependentMetricValues) {
        this.dependentMetricValues = dependentMetricValues;
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

    public double getStatistic(String statistic) {
        return statistics.get(statistic);
    }

    public void setStatistic(String statistic, Double value) {
        this.statistics.put(statistic, value);
    }

    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.dependentMetric != null ? this.dependentMetric.hashCode() : 0);
        return hash;
    }

    public MonitoredElement getMonitoredElement() {
        return monitoredElement;
    }

    public void setMonitoredElement(MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
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

    public ElasticityDependencyElement withMonitoredElement(final MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
        return this;
    }

    public ElasticityDependencyElement withDependentMetric(final Metric dependentMetric) {
        this.dependentMetric = dependentMetric;
        return this;
    }

    public ElasticityDependencyElement withDependentMetricValues(final List<MetricValue> dependentMetricValues) {
        this.dependentMetricValues = dependentMetricValues;
        return this;
    }

    public ElasticityDependencyElement withCoefficients(final Collection<ElasticityDependencyCoefficient> coefficients) {
        this.coefficients = coefficients;
        return this;
    }

    public ElasticityDependencyElement withInterceptor(final Double interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    public ElasticityDependencyElement withStatistic(String statistic, final Double value) {
        this.statistics.put(statistic, value);
        return this;
    }

    public Map<String, Double> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Double> statistics) {
        this.statistics = statistics;
    }

}
