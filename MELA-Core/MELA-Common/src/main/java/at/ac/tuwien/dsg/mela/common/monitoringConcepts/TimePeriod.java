/*
 *  Copyright 2015 Technische Universitat Wien (TUW), Distributed Systems Group E184
 * 
 *  This work was partially supported by the European Commission in terms of the 
 *  CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package at.ac.tuwien.dsg.mela.common.monitoringConcepts;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "TimePeriod")
@XmlEnum
public enum TimePeriod implements Serializable {

    @XmlEnumValue("s")
    SECOND("s"),
    @XmlEnumValue("m")
    MINUTE("m"),
    @XmlEnumValue("h")
    HOUR("h"),
    @XmlEnumValue("d")
    DAY("d");

    private final String period;

    TimePeriod(String period) {
        this.period = period;
    }

    @Override
    public String toString() {
        return period;
    }

    public boolean equals(String string) {
        return this.period.equals(string);
    }

    public Long toSeconds() {

        Long time = 0l;
        switch (period) {
            case "s":
                time = 1l;
                break;
            case "m":
                time = 60l;
                break;
            case "h":
                time = 3600l;
                break;
            case "d":
                time = 86400l;
                break;
        }

        return time;
    }

    public static TimePeriod fromString(String timePeriod) {
        TimePeriod period = TimePeriod.SECOND;
        switch (timePeriod) {
            case "s":
                period = TimePeriod.SECOND;
                break;
            case "m":
                period = TimePeriod.MINUTE;
                break;
            case "h":
                period = TimePeriod.HOUR;
                break;
            case "d":
                period = TimePeriod.DAY;
                break;
            default:
                LoggerFactory.getLogger(TimePeriod.class).error("String " + timePeriod
                        + " could not be converted to TimePeriod. Assuming its per second");
        }

        return period;
    }

}
