/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.costeval.utils.mapping;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class CostSchemeMapAdapter extends XmlAdapter<CostSchemeEntries, Map<MonitoredElement, CostFunction>> {

    @Override
    public Map<MonitoredElement, CostFunction> unmarshal(CostSchemeEntries in) throws Exception {
        Map<MonitoredElement, CostFunction> hashMap = new HashMap<>();
        for (CostSchemeEntry entry : in.entries()) {
            hashMap.put(entry.getMonitoredElement(), entry.getCostFunction());
        }
        return hashMap;
    }

    @Override
    public CostSchemeEntries marshal(Map<MonitoredElement, CostFunction> map) throws Exception {
        CostSchemeEntries props = new CostSchemeEntries();
        for (Map.Entry<MonitoredElement, CostFunction> entry : map.entrySet()) {
            props.addEntry(new CostSchemeEntry(entry.getKey(), entry.getValue()));
        }
        return props;
    }

}
