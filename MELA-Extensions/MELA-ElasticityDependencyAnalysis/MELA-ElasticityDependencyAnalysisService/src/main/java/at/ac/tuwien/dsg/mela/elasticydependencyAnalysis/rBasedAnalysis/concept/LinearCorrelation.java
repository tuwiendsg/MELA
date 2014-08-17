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
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class LinearCorrelation {

    public static final String ADJUSTED_R = "adjustedR";
    public static final String ESTIMATION_ERROR_MIN = "minEstimationError";
    public static final String ESTIMATION_ERROR_MAX = "maxEstimationError";
    public static final String ESTIMATION_ERROR_AVERAGE = "avgEstimationError";
    public static final String ESTIMATION_ERROR_STD_DEVIATION = "absoluteStandardDeviation";

    private Variable dependent;
    private double intercept;

    private List<Coefficient> predictors;

    private Map<String, Double> statistics;

    {
        statistics = new ConcurrentHashMap<>();
        predictors = new ArrayList<Coefficient>();
    }

    public LinearCorrelation() {
    }

    public LinearCorrelation(Variable dependent) {
        this.dependent = dependent;
    }

    public double getIntercept() {
        return intercept;
    }

    public void setIntercept(double intercept) {
        this.intercept = intercept;
    }

    public double getAdjustedRSquared() {
        return statistics.get(ADJUSTED_R);
    }

    public void setAdjustedRSquared(double adjustedRSquared) {
        this.statistics.put(ADJUSTED_R, adjustedRSquared);
    }

    public double getStatistic(String statistic) {
        return statistics.get(statistic);
    }

    public void setStatistic(String statistic, Double value) {
        this.statistics.put(statistic, value);
    }

    public static class Coefficient {

        private Variable variable;
        private double coefficient;
        private double stdError;

        //lag of Dependent with respect to Coefficient
        //a lag of 2 implies the Coefficient is at +2 w.r.t. Dependent
        private int lag;

        public Variable getVariable() {
            return variable;
        }

        public void setVariable(Variable variable) {
            this.variable = variable;
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

        public Coefficient() {
        }

        public Coefficient(Variable v, double coefficient) {
            this.variable = v;
            this.coefficient = coefficient;
        }

        public Coefficient(Variable v, double coefficient, double stdError) {
            this.variable = v;
            this.coefficient = coefficient;
            this.stdError = stdError;
        }

        public Coefficient(Variable v, double coefficient, double stdError, int lag) {
            this.variable = v;
            this.coefficient = coefficient;
            this.stdError = stdError;
            this.lag = lag;
        }

        public int getLag() {
            return lag;
        }

        public void setLag(int lag) {
            this.lag = lag;
        }

    }

    public Variable getDependent() {
        return dependent;
    }

    public void setDependent(Variable dependent) {
        this.dependent = dependent;
    }

    public List<Coefficient> getPredictors() {
        return predictors;
    }

    public void setPredictors(List<Coefficient> predictors) {
        this.predictors = predictors;
    }

    public void addPredictors(List<Coefficient> predictors) {
        this.predictors.addAll(predictors);
    }

    public void addPredictor(Coefficient predictor) {
        this.predictors.add(predictor);
    }

    public void removePredictors(List<Coefficient> predictors) {
        this.predictors.removeAll(predictors);
    }

    public void removePredictor(Coefficient predictor) {
        this.predictors.remove(predictor);
    }

    /**
     * Corelation exists if this.adjustedRSquared < Double.POSITIVE_INFINITY
     * @return
     */
    public boolean existsCorrelation() {
        return this.getAdjustedRSquared() < Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        String description = "LinearCorrelation {RSquared= " + this.getAdjustedRSquared() + ":    " + dependent.getId() + " = " + intercept + " ";
        for (Coefficient c : predictors) {
            description += "+ " + c.coefficient + "*" + c.variable.getId();
        }
        description += '}';
        return description;
    }

    public LinearCorrelation withDependent(final Variable dependent) {
        this.dependent = dependent;
        return this;
    }

    public LinearCorrelation withIntercept(final double intercept) {
        this.intercept = intercept;
        return this;
    }

    public LinearCorrelation withPredictors(final List<Coefficient> predictors) {
        this.predictors = predictors;
        return this;
    }

    public LinearCorrelation withAdjustedRSquared(final double adjustedRSquared) {
        this.setAdjustedRSquared(adjustedRSquared);
        return this;
    }
    
    
    public LinearCorrelation withStatistic(final String statisticName, final double value) {
        this.setStatistic(statisticName, value);
        return this;
    }
    
    

}
