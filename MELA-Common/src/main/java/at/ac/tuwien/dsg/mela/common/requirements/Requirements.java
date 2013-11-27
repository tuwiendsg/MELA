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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Requirements")
public class Requirements {

    @XmlAttribute(name = "TargetServiceID", required = true)
    private String targetServiceID;
    @XmlAttribute(name = "Name", required = true)
    private String name;

    {
        name = UUID.randomUUID().toString();
    }
    @XmlElement(name = "Requirement")
    private List<Requirement> requirements;

    {
        requirements = new ArrayList<Requirement>();
    }

    public String getTargetServiceID() {
        return targetServiceID;
    }

    public void setTargetServiceID(String targetServiceID) {
        this.targetServiceID = targetServiceID;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<Requirement> requirements) {
        this.requirements = requirements;
    }

    public void addRequirement(Requirement requirement) {
        this.requirements.add(requirement);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void deleteRequirement(Requirement requirement) {
        this.requirements.remove(requirement);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
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
        final Requirements other = (Requirements) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Requirements{" + "name=" + name + '}';
    }
    
    public Requirements clone(){
        Requirements r = new Requirements();
        r.targetServiceID = targetServiceID;
        r.name = name;
        for(Requirement req: requirements){
            r.addRequirement(req.clone());
        }
        return r;
    }
}
