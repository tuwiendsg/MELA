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
import static at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.ElasticityCapabilityDAO.VOLATILITY_MAX_CHANGES;
import static at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.ElasticityCapabilityDAO.VOLATILITY_TIME_UNIT;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Unit;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Volatility;
import static at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CostElementDAO.log;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.transaction.Status;
import javax.transaction.SystemException;
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
public class CostFunctionDAO extends Neo4JDAO {

    static final Logger log = LoggerFactory.getLogger(CostFunctionDAO.class);

    public static final Label LABEL = new Label() {
        public String name() {
            return "CostFunction";
        }
    };
    public static final String KEY = "name";
    //separates metricName from metricUnit in property name
//    public static final String PROPERTY_SEPARATOR = ":";

    public static final String COST_METRIC_NAME = "cost_metric_name";
    public static final String COST_METRIC_UNIT = "cost_metric_unit";
    public static final String COST_METRIC_TYPE = "cost_metric_type";
    public static final String COST_METRIC_VALUE = "cost_metric_value";

    public static final String UUID = "uuid";

    static List<ElasticityCapability.Dependency> getElasticityCapabilitiesTargetsCostFunctionNode(long nodeID, EmbeddedGraphDatabase database) {
        List<ElasticityCapability.Dependency> elTargets = new ArrayList<ElasticityCapability.Dependency>();
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
                return elTargets;
            }

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.elasticityCapabilityFor, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node node = path.endNode();

                if (!node.hasLabel(LABEL)) {
                    continue;
                }

                CostFunction costFunction = new CostFunction();
                costFunction.setId(node.getId());

                if (node.hasProperty(KEY)) {
                    costFunction.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CostFunction " + node + " has no " + KEY);
                }

                if (node.hasProperty(UUID)) {
                    costFunction.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + costFunction + " has no " + UUID);
                }

                //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
                costFunction.getAppliedIfServiceInstanceUses().addAll(getAppliedInConjunctionWithEntities(node.getId(), database));
                costFunction.getCostElements().addAll(CostElementDAO.getCostElementPropertiesForNode(node.getId(), database));

                Relationship relationship = path.lastRelationship();

                if (relationship != null) {
                    String type = relationship.getProperty(ElasticityCapabilityDAO.TYPE).toString();
                    ElasticityCapability.Dependency dependency = new ElasticityCapability.Dependency(costFunction, type);
                    elTargets.add(dependency);

                    Volatility volatility = new Volatility();
                    if (relationship.hasProperty(VOLATILITY_TIME_UNIT)) {
                        String unit = relationship.getProperty(VOLATILITY_TIME_UNIT).toString();
                        volatility.setMinimumLifetimeInHours(Integer.parseInt(unit));
                    } else {
                        log.warn("Retrieved ElasticityCapability " + node + " has no " + VOLATILITY_TIME_UNIT);
                    }

                    if (relationship.hasProperty(VOLATILITY_MAX_CHANGES)) {
                        String unit = relationship.getProperty(VOLATILITY_MAX_CHANGES).toString();
                        volatility.setMaxNrOfChanges(Double.parseDouble(unit));
                    } else {
                        log.warn("Retrieved ElasticityCapability " + node + " has no " + VOLATILITY_TIME_UNIT);
                    }

                    dependency.setVolatility(volatility);
                } else {
                    log.warn("No relationship found starting from " + parentNode.getProperty(KEY).toString() + " and ending at " + node.getProperty(KEY).toString());
                    new Exception().printStackTrace();
                }

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
        return elTargets;
    }

    private CostFunctionDAO() {
    }

    /**
     * for a service unit ID, it gets all relationships to a Quality node, and
     * based on the different relationships, instantiates the options.
     *
     * @param serviceUnitNodeID
     * @param costFunctionNodeID
     * @param database
     * @return
     */
    public static List<CostFunction> getCostFunctionOptionsForServiceUnitNode(Long serviceUnitNodeID, Long costFunctionNodeID, EmbeddedGraphDatabase database) {

        List<CostFunction> costFunctions = new ArrayList<CostFunction>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            Node costFunctionNode = database.getNodeById(costFunctionNodeID);
            Node serviceUnitNode = database.getNodeById(serviceUnitNodeID);

            if (costFunctionNode == null) {
                return costFunctions;
            }

            for (Relationship relationship : serviceUnitNode.getRelationships(ServiceUnitRelationship.hasQuality, Direction.OUTGOING)) {
                //if relationship from ServiceUnit to Resource
                if (!relationship.getEndNode().equals(costFunctionNode)) {
                    continue;
                }
                //the resource is created based on the resourceNode and the relationship properties
                CostFunction costFunction = CostFunctionDAO.getCostFunction(costFunctionNode.getId(), database);
                if (costFunction != null) {
                    costFunctions.add(costFunction);
                }
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

        return costFunctions;

    }

    /**
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static List<CostFunction> searchForCostFunctions(CostFunction resourceToSearchFor, EmbeddedGraphDatabase database) {

        List<CostFunction> costFunctions = new ArrayList<CostFunction>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                CostFunction costFunction = new CostFunction();
                costFunction.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    costFunction.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CostFunction " + resourceToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(UUID)) {
                    costFunction.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + costFunction + " has no " + UUID);
                }

                //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
                costFunction.getAppliedIfServiceInstanceUses().addAll(getAppliedInConjunctionWithEntities(node.getId(), database));
                costFunction.getCostElements().addAll(CostElementDAO.getCostElementPropertiesForNode(node.getId(), database));
                costFunctions.add(costFunction);
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
        return costFunctions;
    }

    /**
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static CostFunction searchForCostFunctionsUniqueResult(CostFunction resourceToSearchFor, EmbeddedGraphDatabase database) {
        CostFunction resourceFound = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                CostFunction costFunction = new CostFunction();
                costFunction.setId(node.getId());

                if (node.hasProperty("name")) {
                    String name = node.getProperty("name").toString();
                    if (!name.equals(resourceToSearchFor.getName())) {
                        continue;
                    }
                } else {
                    log.warn("Retrieved CostFunction " + resourceToSearchFor + " has no name");
                }

                if (node.hasProperty(UUID)) {
                    costFunction.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + costFunction + " has no " + UUID);
                }

                if (node.hasProperty(KEY)) {
                    costFunction.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CostFunction " + resourceToSearchFor + " has no " + KEY);
                }

                //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
                costFunction.getAppliedIfServiceInstanceUses().addAll(getAppliedInConjunctionWithEntities(node.getId(), database));
                costFunction.getCostElements().addAll(CostElementDAO.getCostElementPropertiesForNode(node.getId(), database));
                resourceFound = costFunction;

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
//            log.warn( "CostFunction " + resourceToSearchFor + " was not found");
//        }
        return resourceFound;
    }

    public static CostFunction getCostFunction(Long id, EmbeddedGraphDatabase database) {

        Node node = database.getNodeById(id);
        CostFunction costFunction = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            if (node == null) {
                return new CostFunction();
            }

            costFunction = new CostFunction();
            costFunction.setId(node.getId());

            if (node.hasProperty(KEY)) {
                costFunction.setName(node.getProperty(KEY).toString());
            } else {
                log.warn("Retrieved CostFunction " + node + " has no " + KEY);
            }

            if (node.hasProperty(UUID)) {
                costFunction.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
            } else {
                log.warn("Retrieved CloudProvider " + costFunction + " has no " + UUID);
            }

            //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
            costFunction.getAppliedIfServiceInstanceUses().addAll(getAppliedInConjunctionWithEntities(id, database));
            costFunction.getCostElements().addAll(CostElementDAO.getCostElementPropertiesForNode(id, database));
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
        return costFunction;
    }

    public static List<Unit> getAppliedInConjunctionWithEntities(Long nodeID, EmbeddedGraphDatabase database) {

        List<Unit> entities = new ArrayList<Unit>();

        Node parentNode = null;

        try {
            parentNode = database.getNodeById(nodeID);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return entities;
        }

        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.IN_CONJUNCTION_WITH, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node lastPathNode = path.endNode();

                if (lastPathNode.hasLabel(ServiceUnitDAO.LABEL)) {
                    entities.add(ServiceUnitDAO.getByID(lastPathNode.getId(), database));

                } else if (lastPathNode.hasLabel(ResourceDAO.LABEL)) {

                    Resource resource = ResourceDAO.getByID(lastPathNode.getId(), database);

                    //extract properties from relationships
                    Relationship relationship = path.lastRelationship();
                    String costMetricName = relationship.getProperty(COST_METRIC_NAME).toString();
                    String costMetricUnit = relationship.getProperty(COST_METRIC_UNIT).toString();
                    String costMetricType = relationship.getProperty(COST_METRIC_TYPE).toString();
                    String costMetricValue = relationship.getProperty(COST_METRIC_VALUE).toString();

                    Metric metric = new Metric(costMetricName, costMetricUnit, Metric.MetricType.valueOf(costMetricType));
                    MetricValue metricValue = new MetricValue(costMetricValue);
                    resource.addProperty(metric, metricValue);

                    entities.add(resource);
                } else if (lastPathNode.hasLabel(QualityDAO.LABEL)) {
                    Quality quality = QualityDAO.getByID(lastPathNode.getId(), database);

                    //extratc proeprties from relationships
                    Relationship relationship = path.lastRelationship();
                    String costMetricName = relationship.getProperty(COST_METRIC_NAME).toString();
                    String costMetricUnit = relationship.getProperty(COST_METRIC_UNIT).toString();
                    String costMetricType = relationship.getProperty(COST_METRIC_TYPE).toString();
                    String costMetricValue = relationship.getProperty(COST_METRIC_VALUE).toString();

                    Metric metric = new Metric(costMetricName, costMetricUnit, Metric.MetricType.valueOf(costMetricType));
                    MetricValue metricValue = new MetricValue(costMetricValue);
                    quality.addProperty(metric, metricValue);
                    entities.add(quality);
                }
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

        return entities;

    }

    public static List<CostFunction> getCostFunctionsForNode(Long id, EmbeddedGraphDatabase database) {

        List<CostFunction> costFunctions = new ArrayList<CostFunction>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {

            Node parentNode = database.getNodeById(id);

            if (parentNode == null) {
                return costFunctions;
            }

            //search from this node with ID=id the target nodes for which it has a HAS_COST_FUNCTION relationship
            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.hasCostFunction, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node node = path.endNode();
                CostFunction costFunction = new CostFunction();
                costFunction.setId(node.getId());

                if (node.hasProperty(KEY)) {
                    costFunction.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CostFunction " + node + " has no " + KEY);
                }

                if (node.hasProperty(UUID)) {
                    costFunction.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + costFunction + " has no " + UUID);
                }

                //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
                costFunction.getAppliedIfServiceInstanceUses().addAll(getAppliedInConjunctionWithEntities(node.getId(), database));
                //need to also retrieve Resurce and Quality

                costFunction.getCostElements().addAll(CostElementDAO.getCostElementPropertiesForNode(node.getId(), database));

                if (costFunction != null) {
                    //hack. if the costFunction has allready been added (equals is done on the DB Node),
                    //this means ServiceUnit has elasticity capability on it, and the old is also removed
                    if (costFunctions.contains(costFunction)) {
                        costFunctions.remove(costFunction);
                    } else {
                        costFunctions.add(costFunction);
                    }
                }
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

        return costFunctions;
    }

    /**
     * Actually persists only CostFunction and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static Node persistCostFunction(CostFunction resourceToPersist, EmbeddedGraphDatabase database) {

        Node costFunctionNode = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            costFunctionNode = database.createNode();
            costFunctionNode.setProperty(KEY, resourceToPersist.getName());
            costFunctionNode.setProperty(UUID, resourceToPersist.getUuid().toString());
            costFunctionNode.addLabel(LABEL);

            //persist Cost Elements
            for (CostElement costElement : resourceToPersist.getCostElements()) {
                CostElement costElementFound = CostElementDAO.searchForCostElementEntitiesUniqueResult(costElement, database);
                //costFunction does not exist need to persist it
                Node costElementNode = null;
                if (costElementFound == null) {
                    costElementNode = CostElementDAO.persistCostElementEntity(costElement, database);
                } else {
                    //retrieve the costFunction to have its ID
                    //add relationship from CostElement to CostFunction
                    costElementNode = database.getNodeById(costElementFound.getId());
                }

                Relationship relationship = costFunctionNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.hasCostElement);
                /**
                 * add all properties on the HAS_COST_ELEMENT relationship (thus
                 * we can have same cost element (ex cost per IO), but diff
                 * properties on the relationship (ex diff cost value and
                 * interval)
                 */
                for (Map.Entry<MetricValue, Double> entry : costElement.getCostIntervalFunction().entrySet()) {
                    MetricValue metricValue = entry.getKey();
                    String propertyKey = metricValue.getValueRepresentation();
                    relationship.setProperty(propertyKey, entry.getValue());
                }

            }

            //persist ServiceUnits for which cost is applied in conjunction with
            for (Unit entity : resourceToPersist.getAppliedIfServiceInstanceUses()) {
                Node appliedInConjunctionWithNode = null;
                if (entity instanceof CloudOfferedService) {
                    CloudOfferedService serviceUnit = (CloudOfferedService) entity;
                    CloudOfferedService appliedInConjunctionWith = ServiceUnitDAO.searchForCloudServiceUnitsUniqueResult(serviceUnit, database);
                    //costFunction does not exist need to persist it
                    if (appliedInConjunctionWith == null) {
                        appliedInConjunctionWithNode = ServiceUnitDAO.persistServiceUnit(serviceUnit, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        appliedInConjunctionWithNode = database.getNodeById(appliedInConjunctionWith.getId());
                    }

                    Relationship relationship = costFunctionNode.createRelationshipTo(appliedInConjunctionWithNode, ServiceUnitRelationship.IN_CONJUNCTION_WITH);

                } else if (entity instanceof Resource) {
                    Resource resource = (Resource) entity;
                    Resource appliedInConjunctionWith = ResourceDAO.searchForResourcesUniqueResult(resource, database);
                    //costFunction does not exist need to persist it
                    if (appliedInConjunctionWith == null) {
                        appliedInConjunctionWithNode = ResourceDAO.persistResource(resource, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        appliedInConjunctionWithNode = database.getNodeById(appliedInConjunctionWith.getId());
                    }
                    Relationship relationship = costFunctionNode.createRelationshipTo(appliedInConjunctionWithNode, ServiceUnitRelationship.IN_CONJUNCTION_WITH);
                    //here I need to insert on the relationship the unit's properties
                    for (Map.Entry<Metric, MetricValue> entry : resource.getProperties().entrySet()) {
                        Metric metric = entry.getKey();
                        relationship.setProperty(COST_METRIC_NAME, metric.getName());
                        relationship.setProperty(COST_METRIC_UNIT, metric.getMeasurementUnit());
                        relationship.setProperty(COST_METRIC_TYPE, metric.getType().toString());
                        relationship.setProperty(COST_METRIC_VALUE, entry.getValue().toString());

                    }
                } else if (entity instanceof Quality) {
                    Quality quality = (Quality) entity;
                    Quality appliedInConjunctionWith = QualityDAO.searchForQualityEntitiesUniqueResult(quality, database);
                    //costFunction does not exist need to persist it
                    if (appliedInConjunctionWith == null) {
                        appliedInConjunctionWithNode = QualityDAO.persistQualityEntity(quality, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        appliedInConjunctionWithNode = database.getNodeById(appliedInConjunctionWith.getId());
                    }
                    Relationship relationship = costFunctionNode.createRelationshipTo(appliedInConjunctionWithNode, ServiceUnitRelationship.IN_CONJUNCTION_WITH);
                    //here I need to insert on the relationship the unit's properties
                    for (Map.Entry<Metric, MetricValue> entry : quality.getProperties().entrySet()) {
                        Metric metric = entry.getKey();
                        relationship.setProperty(COST_METRIC_NAME, metric.getName());
                        relationship.setProperty(COST_METRIC_UNIT, metric.getMeasurementUnit());
                        relationship.setProperty(COST_METRIC_TYPE, metric.getType().toString());
                        relationship.setProperty(COST_METRIC_VALUE, entry.getValue().toString());
                    }
                }

            }
            if (!transactionAllreadyRunning) {
                tx.success();
            }

        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

        return costFunctionNode;

    }

    /**
     * Actually persists only CostFunction and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static void persistCostFunctions(List<CostFunction> resourcesToPersist, EmbeddedGraphDatabase database) {
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (CostFunction resourceToPersist : resourcesToPersist) {
                Node costFunctionNode = null;

                costFunctionNode = database.createNode();
                costFunctionNode.setProperty(KEY, resourceToPersist.getName());
                costFunctionNode.addLabel(LABEL);
                costFunctionNode.setProperty(UUID, resourceToPersist.getUuid().toString());

                //persist Cost Elements
                for (CostElement costElement : resourceToPersist.getCostElements()) {
                    CostElement costElementFound = CostElementDAO.searchForCostElementEntitiesUniqueResult(costElement, database);
                    //costFunction does not exist need to persist it
                    Node costElementNode = null;
                    if (costElementFound == null) {
                        costElementNode = CostElementDAO.persistCostElementEntity(costElement, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CostFunction
                        costElementNode = database.getNodeById(costElementFound.getId());
                    }

                    Relationship relationship = costFunctionNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.hasCostElement);
                    /**
                     * add all properties on the HAS_COST_ELEMENT relationship
                     * (thus we can have same cost element (ex cost per IO), but
                     * diff properties on the relationship (ex diff cost value
                     * and interval)
                     */
                    for (Map.Entry<MetricValue, Double> entry : costElement.getCostIntervalFunction().entrySet()) {
                        MetricValue metricValue = entry.getKey();
                        String propertyKey = metricValue.getValueRepresentation();
                        relationship.setProperty(propertyKey, entry.getValue());
                    }

                }
                //persist ServiceUnits for which cost is applied in conjunction with
                for (Unit entity : resourceToPersist.getAppliedIfServiceInstanceUses()) {
                    Node appliedInConjunctionWithNode = null;
                    if (entity instanceof CloudOfferedService) {
                        CloudOfferedService serviceUnit = (CloudOfferedService) entity;
                        CloudOfferedService appliedInConjunctionWith = ServiceUnitDAO.searchForCloudServiceUnitsUniqueResult(serviceUnit, database);
                        //costFunction does not exist need to persist it
                        if (appliedInConjunctionWith == null) {
                            appliedInConjunctionWithNode = ServiceUnitDAO.persistServiceUnit(serviceUnit, database);
                        } else {
                            //retrieve the costFunction to have its ID
                            //add relationship from CostElement to CloudProvider
                            appliedInConjunctionWithNode = database.getNodeById(appliedInConjunctionWith.getId());
                        }

                        Relationship relationship = costFunctionNode.createRelationshipTo(appliedInConjunctionWithNode, ServiceUnitRelationship.IN_CONJUNCTION_WITH);

                    } else if (entity instanceof Resource) {
                        Resource resource = (Resource) entity;
                        Resource appliedInConjunctionWith = ResourceDAO.searchForResourcesUniqueResult(resource, database);
                        //costFunction does not exist need to persist it
                        if (appliedInConjunctionWith == null) {
                            appliedInConjunctionWithNode = ResourceDAO.persistResource(resource, database);
                        } else {
                            //retrieve the costFunction to have its ID
                            //add relationship from CostElement to CloudProvider
                            appliedInConjunctionWithNode = database.getNodeById(appliedInConjunctionWith.getId());
                        }
                        Relationship relationship = costFunctionNode.createRelationshipTo(appliedInConjunctionWithNode, ServiceUnitRelationship.IN_CONJUNCTION_WITH);
                        //here I need to insert on the relationship the unit's properties
                        for (Map.Entry<Metric, MetricValue> entry : resource.getProperties().entrySet()) {
                            Metric metric = entry.getKey();
                            relationship.setProperty(COST_METRIC_NAME, metric.getName());
                            relationship.setProperty(COST_METRIC_UNIT, metric.getMeasurementUnit());
                            relationship.setProperty(COST_METRIC_TYPE, metric.getType().toString());
                            relationship.setProperty(COST_METRIC_VALUE, entry.getValue().toString());
                        }
                    } else if (entity instanceof Quality) {
                        Quality quality = (Quality) entity;
                        Quality appliedInConjunctionWith = QualityDAO.searchForQualityEntitiesUniqueResult(quality, database);
                        //costFunction does not exist need to persist it
                        if (appliedInConjunctionWith == null) {
                            appliedInConjunctionWithNode = QualityDAO.persistQualityEntity(quality, database);
                        } else {
                            //retrieve the costFunction to have its ID
                            //add relationship from CostElement to CloudProvider
                            appliedInConjunctionWithNode = database.getNodeById(appliedInConjunctionWith.getId());
                        }
                        Relationship relationship = costFunctionNode.createRelationshipTo(appliedInConjunctionWithNode, ServiceUnitRelationship.IN_CONJUNCTION_WITH);
                        //here I need to insert on the relationship the unit's properties
                        for (Map.Entry<Metric, MetricValue> entry : quality.getProperties().entrySet()) {
                            Metric metric = entry.getKey();
                            relationship.setProperty(COST_METRIC_NAME, metric.getName());
                            relationship.setProperty(COST_METRIC_UNIT, metric.getMeasurementUnit());
                            relationship.setProperty(COST_METRIC_TYPE, metric.getType().toString());
                            relationship.setProperty(COST_METRIC_VALUE, entry.getValue().toString());
                        }
                    }

                }
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
