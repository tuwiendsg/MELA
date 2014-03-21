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

import java.io.Serializable;
import javax.xml.bind.annotation.*;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Action")
public class Action implements Serializable {

    @XmlAttribute(name = "targetEntityID", required = true)
    private String targetEntityID;
    @XmlAttribute(name = "action", required = true)
    private String action;

    public Action() {
    }

    public Action(String targetEntityID, String action) {
        this.targetEntityID = targetEntityID;
        this.action = action;
    }

    public String getTargetEntityID() {
        return targetEntityID;
    }

    public void setTargetEntityID(String targetEntityID) {
        this.targetEntityID = targetEntityID;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.targetEntityID != null ? this.targetEntityID.hashCode() : 0);
        hash = 31 * hash + (this.action != null ? this.action.hashCode() : 0);
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
        final Action other = (Action) obj;
        if ((this.targetEntityID == null) ? (other.targetEntityID != null) : !this.targetEntityID.equals(other.targetEntityID)) {
            return false;
        }
        if ((this.action == null) ? (other.action != null) : !this.action.equals(other.action)) {
            return false;
        }
        return true;
    }
    
    
    
}
