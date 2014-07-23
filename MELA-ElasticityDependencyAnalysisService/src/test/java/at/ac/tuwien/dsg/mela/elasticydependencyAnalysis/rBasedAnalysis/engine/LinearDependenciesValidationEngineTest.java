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

import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.api.ElasticityDependencyAnalysisManager;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rcaller.RCaller;
import rcaller.RCode;

/**
 *
 * @author daniel-tuwien
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath*:mela-dependencies-analysis-service-context.xml"})
public class LinearDependenciesValidationEngineTest {

//    @Autowired
//    private ElasticityDependencyAnalysisManager elasticityDependencyAnalysisManager;
//
//    @Autowired
//    private PersistenceDelegate persistenceDelegate;

    public LinearDependenciesValidationEngineTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of validateDependencies method, of class
     * LinearDependenciesValidationEngine.
     */
    @Test
    public void testValidateDependencies() {
//        System.out.println("validateDependencies");
//
//        MonitoredElement element = new MonitoredElement("CloudService");
//        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
//
//        ServiceElasticityDependencies dependencies = elasticityDependencyAnalysisManager.analyzeElasticityDependencies(element);
//
//        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(element.getId());
//
//        List<List<String>> column = new ArrayList<>();
//
//        for (MonitoredElementElasticityDependency dependency : dependencies.getElasticityDependencies()) {
//            Map<Metric, List<MetricValue>> dataForDependentElement = space.getMonitoredDataForService(dependency.getMonitoredElement());
//            for (ElasticityDependencyElement dependencyElement : dependency.getContainedElements()) {
//
//                List<String> dataColumn = new ArrayList<>();
//                column.add(dataColumn);
//
//                List<MetricValue> dependentMetricValues = dataForDependentElement.get(dependencyElement.getDependentMetric());
//
//                String dependencyDescription = dependencyElement.getDependentMetric().getName() + ":" + dependency.getMonitoredElement().getId() + "= " + dependencyElement.getInterceptor() + " + ";
//
//                for (ElasticityDependencyCoefficient coefficient : dependencyElement.getCoefficients()) {
//                    dependencyDescription += "" + coefficient.getCoefficient() + "*" + coefficient.getMetric().getName() + ":" + coefficient.getMonitoredElement().getId();
//                }
//
////                System.out.println("\n !!! " + dependencyDescription);
//                dataColumn.add(dependencyDescription);
//
//                //just test if the prediction applies
//                {
//                    List<String> dependencyDescriptionInStrings = new ArrayList<>();
//                    List<Double> computed = new ArrayList<>();
//
//                    //instantiate with name of dependent metric and monitored value
//                    for (MetricValue value : dependentMetricValues) {
//                        dependencyDescriptionInStrings.add(value.getValueRepresentation() + "= ");
//                        computed.add(0.0d);
//                    }
//
//                    for (ElasticityDependencyCoefficient dependencyCoefficient : dependencyElement.getCoefficients()) {
//                        List<MetricValue> coefficientMetricValues = space.getMonitoredDataForService(dependencyCoefficient.getMonitoredElement()).get(dependencyCoefficient.getMetric());
//                        for (int i = 0; i < dependentMetricValues.size() && i < coefficientMetricValues.size(); i++) {
//                            String descr = dependencyDescriptionInStrings.get(i);
//                            descr += "" + dependencyCoefficient.getCoefficient() + "*" + coefficientMetricValues.get(i) + "+";
//                            dependencyDescriptionInStrings.set(i, descr);
//                            computed.set(i, computed.get(i) + (dependencyCoefficient.getCoefficient() * (Double) coefficientMetricValues.get(i).getValue()));
//                        }
//                    }
//                    for (int i = 0; i < computed.size(); i++) {
////                        System.out.println(dependencyDescriptionInStrings.get(i));
//                        dataColumn.add(dependentMetricValues.get(i).getValueRepresentation() + "= " + computed.get(i));
//                    }
//                }
//            }
//        }
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./experiments/predictedShit.csv")));
//
//            //max nr of records
//            int max = column.stream().max(new Comparator<List<String>>() {
//
//                @Override
//                public int compare(List<String> o1, List<String> o2) {
//                    return ((Integer) o1.size()).compareTo(o2.size());
//                }
//
//            }).get().size();
//
//            for (int i = 0; i < max; i++) {
//                for (List<String> values : column) {
//                    String columnEntry = (values.size() > i) ? values.get(i) : "";
//                    writer.write(columnEntry + ",");
//                }
//                writer.newLine();
//            }
//
//            writer.flush();
//            writer.close();
//
//        } catch (IOException ex) {
//            Logger.getLogger(LinearDependenciesValidationEngineTest.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        // decided that validating using SOM is actually crap. maybe If I also construct SOM on percentages, but then it  will validate everything
//        // LinearDependenciesValidationEngine.validateDependencies(dependencies, space);

        /*
         * Creating RCaller
         */
        RCaller caller = new RCaller();

        /*
         * Full path of the Rscript. Rscript is an executable file shipped with R.
         * It is something like C:\\Program File\\R\\bin.... in Windows
         */
        caller.setRscriptExecutable("/usr/bin/Rscript");
        RCode rCode = new RCode();

        caller.setRCode(rCode);
        caller.redirectROutputToConsole();

        String predictorNames = "";
        int coefficientIndex = 1;

        //copy values only until minimLength, thus do data trimmingg 
        //extract values such they can be trimmed to same length without affecting the input
        double[] a = new double[500];
        double[] b = new double[500];

        for (int i = 0; i < 450; i += 50) {
            if (i % 100 == 0) {
                for (int j = i; j < i + 50; j++) {
                    a[j] = 0;
                }
            } else {
                for (int j = i; j < i + 50; j++) {
                    a[j] = 1;
                }
            }
        }

        //shift A by 5 in B
        for (int i = 0; i < 2; i++) {
            b[i] = 0;
        }

        for (int i = 2; i < 500; i++) {
            b[i] = a[i - 2];
        }

        rCode.addDoubleArray("a", a);
        rCode.addDoubleArray("b", b);

        //add code which performs linear evaluation in R
        rCode.addRCode("res <- ccf(a,b,plot = FALSE)");
        rCode.addRCode("lag <- res$lag[which.max(res$acf)]");

        caller.runAndReturnResult("lag");

        //will contain all removed predictors
        //predictors are removed if their coeff is NaN or if their predicted error > estimated/10
        List<Variable> predictorsToRemove = new ArrayList<Variable>();

        //get coefficients
        {

            for (String name : caller.getParser().getNames()) {

                //check if any coefficient has NaS as number, indicating it does not belong to relationship
                //from 1 to skip Interceptor, as linear dependency is f(dependent) = interceptor + coeff_1*predictor_1+...+coeff_n*predictor_n
                System.out.println(name);

            }

        }

        int[] lag = caller.getParser().getAsIntArray("lag");

        if (lag[0] > 0) {

            for (int i = 0; i < b.length; i++) {
                int target = i + lag[0];
                if (target > b.length - 1) {
                    target -= b.length - 1;
                }
                b[target] = b[i];
            }

        } else {
            for (int i = 0; i < b.length; i++) {
                int target = i + lag[0];
                if (target < 0) {
                    target += b.length - 1;
                }
                b[target] = b[i];
            }

        }

        caller.stopStreamConsumers();

        //shift B acording to A's lag
        caller = new RCaller();
        /*
         * Full path of the Rscript. Rscript is an executable file shipped with R.
         * It is something like C:\\Program File\\R\\bin.... in Windows
         */
        caller.setRscriptExecutable("/usr/bin/Rscript");
        caller.setRCode(rCode);

        rCode.addDoubleArray("a", a);
        rCode.addDoubleArray("b", b);

        //add code which performs linear evaluation in R
        rCode.addRCode("res <- ccf(a,b,plot = FALSE)");
//        rCode.addRCode("lag <- res$lag[which.max(res$acf)]");
//
//        caller.runAndReturnResult("lag");
//        //get coefficients
//        {
//
//            for (String name : caller.getParser().getNames()) {
//
//                //check if any coefficient has NaS as number, indicating it does not belong to relationship
//                //from 1 to skip Interceptor, as linear dependency is f(dependent) = interceptor + coeff_1*predictor_1+...+coeff_n*predictor_n
//                System.out.println(name);
//
//            }
//
//        }
//
//        lag = caller.getParser().getAsIntArray("lag");
//        System.out.println(lag);
        //recompute lag
//        
//        
        //add code which performs linear evaluation in R
        rCode.addRCode("res <- lm(a~b)");


//        System.out.println("res <- lm(" + dependent.getId() + "~" + predictorNames + ")");
        //start processing result
        caller.runAndReturnResult("res");

        String result = "";
        //will contain all removed predictors
        //predictors are removed if their coeff is NaN or if their predicted error > estimated/10

        //get coefficients
        {

            String[] results = caller.getParser().getAsStringArray("coefficients");

            //check if any coefficient has NaS as number, indicating it does not belong to relationship
            //from 1 to skip Interceptor, as linear dependency is f(dependent) = interceptor + coeff_1*predictor_1+...+coeff_n*predictor_n
            result += results[0] + " ";
            for (int i = 1; i < results.length; i++) {
                //remove predictor if coeff is NA (not applicable) 
                    //remove predictor
                    result += results[i] + " *b";
            }

        }
        
        System.out.println(result);
    }

}
