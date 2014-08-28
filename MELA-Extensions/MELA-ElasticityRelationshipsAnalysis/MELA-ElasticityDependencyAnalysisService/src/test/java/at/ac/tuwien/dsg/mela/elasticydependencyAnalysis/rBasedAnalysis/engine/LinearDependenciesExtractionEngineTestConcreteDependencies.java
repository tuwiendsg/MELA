///*
// * Copyright 2014 daniel-tuwien.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.engine;
//
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyCoefficient;
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyElement;
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.MonitoredElementElasticityDependency;
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ServiceElasticityDependencies;
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
//import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.api.ElasticityDependencyAnalysisManager;
//import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.persistence.PersistenceDelegate;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
///**
// *
// * @author daniel-tuwien
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({"classpath*:mela-dependencies-analysis-service-context.xml"})
//public class LinearDependenciesExtractionEngineTestConcreteDependencies {
//
//    @Autowired
//    private ElasticityDependencyAnalysisManager elasticityDependencyAnalysisManager;
//
//    @Autowired
//    private PersistenceDelegate persistenceDelegate;
//
//    public LinearDependenciesExtractionEngineTestConcreteDependencies() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() {
//    }
//
//    @AfterClass
//    public static void tearDownClass() {
//    }
//
//    @Before
//    public void setUp() {
//    }
//
//    @After
//    public void tearDown() {
//    }
//
//    /**
//     * Test of validateDependencies method, of class
//     * LinearDependenciesValidationEngine.
//     */
//    @Test
//    public void testValidateDependencies() {
//        System.out.println("validateDependencies");
//
//        MonitoredElement element = new MonitoredElement("EventProcessingServiceUnit");
//        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//
//        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace("CloudService");
//
//        Metric a = new Metric("pendingRequests", "count");
////        Metric b = new Metric("responseTime", "ms");
//        Metric b = new Metric("cpuUsage", "%");
//
//        ServiceElasticityDependencies dependencies = elasticityDependencyAnalysisManager.analyzeElasticityDependencies(element, space, a, b);
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
//            Logger.getLogger(LinearDependenciesExtractionEngineTestConcreteDependencies.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        // decided that validating using SOM is actually crap. maybe If I also construct SOM on percentages, but then it  will validate everything
//        // LinearDependenciesValidationEngine.validateDependencies(dependencies, space);
//    }
//
//}
