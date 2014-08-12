/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.xmlAdapters;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Statistic")
public class Statistic {

    @XmlElement(name = "name")
    private String statisticName;
    @XmlElement(name = "value")
    private Double value;

    public Statistic() {
    }

    public Statistic(String statisticName, Double value) {
        this.statisticName = statisticName;
        this.value = value;
    }

    public Statistic withStatisticName(final String statisticName) {
        this.statisticName = statisticName;
        return this;
    }

    public Statistic withValue(final Double value) {
        this.value = value;
        return this;
    }

    public String getStatisticName() {
        return statisticName;
    }

    public void setStatisticName(String statisticName) {
        this.statisticName = statisticName;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

}
