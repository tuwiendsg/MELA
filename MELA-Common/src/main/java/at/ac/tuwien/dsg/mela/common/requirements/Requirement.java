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
package at.ac.tuwien.dsg.mela.common.requirements;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Requirement")
@XmlType(propOrder = {"metric", "conditions", "id", "targetMonitoredElementLevel", "targetMonitoredElementIDs"})
public class Requirement {

    @XmlElement(name = "TargetMetric", required = true)
    private Metric metric;
    @XmlElement(name = "Condition")
    private List<Condition> conditions;
    @XmlAttribute(name = "ID")
    private String id;
    @XmlAttribute(name = "TargetServiceLevel")
    private MonitoredElement.MonitoredElementLevel targetMonitoredElementLevel;
    @XmlElement(name = "TargetMonitoredElementID")
    private List<String> targetMonitoredElementIDs;
    
    @XmlAttribute(name = "name", required = true)
    private String name;
    
    
    {
        conditions = new ArrayList<Condition>();
        targetMonitoredElementIDs = new ArrayList<String>();
    }

    public Requirement() {
    }

    public Requirement(String name) {
        this.name = name;
    }

    
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    

    public MonitoredElement.MonitoredElementLevel getTargetMonitoredElementLevel() {
        return targetMonitoredElementLevel;
    }

    public void setTargetMonitoredElementLevel(MonitoredElement.MonitoredElementLevel targetMonitoredElementLevel) {
        this.targetMonitoredElementLevel = targetMonitoredElementLevel;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public List<String> getTargetMonitoredElementIDs() {
        return targetMonitoredElementIDs;
    }

    public void addTargetServiceID(String id) {
        this.targetMonitoredElementIDs.add(id);
    }

    public void removeTargetServiceID(String id) {
        this.targetMonitoredElementIDs.remove(id);
    }

    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }

    public void removeCondition(Condition condition) {
        this.conditions.remove(condition);
    }

    public void setTargetMonitoredElementIDs(List<String> targetMonitoredElementIDs) {
        this.targetMonitoredElementIDs = targetMonitoredElementIDs;
    }

    public Metric getMetric() {
        return metric;
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    @Override
    public String toString() {
        return "Requirement{" + "conditions=" + conditions + ", metric=" + metric + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + (this.metric != null ? this.metric.hashCode() : 0);
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
        final Requirement other = (Requirement) obj;
        if (this.metric != other.metric && (this.metric == null || !this.metric.equals(other.metric))) {
            return false;
        }
        return true;
    }
    
    public Requirement clone(){
        Requirement r = new Requirement(name);
        r.metric = metric;
        r.conditions = conditions;
        r.id = id;
        r.targetMonitoredElementLevel = targetMonitoredElementLevel;
        r.targetMonitoredElementIDs = targetMonitoredElementIDs;
        return r;
    }
    
    
}
