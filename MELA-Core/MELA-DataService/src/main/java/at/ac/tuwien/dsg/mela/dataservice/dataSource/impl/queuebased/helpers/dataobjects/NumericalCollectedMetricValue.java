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
package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.CollectedMetricValue;
import java.io.Serializable;
import javax.xml.bind.annotation.*;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SerializableCollectedMetricValue")
public class NumericalCollectedMetricValue implements Serializable {
    
    @XmlAttribute(name = "Name", required = true)
    private String name;
    
    @XmlAttribute(name = "Value", required = true)
    private Double value;
    
    @XmlAttribute(name = "Type", required = true)
    private String type;
    
    @XmlAttribute(name = "Units")
    private String units;
    
    @XmlAttribute(name = "MonitoredElementLevel")
    private String monitoredElementLevel;
    
    @XmlAttribute(name = "MonitoredElementID")
    private String monitoredElementID;
    
    @XmlAttribute(name = "TimeSinceCollection")
    //in seconds
    private long timeSinceCollection;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getMonitoredElementLevel() {
        return monitoredElementLevel;
    }
    
    public void setMonitoredElementLevel(String monitoredElementLevel) {
        this.monitoredElementLevel = monitoredElementLevel;
    }
    
    public String getMonitoredElementID() {
        return monitoredElementID;
    }
    
    public void setMonitoredElementID(String monitoredElementID) {
        this.monitoredElementID = monitoredElementID;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUnits() {
        return units;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public long getTimeSinceCollection() {
        return timeSinceCollection;
    }
    
    public void setTimeSinceCollection(long timeSinceCollection) {
        this.timeSinceCollection = timeSinceCollection;
    }
    
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "SerializableCollectedMetricValue{"
                + "name='" + name + '\''
                + ", value='" + value + '\''
                + ", type='" + type + '\''
                + ", units='" + units + '\''
                + ", MonitoredElementLevel='" + monitoredElementLevel + '\''
                + ", MonitoredElementID='" + monitoredElementID + '\''
                + "}";
    }
    
    public NumericalCollectedMetricValue withName(final String name) {
        this.name = name;
        return this;
    }
    
    public NumericalCollectedMetricValue withType(final String type) {
        this.type = type;
        return this;
    }
    
    public NumericalCollectedMetricValue withUnits(final String units) {
        this.units = units;
        return this;
    }
    
    public NumericalCollectedMetricValue withMonitoredElementLevel(final String monitoredElementLevel) {
        this.monitoredElementLevel = monitoredElementLevel;
        return this;
    }
    
    public NumericalCollectedMetricValue withMonitoredElementID(final String monitoredElementID) {
        this.monitoredElementID = monitoredElementID;
        return this;
    }
    
    public boolean hasMonitoredElementID() {
        return monitoredElementID != null && monitoredElementID.length() > 0;
    }
    
    public boolean hasMonitoredElementLevel() {
        return monitoredElementLevel != null && monitoredElementLevel.length() > 0;
    }
    
    public NumericalCollectedMetricValue withValue(final Double value) {
        this.value = value;
        return this;
    }
    
    public NumericalCollectedMetricValue withTimeSinceCollection(final long timeSinceCollection) {
        this.timeSinceCollection = timeSinceCollection;
        return this;
    }
    
    public static NumericalCollectedMetricValue from(CollectedMetricValue cmv) {
        
        NumericalCollectedMetricValue value = new NumericalCollectedMetricValue();
        value.monitoredElementLevel = cmv.getMonitoredElementLevel();
        value.monitoredElementID = cmv.getMonitoredElementID();
        value.name = cmv.getName();
        value.type = cmv.getType();
        value.units = cmv.getUnits();
        value.timeSinceCollection = Long.parseLong(cmv.getTimeSinceCollection());

        //eat the exception, if the converted value is not long, is null
        try {
            value.value = ((Number) cmv.getConvertedValue()).doubleValue();
        } catch (Exception e) {
            return null;
        }
        
        return value;
    }
    
    public CollectedMetricValue toCollectedMetricValue() {
        
        CollectedMetricValue collectedMetricValue = new CollectedMetricValue()
                .withMonitoredElementLevel(this.getMonitoredElementLevel())
                .withMonitoredElementID(this.getMonitoredElementID())
                .withName(this.getName())
                .withType(this.getType())
                .withUnits(this.getUnits())
                .withTimeSinceCollection("" + this.getTimeSinceCollection())
                .withValue(this.getValue().toString());
        
        return collectedMetricValue;
    }
    
}
