/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.xmlAdapters;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class ElasticityDependencyStatisticsXMLAdapter extends XmlAdapter<Statistics, Map<String, Double>> {

    @Override
    public Map<String, Double> unmarshal(Statistics in) throws Exception {
        HashMap<String, Double> hashMap = new HashMap<String, Double>();
        for (Statistic entry : in.entries()) {
            hashMap.put(entry.getStatisticName(), entry.getValue());
        }
        return hashMap;
    }

    @Override
    public Statistics marshal(Map<String, Double> map) throws Exception {
        Statistics props = new Statistics();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            props.addEntry(new Statistic(entry.getKey(), entry.getValue()));
        }
        return props;

    }
}
