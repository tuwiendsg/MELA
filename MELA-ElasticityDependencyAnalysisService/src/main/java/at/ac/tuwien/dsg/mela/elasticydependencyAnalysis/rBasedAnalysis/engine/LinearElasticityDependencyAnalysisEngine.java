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

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRule;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.MonitoredElementElasticityDependency;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticityBehavior;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticityBehavior.ElasticityBehaviorDimension;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.LinearCorrelation;
import at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.concept.Variable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static scala.tools.scalap.scalax.rules.scalasig.NoSymbol.parent;

/**
 * First, it computes the ElasticityBehavior, which transform all metric values
 * in the elasticity space in percentages with respect to the metric's upper
 * boundary
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class LinearElasticityDependencyAnalysisEngine {

    @Autowired
    private LinearCorrelationAnalysisEngine analysisEngine;

    /**
     *
     * @param element
     * @param compositionRulesConfiguration
     * @return a map containing all metrics composed by compositionRules for
     * element Key = resultingMetric, values are source metrics
     */
    private Map<Metric, List<Metric>> getCompositionRulesDependencies(MonitoredElement element, CompositionRulesConfiguration compositionRulesConfiguration) {
        Map<Metric, List<Metric>> m = new HashMap<>();

        ArrayList<CompositionRule> compositionRules = compositionRulesConfiguration.getMetricCompositionRules().getCompositionRules();
        for (CompositionRule compositionRule : compositionRules) {
            if (compositionRule.getTargetMonitoredElementLevel().equals(element.getLevel())) {
                if (compositionRule.getTargetMonitoredElementIDs().isEmpty() || compositionRule.getTargetMonitoredElementIDs().contains(element.getId())) {
                    Metric resulting = compositionRule.getResultingMetric();

                    List<Metric> sources = new ArrayList<>();

                    //as composition operations are cascaded recursively, i need to get all Metrics from child level?
                    List<CompositionOperation> queue = new ArrayList<>();
                    queue.add(compositionRule.getOperation());

                    while (!queue.isEmpty()) {
                        CompositionOperation operation = queue.remove(0);
                        if (operation.getTargetMetric() != null) {
                            sources.add(operation.getTargetMetric());
                        }
                        queue.addAll(operation.getSubOperations());

                    }

                    m.put(resulting, sources);

                }
            }
        }

        return m;

    }

    public List<LinearCorrelation> analyzeElasticityDependenciesAcrossLevel(ElasticityBehavior behavior, CompositionRulesConfiguration compositionRulesConfiguration) {

        final List<LinearCorrelation> corelations = new ArrayList<LinearCorrelation>();

        final Map<MonitoredElement, List<ElasticityBehavior.ElasticityBehaviorDimension>> monitoringData = behavior.getBehavior();

        MonitoredElement service = behavior.getElasticitySpace().getService();
        MonitoredElementElasticityDependency dependencyAnalysis = new MonitoredElementElasticityDependency(service);

        List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
//        List<MonitoredElementElasticityDependency> dependencyQueue = new ArrayList<MonitoredElementElasticityDependency>();
        queue.add(service);
//        dependencyQueue.add(dependencyAnalysis);

        List<Thread> processingThreads = new ArrayList<Thread>();

        while (!queue.isEmpty()) {

            final MonitoredElement element = queue.remove(0);
            final Collection<MonitoredElement> children = new ArrayList<>();

            //from service unit
            if (element.getLevel() != MonitoredElement.MonitoredElementLevel.SERVICE_UNIT) {
                children.addAll(element.getContainedElements());
                queue.addAll(children);
            }

            //get element composition rules
            Map<Metric, List<Metric>> compositionRulesDependentMetrics = getCompositionRulesDependencies(element, compositionRulesConfiguration);

            //analyze dependency for each metric and all children
//            Collection<ElasticityDependencyElement> elasticityElements = new ArrayList<ElasticityDependencyElement>();
            Collection<Variable> elementVariables = new ArrayList<Variable>();

            //this is done because the linear dependency extracts "interceptor" = constant
            //and I apply linear dependency on behavior with respect to the upper boundary
            //so I can get interceptor = 20 which means 20% of upper boundary on dependent metric, not 20 as value
            // so I use this map to convert the interceptor back to value
//            Map<Variable, ElasticityBehavior.ElasticityBehaviorDimension> variablesMapedToBehavior = new HashMap<>();
            {

                for (ElasticityBehavior.ElasticityBehaviorDimension dimension : monitoringData.get(element)) {

                    //
                    Metric dependant = dimension.getMetric();
                    String cleanedName = dependant.getName().replace("/", "_");
                    //convert to R Analysis data format
                    Variable vDependent = new Variable(cleanedName + "_" + element.getId());

//                    variablesMapedToBehavior.put(vDependent, dimension);
                    vDependent.setMetaData(Metric.class.getName(), dependant);
                    vDependent.setMetaData(MonitoredElement.class.getName(), element);

                    {
                        List<Double> dependantValues = dimension.getBoundaryFulfillment();

                        for (int i = 0; i < dependantValues.size(); i++) {
                            vDependent.addValue(dependantValues.get(i));
                        }

                    }
                    elementVariables.add(vDependent);

                }
            }

            for (final Variable elementVar : elementVariables) {

                Thread t = new Thread() {

                    @Override
                    public void run() {
                        {
                            List<Variable> childrenVariables = new ArrayList<Variable>();
                            {
                                for (ElasticityBehavior.ElasticityBehaviorDimension dimension : monitoringData.get(element)) {
                                    Metric dependant = dimension.getMetric();
                                    String cleanedName = dependant.getName().replace("/", "_");

                                    Variable vDependent = new Variable(cleanedName + "_" + element.getId());
                                    vDependent.setMetaData(Metric.class.getName(), dependant);
                                    vDependent.setMetaData(MonitoredElement.class.getName(), element);

                                    if (!elementVar.equals(vDependent)) {

                                        {
                                            List<Double> dependantValues = dimension.getBoundaryFulfillment();

                                            for (int i = 0; i < dependantValues.size(); i++) {
                                                vDependent.addValue(dependantValues.get(i));
                                            }

                                        }

                                        childrenVariables.add(vDependent);
                                    }
                                }
                            }

                            {
                                for (MonitoredElement child : children) {

                                    //go and extract ALL metrics
//                        for (MonitoredElement m : child) {
//                            if (m.getLevel() != MonitoredElement.MonitoredElementLevel.VM) {
//                        Map<Metric, List<MetricValue>> elementMetrics = monitoringData.get(m);
                                    for (ElasticityBehavior.ElasticityBehaviorDimension dimension : monitoringData.get(child)) {
                                        Metric cMetric = dimension.getMetric();
                                        String cleanedName = cMetric.getName().replace("/", "_");

                                        Variable vDependent = new Variable(cleanedName + "_" + child.getId());
                                        vDependent.setMetaData(Metric.class.getName(), cMetric);
                                        vDependent.setMetaData(MonitoredElement.class.getName(), child);

                                        {
                                            List<Double> dependantValues = dimension.getBoundaryFulfillment();

                                            for (int i = 0; i < dependantValues.size(); i++) {
                                                vDependent.addValue(dependantValues.get(i));
                                            }

                                        }
                                        childrenVariables.add(vDependent);

                                    }
                                }
                            }
                            try {

                                //remove from children metrics present in composition rules as sources for the elementVar
                                if (compositionRulesDependentMetrics.containsKey((Metric) elementVar.getMetaData(Metric.class.getName()))) {
                                    List<Metric> sources = compositionRulesDependentMetrics.get((Metric) elementVar.getMetaData(Metric.class.getName()));

                                    Iterator<Variable> it = childrenVariables.iterator();
                                    while (it.hasNext()) {
                                        Variable v = it.next();
                                        if (sources.contains((Metric) v.getMetaData(Metric.class.getName()))) {
                                            it.remove();
                                        }
                                    }

                                }

                                LinearCorrelation correlation = analysisEngine.evaluateLinearCorrelation(elementVar, childrenVariables);

                                if (correlation.getAdjustedRSquared() < Double.POSITIVE_INFINITY) {
                                    corelations.add(correlation);
                                }
                                //System.out.println(correlation);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                };
                t.setDaemon(true);
                processingThreads.add(t);
                t.start();

            }

        }

        for (Thread t : processingThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(LinearElasticityDependencyAnalysisEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return corelations;

    }

    public List<LinearCorrelation> analyzeElasticityDependenciesInSameLevel(ElasticityBehavior behavior, CompositionRulesConfiguration compositionRulesConfiguration) {

        final List<LinearCorrelation> corelations = new ArrayList<LinearCorrelation>();

        //sort the objects per level, and search for inn-level dependencies (e.g. between Service Units, or Service Topologies belonging to same element)
        final Map<MonitoredElement, List<ElasticityBehavior.ElasticityBehaviorDimension>> monitoringData = behavior.getBehavior();

        MonitoredElement service = behavior.getElasticitySpace().getService();
//        MonitoredElementElasticityDependency dependencyAnalysis = new MonitoredElementElasticityDependency(service);

        List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
//        List<MonitoredElementElasticityDependency> dependencyQueue = new ArrayList<MonitoredElementElasticityDependency>();
        queue.add(service);
//        dependencyQueue.add(dependencyAnalysis);

        final List<Thread> processingMainThreads = new ArrayList<Thread>();

        while (!queue.isEmpty()) {

            final MonitoredElement parent = queue.remove(0);
            if (parent.getLevel() == MonitoredElement.MonitoredElementLevel.VM) {
                break;
            }

            //get element composition rules
            Map<Metric, List<Metric>> compositionRulesDependentMetrics = getCompositionRulesDependencies(parent, compositionRulesConfiguration);

            final List<MonitoredElement> siblings = new ArrayList<MonitoredElement>(parent.getContainedElements());
            queue.addAll(siblings);

            //analyze dependency for each metric and all children
//            Collection<ElasticityDependencyElement> elasticityElements = new ArrayList<ElasticityDependencyElement>();
            //for each element, get its dependencies with the rest
            for (int index = 0; index < siblings.size(); index++) {
//               
                final MonitoredElement element = siblings.get(index);

                Thread elementDependencyAnalysisThread = new Thread() {
//
                    @Override
                    public void run() {
//                   
                        Collection<Variable> elementVariables = new ArrayList<Variable>();

                        {

                            for (ElasticityBehavior.ElasticityBehaviorDimension dimension : monitoringData.get(element)) {

                                Metric dependant = dimension.getMetric();
                                String cleanedName = dependant.getName().replace("/", "_");

                                Variable vDependent = new Variable(cleanedName + "_" + element.getId());
                                vDependent.setMetaData(Metric.class.getName(), dependant);
                                vDependent.setMetaData(MonitoredElement.class.getName(), element);

                                {
                                    List<Double> dependantValues = dimension.getBoundaryFulfillment();

                                    for (int i = 0; i < dependantValues.size(); i++) {
                                        vDependent.addValue(dependantValues.get(i));
                                    }

                                }
                                elementVariables.add(vDependent);
                            }
                        }

                        List<Thread> processingThreads = new ArrayList<Thread>();

                        for (final Variable elementVar : elementVariables) {

                            Thread t = new Thread() {
//
//                        @Override
                                public void run() {
                                    {

                                        List<Variable> siblingVariables = new ArrayList<Variable>();

                                        for (MonitoredElement sibling : siblings) {
                                            if (sibling.equals(element)) {
                                                continue;
                                            }
                                            {
                                                for (ElasticityBehavior.ElasticityBehaviorDimension dimension : monitoringData.get(sibling)) {
                                                    Metric cMetric = dimension.getMetric();
                                                    String cleanedName = cMetric.getName().replace("/", "_");

                                                    Variable vDependent = new Variable(cleanedName + "_" + sibling.getId());
                                                    vDependent.setMetaData(Metric.class.getName(), cMetric);
                                                    vDependent.setMetaData(MonitoredElement.class.getName(), sibling);

                                                    {
                                                        List<Double> dependantValues = dimension.getBoundaryFulfillment();

                                                        for (int i = 0; i < dependantValues.size(); i++) {
                                                            vDependent.addValue(dependantValues.get(i));
                                                        }

                                                    }
                                                    siblingVariables.add(vDependent);

                                                }
                                            }
                                        }
                                        try {
                                            //remove from children metrics present in composition rules as sources for the elementVar
                                            if (compositionRulesDependentMetrics.containsKey((Metric) elementVar.getMetaData(Metric.class.getName()))) {
                                                List<Metric> sources = compositionRulesDependentMetrics.get((Metric) elementVar.getMetaData(Metric.class.getName()));

                                                Iterator<Variable> it = siblingVariables.iterator();
                                                while (it.hasNext()) {
                                                    Variable v = it.next();
                                                    if (sources.contains((Metric) v.getMetaData(Metric.class.getName()))) {
                                                        it.remove();
                                                    }
                                                }

                                            }
                                            LinearCorrelation correlation = analysisEngine.evaluateLinearCorrelation(elementVar, siblingVariables);
                                            if (correlation.getAdjustedRSquared() < Double.POSITIVE_INFINITY) {
                                                corelations.add(correlation);
                                            }
                                            //System.out.println(correlation);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            };
                            t.setDaemon(true);
                            processingThreads.add(t);
                            t.start();
                        }
                        for (Thread t : processingThreads) {
                            try {
                                t.join();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(LinearElasticityDependencyAnalysisEngine.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    }
                };
                elementDependencyAnalysisThread.setDaemon(true);
                processingMainThreads.add(elementDependencyAnalysisThread);
                elementDependencyAnalysisThread.start();
            }

        }

        for (Thread t : processingMainThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(LinearElasticityDependencyAnalysisEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return corelations;

    }

    public List<LinearCorrelation> analyzeElasticityDependenciesInSameElement(ElasticityBehavior behavior, CompositionRulesConfiguration compositionRulesConfiguration) {

        final List<LinearCorrelation> corelations = new ArrayList<LinearCorrelation>();

        //sort the objects per level, and search for inn-level dependencies (e.g. between Service Units, or Service Topologies belonging to same element)
        final Map<MonitoredElement, List<ElasticityBehavior.ElasticityBehaviorDimension>> monitoringData = behavior.getBehavior();

        MonitoredElement service = behavior.getElasticitySpace().getService();

        List<MonitoredElement> queue = new ArrayList<MonitoredElement>();
//        List<MonitoredElementElasticityDependency> dependencyQueue = new ArrayList<MonitoredElementElasticityDependency>();
        queue.add(service);

        List<Thread> processingThreads = new ArrayList<Thread>();
        while (!queue.isEmpty()) {

            final MonitoredElement parent = queue.remove(0);
            if (parent.getLevel() == MonitoredElement.MonitoredElementLevel.VM) {
                break;
            }

            Map<Metric, List<Metric>> compositionRulesDependentMetrics = getCompositionRulesDependencies(parent, compositionRulesConfiguration);

            List<MonitoredElement> children = new ArrayList<MonitoredElement>(parent.getContainedElements());
            queue.addAll(children);

            ArrayList<Variable> elementVariables = new ArrayList<Variable>();
            for (ElasticityBehavior.ElasticityBehaviorDimension dimension : monitoringData.get(parent)) {

                Metric dependant = dimension.getMetric();
                String cleanedName = dependant.getName().replace("/", "_");

                Variable vDependent = new Variable(cleanedName + "_" + parent.getId());
                vDependent.setMetaData(Metric.class.getName(), dependant);
                vDependent.setMetaData(MonitoredElement.class.getName(), parent);

                {
                    List<Double> dependantValues = dimension.getBoundaryFulfillment();

                    for (int i = 0; i < dependantValues.size(); i++) {
                        vDependent.addValue(dependantValues.get(i));
                    }

                }
                elementVariables.add(vDependent);
            }

            for (int i = 0; i < elementVariables.size(); i++) {

                Variable elementVar = elementVariables.get(i);
                List<Variable> otherVars = new ArrayList<>();
                if (i > 0) {
                    otherVars.addAll(elementVariables.subList(0, i));
                }
                if (i < elementVariables.size() - 1) {
                    otherVars.addAll(elementVariables.subList(i + 1, elementVariables.size()));
                }

                Thread t = new Thread() {

                    @Override
                    public void run() {
                        try {
                            //remove from children metrics present in composition rules as sources for the elementVar
                            if (compositionRulesDependentMetrics.containsKey((Metric) elementVar.getMetaData(Metric.class.getName()))) {
                                List<Metric> sources = compositionRulesDependentMetrics.get((Metric) elementVar.getMetaData(Metric.class.getName()));

                                Iterator<Variable> it = otherVars.iterator();
                                while (it.hasNext()) {
                                    Variable v = it.next();
                                    if (sources.contains((Metric) v.getMetaData(Metric.class.getName()))) {
                                        it.remove();
                                    }
                                }

                            }
                            LinearCorrelation correlation = analysisEngine.evaluateLinearCorrelation(elementVar, otherVars);
                            if (correlation.getAdjustedRSquared() < Double.POSITIVE_INFINITY) {
                                corelations.add(correlation);
                            }
                            //System.out.println(correlation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                };
                t.setDaemon(true);
                processingThreads.add(t);
                t.start();
            }
        }
        for (Thread t : processingThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(LinearElasticityDependencyAnalysisEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return corelations;

    }

    public List<LinearCorrelation> analyzeElasticityDependenciesBetweenMetrics(MonitoredElement monitoredElement, Metric a, Metric b, ElasticityBehavior behavior) {

        final List<LinearCorrelation> corelations = new ArrayList<LinearCorrelation>();

        //sort the objects per level, and search for inn-level dependencies (e.g. between Service Units, or Service Topologies belonging to same element)
        List<ElasticityBehavior.ElasticityBehaviorDimension> monitoringData = behavior.getBehavior().get(monitoredElement);

        //find dimensions
        ElasticityBehavior.ElasticityBehaviorDimension dimensionA = null;
        ElasticityBehavior.ElasticityBehaviorDimension dimensionB = null;

        for (ElasticityBehavior.ElasticityBehaviorDimension dim : monitoringData) {
            if (dim.getMetric().equals(a)) {
                dimensionA = dim;
            } else if (dim.getMetric().equals(b)) {
                dimensionB = dim;
            }
        }

        Variable aVar = null;
        Variable bVar = null;

        {

            Metric dependant = dimensionA.getMetric();
            String cleanedName = dependant.getName().replace("/", "_");

            aVar = new Variable(cleanedName + "_" + monitoredElement.getId());
            aVar.setMetaData(Metric.class.getName(), dependant);
            aVar.setMetaData(MonitoredElement.class.getName(), monitoredElement);

            {
                List<Double> dependantValues = dimensionA.getBoundaryFulfillment();

                for (int i = 0; i < dependantValues.size(); i++) {
                    aVar.addValue(dependantValues.get(i));
                }

            }

        }
        {

            Metric dependant = dimensionB.getMetric();
            String cleanedName = dependant.getName().replace("/", "_");

            bVar = new Variable(cleanedName + "_" + monitoredElement.getId());
            bVar.setMetaData(Metric.class.getName(), dependant);
            bVar.setMetaData(MonitoredElement.class.getName(), monitoredElement);

            {
                List<Double> dependantValues = dimensionB.getBoundaryFulfillment();

                for (int i = 0; i < dependantValues.size(); i++) {
                    bVar.addValue(dependantValues.get(i));
                }

            }

        }

        List<Variable> list = new ArrayList<>();
        list.add(bVar);

        try {

            LinearCorrelation correlation = analysisEngine.evaluateLinearCorrelation(aVar, list);
            if (correlation.getAdjustedRSquared() < Double.POSITIVE_INFINITY) {
                corelations.add(correlation);
            }
            //System.out.println(correlation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return corelations;

    }

    public List<LinearCorrelation> analyzeElasticityDependenciesBetweenMetrics(ElasticityBehavior behavior) {

        final List<LinearCorrelation> corelations = new ArrayList<LinearCorrelation>();

        final ArrayList<Variable> elementVariables = new ArrayList<Variable>();

        //sort the objects per level, and search for inn-level dependencies (e.g. between Service Units, or Service Topologies belonging to same element)
        final Map<MonitoredElement, List<ElasticityBehavior.ElasticityBehaviorDimension>> monitoringData = behavior.getBehavior();

        //get all el metrics as variables
        for (MonitoredElement element : monitoringData.keySet()) {
            List<ElasticityBehavior.ElasticityBehaviorDimension> dimensions = monitoringData.get(element);

            for (ElasticityBehaviorDimension dimension : dimensions) {

                Metric dependant = dimension.getMetric();
                String cleanedName = dependant.getName().replace("/", "_");

                Variable vDependent = new Variable(cleanedName + "_" + element.getId());
                vDependent.setMetaData(Metric.class.getName(), dependant);
                vDependent.setMetaData(MonitoredElement.class.getName(), element);

                {
                    List<Double> dependantValues = dimension.getBoundaryFulfillment();

                    for (int i = 0; i < dependantValues.size(); i++) {
                        vDependent.addValue(dependantValues.get(i));
                    }

                }
                elementVariables.add(vDependent);
            }
        }

        List<Thread> processingThreads = new ArrayList<Thread>();

        for (int i = 0; i < elementVariables.size(); i++) {

            Variable elementVar = elementVariables.get(i);
            List<Variable> otherVars = new ArrayList<>();
            if (i > 0) {
                otherVars.addAll(elementVariables.subList(0, i));
            }
            if (i < elementVariables.size() - 1) {
                otherVars.addAll(elementVariables.subList(i + 1, elementVariables.size()));
            }
//
            Thread t = new Thread() {

                @Override
                public void run() {
                    try {

                        LinearCorrelation correlation = analysisEngine.evaluateLinearCorrelation(elementVar, otherVars);
                        if (correlation.getAdjustedRSquared() < Double.POSITIVE_INFINITY) {
                            corelations.add(correlation);
                        }
                        //System.out.println(correlation);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            };
            t.setDaemon(true);
            processingThreads.add(t);
            t.start();
        }
        for (Thread t : processingThreads) {
            try {
                t.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(LinearElasticityDependencyAnalysisEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return corelations;

    }

}
