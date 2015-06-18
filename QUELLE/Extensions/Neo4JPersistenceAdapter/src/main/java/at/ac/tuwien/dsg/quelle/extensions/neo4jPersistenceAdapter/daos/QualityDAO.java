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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Volatility;
import java.util.ArrayList;
import java.util.List;
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
public class QualityDAO extends Neo4JDAO {

    static final Logger log = LoggerFactory.getLogger(QualityDAO.class);

    public static final Label LABEL = new Label() {
        public String name() {
            return "Quality";
        }
    };
    public static final String KEY = "name";
//    public static final String TYPE = "type";
    public static final String PROPERTY_SEPARATOR = ":";

    public static final String UUID = "uuid";

                   

    private QualityDAO() {
    }

    /**
     * for a service unit ID, it gets all relationships to a Quality node, and
     * based on the different relationships, instantiates the options.
     *
     * @param serviceUnitNodeID
     * @param resourceNodeID
     * @param database
     * @return
     */
    //removed as in thepory I need to get the QualityOptions from the elasticity capabilities
//    public static List<Quality> getQualityOptionsForServiceUnitNode(Long serviceUnitNodeID, Long qualityNodeID, EmbeddedGraphDatabase database) {
//
//
//        List<Quality> qualitys = new ArrayList<Quality>();
//
//        try {
//            Node qualityNode = database.getNodeById(qualityNodeID);
//            Node serviceUnitNode = database.getNodeById(serviceUnitNodeID);
//
//            if (qualityNode == null) {
//                return qualitys;
//            }
//
//            //get RESOURCE properties from the RELATIONSHIP
//            for (Relationship relationship : serviceUnitNode.getRelationships(ServiceUnitRelationship.hasQuality, Direction.OUTGOING)) {
//                //if relationship from ServiceUnit to Resource
//                if (!relationship.getEndNode().equals(qualityNode)) {
//                    continue;
//                }
//                //the resource is created based on the resourceNode and the relationship properties
//                Quality quality = new Quality();
//                quality.setId(qualityNode.getId());
//
//
//                for (String propertyKey : relationship.getPropertyKeys()) {
//                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    }
//                    if (propertyKey.equals(KEY)) {
//                        quality.setName(qualityNode.getProperty(KEY).toString());
//                    } else {
//
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        MetricValue metricValue = new MetricValue(relationship.getProperty(propertyKey));
//                        quality.addProperty(metric, metricValue);
//                    }
//
//                }
//                qualitys.add(quality);
//            }
//
//
//        } catch (Exception e) {
//           log.error(
//            e.printStackTrace();
//        } finally {
//        }
//
//        return qualitys;
//
//    }
    /**
     * DOES NOT return also properties embedded on the quality relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static List<Quality> searchForQualityEntities(Quality resourceToSearchFor, EmbeddedGraphDatabase database) {

        List<Quality> resources = new ArrayList<Quality>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                Quality quality = new Quality();
                quality.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    quality.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved Quality " + resourceToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(UUID)) {
                    quality.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + quality + " has no " + UUID);
                }

//                if (node.hasProperty(TYPE)) {
//                    quality.setName(node.getProperty(TYPE).toString());
//                } else {
//                    log.warn( "Retrieved Quality " + resourceToSearchFor + " has no " + TYPE);
//                }
                //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
                //and the property value is the metric value
                for (String propertyKey : node.getPropertyKeys()) {

                    //skip the key property
                    if (propertyKey.equals(KEY)) {// || propertyKey.equals(TYPE)) {
                        continue;
                    }
                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        MetricValue metricValue = new MetricValue(node.getProperty(propertyKey));
//                        quality.addProperty(metric, metricValue);
//                    }
                }

                resources.add(quality);
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

        return resources;
    }

    /**
     * DOES NOT return also properties embedded on the quality relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static Quality searchForQualityEntitiesUniqueResult(Quality resourceToSearchFor, EmbeddedGraphDatabase database) {
        Quality resourceFound = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                Quality quality = new Quality();
                quality.setId(node.getId());

                if (node.hasProperty(KEY)) {
                    String name = node.getProperty(KEY).toString();
                    if (!name.equals(resourceToSearchFor.getName())) {
                        continue;
                    } else {
                        quality.setName(name);
                    }
                } else {
                    log.warn("Retrieved Resource " + resourceToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(UUID)) {
                    quality.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + quality + " has no " + UUID);
                }

//                if (node.hasProperty(TYPE)) {
//                    quality.setName(node.getProperty(TYPE).toString());
//                } else {
//                    log.warn( "Retrieved Quality " + resourceToSearchFor + " has no " + TYPE);
//                }
                //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
                //and the property value is the metric value
                for (String propertyKey : node.getPropertyKeys()) {
                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                    //skip the key property
                    if (propertyKey.equals(KEY)) {//|| propertyKey.equals(TYPE)) {
                        continue;
                    }
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        MetricValue metricValue = new MetricValue(node.getProperty(propertyKey));
//                        quality.addProperty(metric, metricValue);
//                    }
                }
                //if we have reached this place, then we have found return the quality and can return it
                resourceFound = quality;

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
//            log.warn( "Quality " + resourceToSearchFor + " was not found");
//        }
        return resourceFound;
    }

    /**
     * Only this method returns Quality properties. Because the quality details
     * for each node are recorded as properties on HAS_QUALITY relationship,
     * they can be retrieved only for specific nodes as multiple nodes can have
     * same quality, with diff details (ex multiple ServiceUnits have
     * memoryOptimzied, but the memory IO/s can be different)
     *
     * @param nodeID
     * @param database
     * @return
     */
    public static List<Quality> getQualityPropertiesForNode(Long nodeID, EmbeddedGraphDatabase database) {

        List<Quality> qualities = new ArrayList<Quality>();
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
                return qualities;
            }

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.hasQuality, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node node = path.endNode();
                Quality quality = new Quality();
                quality.setId(node.getId());

                //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
                //and the property value is the metric value
                for (String propertyKey : node.getPropertyKeys()) {
                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                    //skip the key property
                    if (propertyKey.equals(KEY)) {
                        quality.setName(node.getProperty(KEY).toString());
//                    } else if (propertyKey.equals(TYPE)) {
//                        quality.setName(lastPathNode.getProperty(KEY).toString());
                    } else {
//                        if (metricInfo.length < 2) {
//                            log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                        } else {
//                            Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                            MetricValue metricValue = new MetricValue(lastPathNode.getProperty(propertyKey));
//                            quality.addProperty(metric, metricValue);
//                        }
                    }

                }

                if (node.hasProperty(UUID)) {
                    quality.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + quality + " has no " + UUID);
                }

                //get properties from the RELATIONSHIP
                //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
                //and the property value is the metric value
                Relationship relationship = null;

                for (Relationship r : parentNode.getRelationships(ServiceUnitRelationship.hasQuality, Direction.OUTGOING)) {
                    if (r.getEndNode().equals(node)) {
                        relationship = r;
                    }
                }

                if (relationship != null) {
                    for (String propertyKey : relationship.getPropertyKeys()) {
                        String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                        if (metricInfo.length < 2) {
                            log.warn("Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
                        } else {
                            Metric metric = new Metric(metricInfo[0], metricInfo[1]);
                            MetricValue metricValue = new MetricValue(relationship.getProperty(propertyKey));
                            quality.addProperty(metric, metricValue);
                        }

                    }
                } else {
                    log.warn("No relationship found of type " + ServiceUnitRelationship.hasQuality + " starting from " + parentNode.getProperty(KEY).toString() + " and ending at " + node.getProperty(KEY).toString());
                }

                //if we have reached this place, then we have found return the quality and can return it
                if (quality != null) {
                    //hack. if the quality has allready been added (equals is done on the DB Node),
                    //this means ServiceUnit has elasticity capability on it, and the old is also removed
                    if (qualities.contains(quality)) {
                        qualities.remove(quality);
                    } else {
                        qualities.add(quality);
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

        return qualities;

    }

    public static List<ElasticityCapability.Dependency> getElasticityCapabilityTargetsQualityForNode(Long elasticityCapabilityNodeID, EmbeddedGraphDatabase database) {

        List<ElasticityCapability.Dependency> elTargets = new ArrayList<ElasticityCapability.Dependency>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            Node parentNode = database.getNodeById(elasticityCapabilityNodeID);

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

                Quality quality = new Quality();
                quality.setId(node.getId());

                //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
                //and the property value is the metric value
                for (String propertyKey : node.getPropertyKeys()) {
                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                    //skip the key property
                    if (propertyKey.equals(KEY)) {
                        quality.setName(node.getProperty(KEY).toString());
//                    } else if (propertyKey.equals(TYPE)) {
//                        quality.setName(lastPathNode.getProperty(KEY).toString());
                    } else {
//                        if (metricInfo.length < 2) {
//                            log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                        } else {
//                            Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                            MetricValue metricValue = new MetricValue(lastPathNode.getProperty(propertyKey));
//                            quality.addProperty(metric, metricValue);
//                        }
                    }
                }

                if (node.hasProperty(UUID)) {
                    quality.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + quality + " has no " + UUID);
                }

//                //get properties from the RELATIONSHIP
//                //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
//                //and the property value is the metric value
                Relationship relationship = path.lastRelationship();

                if (relationship != null) {
                    String type = relationship.getProperty(ElasticityCapabilityDAO.TYPE).toString();

                    ElasticityCapability.Dependency dependency = new ElasticityCapability.Dependency(quality, type);
                    elTargets.add(dependency);
                    Volatility volatility = new Volatility();

                    for (String propertyKey : relationship.getPropertyKeys()) {
                        switch (propertyKey) {
                            case VOLATILITY_TIME_UNIT: {
                                String unit = relationship.getProperty(VOLATILITY_TIME_UNIT).toString();
                                volatility.setMinimumLifetimeInHours(Integer.parseInt(unit));
                                break;
                            }
                            case VOLATILITY_MAX_CHANGES: {
                                String unit = relationship.getProperty(VOLATILITY_MAX_CHANGES).toString();
                                volatility.setMaxNrOfChanges(Double.parseDouble(unit));
                                break;
                            }
                            default:
                                String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                                if (metricInfo.length < 2) {
//                                log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
                                } else {
                                    Metric metric = new Metric(metricInfo[0], metricInfo[1]);
                                    MetricValue metricValue = new MetricValue(relationship.getProperty(propertyKey));
                                    quality.addProperty(metric, metricValue);
                                }
                                break;
                        }

                    }

                    dependency.setVolatility(volatility);

                } else {
                    log.warn("No relationship found of type " + ServiceUnitRelationship.hasQuality + " starting from " + parentNode.getProperty(KEY).toString() + " and ending at " + node.getProperty(KEY).toString());
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

    public static Quality getByID(Long id, EmbeddedGraphDatabase database) {
        Quality resourceFound = null;
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
                log.warn("Quality ID " + id + " was not found");
                return null;
            }
            Quality quality = new Quality();
            quality.setId(node.getId());

            //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
            //and the property value is the metric value
            for (String propertyKey : node.getPropertyKeys()) {
                String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                //skip the key property
                if (propertyKey.equals(KEY)) {
                    quality.setName(node.getProperty(KEY).toString());
//                } else if (propertyKey.equals(TYPE)) {
//                    quality.setName(node.getProperty(KEY).toString());
                } else {
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        MetricValue metricValue = new MetricValue(node.getProperty(propertyKey));
//                        quality.addProperty(metric, metricValue);
//                    }
                }
            }
            if (node.hasProperty(UUID)) {
                quality.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
            } else {
                log.warn("Retrieved CloudProvider " + quality + " has no " + UUID);
            }

            //if we have reached this place, then we have found return the quality and can return it
            resourceFound = quality;
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
     * Actually persists only Quality and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static Node persistQualityEntity(Quality resourceToPersist, EmbeddedGraphDatabase database) {

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
            resourceNode.setProperty(KEY, resourceToPersist.getName());
            resourceNode.setProperty (UUID, resourceToPersist.getUuid().toString());
            resourceNode.addLabel(LABEL);

//            for (Map.Entry<Metric, MetricValue> entry : resourceToPersist.getProperties().entrySet()) {
//                Metric metric = entry.getKey();
//                String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
//                resourceNode.setProperty(propertyKey, entry.getValue().getValue());
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
     * Actually persists only Quality and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static void persistQualityEntities(List<Quality> resourcesToPersist, EmbeddedGraphDatabase database) {
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Quality resourceToPersist : resourcesToPersist) {
                Node resourceNode = database.createNode();
                resourceNode.setProperty(KEY, resourceToPersist.getName());
                resourceNode.setProperty (UUID, resourceToPersist.getUuid().toString());
                resourceNode.addLabel(LABEL);

//                for (Map.Entry<Metric, MetricValue> entry : resourceToPersist.getProperties().entrySet()) {
//                    Metric metric = entry.getKey();
//                    String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
//                    resourceNode.setProperty(propertyKey, entry.getValue().getValue());
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
