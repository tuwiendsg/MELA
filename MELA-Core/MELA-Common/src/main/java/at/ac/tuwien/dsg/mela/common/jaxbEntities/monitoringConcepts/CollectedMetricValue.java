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

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import javax.xml.bind.annotation.*;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Metric")
public class CollectedMetricValue {

    @XmlAttribute(name = "Name", required = true)
    private String name;

    /**
     * float, double, int, long, or if left empty is is "text" Type means
     * primitive type
     */
    @XmlAttribute(name = "Value", required = true)
    private String value;
    @XmlAttribute(name = "Type", required = true)
    private String type;
    @XmlAttribute(name = "Units")
    private String units;

    @XmlAttribute(name = "Age")
    //timestamp at which metric was collected
    private String timeSinceCollection;

    @XmlAttribute(name = "MonitoredElementLevel")
    private String monitoredElementLevel;

    @XmlAttribute(name = "MonitoredElementID")
    private String monitoredElementID;

    public static final CollectedMetricValue UNDEFINED = new CollectedMetricValue();

    static {
        UNDEFINED.value = "-1";
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 67 * hash + (this.value != null ? this.value.hashCode() : 0);
        hash = 67 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 67 * hash + (this.units != null ? this.units.hashCode() : 0);
        hash = 67 * hash + (this.monitoredElementLevel != null ? this.monitoredElementLevel.hashCode() : 0);
        hash = 67 * hash + (this.monitoredElementID != null ? this.monitoredElementID.hashCode() : 0);
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
        final CollectedMetricValue other = (CollectedMetricValue) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
//        if ((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
//            return false;
//        }
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        if ((this.units == null) ? (other.units != null) : !this.units.equals(other.units)) {
            return false;
        }
        if ((this.monitoredElementLevel == null) ? (other.monitoredElementLevel != null) : !this.monitoredElementLevel.equals(other.monitoredElementLevel)) {
            return false;
        }
        if ((this.monitoredElementID == null) ? (other.monitoredElementID != null) : !this.monitoredElementID.equals(other.monitoredElementID)) {
            return false;
        }
        return true;
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

    public String getTimeSinceCollection() {
        return timeSinceCollection;
    }

    public void setTimeSinceCollection(String timeSinceCollection) {
        this.timeSinceCollection = timeSinceCollection;
    }

    /**
     * @return Float, Integer or String representations of the value stored as
     * String by Ganglia
     */
    public Object getConvertedValue() {

        if (value.equals(UNDEFINED.value)) {
            return MetricValue.UNDEFINED.getValue();
        } else if (type.toLowerCase().contains("float") || type.toLowerCase().contains("double")) {
            try {
                if (value == null) {
                    return new Float(0);
                } else {
                    return Float.parseFloat(value);
                }
            } catch (NumberFormatException e) {
                return new Float(Float.NaN);
            }
        } else if (type.toLowerCase().contains("int")) {
            try {
                if (value == null) {
                    return new Integer(0);
                } else {
                    return Integer.parseInt(value);
                }
            } catch (NumberFormatException e) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    return new Float(Float.NaN);
                }
            }
        } else if (type.toLowerCase().contains("long")) {
            try {
                if (value == null) {
                    return new Long(0);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                return new Float(Float.NaN);
            }
        } else {
            if (value == null) {
                return "";
            } else {
                return value;
            }
        }

    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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

    @Override
    public String toString() {
        return "GangliaMetricInfo{"
                + "name='" + name + '\''
                + ", value='" + value + '\''
                + ", type='" + type + '\''
                + ", units='" + units + '\''
                + "}";
    }

    public CollectedMetricValue withName(final String name) {
        this.name = name;
        return this;
    }

    public CollectedMetricValue withValue(final String value) {
        this.value = value;
        return this;
    }

    public CollectedMetricValue withType(final String type) {
        this.type = type;
        return this;
    }

    public CollectedMetricValue withUnits(final String units) {
        this.units = units;
        return this;
    }

    public CollectedMetricValue withMonitoredElementLevel(final String monitoredElementLevel) {
        this.monitoredElementLevel = monitoredElementLevel;
        return this;
    }

    public CollectedMetricValue withMonitoredElementID(final String monitoredElementID) {
        this.monitoredElementID = monitoredElementID;
        return this;
    }

    public boolean hasMonitoredElementID() {
        return monitoredElementID != null && monitoredElementID.length() > 0;
    }

    public boolean hasMonitoredElementLevel() {
        return monitoredElementLevel != null && monitoredElementLevel.length() > 0;
    }

    public CollectedMetricValue withTimeSinceCollection(final String timeSinceCollection) {
        this.timeSinceCollection = timeSinceCollection;
        return this;
    }

}
