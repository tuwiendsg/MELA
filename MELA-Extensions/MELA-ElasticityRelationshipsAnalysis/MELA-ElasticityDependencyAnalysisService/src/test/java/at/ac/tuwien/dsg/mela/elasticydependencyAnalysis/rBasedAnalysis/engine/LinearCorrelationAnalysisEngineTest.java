/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.engine;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.LinearCorrelation;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.Variable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author daniel-tuwien
 */
public class LinearCorrelationAnalysisEngineTest {

    public LinearCorrelationAnalysisEngineTest() {
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
     * Test of evaluateLinearCorrelation method, of class
     * LinearCorrelationAnalysisEngine.
     */
    @Test
    public void testEvaluateLinearCorrelation() {
        System.out.println("evaluateLinearCorrelation");
        Variable dependent = new Variable("dependent");
        dependent.setMetaData(Metric.class.getName(), "A");
        Variable predictor = new Variable("predictor");
        predictor.setMetaData(Metric.class.getName(), "B");

        Random random = new Random();

        int count = 10;
        int outliers = 2;

        for (int i = 0; i < count; i++) {
            dependent.addValue(0d + i);
            predictor.addValue(2d * i);
        }
        {
            //add random outliers in dependent and predictor
//        for (int i = 0; i < outliers; i++) {
//            dependent.getValues().set(0, 100d);
//            dependent.getValues().set(count-1, 100d);
//        }
        }
        //add random outliers in dependent and predictor
//        for (int i = 0; i < outliers; i++) {
//            int index = random.nextInt(count);
            predictor.getValues().set(4, 200d);
            predictor.getValues().set(5, 200d);
//        }

        List<Variable> predictors = new ArrayList<>();
        predictors.add(predictor);
//        predictor.shiftData(-1);
        
        LinearCorrelationAnalysisEngine instance = new LinearCorrelationAnalysisEngine();

        LinearCorrelation result = instance.evaluateLinearCorrelation(dependent, predictors);

        System.out.println(result.toString());

    }

}
