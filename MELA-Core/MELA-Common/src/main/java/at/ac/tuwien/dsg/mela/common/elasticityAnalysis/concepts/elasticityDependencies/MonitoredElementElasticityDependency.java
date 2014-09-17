/*
 * Copyright 2014 daniel-tuwien.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementElasticityDependency")
public class MonitoredElementElasticityDependency {

    @XmlElement(name = "MonitoredElement", required = true)
    private MonitoredElement monitoredElement;

    /**
     * For each metric of the monitoredElement that depends on something, we
     * have an ElasticityDependencyElement
     */
    @XmlElement(name = "MonitoredElementElasticityDependency", required = false)
    private Collection<ElasticityDependencyElement> containedElements;

    {
        containedElements = new ArrayList<ElasticityDependencyElement>();
    }

    public MonitoredElementElasticityDependency() {
    }

    public MonitoredElementElasticityDependency(MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
    }

    public void addElement(ElasticityDependencyElement element) {
        containedElements.add(element);
    }

    public void removeElement(ElasticityDependencyElement element) {
        containedElements.remove(element);
    }

    public MonitoredElement getMonitoredElement() {
        return monitoredElement;
    }

    public void setMonitoredElement(MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
    }

    public Collection<ElasticityDependencyElement> getContainedElements() {
        return containedElements;
    }

    public void setContainedElements(Collection<ElasticityDependencyElement> containedElements) {
        this.containedElements = containedElements;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.monitoredElement != null ? this.monitoredElement.hashCode() : 0);
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
        final MonitoredElementElasticityDependency other = (MonitoredElementElasticityDependency) obj;
        if (this.monitoredElement != other.monitoredElement && (this.monitoredElement == null || !this.monitoredElement.equals(other.monitoredElement))) {
            return false;
        }
        return true;
    }

    public MonitoredElementElasticityDependency withMonitoredElement(final MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
        return this;
    }

    public MonitoredElementElasticityDependency withContainedElements(final Collection<ElasticityDependencyElement> containedElements) {
        this.containedElements = containedElements;
        return this;
    }
    
    

}
