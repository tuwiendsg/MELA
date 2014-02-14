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
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at Decorates an
 * elasticity space with elasticity behavior information
 */
public class ElasticityBehavior {

    private ElasticitySpace elasticitySpace;
    private Map<MonitoredElement, List<ElasticityBehaviorDimension>> behavior;

    {
        behavior = new HashMap<MonitoredElement, List<ElasticityBehaviorDimension>>();
    }

    public ElasticityBehavior(ElasticitySpace elasticitySpace) {
        this.elasticitySpace = elasticitySpace;
        ElasticitySpaceBoundary elasticitySpaceBoundary = elasticitySpace.getElasticitySpaceBoundary();

        Map<MonitoredElement, Map<Metric, List<MetricValue>>> spaceData = elasticitySpace.getMonitoringData();
        for (MonitoredElement element : spaceData.keySet()) {
            List<ElasticityBehaviorDimension> elementDimensions = new ArrayList<ElasticityBehaviorDimension>();

            Map<Metric, List<MetricValue>> values = spaceData.get(element);

            for (Metric m : values.keySet()) {

                MetricValue upperBoundaryValue = elasticitySpaceBoundary.getUpperBoundary(element, m);
                MetricValue lowerBoundaryValue = elasticitySpaceBoundary.getLowerBoundary(element, m);

                //test if data ok
                if (upperBoundaryValue == null || lowerBoundaryValue == null || upperBoundaryValue.getValue().equals(Double.NaN) || lowerBoundaryValue.getValue().equals(Double.NaN)) {
                    Logger.getLogger(ElasticityBehaviorDimension.class.getName()).log(Level.WARN, "Boundaries for " + m + " are Upper: " + upperBoundaryValue + " and Lower:" + lowerBoundaryValue);
                } else {
                    elementDimensions.add(new ElasticityBehaviorDimension(m, upperBoundaryValue, lowerBoundaryValue, values.get(m)));
                }

            }

            behavior.put(element, elementDimensions);
        }
    }

    public Map<MonitoredElement, List<ElasticityBehaviorDimension>> getBehavior() {
        return behavior;
    }

    public ElasticitySpace getElasticitySpace() {
        return elasticitySpace;
    }

    //contains behavior of one metric
    public class ElasticityBehaviorDimension {

        private Metric metric;

        private Double upperBoundary;
        private Double lowerBoundary;

        /**
         * We consider upperBoundary = 100%, lowerBoundary = x% from
         * upperBoundaryThis means if linear dependencies such as x% for Service
         * Unit 1 determines y% for Service Unit 2
         */
        private List<Double> boundaryFulfillment;

        {
            boundaryFulfillment = new ArrayList<Double>();
        }

        public ElasticityBehaviorDimension(Metric metric, MetricValue upperBoundaryValue, MetricValue lowerBoundaryValue, List<MetricValue> values) {
            this.metric = metric;
            if (!upperBoundaryValue.getValueType().equals(MetricValue.ValueType.NUMERIC)) {
                Logger.getLogger(ElasticityBehaviorDimension.class.getName()).log(Level.WARN, "Upper boundary for " + metric + " is not numeric  " + upperBoundaryValue.getValueRepresentation());
                this.upperBoundary = Double.POSITIVE_INFINITY;
            } else {
                this.upperBoundary = (Double) upperBoundaryValue.getValue();
            }

            if (!lowerBoundaryValue.getValueType().equals(MetricValue.ValueType.NUMERIC)) {
                Logger.getLogger(ElasticityBehaviorDimension.class.getName()).log(Level.WARN, "Lower boundary for " + metric + " is not numeric  " + lowerBoundaryValue.getValueRepresentation());
                this.lowerBoundary = Double.POSITIVE_INFINITY;
            } else {
                this.lowerBoundary = (Double) lowerBoundaryValue.getValue();
            }

            for (MetricValue metricValue : values) {

                //we process only numeric values
                if (!metricValue.getValueType().equals(MetricValue.ValueType.NUMERIC)) {
                    Logger.getLogger(ElasticityBehaviorDimension.class.getName()).log(Level.WARN, "Values for " + metric + " are not numeric.");
                    break;
                }

                Double fulfillment = (((Double) metricValue.getValue()) * 100) / upperBoundary;
                boundaryFulfillment.add(fulfillment);

            }
        }

        public Metric getMetric() {
            return metric;
        }

        public Double getUpperBoundary() {
            return upperBoundary;
        }

        public List<Double> getBoundaryFulfillment() {
            return boundaryFulfillment;
        }

        public Double getLowerBoundary() {
            return lowerBoundary;
        }

    }

}
