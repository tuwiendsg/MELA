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
import java.text.DecimalFormat;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
//TODO: I should embed the XML operation processing engine also in requirements, to be able to say
// requiremment: cost < reqsNumber / 1000 * 0.01 (e.g., if I charge 0.01$ per 1000 requests, I want to have profit)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MetricValue")
public class MetricValue implements Comparable<MetricValue>, Serializable {

    //CloudCom 2014 experiments
    //private static final long serialVersionUID = -2661411865624134343l;
    
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "ValueType")
    @XmlEnum
    public enum ValueType implements Serializable {

        @XmlEnumValue("NUMERIC")
        NUMERIC, @XmlEnumValue("TEXT")
        TEXT, @XmlEnumValue("ENUMERATION")
        ENUM
    }

    private Double freshness;

    /**
     * In seconds.
     */
    private Long timeSinceCollection;

    public static final MetricValue UNDEFINED = new MetricValue(-1);

    @XmlElement(name = "Value", required = true)
    private Object value;

    @XmlAttribute(name = "ValueType", required = true)
    private ValueType valueType;

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public Long getTimeSinceCollection() {
        return timeSinceCollection;
    }

    public void setTimeSinceCollection(Long timeSinceCollection) {
        this.timeSinceCollection = timeSinceCollection;
    }

    public MetricValue(Object value) {
        this.value = value;
        if (value instanceof Number) {
            valueType = ValueType.NUMERIC;
        } else if (value instanceof String) {
            String string = value.toString();
            if (string.contains(",")) {
                valueType = ValueType.ENUM;
            } else {
                valueType = ValueType.TEXT;
            }
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.WARN, "Unknown elements: " + value);

        }
    }

    public MetricValue() {
    }

    public Object getValue() {
        return value;
    }

    public String getValueRepresentation() {
        if (value instanceof Number) {
            DecimalFormat df = new DecimalFormat("0.####");
            return df.format(value);
        } else if (value instanceof String) {
            return value.toString();
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.WARN, "Unknown elements: " + value);
            return "-1";
        }
    }

    public MetricValue clone() {
//        Object cloneValue = null;
//        if (  value instanceof Number) {
//            cloneValue = ((Number)value).doubleValue();
//        } else if  (value instanceof String) {
//            cloneValue =  value;
//        }
//        return new MetricValue(cloneValue);
        return new MetricValue(value).withCollectionTimestamp(timeSinceCollection).withFreshness(freshness);
    }

    public void setValue(Object value) {
        this.value = value;
        if (value instanceof Number) {
            valueType = ValueType.NUMERIC;
        } else if (value instanceof String) {
            String string = value.toString();
            if (string.contains(",")) {
                valueType = ValueType.ENUM;
            } else {
                valueType = ValueType.TEXT;
            }
        } else {
            Logger.getLogger(this.getClass().getName()).log(Level.WARN, "Unknown elements: " + value);
        }
    }

    public void sum(MetricValue metricValue) {
        if (value instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            double newVal = ((Number) metricValue.getValue()).doubleValue();
            this.value = oldVal + newVal;
        }
    }

    public void sub(MetricValue metricValue) {
        if (value instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            double newVal = ((Number) metricValue.getValue()).doubleValue();
            this.value = oldVal - newVal;
        }
    }

    public void sum(double metricValue) {
        if (value instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            this.value = oldVal + metricValue;
        }
    }

    public void sub(double metricValue) {
        if (value instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            this.value = oldVal - metricValue;
        }
    }

    public void divide(double size) {
        if (value instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            this.value = oldVal / size;

        }
    }

    public void multiply(double size) {
        if (value instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            this.value = oldVal * size;
        }
    }

    public void divide(MetricValue metricValue) {
        if (value instanceof Number && metricValue.getValue() instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            double metricValueVal = ((Number) metricValue.getValue()).doubleValue();

            this.value = oldVal / metricValueVal;
        }
    }

    public void multiply(MetricValue metricValue) {
        if (value instanceof Number && metricValue.getValue() instanceof Number) {
            double oldVal = ((Number) value).doubleValue();
            double metricValueVal = ((Number) metricValue.getValue()).doubleValue();

            this.value = oldVal * metricValueVal;
        }
    }

    /**
     * @param o
     * @return Used to compare different metric values. Currently is returns 0
     * if the values can't be compared. -1 if this smaller than argument, 1 if
     * greater, 0 if equal or can't compare
     */
    public int compareTo(MetricValue o) {

        Object otherValue = o.getValue();
        switch (valueType) {
            case NUMERIC:
                return new Double(((Number) value).doubleValue()).compareTo(((Number) otherValue).doubleValue());
            case TEXT:
                return ((String) value).compareTo((String) otherValue);
            case ENUM:
                return value.toString().compareTo(otherValue.toString());
            default:
                System.err.println("Incomparable elements: " + value + ", " + otherValue);
                return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetricValue that = (MetricValue) o;

        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    public MetricValue withValue(final Object value) {
        this.value = value;
        return this;
    }

    public MetricValue withValueType(final ValueType valueType) {
        this.valueType = valueType;
        return this;
    }

    public boolean isUndefined() {
        return this.value != null && this.value.toString().equals(UNDEFINED.getValue().toString());
    }

    public MetricValue withCollectionTimestamp(final Long collectionTimestamp) {
        this.timeSinceCollection = collectionTimestamp;
        return this;
    }

    public MetricValue withFreshness(final Double freshness) {
        this.freshness = freshness;
        return this;
    }

    public Double getFreshness() {
        return freshness;
    }

    public void setFreshness(Double freshness) {
        this.freshness = freshness;
    }

}
