/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.quelle.graphVizOutputFormatters;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.RequirementsMatchingEngine.RequirementsMatchingReport;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.ServiceUnitConfigurationSolution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class SESConfigurationRecommandationToGraphViz {

    //used to index nodes
    protected static AtomicInteger createdNodesCount = new AtomicInteger(0);

    static void clear() {
//        createdSoFar.clear();
    }
    protected String name;
    protected int thisIndex;
    protected Map<String, String> qualityProperties;
    protected List<SelectedOptionalDependencyDot> selectedOptionalQualityDependencies;
    protected List<SelectedOptionalDependencyDot> selectedOptionalCostDependencies;
    protected List<SelectedOptionalDependencyDot> selectedOptionalResourceDependencies;
    protected Map<String, String> costProperties;
    protected Map<String, String> resourceProperties;
    protected String metaInfo = "shape=Mrecord";
    protected static List<SESConfigurationRecommandationToGraphViz> createdSoFar;
    protected List<SESConfigurationRecommandationToGraphViz> associatedServiceUnits;

    static {
        createdSoFar = new ArrayList<SESConfigurationRecommandationToGraphViz>();
    }

    {
        qualityProperties = new LinkedHashMap<String, String>();
        costProperties = new LinkedHashMap<String, String>();
        resourceProperties = new LinkedHashMap<String, String>();
        associatedServiceUnits = new ArrayList<SESConfigurationRecommandationToGraphViz>();
        selectedOptionalQualityDependencies = new ArrayList<>();
        selectedOptionalCostDependencies = new ArrayList<>();
        selectedOptionalResourceDependencies = new ArrayList<>();

        thisIndex = createdNodesCount.incrementAndGet();
    }

    public SESConfigurationRecommandationToGraphViz(String name) {
        this.name = name;
    }

    protected void addAssociatedServiceUnit(SESConfigurationRecommandationToGraphViz cGSolDotNode) {
        associatedServiceUnits.add(cGSolDotNode);
    }

    protected void removeAssociatedServiceUnit(SESConfigurationRecommandationToGraphViz cGSolDotNode) {
        associatedServiceUnits.remove(cGSolDotNode);
    }

    protected void addSelectedOptionalQualityDependency(SelectedOptionalDependencyDot dependency) {
        selectedOptionalQualityDependencies.add(dependency);
    }

    protected void removeSelectedOptionalQualityDependency(SelectedOptionalDependencyDot dependency) {
        selectedOptionalQualityDependencies.remove(dependency);
    }

    protected void addSelectedOptionalCostDependency(SelectedOptionalDependencyDot dependency) {
        selectedOptionalCostDependencies.add(dependency);
    }

    protected void removeSelectedCostQualityDependency(SelectedOptionalDependencyDot dependency) {
        selectedOptionalCostDependencies.remove(dependency);
    }

    protected void addSelectedOptionalResourceDependency(SelectedOptionalDependencyDot dependency) {
        selectedOptionalResourceDependencies.add(dependency);
    }

    protected void removeSelectedOptionalResourceDependency(SelectedOptionalDependencyDot dependency) {
        selectedOptionalResourceDependencies.remove(dependency);
    }

    protected void addQualityProperty(String key, String value) {
        qualityProperties.put(key, value);
    }

    protected void removeQualityProperty(String key, String value) {
        qualityProperties.put(key, value);
    }

    protected void addCostProperty(String key, String value) {
        costProperties.put(key, value);
    }

    protected void removeCostProperty(String key, String value) {
        costProperties.put(key, value);
    }

    protected void addResourceProperty(String key, String value) {
        resourceProperties.put(key, value);
    }

    protected void removeResourceProperty(String key, String value) {
        resourceProperties.put(key, value);
    }

    public String getNodeID() {
        return "N" + thisIndex;
    }

    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
    }

    public void addMetaInfo(String metaInfo) {
        this.metaInfo += metaInfo;
    }

    protected static class SelectedOptionalDependencyDot {

        protected String name;
        protected Map<String, String> properties;

        {
            properties = new HashMap<>();
        }

        public SelectedOptionalDependencyDot(String name) {
            this.name = name;
        }

        public SelectedOptionalDependencyDot(String name, Map<String, String> properties) {
            this.name = name;
            this.properties = properties;
        }

        protected void addProperty(String key, String value) {
            properties.put(key, value);
        }

        protected void removeProperty(String key, String value) {
            properties.put(key, value);
        }

        public String getName() {
            return name.split("Cost")[0];
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }

    }

    public String toDotString() {
        String dotString = " N" + thisIndex + " [   " + metaInfo + "\n"
                + "label = \"{ " + name + " ";

//struct4 [label="{ EBS  | { { Resources }  | { Quality |  'IOperformance:IOPS' = '4000' }  | Cost}}"];
        //add resource properties
        dotString += " | { Resources | ";

        if (!resourceProperties.entrySet().isEmpty()) {

            for (Map.Entry<String, String> entry : resourceProperties.entrySet()) {
                dotString += " '" + entry.getKey() + "' = '" + entry.getValue() + "' \\l ";
            }
            dotString += "|";
        }

        for (SelectedOptionalDependencyDot dependencyDot : selectedOptionalResourceDependencies) {
            dotString += "" + dependencyDot.getName() + " :";
            for (Map.Entry<String, String> entry : dependencyDot.getProperties().entrySet()) {
                dotString += " '" + entry.getKey() + "' = '" + entry.getValue() + "' \\l ";
            }

        }
        dotString += "}  ";

        //add quality properties
        dotString += " | { Quality | ";

        if (!qualityProperties.entrySet().isEmpty()) {
            for (Map.Entry<String, String> entry : qualityProperties.entrySet()) {
                dotString += " '" + entry.getKey() + "' = '" + entry.getValue() + "' \\l ";
            }
            dotString += "|";
        }

        for (SelectedOptionalDependencyDot dependencyDot : selectedOptionalQualityDependencies) {
            dotString += dependencyDot.getName() + " :";
            for (Map.Entry<String, String> entry : dependencyDot.getProperties().entrySet()) {
                dotString += " '" + entry.getKey() + "' = '" + entry.getValue() + "' \\l ";
            }

        }

        dotString += "}  ";

        //add cost properties
        dotString += " | { Cost  |";

        if (!costProperties.entrySet().isEmpty()) {

            for (Map.Entry<String, String> entry : costProperties.entrySet()) {
                dotString += " '" + entry.getKey() + "' = '" + entry.getValue() + "' \\l ";
            }
            dotString += "|";
        }

        for (SelectedOptionalDependencyDot dependencyDot : selectedOptionalCostDependencies) {
            dotString += "" + dependencyDot.getName() + " :";
            for (Map.Entry<String, String> entry : dependencyDot.getProperties().entrySet()) {
                dotString += " '" + entry.getKey() + "' = '" + entry.getValue() + "' \\l ";
            }

        }

        dotString += "}   ";

        dotString += "}\"  ] \n";

        for (SESConfigurationRecommandationToGraphViz dotNode : associatedServiceUnits) {
            dotNode.setMetaInfo(this.metaInfo);
            dotString += dotNode.toDotString();
            dotString += createRelationship(this, dotNode, "associatedWith");
        }

        return dotString;
    }

    /**
     * Just returns the DOT representation of a particular Entity. It IGNORES
     * ALL RELATIONSHIPS
     *
     * @param solution
     * @return
     */
    public static SESConfigurationRecommandationToGraphViz toDotNode(ServiceUnitConfigurationSolution solution) {
        SESConfigurationRecommandationToGraphViz dotNode = new SESConfigurationRecommandationToGraphViz(solution.getServiceUnit().getName());

        createdSoFar.add(dotNode);

        //fixedResources
        for (Resource resource : solution.getServiceUnit().getResourceProperties()) {
            for (Map.Entry<Metric, MetricValue> entry : resource.getProperties().entrySet()) {
                Metric metric = entry.getKey();
                dotNode.addResourceProperty(metric.getName() + ":" + metric.getMeasurementUnit(), entry.getValue().getValueRepresentation());
            }
        }

        //chosen Resources
        for (RequirementsMatchingReport<Resource> report : solution.getChosenResourceOptions()) {
            Resource concreteResourceCfg = report.getConcreteConfiguration();
            SelectedOptionalDependencyDot resourceDependency = new SelectedOptionalDependencyDot(concreteResourceCfg.getName());

            for (Map.Entry<Metric, MetricValue> entry : concreteResourceCfg.getProperties().entrySet()) {
                Metric metric = entry.getKey();
                resourceDependency.addProperty(metric.getName() + ":" + metric.getMeasurementUnit(), entry.getValue().getValueRepresentation());
            }

            dotNode.addSelectedOptionalResourceDependency(resourceDependency);
        }

        //fixed Quality
        for (Quality quality : solution.getServiceUnit().getQualityProperties()) {
            for (Map.Entry<Metric, MetricValue> entry : quality.getProperties().entrySet()) {
                Metric metric = entry.getKey();
                dotNode.addQualityProperty(metric.getName() + ":" + metric.getMeasurementUnit(), entry.getValue().getValueRepresentation());
            }
        }

        //chosen Quality
        for (RequirementsMatchingReport<Quality> report : solution.getChosenQualityOptions()) {
            Quality concreteResourceCfg = report.getConcreteConfiguration();
            SelectedOptionalDependencyDot resourceDependency = new SelectedOptionalDependencyDot(concreteResourceCfg.getName());
            for (Map.Entry<Metric, MetricValue> entry : concreteResourceCfg.getProperties().entrySet()) {
                Metric metric = entry.getKey();
                resourceDependency.addProperty(metric.getName() + ":" + metric.getMeasurementUnit(), entry.getValue().getValueRepresentation());
            }
            dotNode.addSelectedOptionalQualityDependency(resourceDependency);
        }

        //fixed Cost
        for (CostFunction costFunction : solution.getCostFunctions()) {

            SelectedOptionalDependencyDot resourceDependency = new SelectedOptionalDependencyDot(costFunction.getName());

            for (CostElement ce : costFunction.getCostElements()) {
                Metric costMetric = ce.getCostMetric();
                String cost = costMetric.getName() + ":" + costMetric.getMeasurementUnit();
                String costValue = "";
                for (Map.Entry<MetricValue, Double> entry : ce.getCostIntervalFunction().entrySet()) {
                    costValue += " [" + entry.getKey().getValueRepresentation() + ", " + entry.getValue() + "] ";
                }
                resourceDependency.addProperty(cost, costValue);
            }
            dotNode.addSelectedOptionalCostDependency(resourceDependency);
        }

        for (ServiceUnitConfigurationSolution configurationSolution : solution.getMandatoryAssociatedServiceUnits()) {
            dotNode.addAssociatedServiceUnit(toDotNode(configurationSolution));
        }

        for (ServiceUnitConfigurationSolution configurationSolution : solution.getOptionallyAssociatedServiceUnits()) {
            dotNode.addAssociatedServiceUnit(toDotNode(configurationSolution));
        }

        return dotNode;
    }

    public static String createRelationship(SESConfigurationRecommandationToGraphViz dn1, SESConfigurationRecommandationToGraphViz dn2, String relationshipName) {
        String relationshipString = "";
        relationshipString = dn1.getNodeID() + " -> " + dn2.getNodeID() + " [\n"
                + "label = \"" + relationshipName + " \\l \" ] \n ";
        return relationshipString;

    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + this.thisIndex;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SESConfigurationRecommandationToGraphViz other = (SESConfigurationRecommandationToGraphViz) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
