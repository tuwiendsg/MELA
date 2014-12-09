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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.LinearCorrelation;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.Variable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import rcaller.Globals;
import rcaller.RCaller;
import rcaller.RCode;
import static scala.xml.Null.value;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class LinearCorrelationAnalysisEngine {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(LinearCorrelationAnalysisEngine.class);
    static final org.slf4j.Logger rProcesssingLogger = LoggerFactory.getLogger("at.ac.tuwien.dsg.rProcessing");

    public LinearCorrelation evaluateLinearCorrelation(final Variable dependent, final List<Variable> predictors) {

        //in the case no predictor is left (purged by previous recursive calls)
        if (predictors.isEmpty()) {
            LinearCorrelation correlation = new LinearCorrelation(dependent);
            correlation.setAdjustedRSquared(Double.POSITIVE_INFINITY);
            return correlation;
        }

        { // log
            if (dependent.getMetaData().containsKey(Metric.class.getName()) && dependent.getMetaData().containsKey(MonitoredElement.class.getName())) {
                String predictorsNames = ((Metric) dependent.getMetaData(Metric.class.getName())).getName() + ":" + ((MonitoredElement) dependent.getMetaData(MonitoredElement.class.getName())).getId() + " <- ";
                for (Variable v : predictors) {
                    predictorsNames += ((Metric) v.getMetaData(Metric.class.getName())).getName() + ":" + ((MonitoredElement) v.getMetaData(MonitoredElement.class.getName())).getId() + ", ";
                }
                rProcesssingLogger.info("Evaluating " + predictorsNames);
            }
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
        String descr = dependent.getMetaData(Metric.class.getName()).toString() + ".length=" + minimLength + " ";
        for (Variable v : predictors) {
            int vLength = v.getValues().size();
            descr += ", " + v.getMetaData(Metric.class.getName()).toString() + ".length=" + vLength + " , ";
            if (vLength < minimLength) {
                minimLength = vLength;
            }
        }

//        log.info(
//                "Min value " + minimLength);
//        log.info(
//                "Classified DATA lengths " + descr);

        /*
         * Creating RCaller
         */
        RCaller caller = new RCaller();

        Globals.detect_current_rscript();

        caller.setRscriptExecutable(Globals.Rscript_current);
        RCode rCode = new RCode();

        caller.setRCode(rCode);

        //caller.redirectROutputToConsole();

        String predictorNames = "";
//
        //compute outliers and remove them
        //for each dependent variable, I compute time lag with respect to dependent
        //then i shift it with this lag such that linear modelling is more accurate
        List<Thread> outliersRemovalThreads = new ArrayList<>();

        List<Variable> allvars = new ArrayList<>(predictors);
        allvars.add(dependent);

        for (final Variable v : allvars) {
//
            Thread outliersRemovalThread = new Thread() {

                @Override
                public void run() {
                    RCaller lagCaller = new RCaller();
                    Globals.detect_current_rscript();
                    lagCaller.setRscriptExecutable(Globals.Rscript_current);

                    RCode lagRCode = new RCode();

                    String evaluationCommand = "";

                    try {

                        lagCaller.setRCode(lagRCode);
                        lagCaller.redirectROutputToConsole();

                        List<Double> data = v.getValues();

                        double[] vDataAsDoubleArray = new double[v.getValues().size()];
                        for (int i = 0; i < v.getValues().size(); i++) {
                            vDataAsDoubleArray[i] = data.get(i);
                        }

                        lagRCode.addDoubleArray(v.getId(), vDataAsDoubleArray);

                        lagRCode.addRCode("outLiers <- boxplot(" + v.getId() + ",plot=FALSE)$out");

                        evaluationCommand = "outLiers <- boxplot(" + v.getId() + ",plot=FALSE)$out";

                        lagCaller.runAndReturnResult("outLiers");

                        double[] outliers = lagCaller.getParser().getAsDoubleArray("outLiers");

                        //list to be ablke to check if equals really fast
                        List<Double> outliersList = new ArrayList<>();
                        for (int i = 0; i < outliers.length; i++) {
                            BigDecimal bd = new BigDecimal(outliers[i]);
                            bd = bd.setScale(4, RoundingMode.HALF_UP);
                            outliersList.add(bd.doubleValue());
                        }

                        //go trough variable values and remove outliers
                        //replace outliers by their neighbours average
                        for (int i = 0; i < v.getValues().size(); i++) {

                            BigDecimal bd = new BigDecimal(data.get(i));
                            bd = bd.setScale(4, RoundingMode.HALF_UP);
                            Double value = bd.doubleValue();

                            if (outliersList.contains(value)) {

                                double prevValue = 0d;
                                double nextValue = 0d;

                                //get previous and next which are NOT outliers and compute average, and put in place of outlier
                                for (int j = i - 1; j > 0; j--) {

                                    BigDecimal bdPrev = new BigDecimal(data.get(j));
                                    bdPrev = bdPrev.setScale(4, RoundingMode.HALF_UP);
                                    Double prevValueDouble = bdPrev.doubleValue();

                                    if (!outliersList.contains(prevValueDouble)) {
                                        prevValue = data.get(j);
                                        break;
                                    }
                                }

                                //get next which are NOT outliers and compute average, and put in place of outlier
                                for (int j = i + 1; j < v.getValues().size(); j++) {

                                    BigDecimal bdNext = new BigDecimal(data.get(j));
                                    bdNext = bdNext.setScale(4, RoundingMode.HALF_UP);
                                    Double nextValueDouble = bdNext.doubleValue();

                                    if (!outliersList.contains(nextValueDouble)) {
                                        nextValue = data.get(j);
                                        break;
                                    }
                                }

                                int divAmount = 1;
                                if (prevValue > 0 && nextValue > 0) {
                                    divAmount = 2;
                                }

                                data.set(i, (prevValue + nextValue) / divAmount);

                            }
                        }

//                        {
//
//                            double[] dataAfter = new double[v.getValues().size()];
//                            for (int i = 0; i < v.getValues().size(); i++) {
//                                dataAfter[i] = data.get(i);
//                            }
//                            rCode.addDoubleArray("AFTER_" + v.getId(), dataAfter);
//
//                        }
//                        log.info(lagRCode.getCode().toString());
                    } catch (Exception e) {

                        log.error(evaluationCommand);
                        log.error(e.getMessage(), e);

                        lagCaller.stopStreamConsumers();
                        //log.error("Start Logging code which generated previous error -------------");
                        //log.error(lagRCode.getCode().toString());
                        //log.error("End logging code which generated previous error -------------");
                    }

                }
            };

            outliersRemovalThreads.add(outliersRemovalThread);
        }

        for (Thread t : outliersRemovalThreads) {
            t.setDaemon(true);
            t.start();
        }
        for (Thread t : outliersRemovalThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(LinearCorrelationAnalysisEngine.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }

        //for each dependent variable, I compute time lag with respect to dependent
        //then i shift it with this lag such that linear modelling is more accurate
        List<Thread> lagComputationThreads = new ArrayList<>();

        final int trimmedLength = minimLength;

        for (final Variable predictor : predictors) {

            Thread lagComputationThread = new Thread() {

                @Override
                public void run() {

                    RCaller lagCaller = new RCaller();
                    Globals.detect_current_rscript();
                    lagCaller.setRscriptExecutable(Globals.Rscript_current);

                    String evaluationCommand = "";

                    RCode lagRCode = new RCode();
                    try {

                        lagCaller.setRCode(lagRCode);
                        lagCaller.redirectROutputToConsole();

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

                        lagRCode.addDoubleArray(dependent.getId(), dependentDataAsDoubleArray);
                        lagRCode.addDoubleArray(predictor.getId(), predictorDataAsDoubleArray);

                        lagRCode.addRCode("res <- ccf(" + dependent.getId() + "," + predictor.getId() + ",plot = FALSE)");
                        lagRCode.addRCode("lag <- res$lag[which.max(res$acf)]");

                        evaluationCommand = "res <- ccf(" + dependent.getId() + "," + predictor.getId() + ",plot = FALSE)";

                        lagCaller.runAndReturnResult("lag");

                        int lags[] = lagCaller.getParser().getAsIntArray("lag");
                        int lag = 0;
                        if (lags != null && lags.length > 0) {
                            lag = lags[0];
                        }

                        //shift predictor according to its lag
                        predictor.shiftData(lag);
                        
//                        rProcesssingLogger.debug(lagRCode.toString());

                        //revert lag to make more sense when computing dependent from coefficient
//                        lagMap.put(predictor, -1 * lag);
                    } catch (Exception e) {

                        log.error(evaluationCommand);
                        log.error(e.getMessage(), e);
                        lagCaller.stopStreamConsumers();
                        //log.error("Start Logging code which generated previous error -------------");
                        log.info(lagRCode.getCode().toString());

                        //log.error("End logging code which generated previous error -------------");
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
        //copy values only until minimLength, thus do data trimmingg 
        //extract values such they can be trimmed to same length without affecting the input
        double[] dependentValues = new double[minimLength];
        double[][] predictorsValues = new double[predictors.size()][minimLength];

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

//        log.info(rCode.getCode().toString());
        try {
            caller.runAndReturnResult("res");

        } catch (Exception e) {

            log.error("res <- lm(" + dependent.getId() + "~" + predictorNames + ")");
            log.error(e.getMessage(), e);

            //if error, something wrong with result. So, means some metric has invalid values at one point. 
            //As I do not know which, I go through each combination of predictors, and test if I can get non-null dependencies
            for (Variable v : predictors) {
                List<Variable> otherPredictors = new ArrayList<>();
                for (Variable p : predictors) {
                    if (p.equals(v)) {
                        continue;
                    }
                    otherPredictors.add(p);
                }

                LinearCorrelation correlation = evaluateLinearCorrelation(dependent, otherPredictors);
                if (correlation != null) {
                    return correlation;
                }

            }

        }

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
                Globals.detect_current_rscript();
                caller.setRscriptExecutable(Globals.Rscript_current);

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
                    LinearCorrelation.Coefficient coefficient = new LinearCorrelation.Coefficient(predictors.get(i - 1), estimates[i], estimateError[i], 0);
                    coefficients.add(coefficient);
                }

                //issue another R call to get adjusted R
                {
                    caller.stopStreamConsumers();
                    caller = new RCaller();
                    Globals.detect_current_rscript();
                    caller.setRscriptExecutable(Globals.Rscript_current);

                    caller.setRCode(rCode);
                    caller.runAndReturnResult("res");
                    double[] adjustedR = caller.getParser().getAsDoubleArray("adj_r_squared");
                    correlation.setAdjustedRSquared(adjustedR[0]);
                }

                return correlation;

            }

        }

    }

    //TODO: check why outliers do not work
    //check what is with lag
    //check why outputing nicely on 
}
