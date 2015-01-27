/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.dsg.mela.costeval.utils.mapping;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.util.conversions.helper.CostIntervalEntry;
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
@XmlRootElement(name = "CostSchemeEntries")
public class CostSchemeEntries {

    @XmlElement(name = "CostSchemeEntry")
    private List<CostSchemeEntry> entries = new ArrayList<CostSchemeEntry>();

    List<CostSchemeEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    void addEntry(CostSchemeEntry entry) {
        entries.add(entry);
    }
}