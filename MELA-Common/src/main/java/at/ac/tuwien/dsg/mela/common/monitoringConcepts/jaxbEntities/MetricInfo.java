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
package at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "METRIC")
public class MetricInfo {

    @XmlAttribute(name = "NAME", required = true)
    private String name;
    @XmlAttribute(name = "VAL", required = true)
    private String value;
    @XmlAttribute(name = "TYPE", required = true)
    private String type;
    @XmlAttribute(name = "UNITS")
    private String units;
    @XmlAttribute(name = "TN")
    private String tn;
    @XmlAttribute(name = "TMAX")
    private String tmax;
    @XmlAttribute(name = "DMAX")
    private String dmax;
    @XmlAttribute(name = "SLOPE")
    private String slope;
    private Object convertedValue;
    @XmlAttribute(name = "SOURCE")
    private String source;
    @XmlElement(name = "EXTRA_DATA")
    private Collection<ExtraDataInfo> gangliaExtraDataInfoCollection;

    {
        gangliaExtraDataInfoCollection = new ArrayList<ExtraDataInfo>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Float, Integer or String representations of the value stored as
     * String by Ganglia
     */
    public Object getConvertedValue() {


        if (type.toLowerCase().contains("float") || type.toLowerCase().contains("double")) {
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

    public String getTn() {
        return tn;
    }

    public void setTn(String tn) {
        this.tn = tn;
    }

    public String getTmax() {
        return tmax;
    }

    public void setTmax(String tmax) {
        this.tmax = tmax;
    }

    public String getDmax() {
        return dmax;
    }

    public void setDmax(String dmax) {
        this.dmax = dmax;
    }

    public String getSlope() {
        return slope;
    }

    public void setSlope(String slope) {
        this.slope = slope;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Collection<ExtraDataInfo> getGangliaExtraDataInfoCollection() {
        return gangliaExtraDataInfoCollection;
    }

    public void setGangliaExtraDataInfoCollection(Collection<ExtraDataInfo> gangliaExtraDataInfoCollection) {
        this.gangliaExtraDataInfoCollection = gangliaExtraDataInfoCollection;
    }

    @Override
    public String toString() {
        String info = "GangliaMetricInfo{"
                + "name='" + name + '\''
                + ", value='" + value + '\''
                + ", type='" + type + '\''
                + ", units='" + units + '\''
                + ", tn='" + tn + '\''
                + ", tmax='" + tmax + '\''
                + ", dmax='" + dmax + '\''
                + ", slope='" + slope + '\''
                + ", source='" + source + '\''
                + ", gangliaExtraDataInfoCollection=";

        for (ExtraDataInfo dataInfo : gangliaExtraDataInfoCollection) {
            info += "\t " + dataInfo.toString() + "\n";
        }
        info += '}';
        return info;
    }
}
