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
public class MetricInfo {

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

    @XmlAttribute(name = "MonitoredElementLevel")
    private String monitoredElementLevel;

    @XmlAttribute(name = "MonitoredElementID")
    private String monitoredElementID;

    private Object convertedValue;

    public static final MetricInfo UNDEFINED = new MetricInfo();

    static {
        UNDEFINED.value = "-1";
    }

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
                return new Float(Float.NaN);
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

    public void setConvertedValue(Object convertedValue) {
        this.convertedValue = convertedValue;
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

    public MetricInfo withName(final String name) {
        this.name = name;
        return this;
    }

    public MetricInfo withValue(final String value) {
        this.value = value;
        return this;
    }

    public MetricInfo withType(final String type) {
        this.type = type;
        return this;
    }

    public MetricInfo withUnits(final String units) {
        this.units = units;
        return this;
    }

    public MetricInfo withConvertedValue(final Object convertedValue) {
        this.convertedValue = convertedValue;
        return this;
    }

    public MetricInfo withMonitoredElementLevel(final String monitoredElementLevel) {
        this.monitoredElementLevel = monitoredElementLevel;
        return this;
    }

    public MetricInfo withMonitoredElementID(final String monitoredElementID) {
        this.monitoredElementID = monitoredElementID;
        return this;
    }

    public boolean hasMonitoredElementID() {
        return monitoredElementID != null && monitoredElementID.length() > 0;
    }

    public boolean hasMonitoredElementLevel() {
        return monitoredElementLevel != null && monitoredElementLevel.length() > 0;
    }

}
