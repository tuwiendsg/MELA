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
package at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Configuration")
public class ConfigurationXMLRepresentation implements Serializable {

    @XmlElement(name = "ServiceStructure", required = false)
    private MonitoredElement serviceConfiguration;

    @XmlElement(name = "CompositionRulesConfiguration", required = false)
    private CompositionRulesConfiguration compositionRulesConfiguration;

    @XmlElement(name = "Requirements", required = false)
    private Requirements requirements;

    public MonitoredElement getServiceConfiguration() {
        return serviceConfiguration;
    }

    public void setServiceConfiguration(MonitoredElement serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    public CompositionRulesConfiguration getCompositionRulesConfiguration() {
        return compositionRulesConfiguration;
    }

    public void setCompositionRulesConfiguration(
            CompositionRulesConfiguration compositionRulesConfiguration) {
        this.compositionRulesConfiguration = compositionRulesConfiguration;
    }

    public Requirements getRequirements() {
        return requirements;
    }

    public void setRequirements(Requirements requirements) {
        this.requirements = requirements;
    }

    public ConfigurationXMLRepresentation(MonitoredElement serviceConfiguration,
            CompositionRulesConfiguration compositionRulesConfiguration,
            Requirements requirements) {
        super();
        this.serviceConfiguration = serviceConfiguration;
        this.compositionRulesConfiguration = compositionRulesConfiguration;
        this.requirements = requirements;
    }

    public ConfigurationXMLRepresentation() {

    }

    public ConfigurationXMLRepresentation withServiceConfiguration(final MonitoredElement serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
        return this;
    }

    public ConfigurationXMLRepresentation withCompositionRulesConfiguration(final CompositionRulesConfiguration compositionRulesConfiguration) {
        this.compositionRulesConfiguration = compositionRulesConfiguration;
        return this;
    }

    public ConfigurationXMLRepresentation withRequirements(final Requirements requirements) {
        this.requirements = requirements;
        return this;
    }

}
