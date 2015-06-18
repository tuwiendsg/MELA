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
package at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos;

import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.helper.ServiceUnitRelationship;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import static at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CostElementDAO.UUID;
import static java.nio.channels.spi.SelectorProvider.provider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.transaction.Status;
import javax.transaction.SystemException;
import static org.neo4j.cypher.internal.commands.Pattern$class.node;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class CostElementDAO extends Neo4JDAO {

    static final Logger log = LoggerFactory.getLogger(CostElementDAO.class);

    public static final Label LABEL = new Label() {
        public String name() {
            return "CostElement";
        }
    };
    public static final String KEY = "name";
    // replace this shit with 
    // public static final String METRIC = "metric";
    public static final String COST_METRIC_NAME = "cost_metric_name";
    public static final String COST_METRIC_UNIT = "cost_metric_unit";
    public static final String COST_METRIC_TYPE = "cost_metric_type";

    public static final String TYPE = "type";
    public static final String UUID = "uuid";

    private CostElementDAO() {
    }

    /**
     * DOES NOT return also properties embedded on the quality relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static List<CostElement> searchForCostElementEntities(CostElement resourceToSearchFor, EmbeddedGraphDatabase database) {

        List<CostElement> costElements = new ArrayList<CostElement>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {

            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                CostElement costElement = new CostElement();
                costElement.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    costElement.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CostElement " + resourceToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(TYPE)) {
                    costElement.setType(node.getProperty(TYPE).toString());
                } else {
                    log.warn("Retrieved CostFunction " + costElement + " has no " + TYPE);
                }

                if (node.hasProperty(UUID)) {
                    costElement.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + costElement + " has no " + UUID);
                }

                if (node.hasProperty(COST_METRIC_NAME)) {
                    String costMetricName = node.getProperty(COST_METRIC_NAME).toString();
                    String costMetricUnit = node.getProperty(COST_METRIC_UNIT).toString();
                    String costMetricType = node.getProperty(COST_METRIC_TYPE).toString();

                    Metric metric = new Metric(costMetricName, costMetricUnit, Metric.MetricType.valueOf(costMetricType));
                    costElement.setCostMetric(metric);

                } else {
                    log.warn("Retrieved CostElement " + resourceToSearchFor + " has no " + COST_METRIC_NAME);
                }

//                //the format assumed for each property of a CostElement is "property key =" metricName : metricValue " (separated by :), 
//                //and the property value is the metric value
//                for (String propertyKey : node.getPropertyKeys()) {
//
//                    //skip the key property
//                    if (propertyKey.equals(KEY) || propertyKey.equals(METRIC)) {
//                        continue;
//                    }
//                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        MetricValue metricValue = new MetricValue(metricInfo[0]);
//                        Double cost = Double.parseDouble(metricInfo[1]);
//                        costElement.addCostInterval(metricValue, cost);
//                    }
//                }
                costElements.add(costElement);
            }

            if (!transactionAllreadyRunning) {
                tx.success();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

        return costElements;
    }

    /**
     * DOES NOT return also properties embedded on the quality relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static CostElement searchForCostElementEntitiesUniqueResult(CostElement resourceToSearchFor, EmbeddedGraphDatabase database) {
        CostElement resourceFound = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {

            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                CostElement costElement = new CostElement();
                costElement.setId(node.getId());

                if (node.hasProperty(KEY)) {
                    String name = node.getProperty(KEY).toString();
                    if (!name.equals(resourceToSearchFor.getName())) {
                        continue;
                    } else {
                        costElement.setName(name);
                    }
                } else {
                    log.warn("Retrieved Resource " + resourceToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(UUID)) {
                    costElement.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + costElement + " has no " + UUID);
                }

                if (node.hasProperty(TYPE)) {
                    costElement.setType(node.getProperty(TYPE).toString());
                } else {
                    log.warn("Retrieved CostFunction " + costElement + " has no " + TYPE);
                }

                if (node.hasProperty(COST_METRIC_NAME)) {
                    String costMetricName = node.getProperty(COST_METRIC_NAME).toString();
                    String costMetricUnit = node.getProperty(COST_METRIC_UNIT).toString();
                    String costMetricType = node.getProperty(COST_METRIC_TYPE).toString();

                    Metric metric = new Metric(costMetricName, costMetricUnit, Metric.MetricType.valueOf(costMetricType));
                    costElement.setCostMetric(metric);

                } else {
                    log.warn("Retrieved CostElement " + resourceToSearchFor + " has no " + COST_METRIC_NAME);
                }

//                //the format assumed for each property of a CostElement is "property key =" metricName : metricValue " (separated by :), 
//                //and the property value is the metric value
//                for (String propertyKey : node.getPropertyKeys()) {
//
//                    //skip the key property
//                    if (propertyKey.equals(KEY) || propertyKey.equals(METRIC)) {
//                        continue;
//                    }
//                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        MetricValue metricValue = new MetricValue(metricInfo[0]);
//                        Double cost = Double.parseDouble(metricInfo[1]);
//                        costElement.addCostInterval(metricValue, cost);
//                    }
//                }
                resourceFound = costElement;

                break;
            }
            if (!transactionAllreadyRunning) {
                tx.success();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

//        if (resourceFound == null) {
//            log.warn( "CostElement " + resourceToSearchFor + " was not found");
//        }
        return resourceFound;
    }

    /**
     * Only this method returns properties. Because the properties details for
     * each node are recorded as properties on HAS_COST_ELEMENT relationship,
     * they can be retrieved only for specific nodes as multiple nodes can have
     * same cost element (ex cost per IO) but with diff details (diff cost per
     * IO)
     *
     *
     */
    public static List<CostElement> getCostElementPropertiesForNode(Long nodeID, EmbeddedGraphDatabase database) {

        List<CostElement> costElements = new ArrayList<CostElement>();

        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            Node parentNode = database.getNodeById(nodeID);

            if (parentNode == null) {
                return costElements;
            }

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.hasCostElement, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node node = path.endNode();
                CostElement costElement = new CostElement();
                costElement.setId(node.getId());

                if (node.hasProperty(KEY)) {
                    costElement.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CostElement " + nodeID + " has no " + KEY);
                }

                if (node.hasProperty(TYPE)) {
                    costElement.setType(node.getProperty(TYPE).toString());
                } else {
                    log.warn("Retrieved CostFunction " + costElement + " has no " + TYPE);
                }

                if (node.hasProperty(UUID)) {
                    costElement.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + costElement + " has no " + UUID);
                }

                if (node.hasProperty(COST_METRIC_NAME)) {
                    String costMetricName = node.getProperty(COST_METRIC_NAME).toString();
                    String costMetricUnit = node.getProperty(COST_METRIC_UNIT).toString();
                    String costMetricType = node.getProperty(COST_METRIC_TYPE).toString();

                    Metric metric = new Metric(costMetricName, costMetricUnit, Metric.MetricType.valueOf(costMetricType));
                    costElement.setCostMetric(metric);

                } else {
                    log.warn("Retrieved CostElement " + nodeID + " has no " + COST_METRIC_NAME);
                }

                //get properties from the RELATIONSHIP
                Relationship relationship = path.lastRelationship();

                if (relationship != null) {
                    for (String propertyKey : relationship.getPropertyKeys()) {
                        //assume property is interval, so must be numeric
                        Object propertyKeyValue = propertyKey;
                        //not sure why writing positive infinity to string in neo4j reduces to this/
                        if (propertyKey.equals("∞")) {
                            propertyKeyValue = Double.POSITIVE_INFINITY;
                        } else if (propertyKey.matches("[0-9]*")) {
                            try {
                                propertyKeyValue = Integer.parseInt(propertyKey);
                            } catch (NumberFormatException e) {
                                try {
                                    propertyKeyValue = Long.parseLong(propertyKey);
                                } catch (NumberFormatException ex) {
                                    //maybe long if not int
                                }
                            }
                        } else if (propertyKey.matches("[0-9\\.]*")) {
                            try {
                                propertyKeyValue = Double.parseDouble(propertyKey);
                            } catch (NumberFormatException exception) {
                            }
                        }

                        MetricValue metricValue = new MetricValue(propertyKeyValue);
                        Double cost = (Double) relationship.getProperty(propertyKey);
                        costElement.addBillingInterval(metricValue, cost);
                    }
                } else {
                    log.warn("No relationship found of type " + ServiceUnitRelationship.hasResource + " starting from " + parentNode + " and ending at " + node);
                }

//                //the format assumed for each property of a CostElement is "property key =" metricName : metricValue " (separated by :), 
//                //and the property value is the metric value
//                for (String propertyKey : node.getPropertyKeys()) {
//
//                    //skip the key property
//                    if (propertyKey.equals(KEY) || propertyKey.equals(METRIC)) {
//                        continue;
//                    }
//                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        MetricValue metricValue = new MetricValue(node.getProperty(metricInfo[0]));
//                        Double cost = Double.parseDouble(metricInfo[1]);
//                        costElement.addCostInterval(metricValue, cost);
//                    }
//                }
                costElements.add(costElement);
            }
            if (!transactionAllreadyRunning) {
                tx.success();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

        return costElements;

    }

    public static CostElement getByID(Long id, EmbeddedGraphDatabase database) {
        CostElement resourceFound = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            Node node = database.getNodeById(id);
            if (node == null) {
                log.warn("CostElement ID " + id + " was not found");
                return null;
            }
            CostElement costElement = new CostElement();
            costElement.setId(node.getId());

            if (node.hasProperty(KEY)) {
                costElement.setName(node.getProperty(KEY).toString());
            } else {
                log.warn("Retrieved CostElement " + id + " has no " + KEY);
            }

            if (node.hasProperty(TYPE)) {
                costElement.setType(node.getProperty(TYPE).toString());
            } else {
                log.warn("Retrieved CostFunction " + costElement + " has no " + TYPE);
            }

            if (node.hasProperty(UUID)) {
                costElement.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
            } else {
                log.warn("Retrieved CloudProvider " + costElement + " has no " + UUID);
            }

            if (node.hasProperty(COST_METRIC_NAME)) {
                String costMetricName = node.getProperty(COST_METRIC_NAME).toString();
                String costMetricUnit = node.getProperty(COST_METRIC_UNIT).toString();
                String costMetricType = node.getProperty(COST_METRIC_TYPE).toString();

                Metric metric = new Metric(costMetricName, costMetricUnit, Metric.MetricType.valueOf(costMetricType));
                costElement.setCostMetric(metric);

            } else {
                log.warn("Retrieved CostElement " + node + " has no " + COST_METRIC_NAME);
            }

//            //the format assumed for each property of a CostElement is "property key =" metricName : metricValue " (separated by :), 
//            //and the property value is the metric value
//            for (String propertyKey : node.getPropertyKeys()) {
//
//                //skip the key property
//                if (propertyKey.equals(KEY) || propertyKey.equals(METRIC)) {
//                    continue;
//                }
//                String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                if (metricInfo.length < 2) {
//                    log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                } else {
//                    MetricValue metricValue = new MetricValue(node.getProperty(metricInfo[0]));
//                    Double cost = Double.parseDouble(metricInfo[1]);
//                    costElement.addCostInterval(metricValue, cost);
//                }
//            }
            resourceFound = costElement;

            if (!transactionAllreadyRunning) {
                tx.success();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

        return resourceFound;
    }

    /**
     * Actually persists only CostElement and Properties
     *
     * @param entityToPersist
     * @param database connection to DB
     */
    public static Node persistCostElementEntity(CostElement entityToPersist, EmbeddedGraphDatabase database) {

        Node resourceNode = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            resourceNode = database.createNode();
            resourceNode.setProperty(KEY, entityToPersist.getName());
            resourceNode.setProperty(COST_METRIC_NAME, entityToPersist.getCostMetric().getName());
            resourceNode.setProperty(COST_METRIC_TYPE, entityToPersist.getCostMetric().getType().toString());
            resourceNode.setProperty(COST_METRIC_UNIT, entityToPersist.getCostMetric().getMeasurementUnit());
            resourceNode.setProperty(TYPE, entityToPersist.getType());
            resourceNode.setProperty(UUID, entityToPersist.getUuid().toString());
            resourceNode.addLabel(LABEL);

//            for (Map.Entry<MetricValue, Double> entry : entityToPersist.getCostIntervalFunction().entrySet()) {
//                MetricValue metricValue = entry.getKey();
//                String propertyKey = metricValue.getValueRepresentation();
//                resourceNode.setProperty(propertyKey, entry.getValue());
//            }
            if (!transactionAllreadyRunning) {
                tx.success();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

        return resourceNode;
    }

    /**
     * Actually persists only CostElement and Properties
     *
     * @param entityToPersist
     * @param database connection to DB
     */
    public static void persistCostElementEntities(List<CostElement> resourcesToPersist, EmbeddedGraphDatabase database) {
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (CostElement entityToPersist : resourcesToPersist) {
                Node resourceNode = database.createNode();
                resourceNode.setProperty(KEY, entityToPersist.getName());
                resourceNode.setProperty(COST_METRIC_NAME, entityToPersist.getCostMetric().getName());
                resourceNode.setProperty(COST_METRIC_TYPE, entityToPersist.getCostMetric().getType().toString());
                resourceNode.setProperty(COST_METRIC_UNIT, entityToPersist.getCostMetric().getMeasurementUnit());
                resourceNode.setProperty(TYPE, entityToPersist.getType());
                resourceNode.setProperty(UUID, entityToPersist.getUuid().toString());
                resourceNode.addLabel(LABEL);
//
//                for (Map.Entry<MetricValue, Double> entry : entityToPersist.getCostIntervalFunction().entrySet()) {
//                    MetricValue metricValue = entry.getKey();
//                    String propertyKey = metricValue.getValueRepresentation();
//                    resourceNode.setProperty(propertyKey, entry.getValue());
//                }
            }

            if (!transactionAllreadyRunning) {
                tx.success();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }
    }
}
