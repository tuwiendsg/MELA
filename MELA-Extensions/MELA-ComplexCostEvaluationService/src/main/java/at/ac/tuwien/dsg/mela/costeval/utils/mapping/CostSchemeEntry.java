/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.costeval.utils.mapping;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CostSchemeEntry")
public class CostSchemeEntry {

    @XmlElement(name = "monitoredElement")
    private MonitoredElement monitoredElement;
    @XmlElement(name = "value")
    private CostFunction costFunction;

    public CostSchemeEntry() {
    }

    public CostSchemeEntry(MonitoredElement monitoredElement, CostFunction costFunction) {
        this.monitoredElement = monitoredElement;
        this.costFunction = costFunction;
    }

    public MonitoredElement getMonitoredElement() {
        return monitoredElement;
    }

    public void setMonitoredElement(MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
    }

    public CostFunction getCostFunction() {
        return costFunction;
    }

    public void setCostFunction(CostFunction costFunction) {
        this.costFunction = costFunction;
    }

    public CostSchemeEntry withMonitoredElement(final MonitoredElement monitoredElement) {
        this.monitoredElement = monitoredElement;
        return this;
    }

    public CostSchemeEntry withCostFunction(final CostFunction costFunction) {
        this.costFunction = costFunction;
        return this;
    }
    

}
