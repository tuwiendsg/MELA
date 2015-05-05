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
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class actually associates using a map the pathway for each monitored element at each monitored element level
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class ServiceElasticityPathway implements Serializable{

    static final Logger logger = LoggerFactory.getLogger(ServiceElasticityPathway.class);

    private int timestampID;

    // stores monitoring information by LEVEL, then by MonitoredElement. Service Element also stores hierarchical info
    private Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, LightweightEncounterRateElasticityPathway>> monitoredData;

    {
        monitoredData = Collections.synchronizedMap(new EnumMap<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, LightweightEncounterRateElasticityPathway>>(MonitoredElement.MonitoredElementLevel.class));
    }

    public Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, LightweightEncounterRateElasticityPathway>> getPathway() {
        return monitoredData;
    }

    public void setPathway(Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, LightweightEncounterRateElasticityPathway>> monitoredData) {
        this.monitoredData = monitoredData;
    }

    public int getTimestampID() {
        return timestampID;
    }

    public void setTimestampID(int timestampID) {
        this.timestampID = timestampID;
    }

    public void addPathway(MonitoredElement element, LightweightEncounterRateElasticityPathway pathway) {
        MonitoredElement.MonitoredElementLevel level = element.getLevel();

        //if data contains level and if contains element, than just add metrics, otherwise put new metrics
        if (monitoredData.containsKey(level)) {
            monitoredData.get(level).put(element, pathway);
        } else {
            Map<MonitoredElement, LightweightEncounterRateElasticityPathway> map = Collections.synchronizedMap(new LinkedHashMap<MonitoredElement, LightweightEncounterRateElasticityPathway>());
            map.put(element, pathway);
            monitoredData.put(level, map);
        }

    }

    public void addPathway(MonitoredElement.MonitoredElementLevel level, Map<MonitoredElement, LightweightEncounterRateElasticityPathway> elementsData) {

        for (Map.Entry<MonitoredElement, LightweightEncounterRateElasticityPathway> entry : elementsData.entrySet()) {
            //if data contains level and if contains element, than just add metrics, otherwise put new metrics
            if (monitoredData.containsKey(level)) {
                monitoredData.get(level).put(entry.getKey(), entry.getValue());
            } else {
                Map<MonitoredElement, LightweightEncounterRateElasticityPathway> map = Collections.synchronizedMap(new LinkedHashMap<MonitoredElement, LightweightEncounterRateElasticityPathway>());
                map.put(entry.getKey(), entry.getValue());
                monitoredData.put(level, map);
            }
        }

    }

    /**
     * @param level
     * @return the monitored snapshots and serviceStructure element for the
     * specified serviceStructure level
     */
    public Map<MonitoredElement, LightweightEncounterRateElasticityPathway> getPathway(MonitoredElement.MonitoredElementLevel level) {
        return monitoredData.get(level);
    }

    public boolean contains(MonitoredElement.MonitoredElementLevel level) {
        return monitoredData.containsKey(level);
    }

    public boolean contains(MonitoredElement.MonitoredElementLevel level, MonitoredElement element) {
        return monitoredData.containsKey(level) && monitoredData.get(level).containsKey(element);
    }

    /**
     * @param level
     * @param MonitoredElementIDs
     * @return the monitored snapshots and serviceStructure element for the
     * specified serviceStructure level and specified serviceStructure elements
     * IDs
     */
    public Map<MonitoredElement, LightweightEncounterRateElasticityPathway> getPathway(MonitoredElement.MonitoredElementLevel level, Collection<String> MonitoredElementIDs) {
        if (!monitoredData.containsKey(level)) {
            return new LinkedHashMap<MonitoredElement, LightweightEncounterRateElasticityPathway>();
        }
        if (MonitoredElementIDs == null || MonitoredElementIDs.size() == 0) {
            return monitoredData.get(level);
        } else {
            Map<MonitoredElement, LightweightEncounterRateElasticityPathway> filtered = Collections.synchronizedMap(new LinkedHashMap<MonitoredElement, LightweightEncounterRateElasticityPathway>());

            for (Map.Entry<MonitoredElement, LightweightEncounterRateElasticityPathway> entry : monitoredData.get(level).entrySet()) {
                if (MonitoredElementIDs.contains(entry.getKey().getId())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return filtered;
        }

    }

    public LightweightEncounterRateElasticityPathway getPathway(MonitoredElement monitoredElement) {
        if (!monitoredData.containsKey(monitoredElement.getLevel())) {
            logger.error("No pathway found for level " + monitoredElement.getLevel().toString());
            return new LightweightEncounterRateElasticityPathway(0);
        }
        for (Map.Entry<MonitoredElement, LightweightEncounterRateElasticityPathway> entry : monitoredData.get(monitoredElement.getLevel()).entrySet()) {
            if (monitoredElement.equals(entry.getKey())) {
                return entry.getValue();
            }
        }

        logger.error("No pathway found for " + monitoredElement.getId() + " " + monitoredElement.getLevel().toString());
        return new LightweightEncounterRateElasticityPathway(0);
    }

    public MonitoredElement getMonitoredService() {
        if (monitoredData.containsKey(MonitoredElement.MonitoredElementLevel.SERVICE)) {
            return monitoredData.get(MonitoredElement.MonitoredElementLevel.SERVICE).keySet().iterator().next();
        } else {
            return new MonitoredElement();
        }
    }

    public Collection<MonitoredElement> getMonitoredElements(MonitoredElement.MonitoredElementLevel level) {
        return monitoredData.get(level).keySet();
    }

    public Collection<MonitoredElement> getMonitoredElements(MonitoredElement.MonitoredElementLevel level, Collection<String> MonitoredElementIDs) {
        if (!monitoredData.containsKey(level)) {
            return new ArrayList<MonitoredElement>();
        }
        if (MonitoredElementIDs == null || MonitoredElementIDs.size() == 0) {
            return monitoredData.get(level).keySet();
        } else {
            Collection<MonitoredElement> filtered = new ArrayList<MonitoredElement>();

            for (Map.Entry<MonitoredElement, LightweightEncounterRateElasticityPathway> entry : monitoredData.get(level).entrySet()) {
                if (MonitoredElementIDs.contains(entry.getKey().getId())) {
                    filtered.add(entry.getKey());
                }
            }
            return filtered;
        }

    }

}
