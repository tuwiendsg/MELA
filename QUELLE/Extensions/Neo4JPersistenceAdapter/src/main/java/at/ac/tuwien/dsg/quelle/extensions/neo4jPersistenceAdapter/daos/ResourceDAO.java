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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability.Dependency;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
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
public class ResourceDAO extends Neo4JDAO {

    static final Logger log = LoggerFactory.getLogger(ResourceDAO.class);

    public static final Label LABEL = new Label() {
        public String name() {
            return "Resource";
        }
    };
    public static final String KEY = "name";
    //separates metricName from metricUnit in property name
    public static final String PROPERTY_SEPARATOR = ":";

    public static final String UUID = "uuid";
 
                
             
    private ResourceDAO() {
    }

    /**
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static List<Resource> searchForResources(Resource resourceToSearchFor, EmbeddedGraphDatabase database) {

        List<Resource> resources = new ArrayList<Resource>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {

            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                Resource resource = new Resource();
                resource.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    resource.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved Resource " + resourceToSearchFor + " has no " + KEY);
                }
                if (node.hasProperty(UUID)) {
                    resource.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + resource + " has no " + UUID);
                }
                //resource properties moved on the HAS_RESOURCE relationshio, so we can merge multiple ServiceUnits and spaces
//                //the format assumed for each property of a Resource is "property key =" metricName : metricValue " (separated by :), 
//                //and the property value is the metric value
//                for (String propertyKey : node.getPropertyKeys()) {
//
//                    //skip the key property
//                    if (propertyKey.equals(KEY)) {
//                        continue;
//                    }
//                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        MetricValue metricValue = new MetricValue(node.getProperty(propertyKey));
//                        resource.addProperty(metric, metricValue);
//                    }
//                }

                //extract quality
                //resource.addQualityProperty(q);.addAll(QualityDAO.getQualityPropertiesForNode(node.getId(), database));
                resources.add(resource);
            }

            if (!transactionAllreadyRunning) { tx.success();}
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
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static Resource searchForResourcesUniqueResult(Resource resourceToSearchFor, EmbeddedGraphDatabase database) {
        Resource resourceFound = null;
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {

            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                Resource resource = new Resource();
                resource.setId(node.getId());

                if (node.hasProperty("name")) {
                    String name = node.getProperty("name").toString();
                    if (!name.equals(resourceToSearchFor.getName())) {
                        continue;
                    }
                } else {
                    log.warn("Retrieved Resource " + resourceToSearchFor + " has no name");
                }
                 if (node.hasProperty(UUID)) {
                    resource.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + resource + " has no " + UUID);
                }

//                //the format assumed for each property of a Resource is "property key =" metricName : metricValue " (separated by :), 
//                //and the property value is the metric value
//                for (String propertyKey : node.getPropertyKeys()) {
//                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    //skip the key property
//                    if (node.hasProperty(KEY)) {
//                        resource.setName(node.getProperty(KEY).toString());
//                    } else {
//                        log.warn( "Retrieved Resource " + resourceToSearchFor + " has no " + KEY);
//                    }
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        MetricValue metricValue = new MetricValue(node.getProperty(propertyKey));
//                        resource.addProperty(metric, metricValue);
//                    }
//                }
                //extract quality
                //resource.addQualityProperty(q);.addAll(QualityDAO.getQualityPropertiesForNode(node.getId(), database));
                resourceFound = resource;

                break;
            }
            if (!transactionAllreadyRunning) { tx.success();}
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (!transactionAllreadyRunning) {
                tx.finish();
            }
        }

//        if (resourceFound == null) {
//            log.warn( "Resource " + resourceToSearchFor + " was not found");
//        }
        return resourceFound;
    }

    public static Resource getByID(Long id, EmbeddedGraphDatabase database) {
        Resource resourceFound = null;

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
                log.warn("Resource ID " + id + " was not found");
            }
            Resource resource = new Resource();
            resource.setId(node.getId());

            //the format assumed for each property of a Resource is "property key =" metricName : metricValue " (separated by :), 
            //and the property value is the metric value
            for (String propertyKey : node.getPropertyKeys()) {
                String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                //skip the key property
                if (propertyKey.equals(KEY)) {
                    resource.setName(node.getProperty(KEY).toString());
                } else {
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        MetricValue metricValue = new MetricValue(node.getProperty(propertyKey));
//                        resource.addProperty(metric, metricValue);
//                    }
                }
            }
            
             if (node.hasProperty(UUID)) {
                    resource.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + resource + " has no " + UUID);
                }

            //resource.addQualityProperty(q);.addAll(QualityDAO.getQualityPropertiesForNode(id, database));
            resourceFound = resource;

            if (!transactionAllreadyRunning) { tx.success();}
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
     * Only this method returns properties. Because the properties details for
     * each node are recorded as properties on HAS_RESOURCE relationship, they
     * can be retrieved only for specific nodes as multiple nodes can have same
     * resource, with diff details (ex multiple ServiceUnits have memory, but
     * the memory size can be different)
     *
     * @param nodeID
     * @param database
     * @return
     */
    public static List<Resource> getResourcePropertiesForNode(Long nodeID, EmbeddedGraphDatabase database) {

        List<Resource> resources = new ArrayList<Resource>();

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
                return resources;
            }

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.hasResource, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node node = path.endNode();
                Resource resource = new Resource();
                resource.setId(node.getId());

                //the format assumed for each property of a RESOURCE is "property key =" metricName : metricValue " (separated by :), 
                //and the property value is the metric value
                for (String propertyKey : node.getPropertyKeys()) {
                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                    //skip the key property
                    if (propertyKey.equals(KEY)) {
                        resource.setName(node.getProperty(KEY).toString());
                    } else {
//                        if (metricInfo.length < 2) {
//                            log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                        } else {
//                            Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                            MetricValue metricValue = new MetricValue(lastPathNode.getProperty(propertyKey));
//                            resource.addProperty(metric, metricValue);
//                        }
                    }
                }
                
                 if (node.hasProperty(UUID)) {
                    resource.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + resource + " has no " + UUID);
                }

                //get RESOURCE properties from the RELATIONSHIP
                Relationship relationship = null;

                for (Relationship r : parentNode.getRelationships(ServiceUnitRelationship.hasResource, Direction.OUTGOING)) {
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
                            resource.addProperty(metric, metricValue);
                        }

                    }
                } else {
                    log.warn("No relationship found of type " + ServiceUnitRelationship.hasResource + " starting from " + parentNode + " and ending at " + node);
                }

                if (resource != null) {
                    //hack. if the resource has allready been added (equals is done on the DB Node),
                    //this means ServiceUnit has elasticity capability on it, and the old is also removed
                    if (resources.contains(resource)) {
                        resources.remove(resource);
                    } else {
                        resources.add(resource);

                    }
                }
            }
            if (!transactionAllreadyRunning) { tx.success();}
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
     * for a service unit ID, it gets all relationships to a Resource node, and
     * based on the different relationships, instantiates the options.
     *
     * @param serviceUnitNodeID
     * @param resourceNodeID
     * @param database
     * @return
     */
    public static List<Resource> geResourceOptionsForServiceUnitNode(Long serviceUnitNodeID, Long resourceNodeID, EmbeddedGraphDatabase database) {

        List<Resource> resources = new ArrayList<Resource>();

        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            Node resourceNode = database.getNodeById(resourceNodeID);
            Node serviceUnitNode = database.getNodeById(serviceUnitNodeID);

            if (resourceNode == null) {
                return resources;
            }

            //get RESOURCE properties from the RELATIONSHIP
            for (Relationship relationship : serviceUnitNode.getRelationships(ServiceUnitRelationship.hasResource, Direction.OUTGOING)) {
                //if relationship from ServiceUnit to Resource
                if (!relationship.getEndNode().equals(resourceNode)) {
                    continue;
                }
                //the resource is created based on the resourceNode and the relationship properties
                Resource resource = new Resource();
                resource.setId(resourceNode.getId());

                for (String propertyKey : relationship.getPropertyKeys()) {
                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);

                    if (propertyKey.equals(KEY)) {
                        resource.setName(resourceNode.getProperty(KEY).toString());
                    } else {
                        if (metricInfo.length < 2) {
                            log.warn("Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
                        } else {
                            Metric metric = new Metric(metricInfo[0], metricInfo[1]);
                            MetricValue metricValue = new MetricValue(relationship.getProperty(propertyKey));
                            resource.addProperty(metric, metricValue);
                        }
                    }

                }
                
                
                resources.add(resource);
            }

            if (!transactionAllreadyRunning) { tx.success();}
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

    public static List<ElasticityCapability.Dependency> getElasticityCapabilityTargetResourcesForNode(Long nodeID, EmbeddedGraphDatabase database) {

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
                Resource resource = new Resource();
                resource.setId(node.getId());

                //the format assumed for each property of a Quality is "property key =" metricName : metricValue " (separated by :), 
                //and the property value is the metric value
                for (String propertyKey : node.getPropertyKeys()) {
                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
                    //skip the key property
                    if (propertyKey.equals(KEY)) {
                        resource.setName(node.getProperty(KEY).toString());
                    } else {
//                        
                    }
                }
                
                 if (node.hasProperty(UUID)) {
                    resource.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + resource + " has no " + UUID);
                }
                 
                //if we have reached this place, then we have found return the resource and can return it
                //add quality properties for resource
                //resource.addQualityProperty(q);.addAll(QualityDAO.getQualityPropertiesForNode(lastPathNode.getId(), database));  

                //get RESOURCE properties from the RELATIONSHIP
                Relationship relationship = path.lastRelationship();

                if (relationship != null) {
                    String type = relationship.getProperty(ElasticityCapabilityDAO.TYPE).toString();
                    Dependency dependency = new Dependency(resource, type);
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
                    log.warn("No relationship found of type " + ServiceUnitRelationship.hasResource + " starting from " + parentNode + " and ending at " + node);
                }

            }
            if (!transactionAllreadyRunning) { tx.success();}
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
     * Actually persists only Resource and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static Node persistResource(Resource resourceToPersist, EmbeddedGraphDatabase database) {
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
//
//            for (Map.Entry<Metric, MetricValue> entry : resourceToPersist.getProperties().entrySet()) {
//                Metric metric = entry.getKey();
//                String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
//                resourceNode.setProperty(propertyKey, entry.getValue().getValue());
//            }

            //persist node qualities (relationship to qualities, or whole quality if none exists)
//            for (Quality q : resourceToPersist.getResourceQuality()) {
//                Quality quality = QualityDAO.searchForQualityEntitiesUniqueResult(q, database);
//                //quality does not exist need to persist it
//                Node qualityNode;
//                if (quality == null) {
//                    qualityNode = QualityDAO.persistQualityEntity(q, database);
//                } else {
//                    //retrieve the quality to have its ID
//                    //add relationship from Resource to Quality
//                    qualityNode = database.getNodeById(quality.getId());
//                }
//
//                Relationship relationship = resourceNode.createRelationshipTo(qualityNode, UtilityRelationship.HAS_QUALITY);
//                /**
//                 * add all resource Quality properties on the HAS_QUALITY
//                 * relationship (thus we can have same quality (ex
//                 * storageOptimized) targeted by diff resources with diff
//                 * quality properties (ex IOps),
//                 */
//                for (Map.Entry<Metric, MetricValue> entry : q.getProperties().entrySet()) {
//                    Metric metric = entry.getKey();
//                    String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
//                    relationship.setProperty(propertyKey, entry.getValue().getValue());
//                }
//
//            }
            if (!transactionAllreadyRunning) { tx.success();}
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
     * Actually persists only Resource and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static void persistResources(List<Resource> resourcesToPersist, EmbeddedGraphDatabase database) {
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Resource resourceToPersist : resourcesToPersist) {

                Node resourceNode = database.createNode();
                resourceNode.setProperty(KEY, resourceToPersist.getName());
                resourceNode.setProperty (UUID, resourceToPersist.getUuid().toString());
                resourceNode.addLabel(LABEL);

//                for (Map.Entry<Metric, MetricValue> entry : resourceToPersist.getProperties().entrySet()) {
//                    Metric metric = entry.getKey();
//                    String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
//                    resourceNode.setProperty(propertyKey, entry.getValue().getValue());
//                }
//                //persist node qualities (relationship to qualities, or whole quality if none exists)
//                for (Quality q : resourceToPersist.getResourceQuality()) {
//                    Quality quality = QualityDAO.searchForQualityEntitiesUniqueResult(q, database);
//                    Node qualityNode;
//                    if (quality == null) {
//                        qualityNode = QualityDAO.persistQualityEntity(q, database);
//                    } else {
//                        //retrieve the quality to have its ID
//                        //add relationship from Resource to Quality
//                        qualityNode = database.getNodeById(quality.getId());
//                    }
//
//                    Relationship relationship = resourceNode.createRelationshipTo(qualityNode, UtilityRelationship.HAS_QUALITY);
//                    /**
//                     * add all resource Quality properties on the HAS_QUALITY
//                     * relationship (thus we can have same quality (ex
//                     * storageOptimized) targeted by diff resources with diff
//                     * quality properties (ex IOps),
//                     */
//                    for (Map.Entry<Metric, MetricValue> entry : q.getProperties().entrySet()) {
//                        Metric metric = entry.getKey();
//                        String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
//                        relationship.setProperty(propertyKey, entry.getValue().getValue());
//                    }
//                }
            }
            if (!transactionAllreadyRunning) { tx.success();}
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
