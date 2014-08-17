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
package at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ElasticitySpace")
public class ElasticitySpaceXML {

    @XmlElement(name = "MonitoredElement")
    private MonitoredElement element;
    
    @XmlElement(name = "ElasticitySpaceDimension")
    private Collection<ElasticitySpaceDimensionXML> dimensions;
    
    {
        dimensions = new ArrayList<ElasticitySpaceDimensionXML>();
    }

    public Collection<ElasticitySpaceDimensionXML> getDimensions() {
        return dimensions;
    }

    public void setDimensions(Collection<ElasticitySpaceDimensionXML> dimensions) {
        this.dimensions = dimensions;
    }
    
    
    public void addDimensions(Collection<ElasticitySpaceDimensionXML> dimensions) {
        this.dimensions.addAll(dimensions);
    }
    
    public void addDimension(ElasticitySpaceDimensionXML dimension) {
        this.dimensions.add(dimension);
    }

    public MonitoredElement getElement() {
        return element;
    }

    public void setElement(MonitoredElement element) {
        this.element = element;
    }
    
    
}
