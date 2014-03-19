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
import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
/**
 * Contains a MonitoredElement ID and associated metrics. Used in the
 * ServiceMonitoringSnapshotṡ Represents also in XML a tree structure of
 * monitored data, containing for each node the MonitoredElement, a map of
 * monitored data, and a list of children monitoring datas.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementSnapshots")
public class MonitoredElementMonitoringSnapshots implements Serializable{

    @XmlElement(name = "MonitoredElementSnapshot")
    private Collection<MonitoredElementMonitoringSnapshot> children;

    {

        children = new ArrayList<MonitoredElementMonitoringSnapshot>();
    }

    public MonitoredElementMonitoringSnapshots() {
    }

    public synchronized Collection<MonitoredElementMonitoringSnapshot> getChildren() {
        return children;
    }

    public synchronized void setChildren(Collection<MonitoredElementMonitoringSnapshot> children) {
        this.children = children;
    }

    public synchronized void addChild(MonitoredElementMonitoringSnapshot child) {
        this.children.add(child);
    }

    public synchronized void removeChild(MonitoredElementMonitoringSnapshot child) {
        this.children.remove(child);
    }

}
