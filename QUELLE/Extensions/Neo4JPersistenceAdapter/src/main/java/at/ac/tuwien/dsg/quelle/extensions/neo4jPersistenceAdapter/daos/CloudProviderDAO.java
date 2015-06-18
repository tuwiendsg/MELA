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
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import static at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.transaction.Status;
import javax.transaction.SystemException;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class CloudProviderDAO extends Neo4JDAO {

    static final Logger log = LoggerFactory.getLogger(CloudProviderDAO.class);

    public static final Label LABEL = new Label() {
        public String name() {
            return "CloudProvider";
        }
    };
    public static final String KEY = "name";
    public static final String TYPE = "type";
    public static final String UUID = "uuid";
    
    
    //separates metricName from metricUnit in property name
//    public static final String PROPERTY_SEPARATOR = ":";

    private CloudProviderDAO() {
    }

    public static List<CloudProvider> searchForCloudProviders(CloudProvider resourceToSearchFor, EmbeddedGraphDatabase database) {
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        List<CloudProvider> cloudProviders = new ArrayList<CloudProvider>();
        try {

            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                CloudProvider cloudProvider = new CloudProvider();
                cloudProvider.setId(node.getId());
                if (node.hasProperty(KEY)) {
                    cloudProvider.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CloudProvider " + resourceToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(UUID)) {
                    cloudProvider.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + cloudProvider + " has no " + UUID);
                }

                //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
                cloudProvider.getCloudOfferedServices().addAll(ServiceUnitDAO.getCloudServiceUnitsForCloudProviderNode(node.getId(), database));
                cloudProviders.add(cloudProvider);
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

        return cloudProviders;
    }

    public static List<CloudProvider> getAllCloudProviders(EmbeddedGraphDatabase database) {

        List<CloudProvider> cloudProviders = new ArrayList<CloudProvider>();
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Node node : database.getAllNodes()) {
                if (node.hasLabel(LABEL)) {
                    CloudProvider cloudProvider = new CloudProvider();
                    cloudProvider.setId(node.getId());
                    if (node.hasProperty(KEY)) {
                        cloudProvider.setName(node.getProperty(KEY).toString());
                    } else {
                        log.warn("Retrieved CloudProvider " + node.getId() + " has no " + KEY);
                    }

                    if (node.hasProperty(UUID)) {
                        cloudProvider.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                    } else {
                        log.warn("Retrieved CloudProvider " + cloudProvider + " has no " + UUID);
                    }

                    //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
                    cloudProvider.getCloudOfferedServices().addAll(ServiceUnitDAO.getCloudServiceUnitsForCloudProviderNode(node.getId(), database));
                    cloudProviders.add(cloudProvider);
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

        return cloudProviders;
    }

    /**
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static CloudProvider searchForCloudProvidersUniqueResult(CloudProvider resourceToSearchFor, EmbeddedGraphDatabase database) {
        CloudProvider cloudProviders = null;

        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {
                CloudProvider provider = new CloudProvider();
                provider.setId(node.getId());

                if (node.hasProperty("name")) {
                    String name = node.getProperty("name").toString();
                    if (!name.equals(resourceToSearchFor.getName())) {
                        continue;
                    }
                } else {
                    log.warn("Retrieved CloudProvider " + resourceToSearchFor + " has no name");
                }

                if (node.hasProperty(KEY)) {
                    provider.setName(node.getProperty(KEY).toString());
                } else {
                    log.warn("Retrieved CloudProvider " + resourceToSearchFor + " has no " + KEY);
                }

                if (node.hasProperty(TYPE)) {
                    provider.setName(node.getProperty(TYPE).toString());
                } else {
                    log.warn("Retrieved CloudProvider " + resourceToSearchFor + " has no " + TYPE);
                }

                if (node.hasProperty(UUID)) {
                    provider.setUuid(java.util.UUID.fromString(node.getProperty(UUID).toString()));
                } else {
                    log.warn("Retrieved CloudProvider " + provider + " has no " + UUID);
                }

                //carefull. this can lead to infinite recursion (is still a graph. maybe improve later)
                provider.getCloudOfferedServices().addAll(ServiceUnitDAO.getCloudServiceUnitsForCloudProviderNode(node.getId(), database));
                cloudProviders = provider;

                break;
            }

//        if (cloudProviders == null) {
//            log.warn( "CloudProvider " + resourceToSearchFor + " was not found");
//        }
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

        return cloudProviders;
    }

    /**
     * Actually persists only CloudProvider and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static Node persistCloudProvider(CloudProvider resourceToPersist, EmbeddedGraphDatabase database) {
        Node cloudProviderFound = searchForNodeByLabelAndKey(LABEL, KEY, resourceToPersist.getName(), database);

        if (cloudProviderFound != null) {
            return cloudProviderFound;
        }

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
            costFunctionNode.setProperty(TYPE, resourceToPersist.getType());
            costFunctionNode.setProperty(UUID, resourceToPersist.getUuid());

            costFunctionNode.addLabel(LABEL);

            //persist serviceUnit elements
            for (CloudOfferedService serviceUnit : resourceToPersist.getCloudOfferedServices()) {
                CloudOfferedService costElementFound = ServiceUnitDAO.searchForCloudServiceUnitsUniqueResult(serviceUnit, database);
                //costFunction does not exist need to persist it
                Node costElementNode = null;
                if (costElementFound == null) {
                    costElementNode = ServiceUnitDAO.persistServiceUnit(serviceUnit, database);
                } else {
                    //retrieve the costFunction to have its ID
                    //add relationship from CostElement to CloudProvider
                    costElementNode = database.getNodeById(costElementFound.getId());
                }

                Relationship relationship = costFunctionNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.providesServiceUnit);

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
        return costFunctionNode;

    }

    /**
     * Actually persists only CloudProvider and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static void persistCloudProviders(List<CloudProvider> resourcesToPersist, EmbeddedGraphDatabase database) {
        boolean transactionAllreadyRunning = false;
        try {
            transactionAllreadyRunning = (database.getTxManager().getStatus() == Status.STATUS_ACTIVE);
        } catch (SystemException ex) {
            log.error(ex.getMessage(), ex);
        }
        Transaction tx = (transactionAllreadyRunning) ? null : database.beginTx();

        try {
            for (CloudProvider resourceToPersist : resourcesToPersist) {

                Node cloudProviderNode = searchForNodeByLabelAndKey(LABEL, KEY, resourceToPersist.getName(), database);

                if (cloudProviderNode == null) {
                    cloudProviderNode = database.createNode();
                    cloudProviderNode.setProperty(KEY, resourceToPersist.getName());
                    cloudProviderNode.setProperty(TYPE, resourceToPersist.getType());
                  cloudProviderNode.setProperty(UUID, resourceToPersist.getUuid().toString());
                    cloudProviderNode.addLabel(LABEL);
                }
                //persist serviceUnit elements
                for (CloudOfferedService serviceUnit : resourceToPersist.getCloudOfferedServices()) {
                    CloudOfferedService costElementFound = ServiceUnitDAO.searchForCloudServiceUnitsUniqueResult(serviceUnit, database);
                    //costFunction does not exist need to persist it
                    Node costElementNode = null;
                    if (costElementFound == null) {
                        costElementNode = ServiceUnitDAO.persistServiceUnit(serviceUnit, database);
                        Relationship relationship = cloudProviderNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.providesServiceUnit);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        costElementNode = database.getNodeById(costElementFound.getId());
                        //do not duplicate relationship for added service units. from cloud

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
