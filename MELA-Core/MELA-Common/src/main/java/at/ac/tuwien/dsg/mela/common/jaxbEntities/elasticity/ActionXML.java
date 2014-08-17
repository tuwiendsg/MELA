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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Action;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ActionsOnElement")
public class ActionXML {

    @XmlElement(name = "Action")
    private List<Action> actions;

    @XmlAttribute(name = "TargetServiceID", required = true)
    private String targetServiceID;

    {
        actions = new ArrayList<Action>();
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public void addActions(List<Action> actions) {
        this.actions.addAll(actions);
    }

    public void addAction(Action action) {
        this.actions.add(action);
    }

    public String getTargetServiceID() {
        return targetServiceID;
    }

    public void setTargetServiceID(String targetServiceID) {
        this.targetServiceID = targetServiceID;
    }

    public ActionXML withActions(final List<Action> actions) {
        this.actions = actions;
        return this;
    }

    public ActionXML withTargetServiceID(final String targetServiceID) {
        this.targetServiceID = targetServiceID;
        return this;
    }

}
