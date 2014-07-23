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

import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.LinearCorrelation;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rcaller.RCaller;
import rcaller.RCode;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class LinearCorrelationAnalysisEngine {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(LinearCorrelationAnalysisEngine.class);

    @Value("${R_PATH}")
    private String rPath;

    public LinearCorrelation evaluateLinearCorrelation(final Variable dependent, final List<Variable> predictors) {

        //in the case no predictor is left (purged by previous recursive calls)
        if (predictors.isEmpty()) {
            LinearCorrelation correlation = new LinearCorrelation(dependent);
            correlation.setAdjustedRSquared(Double.POSITIVE_INFINITY);
            return correlation;
        }

        // As function uses R, this is sample R output, explaining a bit why
        // things are done as they are
        // Coefficients:
        //                      Estimate Std. Error   t value Pr(>|t|)    
        // (Intercept)         1.000e+00  8.064e-17 1.240e+16   <2e-16 ***
        // cpu_idle_10.99.0.16 1.222e-18  9.245e-19 1.321e+00    0.187    
        // 
        //do 1 pass and extracT minimum length of data, to be able to trim data
        int minimLength = dependent.getValues().size();
        for (Variable v : predictors) {
            int vLength = v.getValues().size();
            if (vLength < minimLength) {
                minimLength = vLength;
            }
        }


        /*
         * Creating RCaller
         */
        RCaller caller = new RCaller();

        /*
         * Full path of the Rscript. Rscript is an executable file shipped with R.
         * It is something like C:\\Program File\\R\\bin.... in Windows
         */
        caller.setRscriptExecutable(rPath);
        RCode rCode = new RCode();

        caller.setRCode(rCode);
        caller.redirectROutputToConsole();

        String predictorNames = "";
        int coefficientIndex = 1;

        //copy values only until minimLength, thus do data trimmingg 
        //extract values such they can be trimmed to same length without affecting the input
        double[] dependentValues = new double[minimLength];
        double[][] predictorsValues = new double[predictors.size()][minimLength];

        final Map<Variable, Integer> lagMap = new ConcurrentHashMap<>();

        //for each dependent variable, I compute time lag with respect to dependent
        //then i shift it with this lag such that linear modelling is more accurate
        List<Thread> lagComputationThreads = new ArrayList<>();

        final int trimmedLength = minimLength;

        for (final Variable predictor : predictors) {

            Thread lagComputationThread = new Thread() {

                @Override
                public void run() {

                    RCaller caller = new RCaller();
                    /*
                     * Creating RCaller
                     */
                    /*
                     * Full path of the Rscript. Rscript is an executable file shipped with R.
                     * It is something like C:\\Program File\\R\\bin.... in Windows
                     */
                    caller.setRscriptExecutable(rPath);
                    RCode rCode = new RCode();
                    try {

                        caller.setRCode(rCode);
                        caller.redirectROutputToConsole();

                        List<Double> dependentData = dependent.getValues();
                        List<Double> predictorData = predictor.getValues();

                        double[] dependentDataAsDoubleArray = new double[trimmedLength];
                        for (int i = 0; i < trimmedLength; i++) {
                            dependentDataAsDoubleArray[i] = dependentData.get(i);
                        }

                        double[] predictorDataAsDoubleArray = new double[trimmedLength];
                        for (int i = 0; i < trimmedLength; i++) {
                            predictorDataAsDoubleArray[i] = predictorData.get(i);
                        }

                        rCode.addDoubleArray("dependent", dependentDataAsDoubleArray);
                        rCode.addDoubleArray("predictor", predictorDataAsDoubleArray);

                        rCode.addRCode("res <- ccf(dependent,predictor,plot = FALSE)");
                        rCode.addRCode("lag <- res$lag[which.max(res$acf)]");

                        caller.runAndReturnResult("lag");

                        int lag = caller.getParser().getAsIntArray("lag")[0];

                        //shift predictor according to its lag
                        predictor.shiftData(lag);

                        //revert lag to make more sense when computing dependent from coefficient
                        lagMap.put(predictor, -1 * lag);

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        caller.stopStreamConsumers();
                        log.error("Start Logging code which generated previous error -------------");
                        log.error(rCode.getCode().toString());
                        log.error("End logging code which generated previous error -------------");
                    }

                }
            };

            lagComputationThreads.add(lagComputationThread);

        }

        for (Thread t : lagComputationThreads) {
            t.setDaemon(true);
            t.start();
        }
        for (Thread t : lagComputationThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(LinearCorrelationAnalysisEngine.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }

        //resume as normal with linear regression
        //add values in R as variables 
        {
            List<Double> data = dependent.getValues();
            for (int i = 0; i < minimLength; i++) {
                dependentValues[i] = data.get(i);
            }
            rCode.addDoubleArray(dependent.getId(), dependentValues);

        }

        {
            for (int j = 0; j < predictors.size(); j++) {
                Variable v = predictors.get(j);

                List<Double> data = v.getValues();
                predictorsValues[j] = new double[minimLength];

                for (int i = 0; i < minimLength; i++) {
                    predictorsValues[j][i] = data.get(i);
                }
                rCode.addDoubleArray(v.getId(), predictorsValues[j]);
                predictorNames += "+" + v.getId();
            }
        }

        predictorNames = predictorNames.substring(1, predictorNames.length());

        //add code which performs linear evaluation in R
        rCode.addRCode("res <- lm(" + dependent.getId() + "~" + predictorNames + ")");

        Logger.getLogger(LinearCorrelationAnalysisEngine.class.getName()).log(Level.DEBUG, "res <- lm(" + dependent.getId() + "~" + predictorNames + ")");

//        System.out.println("res <- lm(" + dependent.getId() + "~" + predictorNames + ")");
        //start processing result
        caller.runAndReturnResult("res");

        //will contain all removed predictors
        //predictors are removed if their coeff is NaN or if their predicted error > estimated/10
        List<Variable> predictorsToRemove = new ArrayList<Variable>();

        //get coefficients
        {

            String[] results = caller.getParser().getAsStringArray("coefficients");

            //check if any coefficient has NaS as number, indicating it does not belong to relationship
            //from 1 to skip Interceptor, as linear dependency is f(dependent) = interceptor + coeff_1*predictor_1+...+coeff_n*predictor_n
            for (int i = 1; i < results.length; i++) {
                //remove predictor if coeff is NA (not applicable) 
                if (results[i].equalsIgnoreCase("NA")) {
                    //remove predictor
                    predictorsToRemove.add(predictors.get(i - 1));
                }
            }

        }

        //if we have coefficients to remove, then remove them and re-evaluate result.
        if (!predictorsToRemove.isEmpty()) {
            for (Variable toRemove : predictorsToRemove) {
                predictors.remove(toRemove);
            }
            //call the evalaution recursively
            return evaluateLinearCorrelation(dependent, predictors);
        } else {

            //reinitialize the caller to be able to get another variable from the result
            {
                caller.stopStreamConsumers();
                caller = new RCaller();
                /*
                 * Full path of the Rscript. Rscript is an executable file shipped with R.
                 * It is something like C:\\Program File\\R\\bin.... in Windows
                 */
                caller.setRscriptExecutable(rPath);
                caller.setRCode(rCode);
                rCode.addRCode("res <- summary(res)");
                caller.runAndReturnResult("res");
            }

            //else continue and remove elements which have too large an estimation error 
            //result is returned as : 4 means result always has 4 columns: Estimate,  Std. Error, t value, and Pr(>|t|) 
            // predictors.size()+1 is the number of variables for which coeff are returned; intercept and predictors
            double[][] results = caller.getParser().getAsDoubleMatrix("coefficients", 4, predictors.size() + 1);

            double[] estimates = results[0];
            double[] estimateError = results[1];
            double[] pr = results[2]; //show P coefficient for each

            //interceptor result is on position [0]
            for (int i = 1; i < estimates.length; i++) {
                //if estimated error is not one order of magnitude smaller than the estimate's absolute, remove coefficient
                if (estimateError[i] > (Math.abs(estimates[i]) / 10)) {
                    predictorsToRemove.add(predictors.get(i - 1));
                }
            }
            //if we have removed more predictors, redo the eval
            if (!predictorsToRemove.isEmpty()) {
                for (Variable v : predictorsToRemove) {
                    predictors.remove(v);
                }
                return evaluateLinearCorrelation(dependent, predictors);
            } else {
                //else construct result and return
                //prepare result
                LinearCorrelation correlation = new LinearCorrelation(dependent);
                List<LinearCorrelation.Coefficient> coefficients = correlation.getPredictors();

                //interceptor result is on position [0]
                correlation.setIntercept(estimates[0]);

                for (int i = 1; i < estimates.length; i++) {
                    //predictors i-1 to offset the fact that on [0] we have intercept
                    LinearCorrelation.Coefficient coefficient = new LinearCorrelation.Coefficient(predictors.get(i - 1), estimates[i], estimateError[i], lagMap.get(predictors.get(i - 1)));
                    coefficients.add(coefficient);
                }

                //issue another R call to get adjusted R
                {
                    caller.stopStreamConsumers();
                    caller = new RCaller();

                    /*
                     * Full path of the Rscript. Rscript is an executable file shipped with R.
                     * It is something like C:\\Program File\\R\\bin.... in Windows
                     */
                    caller.setRscriptExecutable(rPath);
                    caller.setRCode(rCode);
                    caller.runAndReturnResult("res");
                    double[] adjustedR = caller.getParser().getAsDoubleArray("adj_r_squared");
                    correlation.setAdjustedRSquared(adjustedR[0]);
                }
                return correlation;

            }

        }

    }
}
