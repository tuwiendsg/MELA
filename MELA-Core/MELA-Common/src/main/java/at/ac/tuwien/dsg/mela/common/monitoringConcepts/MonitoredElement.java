/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
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
package at.ac.tuwien.dsg.mela.common.monitoringConcepts;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import java.io.Serializable;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElement")
public class MonitoredElement implements Iterable<MonitoredElement>, Serializable {

    @XmlAttribute(name = "id", required = true)
    private String id;

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "level", required = true)
    private MonitoredElementLevel level;

    @XmlElement(name = "MonitoredElement", required = false)
    private Collection<MonitoredElement> containedElements;

    @XmlElement(name = "Relationship", required = false)
    private Collection<Relationship> relationships;

    /**
     * Represent the configuration of the cloud offered services hosting this
     * monitored element (if any): I.E. VM + OS + Monitoring
     */
    @XmlElement(name = "UsedCloudOfferedServiceCfg", required = false)
    private List<UsedCloudOfferedService> cloudOfferedServices;

    public static final long serialVersionUID = -4788504262348634847L;

    {
        relationships = new ArrayList<Relationship>();
        containedElements = new ArrayList<MonitoredElement>();
        cloudOfferedServices = new ArrayList<UsedCloudOfferedService>();
    }

    public MonitoredElement() {
    }

    public MonitoredElement(String id) {
        this.id = id;
        this.name = id;
    }

    public List<UsedCloudOfferedService> getCloudOfferedServices() {
        return cloudOfferedServices;
    }

    public void setCloudOfferedServices(List<UsedCloudOfferedService> cloudOfferedServices) {
        this.cloudOfferedServices = cloudOfferedServices;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MonitoredElement withId(String id) {
        this.id = id;
        return this;
    }

    public MonitoredElement withName(String name) {
        this.name = name;
        return this;
    }

    public MonitoredElement withLevel(MonitoredElementLevel level) {
        this.level = level;
        return this;
    }

    public void addElement(MonitoredElement MonitoredElement) {
        containedElements.add(MonitoredElement);
    }

    public void removeElement(MonitoredElement MonitoredElement) {
        containedElements.remove(MonitoredElement);
    }

    public void addUsedCloudOfferedService(UsedCloudOfferedService offeredService) {
        cloudOfferedServices.add(offeredService);
    }

    public void removeUsedCloudOfferedService(UsedCloudOfferedService offeredService) {
        cloudOfferedServices.remove(offeredService);
    }

    public MonitoredElementLevel getLevel() {
        return level;
    }

    public void setLevel(MonitoredElementLevel level) {
        this.level = level;
    }

    public Collection<MonitoredElement> getContainedElements() {
        return containedElements;
    }

    public void setContainedElements(Collection<MonitoredElement> containedElements) {
        this.containedElements = containedElements;
    }

    public Collection<Relationship> getRelationships() {
        return relationships;
    }

    public Collection<Relationship> getRelationships(Relationship.RelationshipType relationshipType) {
        Collection<Relationship> rels = new ArrayList<Relationship>();
        for (Relationship r : relationships) {
            if (r.getType().equals(relationshipType)) {
                rels.add(r);
            }
        }
        return rels;
    }

    public void setRelationships(Collection<Relationship> relationships) {
        this.relationships = relationships;
    }

    public void addRelationship(Relationship relationship) {
        relationships.add(relationship);
    }

    public void removeRelationship(Relationship relationship) {
        relationships.remove(relationship);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "MonitoredElementLevel")
    @XmlEnum
    public enum MonitoredElementLevel implements Serializable {

        @XmlEnumValue("SERVICE")
        SERVICE,
        @XmlEnumValue("SERVICE_TOPOLOGY")
        SERVICE_TOPOLOGY,
        @XmlEnumValue("SERVICE_UNIT")
        SERVICE_UNIT,
        //TODO: rename from VM to CloudOfferedService, introduce category and subcategory, etc. 
        @XmlEnumValue("VM")
        VM,
        @XmlEnumValue("VIRTUAL_CLUSTER")
        VIRTUAL_CLUSTER
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MonitoredElement that = (MonitoredElement) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }

        //TODO: add also level when checking for equality
        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * @return BREADTH_FIRST iterator
     */
    public Iterator<MonitoredElement> iterator() {
        return new ApplicationComponentIterator(this);
    }

    //traverses the monitored data tree in a breadth-first manner
    public static class ApplicationComponentIterator implements Iterator<MonitoredElement> {

        //        private ApplicationNodeMonitoredData root;
        private List<MonitoredElement> elements;

        private Iterator<MonitoredElement> elementsIterator;

        {
            //todo why is this type of initialization used?
            elements = new ArrayList<MonitoredElement>();
        }

        public ApplicationComponentIterator() {
        }

        private ApplicationComponentIterator(MonitoredElement root) {
//            this.root = root;

            //breadth-first tree traversal to create hierarchical tree structure
            List<MonitoredElement> applicationNodeMonitoredDataList = new ArrayList<MonitoredElement>();

            applicationNodeMonitoredDataList.add(root);
            elements.add(root);

            while (!applicationNodeMonitoredDataList.isEmpty()) {
                MonitoredElement data = applicationNodeMonitoredDataList.remove(0);

                for (MonitoredElement subData : data.getContainedElements()) {
                    applicationNodeMonitoredDataList.add(subData);
                    elements.add(subData);
                }
            }
            elementsIterator = elements.iterator();

        }

        public boolean hasNext() {
            return elementsIterator.hasNext();
        }

        public MonitoredElement next() {
            return elementsIterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("Unsupported yet");
        }
    }

    /**
     * @return a deep clone of all the serviceStructure element, EXCEPT VM
     * LEVEL, as VM level is considered volatile and updated by the monitoring
     * system.
     */
    public MonitoredElement clone() {
        MonitoredElement newMonitoredElement = new MonitoredElement();
        newMonitoredElement.level = level;
        newMonitoredElement.id = id;
        newMonitoredElement.name = name;

        Collection<MonitoredElement> elements = new ArrayList<MonitoredElement>();

        //do not clone VM level. That is retrieved and updated from monitoring system
        for (MonitoredElement el : containedElements) {
//            if (el.getLevel() != MonitoredElementLevel.VM) {
            elements.add(el.clone());
//            }
        }

        newMonitoredElement.cloudOfferedServices = cloudOfferedServices;
        newMonitoredElement.containedElements = elements;
        return newMonitoredElement;

    }

    // fluent interface methods
    @Override
    public String toString() {
        return "MonitoredElement{"
                + ", id='" + id + '\''
                + ", level=" + level
                + ", name='" + name + '\''
                //                + "containedElements=" + containedElements
                + '}';
    }

    public MonitoredElement withContainedElements(final Collection<MonitoredElement> containedElements) {
        this.containedElements = containedElements;
        return this;
    }

    public MonitoredElement withRelationships(final Collection<Relationship> relationships) {
        this.relationships = relationships;
        return this;
    }

    public MonitoredElement withCloudOfferedServices(final List<UsedCloudOfferedService> cloudOfferedServices) {
        this.cloudOfferedServices = cloudOfferedServices;
        return this;
    }

}
