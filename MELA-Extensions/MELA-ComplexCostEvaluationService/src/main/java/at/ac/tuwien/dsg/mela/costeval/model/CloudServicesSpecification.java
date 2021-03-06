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
package at.ac.tuwien.dsg.mela.costeval.model;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CloudServices")
public class CloudServicesSpecification {

    @XmlElement(name = "CloudProvider", required = false)
    private List<CloudProvider> cloudProviders;

    {
        cloudProviders = new ArrayList<CloudProvider>();
    }

    public CloudServicesSpecification withCloudProviders(final List<CloudProvider> cloudProviders) {
        this.cloudProviders = cloudProviders;
        return this;
    }

    public CloudServicesSpecification withCloudProvider(final CloudProvider cloudProvider) {
        this.cloudProviders.add(cloudProvider);
        return this;
    }

    public List<CloudProvider> getCloudProviders() {
        return cloudProviders;
    }

    public void setCloudProviders(List<CloudProvider> cloudProviders) {
        this.cloudProviders = cloudProviders;
    }

    public void addCloudProvider(CloudProvider cloudProvider) {
        this.cloudProviders.add(cloudProvider);
    }

    public void removeCloudProvider(CloudProvider cloudProvider) {
        this.cloudProviders.remove(cloudProvider);
    }

}
