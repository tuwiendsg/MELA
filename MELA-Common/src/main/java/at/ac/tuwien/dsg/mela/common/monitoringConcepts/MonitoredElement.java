/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group E184
 *
 * This work was partially supported by the European Commission in terms of the CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
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

import java.io.Serializable;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElement")
public class MonitoredElement implements Iterable<MonitoredElement>, Serializable {

    @XmlAttribute(name = "id", required = true)
    private String id;
//    @XmlAttribute(name = "ip", required = false)
//    private String ip;
    @XmlAttribute(name = "name", required = true)
    private String name;
    @XmlAttribute(name = "level", required = true)
    private MonitoredElementLevel level;
    @XmlElement(name = "MonitoredElement", required = false)
    private Collection<MonitoredElement> containedElements;

    public MonitoredElement() {
        containedElements = new ArrayList<MonitoredElement>();
    }

    public MonitoredElement(String id) {
        containedElements = new ArrayList<MonitoredElement>();
        this.id = id;
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

    public void addElement(MonitoredElement MonitoredElement) {
        containedElements.add(MonitoredElement);
    }

    public void removeElement(MonitoredElement MonitoredElement) {
        containedElements.remove(MonitoredElement);
    }

//    public String getIp() {
//        return ip;
//    }
//
//    public void setIp(String ip) {
//        this.ip = ip;
//    }
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

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "MonitoredElementLevel")
    @XmlEnum
    public enum MonitoredElementLevel implements Serializable{
        @XmlEnumValue("SERVICE")
        SERVICE,
        @XmlEnumValue("SERVICE_TOPOLOGY")
        SERVICE_TOPOLOGY,
        @XmlEnumValue("SERVICE_UNIT")
        SERVICE_UNIT,
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
//        if (ip != null ? !ip.equals(that.ip) : that.ip != null) return false;
//        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
//        result = 31 * result + (ip != null ? ip.hashCode() : 0);
//        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    /**
     * @return BREADTH_FIRST iterator
     */
    @Override
    public Iterator<MonitoredElement> iterator() {
        return new ApplicationComponentIterator(this);
    }

    //traverses the monitored data tree in a breadth-first manner
    public class ApplicationComponentIterator implements Iterator<MonitoredElement> {

        //        private ApplicationNodeMonitoredData root;
        private List<MonitoredElement> elements;
        private Iterator<MonitoredElement> elementsIterator;

        {
            elements = new ArrayList<MonitoredElement>();
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

        @Override
        public boolean hasNext() {
            return elementsIterator.hasNext();
        }

        @Override
        public MonitoredElement next() {
            return elementsIterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Unsupported yet");
        }
    }

    /**
     *
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
        newMonitoredElement.containedElements = elements;
        return newMonitoredElement;

    }

    @Override
    public String toString() {
        return "MonitoredElement{"
                + ", id='" + id + '\''
                + ", level=" + level
                + ", name='" + name + '\''
                + "containedElements=" + containedElements
                + '}';
    }
    
     
}
