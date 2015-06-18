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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Volatility;
import static at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.ResourceDAO.log;
import static at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.ServiceUnitDAO.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.transaction.Status;
import javax.transaction.SystemException;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
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
public class ServiceUnitDAO extends Neo4JDAO {

    static final Logger log = LoggerFactory.getLogger(ServiceUnitDAO.class);

    public static final Label LABEL = new Label() {
        public String name() {
            return "ServiceUnit";
        }
    };
    public static final String KEY = "name";
    public static final String CATEGORY = "category";
    public static final String SUBCATEGORY = "subcategory";
    public static final String ELASTICITY_CHARACTERISTIC_VALUES = "values";
    //separates metricName from metricUnit in property name
    public static final String PROPERTY_SEPARATOR = ":";
    public static final String UUID = "uuid";

    /**
     * Used by ElasticityCapabilityDAO to get the service unit targeted by an
     * ElasticityCapability
     *
     * @param id
     * @param database
     * @return
     */
    public static List<ElasticityCapability.Dependency> getElasticityCapabilitiesTargetServiceUnitForNode(long elasticityCapabilityID, EmbeddedGraphDatabase database) {

        List<ElasticityCapability.Dependency> elTargets = new ArrayList<ElasticityCapability.Dependency>();

        Node parentNode = database.getNodeById(elasticityCapabilityID);

        if (parentNode == null) {
            return elTargets;
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
                    .evaluator(Evaluators.includeIfAcceptedByAny(new Evaluator() {
                        public Evaluation evaluate(Path path) {
                            if (path.endNode().hasLabel(LABEL)) {
                                return Evaluation.INCLUDE_AND_CONTINUE;
                            } else {
                                return Evaluation.EXCLUDE_AND_CONTINUE;
                            }
                        }
                    }))
                    .relationships(ServiceUnitRelationship.elasticityCapabilityFor, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node node = path.endNode();

                CloudOfferedService serviceUnit = new CloudOfferedService();
                serviceUnit.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    serviceUnit.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + node + " has no " + KEY);
                }

                if (node.hasProperty(CATEGORY)) {
                    serviceUnit.setCategory(node.getProperty(CATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + node + " has no " + CATEGORY);
                }

                if (node.hasProperty(SUBCATEGORY)) {
                    serviceUnit.setSubcategory(node.getProperty(SUBCATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + node + " has no " + SUBCATEGORY);
                }

                if (node.hasProperty(UUID)) {
                    serviceUnit.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + serviceUnit + " has no " + UUID);
                }

                //If this happened because you are updating something, disregard this addAll(serviceUnitDAO.getMandatoryAssociations(node.getId(), database));
                ////serviceUnit.getOptionalAssociations().addAll(serviceUnitDAO.getOptionalAssociations(node.getId(), database));
                serviceUnit.getResourceProperties().addAll(ResourceDAO.getResourcePropertiesForNode(node.getId(), database));
                serviceUnit.getQualityProperties().addAll(QualityDAO.getQualityPropertiesForNode(node.getId(), database));
                serviceUnit.getCostFunctions().addAll(CostFunctionDAO.getCostFunctionsForNode(node.getId(), database));
                serviceUnit.getElasticityCapabilities().addAll(ElasticityCapabilityDAO.getELasticityCapabilitiesForNode(node.getId(), database));
//            //serviceUnit.setElasticityQuantification(getElasticityDependency(node.getId(), database));
                //if we have reached this place, then we have found return the quality and can return it

                Relationship relationship = path.lastRelationship();

                if (relationship != null) {
                    String type = relationship.getProperty(ElasticityCapabilityDAO.TYPE).toString();
                    ElasticityCapability.Dependency dependency = new ElasticityCapability.Dependency(serviceUnit, type);
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
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
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

    /**
     * Counts how many elasticity characteristic nodes point to it with a
     * "elasticityCapabilityFor" relationship
     *
     * @param id
     * @param database
     * @return sum of incoming MANDATORY_ASSOCIATION and OPTIONAL_ASSOCIATION
     * elasticity capabilities if returns -1, means error encountered. otherwise
     * the result is always >= 0
     */
    public static int getElasticityDependency(long id, EmbeddedGraphDatabase database) {

        CloudOfferedService elTarget = null;
        int incomingPaths = 0;
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
                log.error("Node with id " + id + " was not found");
                return 0;
            }

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.hasElasticityCapability, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);

            //for each incoming path, if is MANDATORY_ASSOCIATION decrease the in
            for (Path path : traverser) {
                incomingPaths++;
            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }
        return incomingPaths;
    }

    private ServiceUnitDAO() {
    }

    /**
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static List<CloudOfferedService> searchForCloudServiceUnits(CloudOfferedService serviceUnitToSearchFor, EmbeddedGraphDatabase database) {

        List<CloudOfferedService> serviceUnits = new ArrayList<CloudOfferedService>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            ResourceIterable<Node> nodes = database.findNodesByLabelAndProperty(LABEL, KEY, serviceUnitToSearchFor.getName());

            for (Node node : nodes) {

                CloudOfferedService serviceUnit = new CloudOfferedService();
                serviceUnit.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    serviceUnit.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + serviceUnitToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(CATEGORY)) {
                    serviceUnit.setCategory(node.getProperty(CATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + serviceUnitToSearchFor + " has no " + CATEGORY);
                }

                if (node.hasProperty(SUBCATEGORY)) {
                    serviceUnit.setSubcategory(node.getProperty(SUBCATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + serviceUnitToSearchFor + " has no " + SUBCATEGORY);
                }

                if (node.hasProperty(UUID)) {
                    serviceUnit.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + serviceUnit + " has no " + UUID);
                }

                //If this happened because you are updating something, disregard this e-mail..addAll(serviceUnitDAO.getMandatoryAssociations(node.getId(), database));
                //serviceUnit.getOptionalAssociations().addAll(serviceUnitDAO.getOptionalAssociations(node.getId(), database));
                serviceUnits.add(serviceUnit);
            }

            //extract other referenced members
            for (CloudOfferedService serviceUnit : serviceUnits) {
                serviceUnit.getResourceProperties().addAll(ResourceDAO.getResourcePropertiesForNode(serviceUnit.getId(), database));
                serviceUnit.getQualityProperties().addAll(QualityDAO.getQualityPropertiesForNode(serviceUnit.getId(), database));
                serviceUnit.getCostFunctions().addAll(CostFunctionDAO.getCostFunctionsForNode(serviceUnit.getId(), database));
                serviceUnit.getElasticityCapabilities().addAll(ElasticityCapabilityDAO.getELasticityCapabilitiesForNode(serviceUnit.getId(), database));
                //serviceUnit.setElasticityQuantification(getElasticityDependency(serviceUnit.getId(), database));
            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }
        return serviceUnits;
    }

    /**
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static CloudOfferedService searchForCloudServiceUnitsUniqueResult(CloudOfferedService serviceUnitToSearchFor, EmbeddedGraphDatabase database) {

        CloudOfferedService serviceUnitFound = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, serviceUnitToSearchFor.getName())) {
//                ServiceUnit resource = new ServiceUnit();
//                resource.setId(node.getId());

                if (node.hasProperty("name")) {
                    String name = node.getProperty("name").toString();
                    if (!name.equals(serviceUnitToSearchFor.getName())) {
                        continue;
                    }
                } else {
                    log.warn("Retrieved serviceUnit " + serviceUnitToSearchFor + " has no name");
                }

                CloudOfferedService serviceUnit = new CloudOfferedService();
                serviceUnit.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    serviceUnit.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + serviceUnitToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(CATEGORY)) {
                    serviceUnit.setCategory(node.getProperty(CATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + serviceUnitToSearchFor + " has no " + CATEGORY);
                }

                if (node.hasProperty(SUBCATEGORY)) {
                    serviceUnit.setSubcategory(node.getProperty(SUBCATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + serviceUnitToSearchFor + " has no " + SUBCATEGORY);
                }

                if (node.hasProperty(UUID)) {
                    serviceUnit.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + serviceUnit + " has no " + UUID);
                }

                //If this happened because you are updating something, disregard this e-mail..addAll(serviceUnitDAO.getMandatoryAssociations(node.getId(), database));
                //serviceUnit.getOptionalAssociations().addAll(serviceUnitDAO.getOptionalAssociations(node.getId(), database));
                serviceUnit.getResourceProperties().addAll(ResourceDAO.getResourcePropertiesForNode(node.getId(), database));
                serviceUnit.getQualityProperties().addAll(QualityDAO.getQualityPropertiesForNode(node.getId(), database));
                serviceUnit.getCostFunctions().addAll(CostFunctionDAO.getCostFunctionsForNode(node.getId(), database));
                serviceUnit.getElasticityCapabilities().addAll(ElasticityCapabilityDAO.getELasticityCapabilitiesForNode(node.getId(), database));
                //serviceUnit.setElasticityQuantification(getElasticityDependency(node.getId(), database));
                serviceUnitFound = serviceUnit;

                break;
            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

//        if (serviceUnitFound == null) {
//            log.warn( "serviceUnit " + serviceUnitToSearchFor + " was not found");
//        }
        return serviceUnitFound;
    }

    public static CloudOfferedService getByID(Long nodeID, EmbeddedGraphDatabase database) {

        CloudOfferedService serviceUnit = null;
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
                return serviceUnit;
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
                serviceUnit = new CloudOfferedService();
                serviceUnit.setId(node.getId());

                if (node.hasProperty(KEY)) {
                    serviceUnit.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + nodeID + " has no " + KEY);
                }

                if (node.hasProperty(CATEGORY)) {
                    serviceUnit.setCategory(node.getProperty(CATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + nodeID + " has no " + CATEGORY);
                }

                if (node.hasProperty(SUBCATEGORY)) {
                    serviceUnit.setSubcategory(node.getProperty(SUBCATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + nodeID + " has no " + SUBCATEGORY);
                }
                if (node.hasProperty(UUID)) {
                    serviceUnit.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + serviceUnit + " has no " + UUID);
                }

                serviceUnit.getResourceProperties().addAll(ResourceDAO.getResourcePropertiesForNode(node.getId(), database));
                serviceUnit.getQualityProperties().addAll(QualityDAO.getQualityPropertiesForNode(node.getId(), database));
                serviceUnit.getCostFunctions().addAll(CostFunctionDAO.getCostFunctionsForNode(node.getId(), database));
                serviceUnit.getElasticityCapabilities().addAll(ElasticityCapabilityDAO.getELasticityCapabilitiesForNode(node.getId(), database));
                //serviceUnit.setElasticityQuantification(getElasticityDependency(node.getId(), database));

            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

        return serviceUnit;
    }

    public static List<CloudOfferedService> getCloudServiceUnitsForCloudProviderNode(Long nodeID, EmbeddedGraphDatabase database) {

        List<CloudOfferedService> serviceUnits = new ArrayList<CloudOfferedService>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            Node parentNode = null;

            try {
                parentNode = database.getNodeById(nodeID);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            if (parentNode == null) {
                return serviceUnits;
            }

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.providesServiceUnit, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node node = path.endNode();
                CloudOfferedService serviceUnit = new CloudOfferedService();
                serviceUnit.setId(node.getId());

                if (node.hasProperty(KEY)) {
                    serviceUnit.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + node + " has no " + KEY);
                }

                if (node.hasProperty(CATEGORY)) {
                    serviceUnit.setCategory(node.getProperty(CATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + node + " has no " + CATEGORY);
                }

                if (node.hasProperty(SUBCATEGORY)) {
                    serviceUnit.setSubcategory(node.getProperty(SUBCATEGORY).toString());
                } else {
                    log.warn("Retrieved serviceUnit " + node + " has no " + SUBCATEGORY);
                }
                if (node.hasProperty(UUID)) {
                    serviceUnit.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + serviceUnit + " has no " + UUID);
                }

                //If this happened because you are updating something, disregard this e-mail..addAll(serviceUnitDAO.getMandatoryAssociations(lastPathNode.getId(), database));
                //serviceUnit.getOptionalAssociations().addAll(serviceUnitDAO.getOptionalAssociations(lastPathNode.getId(), database));
                serviceUnit.getElasticityCapabilities().addAll(ElasticityCapabilityDAO.getELasticityCapabilitiesForNode(node.getId(), database));
                serviceUnits.add(serviceUnit);

            }

            for (CloudOfferedService serviceUnit : serviceUnits) {
                serviceUnit.getResourceProperties().addAll(ResourceDAO.getResourcePropertiesForNode(serviceUnit.getId(), database));
                serviceUnit.getQualityProperties().addAll(QualityDAO.getQualityPropertiesForNode(serviceUnit.getId(), database));
                serviceUnit.getCostFunctions().addAll(CostFunctionDAO.getCostFunctionsForNode(serviceUnit.getId(), database));
                //serviceUnit.setElasticityQuantification(getElasticityDependency(serviceUnit.getId(), database));
            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }
        return serviceUnits;

    }

    /**
     * Searches for elasticityCapabilityFor and hasElasticityCapability in both
     * directions
     *
     * @param nodeID
     * @param database
     * @return
     */
    public static List<CloudOfferedService> getConnectedComponentsByElasticityCapabilitiesForNode(Long nodeID, EmbeddedGraphDatabase database) {

        List<CloudOfferedService> serviceUnits = new ArrayList<CloudOfferedService>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            Node parentNode = null;

            try {
                parentNode = database.getNodeById(nodeID);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            if (parentNode == null) {
                return serviceUnits;
            }

            //first extract ServiceUnit instances that target this one
            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.elasticityCapabilityFor, Direction.INCOMING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);

            for (Path path_1 : traverser) {

                //this node is ElasticityCapability, so from it we need to navigate using the hasElasticityCapability relationship
                Node node = path_1.endNode();

                TraversalDescription elCapabiliyTraversal = Traversal.traversal()
                        .evaluator(Evaluators.excludeStartPosition())
                        .relationships(ServiceUnitRelationship.hasElasticityCapability, Direction.INCOMING)
                        .uniqueness(Uniqueness.NODE_PATH);
                Traverser elCapabiliyTraverser = elCapabiliyTraversal.traverse(parentNode);

                for (Path path : elCapabiliyTraverser) {
                    //if not service unit, continue
                    //extract only service units as associated. the Quality, Resource and Cost are
                    //configuration options
                    if (!node.hasLabel(LABEL)) {
                        continue;
                    }
                    CloudOfferedService serviceUnit = new CloudOfferedService();
                    serviceUnit.setId(node.getId());

                    if (node.hasProperty(KEY)) {
                        serviceUnit.setName(node.getProperty(KEY).toString());
                    } else {
                        log.warn("Retrieved serviceUnit " + node + " has no " + KEY);
                    }

                    if (node.hasProperty(CATEGORY)) {
                        serviceUnit.setCategory(node.getProperty(CATEGORY).toString());
                    } else {
                        log.warn("Retrieved serviceUnit " + node + " has no " + CATEGORY);
                    }

                    if (node.hasProperty(SUBCATEGORY)) {
                        serviceUnit.setSubcategory(node.getProperty(SUBCATEGORY).toString());
                    } else {
                        log.warn("Retrieved serviceUnit " + node + " has no " + SUBCATEGORY);
                    }
                    if (node.hasProperty(UUID)) {
                        serviceUnit.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                    } else {
                        log.warn("Retrieved CloudProvider " + serviceUnit + " has no " + UUID);
                    }

                    //If this happened because you are updating something, disregard this e-mail..addAll(serviceUnitDAO.getMandatoryAssociations(lastPathNode.getId(), database));
                    //serviceUnit.getOptionalAssociations().addAll(serviceUnitDAO.getOptionalAssociations(lastPathNode.getId(), database));
                    serviceUnit.getElasticityCapabilities().addAll(ElasticityCapabilityDAO.getELasticityCapabilitiesForNode(node.getId(), database));
                    serviceUnits.add(serviceUnit);
                }
            }

            for (CloudOfferedService serviceUnit : serviceUnits) {
                serviceUnit.getResourceProperties().addAll(ResourceDAO.getResourcePropertiesForNode(serviceUnit.getId(), database));
                serviceUnit.getQualityProperties().addAll(QualityDAO.getQualityPropertiesForNode(serviceUnit.getId(), database));
                serviceUnit.getCostFunctions().addAll(CostFunctionDAO.getCostFunctionsForNode(serviceUnit.getId(), database));
                //serviceUnit.setElasticityQuantification(getElasticityDependency(serviceUnit.getId(), database));
            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }
        return serviceUnits;

    }

    /**
     * Actually persists only serviceUnit and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static Node persistServiceUnit(CloudOfferedService serviceUnitToPersist, EmbeddedGraphDatabase database) {

        Node serviceUnitNode = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            serviceUnitNode = database.createNode();
            serviceUnitNode.setProperty(KEY, serviceUnitToPersist.getName());
            serviceUnitNode.setProperty(CATEGORY, serviceUnitToPersist.getCategory());
            serviceUnitNode.setProperty(SUBCATEGORY, serviceUnitToPersist.getSubcategory());
            serviceUnitNode.setProperty(UUID, serviceUnitToPersist.getUuid().toString());
            serviceUnitNode.addLabel(LABEL);

            //persist Resources
            for (Resource resource : serviceUnitToPersist.getResourceProperties()) {
                Resource resourceFound = ResourceDAO.searchForResourcesUniqueResult(resource, database);

                //quality does not exist need to persist it
                Node persistedElement = null;
                Relationship relationship = null;
                if (resourceFound == null) {
                    persistedElement = ResourceDAO.persistResource(resource, database);
                    relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasResource);
                } else {
                    persistedElement = database.getNodeById(resourceFound.getId());
                    //CHECK IF THERE IS ALLREADY A RELATIONSHIP BETWEEN THEN, AND IF NOT, ADD ONE
                    //WHY WAS THIS HERE? THE CODE SEEMS TO ONLY ADD PROBLEMS
//                boolean relExists = false;
//                for (Relationship r : persistedElement.getRelationships(ServiceUnitRelationship.hasResource)) {
//                    if (r.getStartNode().equals(serviceUnitNode)) {
//                        relExists = true;
//                        relationship = r;
//                        break;
//                    }
//                }
//                if (!relExists) {
                    relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasResource);
//                }
                }

                for (Map.Entry<Metric, MetricValue> entry : resource.getProperties().entrySet()) {
                    Metric metric = entry.getKey();
                    String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
                    relationship.setProperty(propertyKey, entry.getValue().getValue());
                }
            }

            //persist Quality
            for (Quality quality : serviceUnitToPersist.getQualityProperties()) {
                Quality qualityFound = QualityDAO.searchForQualityEntitiesUniqueResult(quality, database);

                //quality does not exist need to persist it
                Node persistedElement = null;
                Relationship relationship = null;
                if (qualityFound == null) {
                    persistedElement = QualityDAO.persistQualityEntity(quality, database);
                    relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasQuality);
                } else {
                    persistedElement = database.getNodeById(qualityFound.getId());
                    //WHY WAS THIS HERE? THE CODE SEEMS TO ONLY ADD PROBLEMS
                    //CHECK IF THERE IS ALLREADY A RELATIONSHIP BETWEEN THEN, AND IF NOT, ADD ONE
//                boolean relExists = false;
//                for (Relationship r : persistedElement.getRelationships(ServiceUnitRelationship.hasQuality)) {
//                    if (r.getStartNode().equals(serviceUnitNode)) {
//                        relExists = true;
//                        relationship = r;
//                        break;
//                    }
//                }
//                if (!relExists) {
                    relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasQuality);
//                }
                }

                /**
                 * add all resource Quality properties on the HAS_QUALITY
                 * relationship (thus we can have same quality (ex
                 * storageOptimized) targeted by diff resources with diff
                 * quality properties (ex IOps),
                 */
                for (Map.Entry<Metric, MetricValue> entry : quality.getProperties().entrySet()) {
                    Metric metric = entry.getKey();
                    String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
                    relationship.setProperty(propertyKey, entry.getValue().getValue());
                }

            }

            //ALWAYS create new Cost Function
            for (CostFunction costFunction : serviceUnitToPersist.getCostFunctions()) {
                Node costElementNode = CostFunctionDAO.persistCostFunction(costFunction, database);
                serviceUnitNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.hasCostFunction);
            }

            //persist ElasticityCapabilities
            for (ElasticityCapability characteristic : serviceUnitToPersist.getElasticityCapabilities()) {
                ElasticityCapability charactFound = ElasticityCapabilityDAO.searchForElasticityCapabilitiesUniqueResult(characteristic, database);
                //costFunction does not exist need to persist it
                Node persistedElement = null;
                if (charactFound == null) {
                    persistedElement = ElasticityCapabilityDAO.persistElasticityCapability(characteristic, database);
                    //create relationship only if newly eprsisted node (we do not create multiple relationshipts towards ElasticityCapability

                } else {
                    persistedElement = database.getNodeById(charactFound.getId());
                }
                Relationship relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasElasticityCapability);
//          //add el capability type and phase on the relationship    
//            relationship.setProperty(ElasticityCapabilityDAO.TYPE, characteristic.getType());
                relationship.setProperty(ElasticityCapabilityDAO.PHASE, characteristic.getPhase());

//            Volatility v = characteristic.getVolatility();
//            if (v != null) {
//                relationship.setProperty(VOLATILITY_TIME_UNIT, v.getMinimumLifetimeInHours());
//                relationship.setProperty(VOLATILITY_MAX_CHANGES, v.getMaxNrOfChanges());
//            }
            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }
        return serviceUnitNode;
    }

    public static void persistCloudServiceUnits(List<CloudOfferedService> cloudServiceUnitsToPersist, EmbeddedGraphDatabase database) {
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (CloudOfferedService serviceUnitToPersist : cloudServiceUnitsToPersist) {
                Node serviceUnitNode = null;

                serviceUnitNode = database.createNode();
                serviceUnitNode.setProperty(KEY, serviceUnitToPersist.getName());
                serviceUnitNode.setProperty(CATEGORY, serviceUnitToPersist.getCategory());
                serviceUnitNode.setProperty(SUBCATEGORY, serviceUnitToPersist.getSubcategory());
                serviceUnitNode.setProperty(UUID, serviceUnitToPersist.getUuid().toString());
                serviceUnitNode.addLabel(LABEL);

                //persist Resources
                for (Resource resource : serviceUnitToPersist.getResourceProperties()) {
                    Resource resourceFound = ResourceDAO.searchForResourcesUniqueResult(resource, database);

                    //quality does not exist need to persist it
                    Node persistedElement = null;
                    Relationship relationship = null;
                    if (resourceFound == null) {
                        persistedElement = ResourceDAO.persistResource(resource, database);
                        relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasResource);
                    } else {
                        persistedElement = database.getNodeById(resourceFound.getId());
                        //CHECK IF THERE IS ALLREADY A RELATIONSHIP BETWEEN THEN, AND IF NOT, ADD ONE
                        //WHY WAS THIS HERE? THE CODE SEEMS TO ONLY ADD PROBLEMS
//                    boolean relExists = false;
//                    for (Relationship r : persistedElement.getRelationships(ServiceUnitRelationship.hasResource)) {
//                        if (r.getStartNode().equals(serviceUnitNode)) {
//                            relExists = true;
//                            relationship = r;
//                            break;
//                        }
//                    }
//                    if (!relExists) {
                        relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasResource);
//                    }
                    }

                    for (Map.Entry<Metric, MetricValue> entry : resource.getProperties().entrySet()) {
                        Metric metric = entry.getKey();
                        String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
                        relationship.setProperty(propertyKey, entry.getValue().getValue());
                    }
                }

                //persist Quality
                for (Quality quality : serviceUnitToPersist.getQualityProperties()) {
                    Quality qualityFound = QualityDAO.searchForQualityEntitiesUniqueResult(quality, database);

                    //quality does not exist need to persist it
                    Node persistedElement = null;
                    Relationship relationship = null;
                    if (qualityFound == null) {
                        persistedElement = QualityDAO.persistQualityEntity(quality, database);
                        relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasQuality);
                    } else {
                        persistedElement = database.getNodeById(qualityFound.getId());
                        //CHECK IF THERE IS ALLREADY A RELATIONSHIP BETWEEN THEN, AND IF NOT, ADD ONE
                        //WHY WAS THIS HERE? THE CODE SEEMS TO ONLY ADD PROBLEMS
                        //WAS REMOVED because when you have multiple possible qualities
//                    boolean relExists = false;
//                    for (Relationship r : persistedElement.getRelationships(ServiceUnitRelationship.hasQuality)) {
//                        if (r.getStartNode().equals(serviceUnitNode)) {
//                            relExists = true;
//                            relationship = r;
//                            break;
//                        }
//                    }
//                    if (!relExists) {
                        relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasQuality);
//                    }
                    }

                    /**
                     * add all resource Quality properties on the HAS_QUALITY
                     * relationship (thus we can have same quality (ex
                     * storageOptimized) targeted by diff resources with diff
                     * quality properties (ex IOps),
                     */
                    for (Map.Entry<Metric, MetricValue> entry : quality.getProperties().entrySet()) {
                        Metric metric = entry.getKey();
                        String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
                        relationship.setProperty(propertyKey, entry.getValue().getValue());
                    }

                }

                //ALWAYS create new Cost Function
                for (CostFunction costFunction : serviceUnitToPersist.getCostFunctions()) {
                    Node costElementNode = CostFunctionDAO.persistCostFunction(costFunction, database);
                    serviceUnitNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.hasCostFunction);
                }

                //persist ElasticityCapabilities
                for (ElasticityCapability characteristic : serviceUnitToPersist.getElasticityCapabilities()) {
                    ElasticityCapability charactFound = ElasticityCapabilityDAO.searchForElasticityCapabilitiesUniqueResult(characteristic, database);
                    //costFunction does not exist need to persist it
                    Node persistedElement = null;
                    if (charactFound == null) {
                        persistedElement = ElasticityCapabilityDAO.persistElasticityCapability(characteristic, database);
                        //create relationship only if newly eprsisted node (we do not create multiple relationshipts towards ElasticityCapability

                    } else {
                        persistedElement = database.getNodeById(charactFound.getId());
                    }

                    Relationship relationship = serviceUnitNode.createRelationshipTo(persistedElement, ServiceUnitRelationship.hasElasticityCapability);
//                relationship.setProperty(ElasticityCapabilityDAO.TYPE, characteristic.getType());
                    relationship.setProperty(ElasticityCapabilityDAO.PHASE, characteristic.getPhase());

//                Volatility v = characteristic.getVolatility();
//                if (v != null) {
//                    relationship.setProperty(VOLATILITY_TIME_UNIT, v.getMinimumLifetimeInHours());
//                    relationship.setProperty(VOLATILITY_MAX_CHANGES, v.getMaxNrOfChanges());
//                }
                }

            }
            if (!transactionAllreadyRunning) {
                if (!transactionAllreadyRunning) {
                    tx.success();
                }
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
