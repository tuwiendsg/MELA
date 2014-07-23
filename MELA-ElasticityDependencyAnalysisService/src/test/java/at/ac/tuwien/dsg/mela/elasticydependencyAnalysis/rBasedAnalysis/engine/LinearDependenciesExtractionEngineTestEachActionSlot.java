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
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
//import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
//import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Action;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
//import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.api.ElasticityDependencyAnalysisManager;
//import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.persistence.PersistenceDelegate;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.text.DecimalFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
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
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
///**
// *
// * Splits the monitored data after executed actions Extracts dependencies on
// * each set of monitoring data between two action executions Checks what
// * dependencies still hold between action executions
// *
// * @author daniel-tuwien
// */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({"classpath*:mela-dependencies-analysis-service-context.xml"})
//public class LinearDependenciesExtractionEngineTestEachActionSlot {
//
//    @Autowired
//    private ElasticityDependencyAnalysisManager elasticityDependencyAnalysisManager;
//
//    @Autowired
//    @Qualifier("ElasticyDependencyAnalysisPersistenceDelegate")
//    private PersistenceDelegate persistenceDelegate;
//
//    public LinearDependenciesExtractionEngineTestEachActionSlot() {
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
//
//        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration("SSSSSSS");
//
//        List<ServiceMonitoringSnapshot> monitoringData = persistenceDelegate.extractMonitoringData(cfg.getServiceConfiguration().getId());
//
//        //trim data
////        monitoringData = monitoringData.subList(0, 50);
//
//        MonitoredElement element = cfg.getServiceConfiguration();
//
//        //map <TimestampID, actions>
//        Map<Integer, String> actions = new LinkedHashMap<>();
//        Map<Integer, List<ServiceMonitoringSnapshot>> monitoredDataSplitAfterActions = new LinkedHashMap<>();
//
//        {
//            String currentExecutingAction = "";
//            List<ServiceMonitoringSnapshot> snapshots = new ArrayList<>();
//            boolean actionExecuting = false;
//
//            for (ServiceMonitoringSnapshot monitoringSnapshot : monitoringData) {
//                String snapshotAction = "";
//                for (Map<MonitoredElement, MonitoredElementMonitoringSnapshot> m : monitoringSnapshot.getMonitoredData().values()) {
//                    for (MonitoredElementMonitoringSnapshot snapshot : m.values()) {
//                        for (Action action : snapshot.getExecutingActions()) {
//                            snapshotAction += action.getAction() + ":" + action.getTargetEntityID() + " and ";
//                        }
//                    }
//                }
//               //if new action appeared, follow it until it dissapears.
//
//                //means action ended
//                if (snapshotAction.length() == 0 && actionExecuting == true) {
//                    actionExecuting = false;
//                    actions.put(monitoringSnapshot.getTimestampID(), currentExecutingAction);
//                    monitoredDataSplitAfterActions.put(monitoringSnapshot.getTimestampID(), snapshots);
//                    snapshots = new ArrayList<>();
////                    currentExecutingAction = "";
//
//                } else if (snapshotAction.length() > 0 && actionExecuting == false) {
//                    actionExecuting = true;
//                    currentExecutingAction = snapshotAction;
//                }
//
//                snapshots.add(monitoringSnapshot);
//
//            }
//        }
//
//        Runtime.getRuntime().gc();
//
//        for (Integer timestamp : actions.keySet()) {
//            System.out.println("" + timestamp + ": " + actions.get(timestamp) + " data: " + monitoredDataSplitAfterActions.get(timestamp).size());
//        }
//
////        System.exit(1);
////        String[] timestamps = new String[actionsExecutionTime.keySet().size()];
////        timestamps = actionsExecutionTime.keySet().toArray(timestamps);
//        //create elasticity spaces from monitored snapshots
//        Map<Integer, ServiceElasticityDependencies> discoveredDependencies = Collections.synchronizedMap(new LinkedHashMap<>());
//        Map<String, List<Integer>> dependenciesPerTimestamp = new LinkedHashMap<>();
//        {
////            List<Thread> toWaitFor = new ArrayList<>();
//
//            //need to shift action names
//            String actionsName = "";
//            for (Integer timestamp : monitoredDataSplitAfterActions.keySet()) {
//
////                Thread t = new Thread() {
////                    @Override
////                    public void run() {
//                ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(element);
//                fct.setRequirements(cfg.getRequirements());
//                fct.trainElasticitySpace(monitoredDataSplitAfterActions.get(timestamp));
//                ElasticitySpace space = fct.getElasticitySpace();
//                ServiceElasticityDependencies dependencies = elasticityDependencyAnalysisManager.analyzeElasticityDependencies(element, fct.getElasticitySpace());
//                discoveredDependencies.put(timestamp, dependencies);
//                Runtime.getRuntime().gc();
//
//                List<List<String>> column = new ArrayList<>();
//
//                for (MonitoredElementElasticityDependency dependency : dependencies.getElasticityDependencies()) {
//                    Map<Metric, List<MetricValue>> dataForDependentElement = space.getMonitoredDataForService(dependency.getMonitoredElement());
//                    for (ElasticityDependencyElement dependencyElement : dependency.getContainedElements()) {
//
//                        List<String> dataColumn = new ArrayList<>();
//                        column.add(dataColumn);
//
//                        List<MetricValue> dependentMetricValues = dataForDependentElement.get(dependencyElement.getDependentMetric());
//
//                        String dependencyDescription = dependencyElement.getDependentMetric().getName() + ":" + dependency.getMonitoredElement().getId() + "= " + new DecimalFormat("#0.##").format(dependencyElement.getInterceptor()) + " + ";
//
//                        for (ElasticityDependencyCoefficient coefficient : dependencyElement.getCoefficients()) {
//                            dependencyDescription += "" + new DecimalFormat("#0.##").format(coefficient.getCoefficient()) + "*" + coefficient.getMetric().getName() + ":" + coefficient.getMonitoredElement().getId() + " + ";
//                        }
//
//                        if (dependenciesPerTimestamp.containsKey(dependencyDescription)) {
//                            dependenciesPerTimestamp.get(dependencyDescription).add(timestamp);
//                        } else {
//                            List<Integer> stamps = new ArrayList<>();
//                            stamps.add(timestamp);
//                            dependenciesPerTimestamp.put(dependencyDescription, stamps);
//                        }
//
//                        dataColumn.add(dependencyDescription);
//
//                        //just test if the prediction applies
//                        {
//                            List<String> dependencyDescriptionInStrings = new ArrayList<>();
//                            List<Double> computed = new ArrayList<>();
//
//                            //instantiate with name of dependent metric and monitored value
//                            for (MetricValue value : dependentMetricValues) {
//                                dependencyDescriptionInStrings.add(value.getValueRepresentation() + "= ");
//                                computed.add(0.0d);
//                            }
//
//                            for (ElasticityDependencyCoefficient dependencyCoefficient : dependencyElement.getCoefficients()) {
//                                List<MetricValue> coefficientMetricValues = space.getMonitoredDataForService(dependencyCoefficient.getMonitoredElement()).get(dependencyCoefficient.getMetric());
//                                for (int i = 0; i < dependentMetricValues.size() && i < coefficientMetricValues.size(); i++) {
//                                    String descr = dependencyDescriptionInStrings.get(i);
//                                    descr += "" + new DecimalFormat("#0.00").format(dependencyCoefficient.getCoefficient()) + "*" + coefficientMetricValues.get(i) + "+";
//                                    dependencyDescriptionInStrings.set(i, descr);
//                                    computed.set(i, computed.get(i) + (dependencyCoefficient.getCoefficient() * (Double) coefficientMetricValues.get(i).getValue()));
//                                }
//                            }
//                            for (int i = 0; i < computed.size(); i++) {
//                                dataColumn.add(dependentMetricValues.get(i).getValueRepresentation() + "= " + computed.get(i));
//                            }
//
//                        }
//                    }
//                }
//
//                try {
//                    BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./experiments/predicted_t_" + timestamp + "_after_" + actionsName + ".csv")));
//                    actionsName = actions.get(timestamp);
//
//                    //max nr of records
//                    int max = column.stream().max(new Comparator<List<String>>() {
//
//                        @Override
//                        public int compare(List<String> o1, List<String> o2) {
//                            return ((Integer) o1.size()).compareTo(o2.size());
//                        }
//
//                    }).get().size();
//
//                    for (int i = 0; i < max; i++) {
//                        for (List<String> values : column) {
//                            String columnEntry = (values.size() > i) ? values.get(i) : "";
//                            writer.write(columnEntry + ",");
//                        }
//                        writer.newLine();
//                    }
//
//                    writer.flush();
//                    writer.close();
//
//                } catch (Exception ex) {
//                    Logger.getLogger(LinearDependenciesExtractionEngineTestEachActionSlot.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                Runtime.getRuntime().gc();
//
////                    }
////                };
////                toWaitFor.add(t);
////                t.setDaemon(true);
////                t.start();
//            }
////            for (Thread t : toWaitFor) {
////                try {
////                    t.join();
////                } catch (InterruptedException ex) {
////                    Logger.getLogger(LinearDependenciesExtractionEngineTestEachActionSlot.class.getName()).log(Level.SEVERE, null, ex);
////                }
////            }
//
//        }
//
//        //dependency matrix
//        Map<String, List<String>> columns = new LinkedHashMap<>();
//
//        ArrayList<String> dependenciesNames = new ArrayList(dependenciesPerTimestamp.keySet());
//        ArrayList<Integer> timestamps = new ArrayList(discoveredDependencies.keySet());
//
//        //initiate matrix
//        for (String depName : dependenciesNames) {
//            ArrayList list = new ArrayList<>(timestamps.size());
//            for (Integer t : timestamps) {
//                list.add("0");
//            }
//            columns.put(depName, list);
//        }
//
//        //TODO: print "timsetampColumnName, and dependenciesNames as columns
//        for (String dependency : dependenciesPerTimestamp.keySet()) {
//            //check what Dependendies this timestamp fulfills, put X on those indexes
//            List<String> row = columns.get(dependency);
//            for (Integer timestamp : dependenciesPerTimestamp.get(dependency)) {
//                if (timestamps.contains(timestamp)) {
//                    row.set(timestamps.indexOf(timestamp), "1");
//                }
//            }
//        }
//
//        // add 
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./experiments/dependencyMatrix.csv")));
//
//            //write header
//            {
//                String header = ",";
//                for (String value : columns.keySet()) {
//                    header += value + ",";
//                }
//                writer.write(header);
//                writer.newLine();
//            }
//
//            //now write for each column its entry
//            {
//
//                for (int i = 0; i < timestamps.size(); i++) {
//                    String actionName = (i > 0) ? actions.get(timestamps.get(i - 1)) : "";
//                    String row = "" + timestamps.get(i) + " " + actionName + ",";
//                    for (String value : columns.keySet()) {
//                        row += columns.get(value).get(i) + ",";
//                    }
//                    writer.write(row);
//                    writer.newLine();
//                }
//            }
//
//            writer.flush();
//            writer.close();
//
//        } catch (Exception ex) {
//            Logger.getLogger(LinearDependenciesExtractionEngineTestEachActionSlot.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//}
