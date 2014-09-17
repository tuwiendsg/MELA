/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.xmlAdapters;

import java.util.ArrayList;
import java.util.Collections;
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
@XmlRootElement(name = "Statistics")
public class Statistics {

    @XmlElement(name = "Statistic")
    private List<Statistic> entries = new ArrayList<Statistic>();

    List<Statistic> entries() {
        return Collections.unmodifiableList(entries);
    }

    void addEntry(Statistic entry) {
        entries.add(entry);
    }

    public Statistics withEntries(final List<Statistic> entries) {
        this.entries = entries;
        return this;
    }
    
    
}