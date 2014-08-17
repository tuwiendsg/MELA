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

package at.ac.tuwien.dsg.mela.common.configuration.metricComposition;

import java.io.Serializable;
import java.util.ArrayList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MetricsCompositionRules")
public class CompositionRulesBlock implements Serializable{

    {
        compositionRules = new ArrayList<CompositionRule>();
    }
    @XmlElement(name = "CompositionRule", required = true)
    private ArrayList<CompositionRule> compositionRules;

    public CompositionRulesBlock() {
        this.compositionRules = new ArrayList<CompositionRule>();
    }

    public ArrayList<CompositionRule> getCompositionRules() {
        return compositionRules;
    }

    public void setCompositionRules(ArrayList<CompositionRule> compositionRules) {
        this.compositionRules = compositionRules;
    }

    public void addCompositionRule(CompositionRule compositionRule) {
        this.compositionRules.add(compositionRule);
    }

    public void removeCompositionRule(CompositionRule compositionRule) {
        this.compositionRules.remove(compositionRule);
    }

    @Override
    public int hashCode() {
        return 3;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompositionRulesBlock other = (CompositionRulesBlock) obj;
        if (this.compositionRules != other.compositionRules && (this.compositionRules == null || !this.compositionRules.equals(other.compositionRules))) {
            return false;
        }
        return true;
    }
}
