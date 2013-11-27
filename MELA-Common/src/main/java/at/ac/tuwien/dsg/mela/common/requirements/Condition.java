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
package at.ac.tuwien.dsg.mela.common.requirements;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 * 
 * Used to define restrictions for serviceStructure selection criteria
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Condition")
public class Condition {

    @XmlAttribute(name = "ID", required = true)
    private String id;

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "ConditionType")
    @XmlEnum
    public enum Type {
        @XmlEnumValue("LESS_THAN")LESS_THAN, @XmlEnumValue("LESS_EQUAL")LESS_EQUAL, @XmlEnumValue("GREATER_THAN")GREATER_THAN,
        @XmlEnumValue("GREATER_EQUAL")GREATER_EQUAL, @XmlEnumValue("EQUAL")EQUAL, @XmlEnumValue("RANGE")RANGE, @XmlEnumValue("ENUMERATION")ENUMERATION
    }

    @XmlAttribute(name = "Type", required = true)
    private Type type;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    /**
     * Restriction values: for Numeric Restrictions  such as LESS_THAN, the "value" list has only one element.
     * For RANGE restriction, only first and last elements of "value" are considered to be the range margins
     * FOR ENUMERATION, value contains an enumeration of possible values.
     */


    @XmlElement(name = "MetricValue", required = false)
    private List<MetricValue> value;

    {
        value = new ArrayList<MetricValue>();
    }

    public Condition() {
    }


    public void setType(Type type) {
        this.type = type;
    }


    public void setValue(List<MetricValue> value) {
        this.value = value;
    }


    public void addValue(MetricValue v) {
        this.value.add(v);
    }

    public void removeValue(MetricValue v) {
        this.value.remove(v);
    }

    public Condition(Type type, Metric metric, MetricValue... value) {
        this.type = type;
        for (MetricValue element : value) {
            this.value.add(element);
        }
    }

    public Type getType() {
        return type;
    }

    /**
     * @param testedValue
     * @return Checks if the MetricValue respects the restriction.
     *         Used in SELECTING services (deciding if a serviceStructure respects all restriction criteria).
     */
    public boolean isRespectedByValue(MetricValue testedValue) {
        boolean respected = false;

        switch (this.type) {
            case LESS_THAN:
                //I want what I get to be smaller than what I request
                respected = (value.get(0).compareTo(testedValue) > 0);
                break;
            case LESS_EQUAL:
                respected = (value.get(0).compareTo(testedValue) >= 0);
                break;
            case GREATER_THAN:
                respected = (value.get(0).compareTo(testedValue) < 0);
                break;
            case GREATER_EQUAL:
                respected = (value.get(0).compareTo(testedValue) <= 0);
                break;
            case EQUAL:
                respected = (value.get(0).compareTo(testedValue) == 0);
                break;
            case RANGE:
                respected = ((value.get(0).compareTo(testedValue) >= 0))
                        && ((value.get(value.size() - 1).compareTo(testedValue) <= 0));
                break;
            case ENUMERATION:
                respected = value.contains(testedValue);
                break;
        }

        return respected;
    }



    public List<MetricValue> getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Condition condition = (Condition) o;

        if (id != null ? !id.equals(condition.id) : condition.id != null) return false;
        if (type != condition.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "" +
                " " + type +
                " " + value
                ;
    }
}
