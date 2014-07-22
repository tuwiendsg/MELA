/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.common.monitoringConcepts;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Relationships")
public class Relationships {

    @XmlElement(name = "metric")
    private List<Relationship> relationships;

    {
        relationships = new ArrayList<Relationship>();
    }

    public Relationships withRelationships(final List<Relationship> relationships) {
        this.relationships = relationships;
        return this;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }

    public void addRelationships(List<Relationship> relationships) {
        this.relationships.addAll(relationships);
    }

    public void removeRelationships(List<Relationship> relationships) {
        this.relationships.removeAll(relationships);
    }

    public void addRelationship(Relationship relationship) {
        this.relationships.add(relationship);
    }

    public void removeRelationship(Relationship relationship) {
        this.relationships.remove(relationship);
    }

}
