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
package at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.utils.xml.mappers.PropertiesAdapter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "UsedCloudOfferedServiceCfg")
public class UsedCloudOfferedService implements Serializable {

    @XmlAttribute(name = "cloudProviderID", required = true)
    private UUID cloudProviderID;

    @XmlAttribute(name = "cloudProviderName", required = true)
    private String cloudProviderName;

    @XmlAttribute(name = "uuid", required = true)
    private UUID id;

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlElement(name = "QualityProperties", required = false)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<Metric, MetricValue> qualityProperties;

    @XmlElement(name = "ResourceProperties", required = false)
    @XmlJavaTypeAdapter(PropertiesAdapter.class)
    private Map<Metric, MetricValue> resourceProperties;

    public static final long serialVersionUID = -478804262348634847L;

    {
        qualityProperties = new HashMap<Metric, MetricValue>();
        resourceProperties = new HashMap<Metric, MetricValue>();
    }

    public UsedCloudOfferedService() {
    }

    public UsedCloudOfferedService(UUID cloudProviderID, String cloudProviderName, UUID id, String name) {
        this.cloudProviderID = cloudProviderID;
        this.cloudProviderName = cloudProviderName;
        this.id = id;
        this.name = name;
    }

    public UUID getCloudProviderID() {
        return cloudProviderID;
    }

    public void setCloudProviderID(UUID cloudProviderID) {
        this.cloudProviderID = cloudProviderID;
    }

    public String getCloudProviderName() {
        return cloudProviderName;
    }

    public void setCloudProviderName(String cloudProviderName) {
        this.cloudProviderName = cloudProviderName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UsedCloudOfferedService withName(String name) {
        this.name = name;
        return this;
    }

    public Map<Metric, MetricValue> getQualityProperties() {
        return qualityProperties;
    }

    public void setQualityProperties(Map<Metric, MetricValue> qualityProperties) {
        this.qualityProperties = qualityProperties;
    }

    public Map<Metric, MetricValue> getResourceProperties() {
        return resourceProperties;
    }

    public void setResourceProperties(Map<Metric, MetricValue> resourceProperties) {
        this.resourceProperties = resourceProperties;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final UsedCloudOfferedService other = (UsedCloudOfferedService) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        if (this.qualityProperties != other.qualityProperties && (this.qualityProperties == null || !this.qualityProperties.equals(other.qualityProperties))) {
            return false;
        }
        if (this.resourceProperties != other.resourceProperties && (this.resourceProperties == null || !this.resourceProperties.equals(other.resourceProperties))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public UsedCloudOfferedService withCloudProviderID(final UUID cloudProviderID) {
        this.cloudProviderID = cloudProviderID;
        return this;
    }

    public UsedCloudOfferedService withCloudProviderName(final String cloudProviderName) {
        this.cloudProviderName = cloudProviderName;
        return this;
    }

    public UsedCloudOfferedService withId(final UUID id) {
        this.id = id;
        return this;
    }

    public UsedCloudOfferedService withQualityProperties(final Map<Metric, MetricValue> qualityProperties) {
        this.qualityProperties = qualityProperties;
        return this;
    }

    public UsedCloudOfferedService withResourceProperties(final Map<Metric, MetricValue> resourceProperties) {
        this.resourceProperties = resourceProperties;
        return this;
    }

}
