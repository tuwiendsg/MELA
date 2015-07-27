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
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.api;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyCoefficient;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ElasticityDependencyElement;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ServiceElasticityDependencies;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.MonitoredElementElasticityDependency;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticityBehavior;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.LinearCorrelation;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.LinearCorrelation.Coefficient;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.Variable;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.engine.LinearCorrelationStatisticsComputationEngine;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.engine.LinearElasticityDependencyAnalysisEngine;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.utils.converters.Converter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
public class ElasticityDependencyAnalysisManager {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(ElasticityDependencyAnalysisManager.class);

    @Autowired
    private PersistenceDelegate persistenceDelegate;

    @Autowired
    private LinearElasticityDependencyAnalysisEngine linearElasticityDependencyAnalysisEngine;

    public String analyzeElasticityDependenciesAsCSV(MonitoredElement monitoredElement) {
        return elasticityDependenciesToCSV(monitoredElement, analyzeElasticityDependencies(monitoredElement));

    }

    public String analyzeElasticityDependenciesAsCSVBetweenTimeIntervals(MonitoredElement monitoredElement, int startTime,
            int endTime) {
        return elasticityDependenciesToCSV(monitoredElement, analyzeElasticityDependencies(monitoredElement, startTime, endTime));

    }

    public String analyzeElasticityDependenciesBetweenElMetricsAsCSV(MonitoredElement monitoredElement) {
        return elasticityDependenciesToCSV(monitoredElement, analyzeElasticityDependenciesBetweenElasticityMetrics(monitoredElement));

    }

    public String analyzeElasticityDependenciesBetweenElMetricsAsCSVBetweenTimeIntervals(MonitoredElement monitoredElement, int startTime, int endTime) {
        return elasticityDependenciesToCSV(monitoredElement, analyzeElasticityDependenciesBetweenElasticityMetrics(monitoredElement, startTime, endTime));

    }

    public String elasticityDependenciesToCSV(MonitoredElement monitoredElement, ServiceElasticityDependencies dependencies) {

        StringWriter sw = new StringWriter();

        if (dependencies == null) {
            org.apache.log4j.Logger.getLogger(ElasticityDependencyAnalysisManager.class).log(org.apache.log4j.Level.WARN, "Elasticity analysis disabled, or no service configuration or composition rules configuration");
            return "";
        }

        ElasticitySpace space = persistenceDelegate.extractLatestElasticitySpace(monitoredElement.getId());

        List<List<String>> column = new ArrayList<>();

        for (MonitoredElementElasticityDependency dependency : dependencies.getElasticityDependencies()) {
            Map<Metric, List<MetricValue>> dataForDependentElement = space.getMonitoredDataForService(dependency.getMonitoredElement());

            for (ElasticityDependencyElement dependencyElement : dependency.getContainedElements()) {

                List<String> dataColumn = new ArrayList<>();
                column.add(dataColumn);

                List<MetricValue> dependentMetricValues = dataForDependentElement.get(dependencyElement.getDependentMetric());

                /**
                 * As interceptor is computed on percentages of UpperBoundary, I
                 * need to convert it to value
                 */
                Double interceptorConvertedToValue = dependencyElement.getInterceptor() / 100 * Double.parseDouble(space.getSpaceBoundaryForMetric(dependencyElement.getMonitoredElement(), dependencyElement.getDependentMetric())[1].getValueRepresentation());

                String dependencyDescription = dependencyElement.getDependentMetric().getName() + ":" + dependency.getMonitoredElement().getId() + "<- " + interceptorConvertedToValue + " + ";
                String secondaryHeaderLine = ",Monitored original, Monitored Filtered, Computed Original, Computed Filtered";

                for (ElasticityDependencyCoefficient coefficient : dependencyElement.getCoefficients()) {
                    dependencyDescription += "" + coefficient.getCoefficient() + "*" + coefficient.getMetric().getName() + ":" + coefficient.getMonitoredElement().getId() + " with lag " + coefficient.getLag() + " + ";
                    secondaryHeaderLine += "," + coefficient.getMetric().getName() + " filtered" + "," + coefficient.getMetric().getName() + " original";
                }

                dependencyDescription += " and";
                //write in the name also quality statistics
                for (String statistic : dependencyElement.getStatistics().keySet()) {
                    dependencyDescription += " " + statistic + "=" + dependencyElement.getStatistic(statistic);
                }

                //add to dependencyDescription fake columns for the computed column and each coeff recorded value
                //column for computed
                dependencyDescription += ",,,,";
                for (int i = 0; i < dependencyElement.getCoefficients().size(); i++) {
                    dependencyDescription += ",,";
                }

                dataColumn.add(dependencyDescription);
                dataColumn.add(secondaryHeaderLine);

                //just test if the prediction applies
                {
                    List<String> coefficientValuesColumns = new ArrayList<>();
                    List<Double> computedOriginal = new ArrayList<>();
                    List<Double> computedFiltered = new ArrayList<>();

                    //instantiate with name of dependent metric and monitored value
                    for (MetricValue value : dependentMetricValues) {
                        coefficientValuesColumns.add("");
                        computedOriginal.add(interceptorConvertedToValue);
                        computedFiltered.add(interceptorConvertedToValue);
                    }

                    for (ElasticityDependencyCoefficient dependencyCoefficient : dependencyElement.getCoefficients()) {
                        List<MetricValue> originalCoefficientMetricValues = space.getMonitoredDataForService(dependencyCoefficient.getMonitoredElement()).get(dependencyCoefficient.getMetric());
                        List<MetricValue> filteredCoefficientMetricValues = dependencyCoefficient.getMetricValues();

                        for (int i = 0; i < dependentMetricValues.size() && i < filteredCoefficientMetricValues.size(); i++) {

                            Double filteredCoeffValue = ((Number) filteredCoefficientMetricValues.get(i).getValue()).doubleValue();
                            Double originalCoeffValue = ((Number) originalCoefficientMetricValues.get(i).getValue()).doubleValue();
                            coefficientValuesColumns.set(i, coefficientValuesColumns.get(i) + "," + filteredCoeffValue + "," + originalCoeffValue);

                            computedFiltered.set(i, computedFiltered.get(i) + (dependencyCoefficient.getCoefficient() * filteredCoeffValue));
                            computedOriginal.set(i, computedOriginal.get(i) + (dependencyCoefficient.getCoefficient() * originalCoeffValue));
                        }
                    }

                    for (int i = 0; i < dependentMetricValues.size() && i < dependencyElement.getDependentMetricValues().size()
                            && i < computedOriginal.size() && i < coefficientValuesColumns.size(); i++) {
//                        System.out.println(dependencyDescriptionInStrings.get(i));
                        String description = "," + dependentMetricValues.get(i).getValueRepresentation() + "," + dependencyElement.getDependentMetricValues().get(i) + ", " + computedOriginal.get(i) + "," + computedFiltered.get(i) + coefficientValuesColumns.get(i);

                        dataColumn.add(description);
                    }
                }
            }
        }

        //write to string
        try {
            BufferedWriter writer = new BufferedWriter(sw);

            int max = Integer.MIN_VALUE;
//        List<MetricValue> minList = dataToClassify.values().iterator().next();
            for (List<String> list : column) {
                if (max < list.size()) {
                    max = list.size();
                }
            }

            for (int i = 0; i < max; i++) {
                for (List<String> values : column) {
                    String columnEntry = (values.size() > i) ? values.get(i) : "";
                    writer.write(columnEntry + ",");
                }
                writer.newLine();
            }

            writer.flush();
            writer.close();

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

        return sw.toString();
    }

    /**
     *
     * @param monitoredElement
     * @return JSON repr of el dependencies
     */
    public String analyzeElasticityDependenciesJSON(MonitoredElement monitoredElement, Integer startTimestampID, Integer endTimestampID) {
        return elasticityDependenciesTOJSON(analyzeElasticityDependencies(monitoredElement, startTimestampID, endTimestampID));
    }

    public String analyzeElasticityDependenciesBetweenElMetricsJSON(MonitoredElement monitoredElement, Integer startTimestampID, Integer endTimestampID) {
        return elasticityDependenciesTOJSON(analyzeElasticityDependenciesBetweenElasticityMetrics(monitoredElement, startTimestampID, endTimestampID));
    }

    public String
            elasticityDependenciesTOJSON(ServiceElasticityDependencies dependencies) {

        DecimalFormat df = new DecimalFormat("0.##");

        if (dependencies == null) {
            org.apache.log4j.Logger.getLogger(ElasticityDependencyAnalysisManager.class
            ).log(org.apache.log4j.Level.WARN, "Elasticity analysis disabled, or no service configuration or composition rules configuration");
            JSONObject elSpaceJSON = new JSONObject();

            elSpaceJSON.put("dependencies", "empty");
            return elSpaceJSON.toJSONString();
        }

        JSONArray jSONArray = new JSONArray();

        for (MonitoredElementElasticityDependency dependency : dependencies.getElasticityDependencies()) {
            for (ElasticityDependencyElement dependencyElement : dependency.getContainedElements()) {
//
                JSONObject dependencyJSON = new JSONObject();
                dependencyJSON.put("fromParentName", dependency.getMonitoredElement().getId());
                dependencyJSON.put("fromMetric", dependencyElement.getDependentMetric().getName());
                dependencyJSON.put("interceptor", df.format(dependencyElement.getInterceptor()));

                JSONArray statistics = new JSONArray();
                //write in the name also quality statistics
                for (String statistic : dependencyElement.getStatistics().keySet()) {
                    JSONObject statisticJSON = new JSONObject();
                    statisticJSON.put(statistic, df.format(dependencyElement.getStatistic(statistic)));
                    statistics.add(statisticJSON);
                }
                dependencyJSON.put("statistics", statistics);

                JSONArray dependenciesChildren = new JSONArray();

                for (ElasticityDependencyCoefficient coefficient : dependencyElement.getCoefficients()) {

                    JSONObject childJSON = new JSONObject();
                    childJSON.put("toParentName", coefficient.getMonitoredElement().getId());
                    childJSON.put("toMetric", coefficient.getMetric().getName());

                    childJSON.put("stdError", df.format(coefficient.getStdError()));
                    childJSON.put("coefficient", df.format(coefficient.getCoefficient()));
                    childJSON.put("lag", df.format(coefficient.getLag()));
                    dependenciesChildren.add(childJSON);
                }
                dependencyJSON.put("dependencies", dependenciesChildren);
                jSONArray.add(dependencyJSON);
            }
        }

        return jSONArray.toJSONString();
    }

    public ServiceElasticityDependencies analyzeElasticityDependencies(MonitoredElement monitoredElement) {
        //PersistenceSQLAccess persistenceDelegate = new PersistenceSQLAccess("mela", "mela", "localhost", Configuration.getDataServicePort(), monitoredElement.getId());
        final ElasticitySpace elasticitySpace = persistenceDelegate.extractLatestElasticitySpace(monitoredElement.getId());

        //from this we get composition rules to avoid computing dependencies between metrics created 
        //using composition rules and their source metrics
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(monitoredElement.getId());
        final CompositionRulesConfiguration compositionRulesConfiguration = cfg.getCompositionRulesConfiguration();

        if (elasticitySpace == null) {
            Logger.getLogger(ElasticityDependencyAnalysisManager.class
                    .getName()).log(Level.SEVERE, "Elasticity space for " + monitoredElement + " is null");
            return new ServiceElasticityDependencies();
        }

        ServiceElasticityDependencies determinedDependencies = persistenceDelegate.extractLatestElasticityDependencies(monitoredElement.getId());

        //check if the dependencies and space have same timestamp. if not, then space is more fresh, so dependencie are recomputed
        if (determinedDependencies != null && elasticitySpace.getEndTimestampID() == determinedDependencies.getEndTimestampID()) {

            return determinedDependencies;
        } else {

            //else we recompute dependencies
            final ElasticityBehavior behavior = new ElasticityBehavior(elasticitySpace);

            final List<LinearCorrelation> corelations = Collections.synchronizedList(new ArrayList<LinearCorrelation>());

            //start analysis in separate threads
            Thread crossLevelAnalysis = new Thread() {

                @Override
                public
                        void run() {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.INFO, "Analyzing cross-level behavior");
                    List<LinearCorrelation> crossLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesAcrossLevel(behavior, compositionRulesConfiguration);

                    corelations.addAll(crossLayerCorelations);
                }

            };

            crossLevelAnalysis.setDaemon(true);

            Thread sameLevelAnalysis = new Thread() {

                @Override
                public
                        void run() {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.INFO, "Analyzing behavior in same level");
                    List<LinearCorrelation> sameLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesInSameLevel(behavior, compositionRulesConfiguration);

                    corelations.addAll(sameLayerCorelations);
                }

            };
            sameLevelAnalysis.setDaemon(true);

            Thread sameElementAnalysis = new Thread() {

                @Override
                public
                        void run() {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.INFO, "Analyzing behavior in same level");
                    List<LinearCorrelation> sameLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesInSameElement(behavior, compositionRulesConfiguration);

                    corelations.addAll(sameLayerCorelations);
                }

            };
            sameElementAnalysis.setDaemon(true);

            crossLevelAnalysis.start();
            sameLevelAnalysis.start();
            sameElementAnalysis.start();

            //wait for analysis threads to complete
            try {
                crossLevelAnalysis.join();

            } catch (InterruptedException ex) {
                Logger.getLogger(ElasticityDependencyAnalysisManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            try {
                sameLevelAnalysis.join();

            } catch (InterruptedException ex) {
                Logger.getLogger(ElasticityDependencyAnalysisManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

            try {
                sameElementAnalysis.join();

            } catch (InterruptedException ex) {
                Logger.getLogger(ElasticityDependencyAnalysisManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

            final Map<MonitoredElement, MonitoredElementElasticityDependency> dependencies = Collections.synchronizedMap(new HashMap<MonitoredElement, MonitoredElementElasticityDependency>());

            //confert in seperate threads
            List<Thread> conversionThreads = new ArrayList<Thread>();

            for (final LinearCorrelation c : corelations) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        //transform from LinearCorrelation to MonitoredElementElasticityDependency
                        {

                            //extract data about dependent metric
                            Double adjustedSquare = c.getAdjustedRSquared();
                            Double intercept = c.getIntercept();
                            Variable vDependent = c.getDependent();

                            Metric metric = (Metric) vDependent.getMetaData(Metric.class
                                    .getName());
                            MonitoredElement element = (MonitoredElement) vDependent.getMetaData(MonitoredElement.class.getName());

                            Double upperBoundaryForMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(element, metric)[1].getValueRepresentation());

                            ElasticityDependencyElement dependencyElement = new ElasticityDependencyElement(element, metric, intercept);
                            dependencyElement.withStatistic(ElasticityDependencyElement.ADJUSTED_R, c.getAdjustedRSquared());

                            {
                                List<MetricValue> values = new ArrayList<>();
                                //as values stored in corelation are percentage of upper boundary, convert them back

                                for (Double v : vDependent.getValues()) {
                                    values.add(new MetricValue(v / 100 * upperBoundaryForMetric));
                                }
                                dependencyElement.setDependentMetricValues(values);
                            }
                            //extract each predictor its coefficient
                            List<Coefficient> predictors = c.getPredictors();

                            for (Coefficient coefficient : predictors) {
                                Double coeff = coefficient.getCoefficient();
                                Double stdError = coefficient.getStdError();
                                Variable variable = coefficient.getVariable();
                                Metric predictorMetric = (Metric) variable.getMetaData(Metric.class.getName());
                                MonitoredElement predictorElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());

                                ElasticityDependencyCoefficient elasticityDependencyCoefficient = new ElasticityDependencyCoefficient(predictorElement, predictorMetric, coeff, stdError, coefficient.getLag());

                                {
                                    List<MetricValue> values = new ArrayList<>();
                                    Double upperBoundaryForPredictorMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(predictorElement, predictorMetric)[1].getValueRepresentation());
                                    for (Double v : variable.getValues()) {
                                        values.add(new MetricValue(v / 100 * upperBoundaryForPredictorMetric));
                                    }
                                    elasticityDependencyCoefficient.setMetricValues(values);
                                }

                                dependencyElement.addCoefficient(elasticityDependencyCoefficient);
                            }

                            dependencyElement = LinearCorrelationStatisticsComputationEngine.enrichWithEstimationErrorStatistics(dependencyElement);
                            if (dependencies.containsKey(element)) {
                                dependencies.get(element).addElement(dependencyElement);
                            } else {
                                MonitoredElementElasticityDependency dependency = new MonitoredElementElasticityDependency(element);
                                dependency.addElement(dependencyElement);
                                dependencies.put(element, dependency);
                            }
                        }
                    }

                };

                t.setDaemon(true);
                conversionThreads.add(t);
                t.start();;

            }

            for (Thread t : conversionThreads) {
                try {
                    t.join();

                } catch (InterruptedException ex) {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }

            determinedDependencies = new ServiceElasticityDependencies();
            determinedDependencies.setElasticityDependencies(dependencies.values());
            determinedDependencies.setStartTimestampID(elasticitySpace.getStartTimestampID());
            determinedDependencies.setEndTimestampID(elasticitySpace.getEndTimestampID());

            try {
                persistenceDelegate.writeElasticityDependencies(monitoredElement.getId(), determinedDependencies);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            return determinedDependencies;
        }
    }

    public ServiceElasticityDependencies analyzeElasticityDependencies(MonitoredElement monitoredElement, Integer startTimestampID, Integer endTimestampID) {
        //PersistenceSQLAccess persistenceDelegate = new PersistenceSQLAccess("mela", "mela", "localhost", Configuration.getDataServicePort(), monitoredElement.getId());

        if (startTimestampID == null) {
            startTimestampID = 0;
        }
        if (endTimestampID == null) {
            return analyzeElasticityDependencies(monitoredElement);
        }

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(monitoredElement.getId());
        final CompositionRulesConfiguration compositionRulesConfiguration = cfg.getCompositionRulesConfiguration();

        ServiceElasticityDependencies determinedDependencies = persistenceDelegate.extractLatestElasticityDependencies(monitoredElement.getId(), startTimestampID, endTimestampID);

        //check if the dependencies and space have same timestamp. if not, then space is more fresh, so dependencie are recomputed
        if (determinedDependencies != null) {

            return determinedDependencies;
        } else {

            final ElasticitySpace elasticitySpace = persistenceDelegate.extractLatestElasticitySpace(monitoredElement.getId(), startTimestampID, endTimestampID);

            //else we recompute dependencies
            final ElasticityBehavior behavior = new ElasticityBehavior(elasticitySpace);

            final List<LinearCorrelation> corelations = Collections.synchronizedList(new ArrayList<LinearCorrelation>());

            //start analysis in separate threads
            Thread crossLevelAnalysis = new Thread() {

                @Override
                public
                        void run() {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.INFO, "Analyzing cross-level behavior");
                    List<LinearCorrelation> crossLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesAcrossLevel(behavior, compositionRulesConfiguration);

                    corelations.addAll(crossLayerCorelations);
                }

            };

            crossLevelAnalysis.setDaemon(true);

            Thread sameLevelAnalysis = new Thread() {

                @Override
                public
                        void run() {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.INFO, "Analyzing behavior in same level");
                    List<LinearCorrelation> sameLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesInSameLevel(behavior, compositionRulesConfiguration);

                    corelations.addAll(sameLayerCorelations);
                }

            };
            sameLevelAnalysis.setDaemon(true);

            Thread sameElementAnalysis = new Thread() {

                @Override
                public
                        void run() {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.INFO, "Analyzing behavior in same level");
                    List<LinearCorrelation> sameLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesInSameElement(behavior, compositionRulesConfiguration);

                    corelations.addAll(sameLayerCorelations);
                }

            };
            sameElementAnalysis.setDaemon(true);

            crossLevelAnalysis.start();
            sameLevelAnalysis.start();
            sameElementAnalysis.start();

            //wait for analysis threads to complete
            try {
                crossLevelAnalysis.join();

            } catch (InterruptedException ex) {
                Logger.getLogger(ElasticityDependencyAnalysisManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            try {
                sameLevelAnalysis.join();

            } catch (InterruptedException ex) {
                Logger.getLogger(ElasticityDependencyAnalysisManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

            try {
                sameElementAnalysis.join();

            } catch (InterruptedException ex) {
                Logger.getLogger(ElasticityDependencyAnalysisManager.class
                        .getName()).log(Level.SEVERE, null, ex);
            }

            final Map<MonitoredElement, MonitoredElementElasticityDependency> dependencies = Collections.synchronizedMap(new HashMap<MonitoredElement, MonitoredElementElasticityDependency>());

            //confert in seperate threads
            List<Thread> conversionThreads = new ArrayList<Thread>();

            for (final LinearCorrelation c : corelations) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        //transform from LinearCorrelation to MonitoredElementElasticityDependency
                        {

                            //extract data about dependent metric
                            Double adjustedSquare = c.getAdjustedRSquared();
                            Double intercept = c.getIntercept();
                            Variable vDependent = c.getDependent();

                            Metric metric = (Metric) vDependent.getMetaData(Metric.class
                                    .getName());
                            MonitoredElement element = (MonitoredElement) vDependent.getMetaData(MonitoredElement.class.getName());
                            Double upperBoundaryForMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(element, metric)[1].getValueRepresentation());

                            ElasticityDependencyElement dependencyElement = new ElasticityDependencyElement(element, metric, intercept);
                            dependencyElement.withStatistic(ElasticityDependencyElement.ADJUSTED_R, c.getAdjustedRSquared());

                            {
                                List<MetricValue> values = new ArrayList<>();
                                //as values stored in corelation are percentage of upper boundary, convert them back
                                // Double upperBoundaryForMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(element, metric)[1].getValueRepresentation());
                                for (Double v : vDependent.getValues()) {
                                    values.add(new MetricValue(v / 100 * upperBoundaryForMetric));
                                }
                                dependencyElement.setDependentMetricValues(values);
                            }
                            //extract each predictor its coefficient
                            List<Coefficient> predictors = c.getPredictors();

                            for (Coefficient coefficient : predictors) {
                                Double coeff = coefficient.getCoefficient();
                                Double stdError = coefficient.getStdError();
                                Variable variable = coefficient.getVariable();
                                Metric predictorMetric = (Metric) variable.getMetaData(Metric.class.getName());
                                MonitoredElement predictorElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());

                                ElasticityDependencyCoefficient elasticityDependencyCoefficient = new ElasticityDependencyCoefficient(predictorElement, predictorMetric, coeff, stdError, coefficient.getLag());

                                {
                                    List<MetricValue> values = new ArrayList<>();
                                    Double upperBoundaryForPredictorMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(predictorElement, predictorMetric)[1].getValueRepresentation());
                                    for (Double v : variable.getValues()) {
                                        values.add(new MetricValue(v / 100 * upperBoundaryForPredictorMetric));
                                    }
                                    elasticityDependencyCoefficient.setMetricValues(values);
                                }

                                dependencyElement.addCoefficient(elasticityDependencyCoefficient);
                            }

                            dependencyElement = LinearCorrelationStatisticsComputationEngine.enrichWithEstimationErrorStatistics(dependencyElement);
                            if (dependencies.containsKey(element)) {
                                dependencies.get(element).addElement(dependencyElement);
                            } else {
                                MonitoredElementElasticityDependency dependency = new MonitoredElementElasticityDependency(element);
                                dependency.addElement(dependencyElement);
                                dependencies.put(element, dependency);
                            }
                        }
                    }

                };

                t.setDaemon(true);
                conversionThreads.add(t);
                t.start();;

            }

            for (Thread t : conversionThreads) {
                try {
                    t.join();

                } catch (InterruptedException ex) {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }

            determinedDependencies = new ServiceElasticityDependencies();
            determinedDependencies.setElasticityDependencies(dependencies.values());
            determinedDependencies.setStartTimestampID(elasticitySpace.getStartTimestampID());
            determinedDependencies.setEndTimestampID(elasticitySpace.getEndTimestampID());

            try {
                persistenceDelegate.writeElasticityDependencies(monitoredElement.getId(), determinedDependencies);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            return determinedDependencies;
        }
    }

    public ServiceElasticityDependencies analyzeElasticityDependenciesBetweenElasticityMetrics(MonitoredElement monitoredElement) {
        //PersistenceSQLAccess persistenceDelegate = new PersistenceSQLAccess("mela", "mela", "localhost", Configuration.getDataServicePort(), monitoredElement.getId());
        final ElasticitySpace elasticitySpace = persistenceDelegate.extractLatestElasticitySpace(monitoredElement.getId());

        if (elasticitySpace == null) {
            Logger.getLogger(ElasticityDependencyAnalysisManager.class
                    .getName()).log(Level.SEVERE, "Elasticity space for " + monitoredElement + " is null");
            return new ServiceElasticityDependencies();
        }

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(monitoredElement.getId());
        Requirements requirements = cfg.getRequirements();

        //from Requirements, we extract "Elasticity Metrics" on which Requirements are defined, and search for dependencies between these metrics
        Map<MonitoredElement, List<Metric>> elasticityMetrics = new HashMap<>();

        for (Requirement requirement : requirements.getRequirements()) {
            for (String id : requirement.getTargetMonitoredElementIDs()) {
                MonitoredElement element = new MonitoredElement().withId(id).withLevel(requirement.getTargetMonitoredElementLevel());
                if (elasticityMetrics.containsKey(element)) {
                    elasticityMetrics.get(element).add(requirement.getMetric());
                } else {
                    List<Metric> list = new ArrayList<>();
                    list.add(requirement.getMetric());
                    elasticityMetrics.put(element, list);
                }
            }
        }

        //purge el space and keep only Elasticity Metrics
        Iterator<Map.Entry<MonitoredElement, Map<Metric, List<MetricValue>>>> entryIterator = elasticitySpace.getMonitoringData().entrySet().iterator();

        while (entryIterator.hasNext()) {
            Map.Entry<MonitoredElement, Map<Metric, List<MetricValue>>> entry = entryIterator.next();
            if (elasticityMetrics.containsKey(entry.getKey())) {
                //iterate and remove metriocs not contained in el metrics list
                Iterator<Map.Entry<Metric, List<MetricValue>>> metricsIterator = entry.getValue().entrySet().iterator();

                List<Metric> elMetricsForElement = elasticityMetrics.get(entry.getKey());

                while (metricsIterator.hasNext()) {
                    Map.Entry<Metric, List<MetricValue>> metricEntry = metricsIterator.next();
                    if (!elMetricsForElement.contains(metricEntry.getKey())) {
                        metricsIterator.remove();
                    }
                }
            } else {
                //remove all monitored element data
                entryIterator.remove();
            }
        }

        {
            final ElasticityBehavior behavior = new ElasticityBehavior(elasticitySpace);

            final List<LinearCorrelation> corelations = Collections.synchronizedList(new ArrayList<LinearCorrelation>());

            Logger.getLogger(ElasticityDependencyAnalysisManager.class
                    .getName()).log(Level.INFO, "Analyzing el dependencies between el metrics");

            List<LinearCorrelation> crossLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesBetweenMetrics(behavior);

            corelations.addAll(crossLayerCorelations);

            final Map<MonitoredElement, MonitoredElementElasticityDependency> dependencies = Collections.synchronizedMap(new HashMap<MonitoredElement, MonitoredElementElasticityDependency>());

            //confert in seperate threads
            List<Thread> conversionThreads = new ArrayList<Thread>();

            for (final LinearCorrelation c : corelations) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        //transform from LinearCorrelation to MonitoredElementElasticityDependency
                        {
                            //write in csv the corelation data and result, to allow inspection
//                            List<List<String>> columns = new ArrayList<>();

                            //extract data about dependent metric
                            Double adjustedSquare = c.getAdjustedRSquared();
                            Double intercept = c.getIntercept();
                            Variable vDependent = c.getDependent();

                            Metric metric = (Metric) vDependent.getMetaData(Metric.class.getName());
                            MonitoredElement element = (MonitoredElement) vDependent.getMetaData(MonitoredElement.class.getName());

                            //create column with main dependent var
                            {
                                List<String> column = new ArrayList<>();
                                //put column
                                column.add(vDependent.getId() + ":" + element.getId());
                                for (Double value : vDependent.getValues()) {
                                    column.add("" + value);
                                }
//                                columns.add(column);
                            }

                            Double upperBoundaryForMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(element, metric)[1].getValueRepresentation());

                            ElasticityDependencyElement dependencyElement = new ElasticityDependencyElement(element, metric, intercept);
                            dependencyElement.withStatistic(ElasticityDependencyElement.ADJUSTED_R, c.getAdjustedRSquared());

                            {
                                List<MetricValue> values = new ArrayList<>();

                                for (Double v : vDependent.getValues()) {
                                    values.add(new MetricValue(v / 100 * upperBoundaryForMetric));
                                }
                                dependencyElement.setDependentMetricValues(values);
                            }

                            //extract each predictor its coefficient
                            List<Coefficient> predictors = c.getPredictors();

                            {
//                                List<Double> predictedValues = new ArrayList<>();
//                                {
//                                    for (Double value : vDependent.getValues()) {
//                                        predictedValues.add(c.getIntercept()); // first ad intercept to predicted
//                                    }
//                                }
//                                {
//                                    String coeffNames = "";
//                                    for (Coefficient coefficient : predictors) {
//                                        Double coeff = coefficient.getCoefficient();
//                                        Variable variable = coefficient.getVariable();
//                                        MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//
//                                        coeffNames += variable.getId();
//                                        for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                            predictedValues.set(i, predictedValues.get(i) + coeff * variable.getValues().get(i));
//                                        }
//                                    }
//                                    List<String> predictedColumn = new ArrayList<>();
//                                    predictedColumn.add("predicted");
//                                    for (Double predicted : predictedValues) {
//                                        predictedColumn.add("" + predicted);
//                                    }
//                                    columns.add(predictedColumn);
//                                }

//                                for (Coefficient coefficient : predictors) {
//                                    Double coeff = coefficient.getCoefficient();
//                                    Variable variable = coefficient.getVariable();
//                                    MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//                                    List<String> coefficientColumn = new ArrayList<>();
//                                    String coeffName = variable.getId();
//                                    coefficientColumn.add(c.getIntercept() + " + " + coefficient.getCoefficient() + "*" + coeffName);
//
//                                    for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                        coefficientColumn.add("" + variable.getValues().get(i));
//                                    }
//                                    columns.add(coefficientColumn);
//                                }
                            }

                            for (Coefficient coefficient : predictors) {
                                Double coeff = coefficient.getCoefficient();
                                Double stdError = coefficient.getStdError();
                                Variable variable = coefficient.getVariable();
                                Metric predictorMetric = (Metric) variable.getMetaData(Metric.class.getName());
                                MonitoredElement predictorElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());

                                List<MetricValue> values = new ArrayList<>();
                                Double upperBoundaryForPredictorMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(predictorElement, predictorMetric)[1].getValueRepresentation());
                                for (Double v : variable.getValues()) {
                                    values.add(new MetricValue(v / 100 * upperBoundaryForPredictorMetric));
                                }

                                ElasticityDependencyCoefficient elasticityDependencyCoefficient = new ElasticityDependencyCoefficient(predictorElement, predictorMetric, coeff, stdError, coefficient.getLag());

                                elasticityDependencyCoefficient.withMetricValues(values);
                                dependencyElement.addCoefficient(elasticityDependencyCoefficient);

                            }
                            dependencyElement = LinearCorrelationStatisticsComputationEngine.enrichWithEstimationErrorStatistics(dependencyElement);

                            if (dependencies.containsKey(element)) {
                                dependencies.get(element).addElement(dependencyElement);
                            } else {
                                MonitoredElementElasticityDependency dependency = new MonitoredElementElasticityDependency(element);
                                dependency.addElement(dependencyElement);
                                dependencies.put(element, dependency);
                            }
                        }

                    }

                };

                t.setDaemon(true);
                conversionThreads.add(t);
                t.start();;

            }

            for (Thread t : conversionThreads) {
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            ServiceElasticityDependencies determinedDependencies = new ServiceElasticityDependencies();

            determinedDependencies.setElasticityDependencies(dependencies.values());
            determinedDependencies.setStartTimestampID(elasticitySpace.getStartTimestampID());
            determinedDependencies.setEndTimestampID(elasticitySpace.getEndTimestampID());

            //TODO: also persist this el dependencies between el metrics
//            persistenceDelegate.writeElasticityDependencies(monitoredElement.getId(), determinedDependencies);
            return determinedDependencies;
        }
    }

    public ServiceElasticityDependencies analyzeElasticityDependenciesBetweenElasticityMetrics(MonitoredElement monitoredElement, Integer startTimestampID, Integer endTimestampID) {
        if (startTimestampID == null) {
            startTimestampID = 0;
        }
        if (endTimestampID == null) {
            return analyzeElasticityDependenciesBetweenElasticityMetrics(monitoredElement);
        }

        //PersistenceSQLAccess persistenceDelegate = new PersistenceSQLAccess("mela", "mela", "localhost", Configuration.getDataServicePort(), monitoredElement.getId());
        final ElasticitySpace elasticitySpace = persistenceDelegate.extractLatestElasticitySpace(monitoredElement.getId(), startTimestampID, endTimestampID);

        if (elasticitySpace == null) {
            Logger.getLogger(ElasticityDependencyAnalysisManager.class
                    .getName()).log(Level.SEVERE, "Elasticity space for " + monitoredElement + " is null");
            return new ServiceElasticityDependencies();
        }

        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(monitoredElement.getId());
        Requirements requirements = cfg.getRequirements();

        //from Requirements, we extract "Elasticity Metrics" on which Requirements are defined, and search for dependencies between these metrics
        Map<MonitoredElement, List<Metric>> elasticityMetrics = new HashMap<>();

        for (Requirement requirement : requirements.getRequirements()) {
            for (String id : requirement.getTargetMonitoredElementIDs()) {
                MonitoredElement element = new MonitoredElement().withId(id).withLevel(requirement.getTargetMonitoredElementLevel());
                if (elasticityMetrics.containsKey(element)) {
                    elasticityMetrics.get(element).add(requirement.getMetric());
                } else {
                    List<Metric> list = new ArrayList<>();
                    list.add(requirement.getMetric());
                    elasticityMetrics.put(element, list);
                }
            }
        }

        //purge el space and keep only Elasticity Metrics
        Iterator<Map.Entry<MonitoredElement, Map<Metric, List<MetricValue>>>> entryIterator = elasticitySpace.getMonitoringData().entrySet().iterator();

        while (entryIterator.hasNext()) {
            Map.Entry<MonitoredElement, Map<Metric, List<MetricValue>>> entry = entryIterator.next();
            if (elasticityMetrics.containsKey(entry.getKey())) {
                //iterate and remove metriocs not contained in el metrics list
                Iterator<Map.Entry<Metric, List<MetricValue>>> metricsIterator = entry.getValue().entrySet().iterator();

                List<Metric> elMetricsForElement = elasticityMetrics.get(entry.getKey());

                while (metricsIterator.hasNext()) {
                    Map.Entry<Metric, List<MetricValue>> metricEntry = metricsIterator.next();
                    if (!elMetricsForElement.contains(metricEntry.getKey())) {
                        metricsIterator.remove();
                    }
                }
            } else {
                //remove all monitored element data
                entryIterator.remove();
            }
        }

        {
            final ElasticityBehavior behavior = new ElasticityBehavior(elasticitySpace);

            final List<LinearCorrelation> corelations = Collections.synchronizedList(new ArrayList<LinearCorrelation>());

            Logger
                    .getLogger(ElasticityDependencyAnalysisManager.class
                            .getName()).log(Level.INFO, "Analyzing el dependencies between el metrics");

            List<LinearCorrelation> crossLayerCorelations = linearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesBetweenMetrics(behavior);

            corelations.addAll(crossLayerCorelations);

            final Map<MonitoredElement, MonitoredElementElasticityDependency> dependencies = Collections.synchronizedMap(new HashMap<MonitoredElement, MonitoredElementElasticityDependency>());

            //confert in seperate threads
            List<Thread> conversionThreads = new ArrayList<Thread>();

            for (final LinearCorrelation c : corelations) {
                Thread t = new Thread() {

                    @Override
                    public void run() {
                        //transform from LinearCorrelation to MonitoredElementElasticityDependency
                        {
                            //write in csv the corelation data and result, to allow inspection
//                            List<List<String>> columns = new ArrayList<>();

                            //extract data about dependent metric
                            Double adjustedSquare = c.getAdjustedRSquared();
                            Double intercept = c.getIntercept();
                            Variable vDependent = c.getDependent();

                            Metric metric = (Metric) vDependent.getMetaData(Metric.class.getName());
                            MonitoredElement element = (MonitoredElement) vDependent.getMetaData(MonitoredElement.class.getName());

                            //create column with main dependent var
                            {
                                List<String> column = new ArrayList<>();
                                //put column
                                column.add(vDependent.getId() + ":" + element.getId());
                                for (Double value : vDependent.getValues()) {
                                    column.add("" + value);
                                }
//                                columns.add(column);
                            }

                            Double upperBoundaryForMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(element, metric)[1].getValueRepresentation());

                            ElasticityDependencyElement dependencyElement = new ElasticityDependencyElement(element, metric, intercept);
                            dependencyElement.withStatistic(ElasticityDependencyElement.ADJUSTED_R, c.getAdjustedRSquared());
                            {
                                List<MetricValue> values = new ArrayList<>();

                                for (Double v : vDependent.getValues()) {
                                    values.add(new MetricValue(v / 100 * upperBoundaryForMetric));
                                }
                                dependencyElement.setDependentMetricValues(values);
                            }

                            //extract each predictor its coefficient
                            List<Coefficient> predictors = c.getPredictors();

                            {
//                                List<Double> predictedValues = new ArrayList<>();
//                                {
//                                    for (Double value : vDependent.getValues()) {
//                                        predictedValues.add(c.getIntercept()); // first ad intercept to predicted
//                                    }
//                                }
//                                {
//                                    String coeffNames = "";
//                                    for (Coefficient coefficient : predictors) {
//                                        Double coeff = coefficient.getCoefficient();
//                                        Variable variable = coefficient.getVariable();
//                                        MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//
//                                        coeffNames += variable.getId();
//                                        for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                            predictedValues.set(i, predictedValues.get(i) + coeff * variable.getValues().get(i));
//                                        }
//                                    }
//                                    List<String> predictedColumn = new ArrayList<>();
//                                    predictedColumn.add("predicted");
//                                    for (Double predicted : predictedValues) {
//                                        predictedColumn.add("" + predicted);
//                                    }
//                                    columns.add(predictedColumn);
//                                }

//                                for (Coefficient coefficient : predictors) {
//                                    Double coeff = coefficient.getCoefficient();
//                                    Variable variable = coefficient.getVariable();
//                                    MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//                                    List<String> coefficientColumn = new ArrayList<>();
//                                    String coeffName = variable.getId();
//                                    coefficientColumn.add(c.getIntercept() + " + " + coefficient.getCoefficient() + "*" + coeffName);
//
//                                    for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                        coefficientColumn.add("" + variable.getValues().get(i));
//                                    }
//                                    columns.add(coefficientColumn);
//                                }
                            }

                            for (Coefficient coefficient : predictors) {
                                Double coeff = coefficient.getCoefficient();
                                Double stdError = coefficient.getStdError();
                                Variable variable = coefficient.getVariable();
                                Metric predictorMetric = (Metric) variable.getMetaData(Metric.class.getName());
                                MonitoredElement predictorElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());

                                List<MetricValue> values = new ArrayList<>();
                                Double upperBoundaryForPredictorMetric = Double.parseDouble(elasticitySpace.getSpaceBoundaryForMetric(predictorElement, predictorMetric)[1].getValueRepresentation());
                                for (Double v : variable.getValues()) {
                                    values.add(new MetricValue(v / 100 * upperBoundaryForPredictorMetric));
                                }

                                ElasticityDependencyCoefficient elasticityDependencyCoefficient = new ElasticityDependencyCoefficient(predictorElement, predictorMetric, coeff, stdError, coefficient.getLag());

                                elasticityDependencyCoefficient.withMetricValues(values);
                                dependencyElement.addCoefficient(elasticityDependencyCoefficient);

                            }

                            dependencyElement = LinearCorrelationStatisticsComputationEngine.enrichWithEstimationErrorStatistics(dependencyElement);
                            if (dependencies.containsKey(element)) {
                                dependencies.get(element).addElement(dependencyElement);
                            } else {
                                MonitoredElementElasticityDependency dependency = new MonitoredElementElasticityDependency(element);
                                dependency.addElement(dependencyElement);
                                dependencies.put(element, dependency);
                            }
                        }
                    }

                };

                t.setDaemon(true);
                conversionThreads.add(t);
                t.start();;

            }

            for (Thread t : conversionThreads) {
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            ServiceElasticityDependencies determinedDependencies = new ServiceElasticityDependencies();

            determinedDependencies.setElasticityDependencies(dependencies.values());
            determinedDependencies.setStartTimestampID(elasticitySpace.getStartTimestampID());
            determinedDependencies.setEndTimestampID(elasticitySpace.getEndTimestampID());

            //we do not save these, as we save only the complete deps. To also save these, need to save them in some other place 
//            persistenceDelegate.writeElasticityDependencies(monitoredElement.getId(), determinedDependencies);
            return determinedDependencies;
        }
    }

    /**
     * Does not use any persistence. Does NOT save or cache or load dependencies
     *
     * @param monitoredElement
     * @param elasticitySpace
     * @return
     */
//    public ServiceElasticityDependencies analyzeElasticityDependencies(MonitoredElement monitoredElement, ElasticitySpace elasticitySpace) {
//        //PersistenceSQLAccess persistenceDelegate = new PersistenceSQLAccess("mela", "mela", "localhost", Configuration.getDataServicePort(), monitoredElement.getId());
//
//        if (elasticitySpace == null) {
//            Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, "Elasticity space for " + monitoredElement + " is null");
//            return new ServiceElasticityDependencies();
//        }
//
//        //else we recompute dependencies
//        final ElasticityBehavior behavior = new ElasticityBehavior(elasticitySpace);
//
//        final List<LinearCorrelation> corelations = Collections.synchronizedList(new ArrayList<LinearCorrelation>());
//
//        //start analysis in separate threads
//        Thread crossLevelAnalysis = new Thread() {
//
//            @Override
//            public void run() {
//                Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.INFO, "Analyzing cross-level behavior");
//                List<LinearCorrelation> crossLayerCorelations = LinearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesAcrossLevel(behavior);
//                corelations.addAll(crossLayerCorelations);
//            }
//
//        };
//
//        crossLevelAnalysis.setDaemon(true);
//
//        Thread sameLevelAnalysis = new Thread() {
//
//            @Override
//            public void run() {
//                Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.INFO, "Analyzing behavior in same level");
//                List<LinearCorrelation> sameLayerCorelations = LinearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesInSameLevel(behavior);
//                corelations.addAll(sameLayerCorelations);
//            }
//
//        };
//
//        sameLevelAnalysis.setDaemon(true);
//
//        crossLevelAnalysis.start();
//        sameLevelAnalysis.start();
//
//        //wait for analysis threads to complete
//        try {
//            crossLevelAnalysis.join();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        try {
//            sameLevelAnalysis.join();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
////
//        final Map<MonitoredElement, MonitoredElementElasticityDependency> dependencies = Collections.synchronizedMap(new HashMap<MonitoredElement, MonitoredElementElasticityDependency>());
//
//        //confert in seperate threads
//        List<Thread> conversionThreads = new ArrayList<Thread>();
//
//        for (final LinearCorrelation c : corelations) {
//            Thread t = new Thread() {
//
//                @Override
//                public void run() {
//                    //transform from LinearCorrelation to MonitoredElementElasticityDependency
//                    {
//                        //write in csv the corelation data and result, to allow inspection
//                        List<List<String>> columns = new ArrayList<>();
//
//                        //extract data about dependent metric
//                        Double adjustedSquare = c.getAdjustedRSquared();
//                        Double intercept = c.getIntercept();
//                        Variable vDependent = c.getDependent();
//
//                        Metric metric = (Metric) vDependent.getMetaData(Metric.class.getName());
//                        MonitoredElement element = (MonitoredElement) vDependent.getMetaData(MonitoredElement.class.getName());
//
//                        //create column with main dependent var
//                        {
//                            List<String> column = new ArrayList<>();
//                            //put column
//                            column.add(vDependent.getId() + ":" + element.getId());
//                            for (Double value : vDependent.getValues()) {
//                                column.add("" + value);
//                            }
//                            columns.add(column);
//                        }
//
//                        ElasticityDependencyElement dependencyElement = new ElasticityDependencyElement(metric, intercept, adjustedSquare);
//
//                        //extract each predictor its coefficient
//                        List<Coefficient> predictors = c.getPredictors();
//
//                        {
//                            List<Double> predictedValues = new ArrayList<>();
//                            {
//                                for (Double value : vDependent.getValues()) {
//                                    predictedValues.add(c.getIntercept()); // first ad intercept to predicted
//                                }
//                            }
//                            {
//                                String coeffNames = "";
//                                for (Coefficient coefficient : predictors) {
//                                    Double coeff = coefficient.getCoefficient();
//                                    Variable variable = coefficient.getVariable();
//                                    MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//
//                                    coeffNames += variable.getId();
//                                    for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                        predictedValues.set(i, predictedValues.get(i) + coeff * variable.getValues().get(i));
//                                    }
//                                }
//                                List<String> predictedColumn = new ArrayList<>();
//                                predictedColumn.add("predicted");
//                                for (Double predicted : predictedValues) {
//                                    predictedColumn.add("" + predicted);
//                                }
//                                columns.add(predictedColumn);
//                            }
//
//                            for (Coefficient coefficient : predictors) {
//                                Double coeff = coefficient.getCoefficient();
//                                Variable variable = coefficient.getVariable();
//                                MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//                                List<String> coefficientColumn = new ArrayList<>();
//                                String coeffName = variable.getId();
//                                coefficientColumn.add(c.getIntercept() + " + " + coefficient.getCoefficient() + "*" + coeffName);
//
//                                for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                    coefficientColumn.add("" + variable.getValues().get(i));
//                                }
//                                columns.add(coefficientColumn);
//                            }
//
//                            try {
//                                BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./experiments/corelations/" + vDependent.getId() + "_Timestamp_" + elasticitySpace.getTimestampID() + ".csv")));
//
//                                //max nr of records
//                                int max = columns.stream().max(new Comparator<List<String>>() {
//
//                                    @Override
//                                    public int compare(List<String> o1, List<String> o2) {
//                                        return ((Integer) o1.size()).compareTo(o2.size());
//                                    }
//
//                                }).get().size();
//
//                                for (int i = 0; i < max; i++) {
//                                    for (List<String> values : columns) {
//                                        String columnEntry = (values.size() > i) ? values.get(i) : "";
//                                        writer.write(columnEntry + ",");
//                                    }
//                                    writer.newLine();
//                                }
//
//                                writer.flush();
//                                writer.close();
//
//                            } catch (IOException ex) {
//                                Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//
//                        }
//
//                        for (Coefficient coefficient : predictors) {
//                            Double coeff = coefficient.getCoefficient();
//                            Double stdError = coefficient.getStdError();
//                            Variable variable = coefficient.getVariable();
//                            Metric predictorMetric = (Metric) variable.getMetaData(Metric.class.getName());
//                            MonitoredElement predictorElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//
//                            ElasticityDependencyCoefficient elasticityDependencyCoefficient = new ElasticityDependencyCoefficient(predictorElement, predictorMetric, coeff, stdError);
//                            dependencyElement.addCoefficient(elasticityDependencyCoefficient);
//                        }
//
//                        dependencyElement = LinearCorrelationStatisticsComputationEngine.enrichWithEstimationErrorStatistics(dependencyElement);if (dependencies.containsKey(element)) {
//                            dependencies.get(element).addElement(dependencyElement);
//                        } else {
//                            MonitoredElementElasticityDependency dependency = new MonitoredElementElasticityDependency(element);
//                            dependency.addElement(dependencyElement);
//                            dependencies.put(element, dependency);
//                        }
//                    }
//                }
//
//            };
//
//            t.setDaemon(true);
//            conversionThreads.add(t);
//            t.start();;
//
//        }
//
//        for (Thread t : conversionThreads) {
//            try {
//                t.join();
//            } catch (InterruptedException ex) {
//                Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        ServiceElasticityDependencies determinedDependencies = new ServiceElasticityDependencies();
//        determinedDependencies.setElasticityDependencies(dependencies.values());
//        determinedDependencies.setTimestampID(elasticitySpace.getTimestampID());
//
//        return determinedDependencies;
//    }
//    public ServiceElasticityDependencies analyzeElasticityDependencies(MonitoredElement monitoredElement,
//            ElasticitySpace elasticitySpace, Metric a, Metric b) {
//        //PersistenceSQLAccess persistenceDelegate = new PersistenceSQLAccess("mela", "mela", "localhost", Configuration.getDataServicePort(), monitoredElement.getId());
//
//        if (elasticitySpace == null) {
//            Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, "Elasticity space for " + monitoredElement + " is null");
//            return new ServiceElasticityDependencies();
//        }
//
//        //else we recompute dependencies
//        final ElasticityBehavior behavior = new ElasticityBehavior(elasticitySpace);
//
//        final List<LinearCorrelation> corelations = Collections.synchronizedList(new ArrayList<LinearCorrelation>());
//
//        Thread sameLevelAnalysis = new Thread() {
//
//            @Override
//            public void run() {
//                Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.INFO, "Analyzing behavior in same level");
//                List<LinearCorrelation> sameLayerCorelations = LinearElasticityDependencyAnalysisEngine.analyzeElasticityDependenciesBetweenMetrics(monitoredElement, a, b, behavior);
//                corelations.addAll(sameLayerCorelations);
//            }
//
//        };
//
//        sameLevelAnalysis.setDaemon(true);
//
//        sameLevelAnalysis.start();
//
//        try {
//            sameLevelAnalysis.join();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
////
//        final Map<MonitoredElement, MonitoredElementElasticityDependency> dependencies = Collections.synchronizedMap(new HashMap<MonitoredElement, MonitoredElementElasticityDependency>());
//
//        //confert in seperate threads
//        List<Thread> conversionThreads = new ArrayList<Thread>();
//
//        for (final LinearCorrelation c : corelations) {
//            Thread t = new Thread() {
//
//                @Override
//                public void run() {
//                    //transform from LinearCorrelation to MonitoredElementElasticityDependency
//                    {
//                        //write in csv the corelation data and result, to allow inspection
//                        List<List<String>> columns = new ArrayList<>();
//
//                        //extract data about dependent metric
//                        Double adjustedSquare = c.getAdjustedRSquared();
//                        Double intercept = c.getIntercept();
//                        Variable vDependent = c.getDependent();
//
//                        Metric metric = (Metric) vDependent.getMetaData(Metric.class.getName());
//                        MonitoredElement element = (MonitoredElement) vDependent.getMetaData(MonitoredElement.class.getName());
//
//                        //create column with main dependent var
//                        {
//                            List<String> column = new ArrayList<>();
//                            //put column
//                            column.add(vDependent.getId() + ":" + element.getId());
//                            for (Double value : vDependent.getValues()) {
//                                column.add("" + value);
//                            }
//                            columns.add(column);
//                        }
//
//                        ElasticityDependencyElement dependencyElement = new ElasticityDependencyElement(metric, intercept, adjustedSquare);
//
//                        //extract each predictor its coefficient
//                        List<Coefficient> predictors = c.getPredictors();
//
//                        {
//                            List<Double> predictedValues = new ArrayList<>();
//                            {
//                                for (Double value : vDependent.getValues()) {
//                                    predictedValues.add(c.getIntercept()); // first ad intercept to predicted
//                                }
//                            }
//                            {
//                                String coeffNames = "";
//                                for (Coefficient coefficient : predictors) {
//                                    Double coeff = coefficient.getCoefficient();
//                                    Variable variable = coefficient.getVariable();
//                                    MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//
//                                    coeffNames += variable.getId();
//                                    for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                        predictedValues.set(i, predictedValues.get(i) + coeff * variable.getValues().get(i));
//                                    }
//                                }
//                                List<String> predictedColumn = new ArrayList<>();
//                                predictedColumn.add("predicted");
//                                for (Double predicted : predictedValues) {
//                                    predictedColumn.add("" + predicted);
//                                }
//                                columns.add(predictedColumn);
//                            }
//
//                            for (Coefficient coefficient : predictors) {
//                                Double coeff = coefficient.getCoefficient();
//                                Variable variable = coefficient.getVariable();
//                                MonitoredElement dependentElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//                                List<String> coefficientColumn = new ArrayList<>();
//                                String coeffName = variable.getId();
//                                coefficientColumn.add(c.getIntercept() + " + " + coefficient.getCoefficient() + "*" + coeffName);
//
//                                for (int i = 0; i < vDependent.getValues().size(); i++) {
//                                    coefficientColumn.add("" + variable.getValues().get(i));
//                                }
//                                columns.add(coefficientColumn);
//                            }
//
//                            try {
//                                BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./experiments/corelations/" + vDependent.getId() + "_Timestamp_" + elasticitySpace.getTimestampID() + ".csv")));
//
//                                //max nr of records
//                                int max = columns.stream().max(new Comparator<List<String>>() {
//
//                                    @Override
//                                    public int compare(List<String> o1, List<String> o2) {
//                                        return ((Integer) o1.size()).compareTo(o2.size());
//                                    }
//
//                                }).get().size();
//
//                                for (int i = 0; i < max; i++) {
//                                    for (List<String> values : columns) {
//                                        String columnEntry = (values.size() > i) ? values.get(i) : "";
//                                        writer.write(columnEntry + ",");
//                                    }
//                                    writer.newLine();
//                                }
//
//                                writer.flush();
//                                writer.close();
//
//                            } catch (IOException ex) {
//                                Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.SEVERE, null, ex);
//                            }
//
//                        }
//
//                        for (Coefficient coefficient : predictors) {
//                            Double coeff = coefficient.getCoefficient();
//                            Double stdError = coefficient.getStdError();
//                            Variable variable = coefficient.getVariable();
//                            Metric predictorMetric = (Metric) variable.getMetaData(Metric.class.getName());
//                            MonitoredElement predictorElement = (MonitoredElement) variable.getMetaData(MonitoredElement.class.getName());
//
//                            ElasticityDependencyCoefficient elasticityDependencyCoefficient = new ElasticityDependencyCoefficient(predictorElement, predictorMetric, coeff, stdError);
//                            dependencyElement.addCoefficient(elasticityDependencyCoefficient);
//                        }
//
//                        dependencyElement = LinearCorrelationStatisticsComputationEngine.enrichWithEstimationErrorStatistics(dependencyElement);if (dependencies.containsKey(element)) {
//                            dependencies.get(element).addElement(dependencyElement);
//                        } else {
//                            MonitoredElementElasticityDependency dependency = new MonitoredElementElasticityDependency(element);
//                            dependency.addElement(dependencyElement);
//                            dependencies.put(element, dependency);
//                        }
//                    }
//                }
//
//            };
//
//            t.setDaemon(true);
//            conversionThreads.add(t);
//            t.start();;
//
//        }
//
//        for (Thread t : conversionThreads) {
//            try {
//                t.join();
//            } catch (InterruptedException ex) {
//                Logger.getLogger(ElasticityDependencyAnalysisManager.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//        ServiceElasticityDependencies determinedDependencies = new ServiceElasticityDependencies();
//        determinedDependencies.setElasticityDependencies(dependencies.values());
//        determinedDependencies.setTimestampID(elasticitySpace.getTimestampID());
//
//        return determinedDependencies;
//    }
    /**
     *
     * @param monitoredElement will be used when I update PersistenceSQLAccess
     * to handle more services, i.e. send it as param in
     * extractLatestMonitoringData()
     * @return
     */
    public String getServiceStructureAndMetricsAsJSON(MonitoredElement monitoredElement) {
        //PersistenceSQLAccess persistenceDelegate = new PersistenceSQLAccess("mela", "mela", "localhost", Configuration.getDataServicePort(), monitoredElement.getId());
        ServiceMonitoringSnapshot monitoringSnapshot = persistenceDelegate.extractLatestMonitoringData(monitoredElement.getId());
        String asJSON = Converter.convertMonitoringSnapshotWithoutVM(monitoringSnapshot);

        return asJSON;

    }

    public String getAllManagedServicesIDs() {

        JSONArray array = new JSONArray();

        for (String s : persistenceDelegate.getMonitoringSequencesIDs()) {
            JSONObject o = new JSONObject();
            o.put("id", s);
            array.add(o);
        }
        return array.toJSONString();
    }

}
