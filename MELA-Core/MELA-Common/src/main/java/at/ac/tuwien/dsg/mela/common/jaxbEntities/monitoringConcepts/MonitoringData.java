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
package at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.util.ArrayList;
import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at Contains
 * monitoring information collected for each MonitoredElement
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoringData")
public class MonitoringData {

    //monitoring information collected for each MonitoredElement
    @XmlElement(name = "MonitoredElementData")
    private List<MonitoredElementData> monitoredElementDatas;
    //currently not used. left here in case needed in the future
    @XmlAttribute(name = "Version")
    private String version;
    @XmlAttribute(name = "Source")
    private String source;
    @XmlAttribute(name = "Timestamp")
    private String timestamp;

    {
        monitoredElementDatas = new ArrayList<MonitoredElementData>();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Collection<MonitoredElementData> getMonitoredElementDatas() {
        return monitoredElementDatas;
    }

    public void setMonitoredElementDatas(List<MonitoredElementData> monitoredElementDatas) {
        this.monitoredElementDatas = monitoredElementDatas;
    }

    public void addMonitoredElementDatas(Collection<MonitoredElementData> monitoredElementDatas) {
        this.monitoredElementDatas.addAll(monitoredElementDatas);
    }

    public void addMonitoredElementData(MonitoredElementData monitoredElementData) {
        this.monitoredElementDatas.add(monitoredElementData);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public MonitoringData withMonitoredElementDatas(final List<MonitoredElementData> monitoredElementDatas) {
        this.monitoredElementDatas = monitoredElementDatas;
        return this;
    }

    public MonitoringData withVersion(final String version) {
        this.version = version;
        return this;
    }

    public MonitoringData withSource(final String source) {
        this.source = source;
        return this;
    }

    public MonitoringData withTimestamp(final String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public void withMetricInfo(CollectedMetricValue info) {
        MonitoredElement element = new MonitoredElement(info.getMonitoredElementID())
                .withLevel(MonitoredElement.MonitoredElementLevel.valueOf(info.getMonitoredElementLevel()));
        MonitoredElementData toCheck = new MonitoredElementData().withMonitoredElement(element);

        if (monitoredElementDatas.contains(toCheck)) {
            MonitoredElementData data = monitoredElementDatas.get(monitoredElementDatas.indexOf(toCheck));
            data.withMetricInfo(info);
        } else {
            toCheck.withMetricInfo(info);
            monitoredElementDatas.add(toCheck);
        }
    }

}
