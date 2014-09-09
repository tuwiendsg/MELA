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
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Metric")
public class Metric implements Serializable {

    @XmlAttribute(name = "name", required = true)
    private String name;
    @XmlAttribute(name = "measurementUnit", required = true)
    private String measurementUnit;
    @XmlAttribute(name = "type", required = true)
    private MetricType type = MetricType.RESOURCE;
    @XmlAttribute(name = "MonitoredElementLevel")
    private String monitoredElementLevel;

    @XmlAttribute(name = "MonitoredElementID")
    private String monitoredElementID;

//    @XmlElement(name = "MonitoredElement", required = false)
//    private MonitoredElement monitoredElement;
    public Metric(String name, String measurementUnit) {
        this.name = name;
        this.measurementUnit = measurementUnit;
    }

    public Metric(String name, String measurementUnit, MetricType type) {
        this.name = name;
        this.measurementUnit = measurementUnit;
        this.type = type;
    }

    public Metric(String name, String measurementUnit, String monitoredElementLevel, String monitoredElementID, MetricType type) {
        this.name = name;
        this.measurementUnit = measurementUnit;
        this.monitoredElementLevel = monitoredElementLevel;
        this.monitoredElementID = monitoredElementID;
        this.type = type;
    }

    public Metric withMonitoredElementLevel(final String monitoredElementLevel) {
        this.monitoredElementLevel = monitoredElementLevel;
        return this;
    }

    public Metric withMonitoredElementID(final String monitoredElementID) {
        this.monitoredElementID = monitoredElementID;
        return this;
    }

    public boolean hasMonitoredElementID() {
        return monitoredElementID != null && monitoredElementID.length() > 0;
    }

    public boolean hasMonitoredElementLevel() {
        return monitoredElementLevel != null && monitoredElementLevel.length() > 0;
    }

    public Metric(String name) {
        this.name = name;
    }

    public MetricType getType() {
        return type;
    }

    public void setType(MetricType type) {
        this.type = type;
    }

    public Metric() {
    }

    public String getName() {
        return name;
    }

    public String getMeasurementUnit() {
        return measurementUnit;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

//    public MonitoredElement getMonitoredElement() {
//        return monitoredElement;
//    }
//
//    public void setMonitoredElement(MonitoredElement monitoredElement) {
//        this.monitoredElement = monitoredElement;
//    }
//    
//    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Metric other = (Metric) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Metric{"
                + "name='" + name + '\''
                + ", measurementUnit='" + measurementUnit + '\''
                + '}';
    }

    public Metric clone() {
        return new Metric(name, measurementUnit, monitoredElementLevel, monitoredElementID, type);
    }
    public static final String PERCENTAGE = "%";
    public static final String EURO = "euro";
    public static final String DOLLARS = "dollars";
    public static final String MEGA_BYTES = "MB";
    public static final String GIGA_BYTES = "GB";
    public static final String VIRTUAL_CPU = "V_CPU";
    public static final String TYPE = "ValueType";
    public static final String MONTH_HOURS = "hours/month";
    public static final String MONTHLY = "Month";
    public static final String HOURLY = "Hour";
    public static final String LEVEL = "Level";

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "Metric")
    @XmlEnum
    public enum MetricType implements Serializable {

        @XmlEnumValue("RESOURCE")
        RESOURCE,
        @XmlEnumValue("COST")
        COST,
        @XmlEnumValue("QUALITY")
        QUALITY,
        //describes the elasticity of a monitored element
        @XmlEnumValue("ELASTICITY")
        ELASTICITY
    }

    public Metric withName(final String name) {
        this.name = name;
        return this;
    }

    public Metric withMeasurementUnit(final String measurementUnit) {
        this.measurementUnit = measurementUnit;
        return this;
    }

    public Metric withType(final MetricType type) {
        this.type = type;
        return this;
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

}
