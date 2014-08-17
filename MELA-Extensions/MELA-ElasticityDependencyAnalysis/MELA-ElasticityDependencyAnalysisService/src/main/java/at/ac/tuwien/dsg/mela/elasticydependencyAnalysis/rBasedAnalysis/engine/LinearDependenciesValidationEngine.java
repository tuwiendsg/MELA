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
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.MonitoredElementElasticityDependency;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ServiceElasticityDependencies;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.engine.SOMClassifier.MetricWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Computes a SOM of each dependency pair, and uses the som to validate the
 * linear dependency.
 *
 * The linear dependency predicts. The SOM maps data in clusters. Then, we use
 * the SOM to check if the prediction matches to what SOM has mapped.
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class LinearDependenciesValidationEngine {
    
    
    //it uses SOM to compute other type of dependencies, and then cross-validates
    //proved to be somewhat useless
    public static void validateDependencies(ServiceElasticityDependencies dependencies, ElasticitySpace space) {
        //for each monitored element
        for (MonitoredElementElasticityDependency monitoredElementElasticityDependency : dependencies.getElasticityDependencies()) {

            MonitoredElement dependencySourceElement = monitoredElementElasticityDependency.getMonitoredElement();

            //each dependency metric
            for (ElasticityDependencyElement dependencyElement : monitoredElementElasticityDependency.getContainedElements()) {

                Metric dependencySourceMetric = dependencyElement.getDependentMetric();

                //extract values for source metric from elasticity space
                List<MetricValue> dependentMetricValues = space.getMonitoredDataForService(dependencySourceElement).get(dependencySourceMetric);

                //interceptor is in fact percentage of upper elasticity boundary, so I need to compute its value
                MetricValue upperBoundary = space.getElasticitySpaceBoundary().getUpperBoundary(dependencySourceElement, dependencySourceMetric);
                Double upperBoundaryValue = Double.parseDouble(upperBoundary.getValueRepresentation());
                Double interceptorValue = dependencyElement.getInterceptor();

                //extract largest metric value for normalizing the end confidence
//                MetricValue maxMetricValue = new MetricValue(0.0d);
//                for (MetricValue value : sourceMetricValues) {
//                    if (value.compareTo(maxMetricValue) > 0) {
//                        maxMetricValue = value;
//                    }
//                }
                //used to normalize values to compute distance between 0 and 1
//                Double maxValue = Double.parseDouble(maxMetricValue.getValueRepresentation());
                //LinkedHashMap to keep ordering or keys
                //MAP does not work here, as the Metrics might ahve same name and measurement unit , which generates confusion:
                //for example, CPU usage from whole Data End and from DataNode.
                //so we keep Metrics and List<Values> in separate lists. 
                List<SOMClassifier.MetricWrapper> metricsToClassify = new ArrayList<SOMClassifier.MetricWrapper>();
                Map<SOMClassifier.MetricWrapper, List<MetricValue>> metricValuesToClassify = new HashMap<>();

                //used to keep   predictedCoefficients for all targetMetrics
                List<Double> targetsCoefficients = new ArrayList<Double>();

                SOMClassifier.MetricWrapper dependentMetricWrapper = new SOMClassifier.MetricWrapper(dependencySourceElement, dependencySourceMetric);
                metricsToClassify.add(dependentMetricWrapper);
                metricValuesToClassify.put(dependentMetricWrapper, dependentMetricValues);

                {
                    String dependencyDescription = dependencyElement.getDependentMetric().getName() + ":" + dependencySourceElement.getId() + "= ";

                    for (ElasticityDependencyCoefficient dependencyCoefficient : dependencyElement.getCoefficients()) {
                        MonitoredElement dependencyTargetElement = dependencyCoefficient.getMonitoredElement();
                        Metric dependencyTargetMetric = dependencyCoefficient.getMetric();
                        dependencyDescription += dependencyTargetMetric.getName() + ":" + dependencyTargetElement.getId();
                    }

                    System.out.println("\n !!! " + dependencyDescription);
                }

                //for all dependency metric targets
                for (ElasticityDependencyCoefficient dependencyCoefficient : dependencyElement.getCoefficients()) {
                    MonitoredElement dependencyTargetElement = dependencyCoefficient.getMonitoredElement();
                    Metric dependencyTargetMetric = dependencyCoefficient.getMetric();

                    //get values from elasticity space, construct som, and validate agains som.
                    //extract values for target metric from elasticity space
                    List<MetricValue> targetMetricValues = space.getMonitoredDataForService(dependencyTargetElement).get(dependencyTargetMetric);
                    targetsCoefficients.add(dependencyCoefficient.getCoefficient());
                    SOMClassifier.MetricWrapper targetMetricWrapper = new MetricWrapper(dependencyTargetElement, dependencyTargetMetric);
                    metricsToClassify.add(targetMetricWrapper);
                    metricValuesToClassify.put(targetMetricWrapper, targetMetricValues);

                }

                //we evalate for each source, all dependency targets
                //means we cluster source + all dependencies in case we have source  = c1* t1 + c2*t2 + .... (where t is target)
                //and then validate agains all clusters. while this generates potentially more clusters as the targets might not be related, 
                //it can still be used to validate the source
                SOMClassifier elasticityPathway = new SOMClassifier(metricsToClassify.size());

                elasticityPathway.trainElasticityPathway(metricValuesToClassify);

//                //just test if the prediction applies
//                {
//                    List<String> dependencyDescriptionInStrings = new ArrayList<>();
//                    List<Double> computed = new ArrayList<>();
//
//                    //instantiate with name of dependent metric and monitored value
//                    for (MetricValue value : dependentMetricValues) {
//                        dependencyDescriptionInStrings.add(value.getValueRepresentation() + "= " + dependencyElement.getInterceptor() + " + ");
//                        computed.add(dependencyElement.getInterceptor());
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
//                        System.out.println(dependencyDescriptionInStrings.get(i));
//                        System.out.println(dependentMetricValues.get(i).getValueRepresentation() + "= " + computed.get(i));
//                    }
//                }
                List<Neuron> clusters = elasticityPathway.getSituationGroups();

//                Double lackOfConfidence = 0.0;
                //for each neron, how much of the 
                List<Double> predictionDistancesInPercentageOfPredicted = new ArrayList<Double>();

                //we compute average accuray by SUM(predictionAccuracy * neuronEncounterRate) over all predictions.
                // Then. at the end we divide by the total encounter rate to get average accuracy per 1 encounter.
                Double totalEncounteredValues = 0.0d;

                Collections.sort(clusters, new Comparator<Neuron>() {

                    @Override
                    public int compare(Neuron n1, Neuron n2) {
                        return -1 * (n1.getUsagePercentage().compareTo(n2.getUsagePercentage()));
                    }

                });

                //compute confidence as AVG ( clusterEncounterRate * distance (predicted, clusterValue))
                for (Neuron cluster : clusters) {
                    //validate agains the most encountered cluster
//                    Neuron cluster = clusters.get(0);
                    Double usagePercentage = cluster.getUsagePercentage(); //divide by 100 because usage percent is not divided by 100, it is 7, for 7%

                    List<Double> clusterValues = cluster.getWeights();

                    //first value is source 
                    //translate in percentage with respect to upper boundary
                    Double sourceMappedValue = (clusterValues.remove(0) * 100) / (Double) upperBoundary.getValue();
                    //interceptor is the constant
                    Double predictedValue = interceptorValue;

                    List<ElasticityDependencyCoefficient> coeffList = new ArrayList<>(dependencyElement.getCoefficients());
                    for (int i = 0; i < clusterValues.size(); i++) {
                        ElasticityDependencyCoefficient targetCoefficient = coeffList.get(i);

                        MetricValue upperBoundaryCoefficient = space.getElasticitySpaceBoundary().getUpperBoundary(targetCoefficient.getMonitoredElement(), targetCoefficient.getMetric());
                        Double predictorValue = (clusterValues.get(i) * 100) / (Double) upperBoundaryCoefficient.getValue();

                        predictedValue += targetsCoefficients.get(i) * predictorValue;
                    }

                    //compute distance from predicted to mapped value transformed in distance in percentage. 100% max distance
//                    Double distance = Math.abs(sourceMappedValue - predictedValue);
                    //compute distance as percentage of recorded to be able to say
                    // distance = 20 means 20% from predicted value 100 -> confidence 80%
                    Double distanceFromPerfectPrediction = (sourceMappedValue != 0.0d) ? predictedValue * 100 / sourceMappedValue : predictedValue;
                    //> 101 as due to flaoting point things, you can get 100.000000000000x

                    //if perfect prediction, means 100, thus 100 - 100  = 0
                    distanceFromPerfectPrediction = Math.abs(100 - distanceFromPerfectPrediction);

                    // usage percentate is the weight of the distance. if usage is very small, the confidence of using this data is minimal
                    predictionDistancesInPercentageOfPredicted.add(distanceFromPerfectPrediction * usagePercentage);
                    totalEncounteredValues += usagePercentage;

                }
                //confidence will be between 0 and 1, 0 no confidence, 1 max confidence
                Double depencencyConfidence = predictionDistancesInPercentageOfPredicted.stream().mapToDouble(i -> i).sum() / totalEncounteredValues;

//                if (lackOfConfidence == 0) {
//                    depencencyConfidence = 1.0; //1 means max confidence
//                } else {
//                    // alo normalize lack of confidence to maximum metric value
//                    depencencyConfidence = 100 / lackOfConfidence;
//                }
                String describeConfidence = "" + depencencyConfidence + " - from " + metricsToClassify.remove(0).getName() + " to ";
                for (Metric metric : metricsToClassify) {
                    describeConfidence += metric.getName() + " ";
                }

                System.out.println(describeConfidence);

            }
        }
    }
}
