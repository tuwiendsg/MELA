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

import java.util.Collection;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementElasticityDependencies")
public class MonitoredElementElasticityDependencies {

    @XmlAttribute(name = "TimestampID", required = true)
    private int timestampID;

    @XmlElement(name = "MonitoredElementElasticityDependency", required = true)
    private Collection<MonitoredElementElasticityDependency> elasticityDependencies;

    public Collection<MonitoredElementElasticityDependency> getElasticityDependencies() {
        return elasticityDependencies;
    }

    public void setElasticityDependencies(Collection<MonitoredElementElasticityDependency> elasticityDependencies) {
        this.elasticityDependencies = elasticityDependencies;
    }

    public int getTimestampID() {
        return timestampID;
    }

    public void setTimestampID(int timestampID) {
        this.timestampID = timestampID;
    }

}
