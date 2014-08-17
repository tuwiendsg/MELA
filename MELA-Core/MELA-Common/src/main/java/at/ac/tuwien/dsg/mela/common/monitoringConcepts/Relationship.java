/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.common.monitoringConcepts;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Relationship")
public class Relationship {

    @XmlElement(name = "metric")
    private RelationshipType type;

    @XmlElement(name = "From")
    private MonitoredElement from;
    @XmlElement(name = "To")
    private MonitoredElement to;

    public enum RelationshipType {

        ConnectedTo("CONNECT_TO"),
        HostedOn("HOSTED_ON");

        private String type;

        RelationshipType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }

    public MonitoredElement getFrom() {
        return from;
    }

    public void setFrom(MonitoredElement from) {
        this.from = from;
    }

    public MonitoredElement getTo() {
        return to;
    }

    public void setTo(MonitoredElement to) {
        this.to = to;
    }

    public Relationship() {
    }

    public Relationship withType(final RelationshipType type) {
        this.type = type;
        return this;
    }

    public Relationship withFrom(final MonitoredElement from) {
        this.from = from;
        return this;
    }

    public Relationship withTo(final MonitoredElement to) {
        this.to = to;
        return this;
    }

}
