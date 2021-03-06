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
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.engine;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyCoefficient;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.LinearCorrelation;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.Variable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
//import java.util.Optional;
//import java.util.function.BinaryOperator;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class LinearCorrelationStatisticsComputationEngine {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(LinearCorrelationStatisticsComputationEngine.class);

    /**
     * compute difference between estimated and monitored values, and determine
     * max, min, and average difference as Estimation Error
     *
     * @param correlation modifies the object in place
     * @return the supplied corelation object
     */
    public static LinearCorrelation enrichWithEstimationErrorStatistics(LinearCorrelation correlation) {
        Variable dependent = correlation.getDependent();
        int valuesSize = dependent.getValues().size();
        List<Double> estimationError = new ArrayList<>();
        List<Double> deviation = new ArrayList<>();

        for (int i = 0; i < valuesSize; i++) {
            double computed = dependent.getValues().get(i);
            double estimated = correlation.getIntercept();
            for (LinearCorrelation.Coefficient predictor : correlation.getPredictors()) {
                estimated += predictor.getCoefficient() * predictor.getVariable().getValues().get(i);
            }

            double error = Math.abs(computed - estimated);

            estimationError.add(error);
            deviation.add(Math.pow(error, 2));

        }

        Double maxError = Double.NEGATIVE_INFINITY;
        Double minError = Double.POSITIVE_INFINITY;
        Double sum = 0.0d;
        Double deviationSum = 0.0d;

        for (Double d : estimationError) {
            if (maxError < d) {
                maxError = d;
            }

            if (minError > d) {
                minError = d;
            }

            sum += d;
        }

        for (Double d : deviation) {
            deviationSum += d;
        }

        correlation.setStatistic(LinearCorrelation.ESTIMATION_ERROR_MIN, minError);
        correlation.setStatistic(LinearCorrelation.ESTIMATION_ERROR_MAX, maxError);
        correlation.setStatistic(LinearCorrelation.ESTIMATION_ERROR_AVERAGE, sum / valuesSize);
        correlation.setStatistic(LinearCorrelation.ESTIMATION_ERROR_STD_DEVIATION, Math.sqrt(deviationSum / deviation.size()));

        return correlation;
    }

    public static ElasticityDependencyElement enrichWithEstimationErrorStatistics(ElasticityDependencyElement dependencyElement) {
        List<MetricValue> dependentValues = dependencyElement.getDependentMetricValues();
        int valuesSize = dependentValues.size();
        List<Double> estimationError = new ArrayList<>();
        List<Double> deviation = new ArrayList<>();

        for (int i = 0; i < valuesSize; i++) {
            double computed = Double.parseDouble(dependentValues.get(i).getValueRepresentation());
            double estimated = dependencyElement.getInterceptor();
            for (ElasticityDependencyCoefficient predictor : dependencyElement.getCoefficients()) {
                double predictorVal = Double.parseDouble(predictor.getMetricValues().get(i).getValueRepresentation());
                estimated += predictor.getCoefficient() * predictorVal;
            }

            double error = Math.abs(computed - estimated);

            estimationError.add(error);
            deviation.add(Math.pow(error, 2));

        }

        Double maxError = Double.NEGATIVE_INFINITY;
        Double minError = Double.POSITIVE_INFINITY;
        Double sum = 0.0d;
        Double deviationSum = 0.0d;

        for (Double d : estimationError) {
            if (maxError < d) {
                maxError = d;
            }

            if (minError > d) {
                minError = d;
            }

            sum += d;
        }

        for (Double d : deviation) {
            deviationSum += d;
        }

        dependencyElement.setStatistic(LinearCorrelation.ESTIMATION_ERROR_MIN, minError);
        dependencyElement.setStatistic(LinearCorrelation.ESTIMATION_ERROR_MAX, maxError);
        dependencyElement.setStatistic(LinearCorrelation.ESTIMATION_ERROR_AVERAGE, sum / valuesSize);
        dependencyElement.setStatistic(LinearCorrelation.ESTIMATION_ERROR_STD_DEVIATION, Math.sqrt(deviationSum / deviation.size()));

        return dependencyElement;
    }

    //TODO: check why outliers do not work
    //check what is with lag
    //check why outputing nicely on 
}
