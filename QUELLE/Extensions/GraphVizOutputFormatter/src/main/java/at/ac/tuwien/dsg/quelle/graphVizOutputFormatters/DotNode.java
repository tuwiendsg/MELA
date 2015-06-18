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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability.Dependency;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Unit;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class DotNode {

    private static int index = 0;

    static void clear() {
        createdSoFar.clear();
    }
    private String name;
    private int thisIndex;
    private Map<String, String> properties;
    private String metaInfo = "shape=record,";
    private static List<DotNode> createdSoFar;

    static {
        createdSoFar = new ArrayList<DotNode>();
    }

    {
        properties = new LinkedHashMap<String, String>();
        thisIndex = index++;
    }

    public DotNode(String name) {
        this.name = name;
    }

    private void addProperty(String key, String value) {
        properties.put(key, value);
    }

    private void removeProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getNodeID() {
        return "N" + thisIndex;
    }

    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
    }

    public String toDotString() {
        String dotString = "N" + thisIndex + " [  " + metaInfo + "\n"
                + "label = \"{ '" + name + "' | ";

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            dotString += " '" + entry.getKey() + "' = '" + entry.getValue() + "' \\l ";
        }
        dotString += "}\" \n ] \n";
        return dotString;
    }

    /**
     * Just returns the DOT representation of a particular Entity. It IGNORES
     * ALL RELATIONSHIPS
     *
     * @param entity
     * @return
     */
    public static DotNode toDotNode(Unit entity) {
        DotNode dotNode = new DotNode(entity.getClass().getSimpleName());

        createdSoFar.add(dotNode);

        dotNode.addProperty("name", entity.getName());
        if (entity instanceof CloudOfferedService) {
            CloudOfferedService serviceUnit = (CloudOfferedService) entity;
            dotNode.metaInfo = "shape=Mrecord,style=filled,fillcolor=\"#ACD1E9\",";
            dotNode.addProperty("category", serviceUnit.getCategory());
            dotNode.addProperty("subcategory", serviceUnit.getSubcategory());
        } else if (entity instanceof Resource) {
            Resource r = (Resource) entity;
            dotNode.metaInfo += "style=filled,fillcolor=\"#F5FAFA\",";
            for (Map.Entry<Metric, MetricValue> entry : r.getProperties().entrySet()) {
                Metric metric = entry.getKey();
                dotNode.addProperty(metric.getName() + ":" + metric.getMeasurementUnit(), entry.getValue().getValueRepresentation());
            }
        } else if (entity instanceof Quality) {

            Quality q = (Quality) entity;
            dotNode.metaInfo += "style=filled,fillcolor=\"#F5FAFA\",";
            for (Map.Entry<Metric, MetricValue> entry : q.getProperties().entrySet()) {
                Metric metric = entry.getKey();
                dotNode.addProperty(metric.getName() + ":" + metric.getMeasurementUnit(), entry.getValue().getValueRepresentation());
            }
        } else if (entity instanceof ElasticityCapability) {
            ElasticityCapability elasticityCapability = (ElasticityCapability) entity;
//            if (elasticityCapability.getType().equals(ElasticityCapability.Type.OPTIONAL_ASSOCIATION)) {
//                dotNode.setMetaInfo("style=filled,fillcolor=\"#C1DAD6\",");
//            } else {
//                dotNode.setMetaInfo("style=filled,fillcolor=\"#E77471\",");
//            }
//            dotNode.addProperty(ElasticityCapabilityDAO.TYPE, elasticityCapability.getType());
            dotNode.addProperty("phase", elasticityCapability.getPhase());

        } else if (entity instanceof CostFunction) {
            dotNode.metaInfo += "style=filled,fillcolor=\"#E8D0A9\",";
            CostFunction costFunction = (CostFunction) entity;
        } else if (entity instanceof CostElement) {
            dotNode.metaInfo += "style=filled,fillcolor=\"#E8D0A9\",";
            CostElement costElement = (CostElement) entity;
            dotNode.addProperty("type", costElement.getType());
            Metric metric = costElement.getCostMetric();
            dotNode.addProperty("metric", metric.getName() + ":" + metric.getMeasurementUnit());

            for (Map.Entry<MetricValue, Double> entry : costElement.getCostIntervalFunction().entrySet()) {
                dotNode.addProperty(entry.getKey().getValueRepresentation(), entry.getValue().toString());
            }
        }
        return dotNode;
    }

    /**
     *
     * @param unit
     * @return .dot Cluster containing the static Cost, Quality and Resource
     */
    public static String getCompleteConfigurationAsDOT(CloudOfferedService unit, DotNode serviceUnitNode) {
        String staticCfg = "subgraph cluster_FixCfg" + serviceUnitNode.getNodeID() + " {"
                + "label=\"Fixed Configuration\" \n";
        String relationships = "";

        if (unit.getResourceProperties().isEmpty() && unit.getQualityProperties().isEmpty() && unit.getCostFunctions().isEmpty()) {
            staticCfg = "";
        } else {

            for (Resource r : unit.getResourceProperties()) {
                DotNode rNode = DotNode.toDotNode(r);
                staticCfg += rNode.toDotString();
                relationships += DotNode.getDotRelationship(serviceUnitNode, rNode, "hasResource");
            }

            for (Quality q : unit.getQualityProperties()) {
                DotNode rNode = DotNode.toDotNode(q);
                staticCfg += rNode.toDotString();
                relationships += DotNode.getDotRelationship(serviceUnitNode, rNode, "hasQuality");
            }

            for (CostFunction costFunction : unit.getCostFunctions()) {
                DotNode rNode = DotNode.toDotNode(costFunction);
                staticCfg += rNode.toDotString();
                relationships += DotNode.getDotRelationship(serviceUnitNode, rNode, "hasCostFunction");

                for (CostElement ce : costFunction.getCostElements()) {
                    DotNode ceNode = DotNode.toDotNode(ce);
                    staticCfg += ceNode.toDotString();
                    relationships += DotNode.getDotRelationship(rNode, ceNode, "hasCostElement");
                }

                for (Unit ce : costFunction.getAppliedIfServiceInstanceUses()) {
                    //check if exists in the "createdSoFar", else create it
                    boolean exists = false;
                    DotNode ceNode = DotNode.toDotNode(ce);
                    for (DotNode node : createdSoFar) {
                        if (node.properties.equals(ceNode.properties)) {
                            ceNode = node;
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        createdSoFar.add(ceNode);
                    }

                    staticCfg += ceNode.toDotString();
                    staticCfg += DotNode.getDotRelationship(rNode, ceNode, "inConjunctionWith");
                }

            }

            staticCfg += "} \n";
        }

        //elasticity
        for (ElasticityCapability capability : unit.getElasticityCapabilities()) {
            DotNode ceNode = DotNode.toDotNode(capability);
            staticCfg += DotNode.getElasticityCapabilityAsDOT(capability, ceNode);
            //compute elasticityValue
//            int elasticity;
//            if (capability.getType().equals(ElasticityCapability.Type.OPTIONAL_ASSOCIATION)) {
//                elasticity = capability.getCapabilityTargets().size();
//            } else {
//                elasticity = -1 * capability.getCapabilityTargets().size();
//            }
//            relationships += DotNode.getDotRelationship(serviceUnitNode, ceNode, "hasElasticityCapability: " + elasticity);
        }

        return staticCfg + relationships;
    }

    public static String getElasticityConfigurationAsDOT(CloudOfferedService unit, DotNode serviceUnitNode) {
        String cfg = "";

        //elasticity
        for (ElasticityCapability capability : unit.getElasticityCapabilities()) {
//            int elasticity;
//            if (capability.getType().equals(ElasticityCapability.Type.OPTIONAL_ASSOCIATION)) {
//                elasticity = capability.getCapabilityTargets().size();
//            } else {
//                elasticity = -1 * capability.getCapabilityTargets().size();
//            }

            DotNode ceNode = DotNode.toDotNode(capability);
            cfg += DotNode.getElasticityCapabilityAsDOT(capability, ceNode);
//            cfg += DotNode.getDotRelationship(serviceUnitNode, ceNode, "hasElasticityCapability: " + elasticity);
        }

        return cfg;
    }

    public static String getElasticityCapabilityAsDOT(ElasticityCapability capability, DotNode elasticityCapabilityNode) {
        String cluster = "subgraph cluster_" + capability.getName() + elasticityCapabilityNode.getNodeID() + "{"
                + "label=\" ElasticityCapability: " + capability.getName() + "\" \n";

        cluster += elasticityCapabilityNode.toDotString();

        for (Dependency d : capability.getCapabilityDependencies()) {
            Unit target = d.getTarget();
            DotNode targetNode = DotNode.toDotNode(target);
            cluster += targetNode.toDotString();
            cluster += DotNode.getDotRelationship(elasticityCapabilityNode, targetNode, "elasticityCapabilityFor");

            if (target instanceof CloudOfferedService) {
                cluster += DotNode.getCompleteConfigurationAsDOT((CloudOfferedService) target, targetNode);
            }

            if (target instanceof CostFunction) {

                CostFunction costFunction = (CostFunction) target;

                for (CostElement ce : costFunction.getCostElements()) {
                    DotNode ceNode = DotNode.toDotNode(ce);
                    cluster += ceNode.toDotString();
                    cluster += DotNode.getDotRelationship(targetNode, ceNode, "hasCostElement");
                }

                for (Unit ce : costFunction.getAppliedIfServiceInstanceUses()) {
                    boolean exists = false;
                    DotNode ceNode = DotNode.toDotNode(ce);
                    for (DotNode node : createdSoFar) {
                        if (node.properties.equals(ceNode.properties)) {
                            ceNode = node;
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        createdSoFar.add(ceNode);
                    }
                    cluster += ceNode.toDotString();
                    cluster += DotNode.getDotRelationship(targetNode, ceNode, "inConjunctionWith");
                }

            }
        }

        cluster += "} \n";

        return cluster;
    }

    public static String getDotRelationship(DotNode dn1, DotNode dn2, String relationshipName) {
        String relationshipString = "";
        relationshipString = dn1.getNodeID() + " -> " + dn2.getNodeID() + " [\n"
                + "label = \"" + relationshipName + " \\l \" ] \n ";
        return relationshipString;

    }
}
