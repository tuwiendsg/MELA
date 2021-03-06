/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.mela.common.monitoringConcepts;

import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 * Contains monitoring data structured by MonitoredElementLevel and also
 * hierarchically The contained MAPS are synchronized as much as possible (not
 * completely, so more work needed), maybe not necessary, but I envision
 * parallel composition rule application and data processing The current issue
 * is that Collections.synchronizedMap returns MAP, which JAXB does not know how
 * to process.
 */
public class ServiceMonitoringSnapshot implements Serializable {
    //CloudCom experiments
    //private static final long serialVersionUID = 7738621893115870124l;

    private String timestamp;

    private int timestampID;
    // stores monitoring information by LEVEL, then by MonitoredElement. Service Element also stores hierarchical info
    private Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> monitoredData;

    {
        monitoredData = Collections.synchronizedMap(new EnumMap<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>>(MonitoredElement.MonitoredElementLevel.class));
    }

    public Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> getMonitoredData() {
        return monitoredData;
    }

    public void setMonitoredData(Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> monitoredData) {
        this.monitoredData = monitoredData;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getTimestampID() {
        return timestampID;
    }

    public void setTimestampID(int timestampID) {
        this.timestampID = timestampID;
    }

    public void addMonitoredData(MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot) {
        MonitoredElement MonitoredElement = MonitoredElementMonitoringSnapshot.getMonitoredElement();
        MonitoredElement.MonitoredElementLevel level = MonitoredElement.getLevel();

        //if data contains level and if contains element, than just add metrics, otherwise put new metrics
        if (monitoredData.containsKey(level)) {
            if (monitoredData.get(level).containsKey(MonitoredElement)) {
                monitoredData.get(level).get(MonitoredElement).getMonitoredData().putAll(MonitoredElementMonitoringSnapshot.getMonitoredData());
            } else {
                monitoredData.get(level).put(MonitoredElement, MonitoredElementMonitoringSnapshot);
            }
        } else {
            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> map = Collections.synchronizedMap(new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>());
            map.put(MonitoredElement, MonitoredElementMonitoringSnapshot);
            monitoredData.put(level, map);
        }

    }

    public void addMonitoredData(MonitoredElement.MonitoredElementLevel level, Map<MonitoredElement, MonitoredElementMonitoringSnapshot> elementsData) {

        for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : elementsData.entrySet()) {
            //if data contains level and if contains element, than just add metrics, otherwise put new metrics
            if (monitoredData.containsKey(level)) {
                if (monitoredData.get(level).containsKey(entry.getKey())) {
                    monitoredData.get(level).get(entry.getKey()).getMonitoredData().putAll(entry.getValue().getMonitoredData());
                } else {
                    monitoredData.get(level).put(entry.getKey(), entry.getValue());
                }
            } else {
                Map<MonitoredElement, MonitoredElementMonitoringSnapshot> map = Collections.synchronizedMap(new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>());
                map.put(entry.getKey(), entry.getValue());
                monitoredData.put(level, map);
            }
        }

    }

    public void addMonitoredData(MonitoredElement.MonitoredElementLevel level, MonitoredElement element, Metric metric, MetricValue metricValue) {

        //if data contains level and if contains element, than just add metrics, otherwise put new metrics
        if (monitoredData.containsKey(level)) {
            if (monitoredData.get(level).containsKey(element)) {
                monitoredData.get(level).get(element).getMonitoredData().put(metric, metricValue);
            } else {
                MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(element);
                elementMonitoringSnapshot.getMonitoredData().put(metric, metricValue);
                monitoredData.get(level).put(element, elementMonitoringSnapshot);
            }
        } else {
            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> map = Collections.synchronizedMap(new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>());
            MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = new MonitoredElementMonitoringSnapshot(element);
            elementMonitoringSnapshot.getMonitoredData().put(metric, metricValue);

            map.put(element, elementMonitoringSnapshot);
            monitoredData.put(level, map);
        }

    }

    /**
     * @param level
     * @return the monitored snapshots and serviceStructure element for the
     * specified serviceStructure level
     */
    public Map<MonitoredElement, MonitoredElementMonitoringSnapshot> getMonitoredData(MonitoredElement.MonitoredElementLevel level) {
        return monitoredData.get(level);
    }

    public boolean contains(MonitoredElement.MonitoredElementLevel level) {
        return monitoredData.containsKey(level);
    }

    public boolean contains(MonitoredElement.MonitoredElementLevel level, MonitoredElement element) {
        return monitoredData.containsKey(level) && monitoredData.get(level).containsKey(element);
    }

    public boolean contains(MonitoredElement.MonitoredElementLevel level, MonitoredElement element, Metric metric) {
        return monitoredData.containsKey(level) && monitoredData.get(level).containsKey(element)
                && monitoredData.get(level).get(element).containsMetric(metric);
    }

    /**
     * @param level
     * @param MonitoredElementIDs
     * @return the monitored snapshots and serviceStructure element for the
     * specified serviceStructure level and specified serviceStructure elements
     * IDs
     */
    public Map<MonitoredElement, MonitoredElementMonitoringSnapshot> getMonitoredData(MonitoredElement.MonitoredElementLevel level, Collection<String> MonitoredElementIDs) {
        if (!monitoredData.containsKey(level)) {
            return new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>();
        }
        if (MonitoredElementIDs == null || MonitoredElementIDs.size() == 0) {
            return monitoredData.get(level);
        } else {
            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> filtered = Collections.synchronizedMap(new LinkedHashMap<MonitoredElement, MonitoredElementMonitoringSnapshot>());

            for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : monitoredData.get(level).entrySet()) {
                if (MonitoredElementIDs.contains(entry.getKey().getId())) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            return filtered;
        }

    }

    public MonitoredElementMonitoringSnapshot getMonitoredData(MonitoredElement MonitoredElement) {
        if (!monitoredData.containsKey(MonitoredElement.getLevel())) {
            return new MonitoredElementMonitoringSnapshot(MonitoredElement, new LinkedHashMap<Metric, MetricValue>());
        }
        for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : monitoredData.get(MonitoredElement.getLevel()).entrySet()) {
            if (MonitoredElement.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return new MonitoredElementMonitoringSnapshot(MonitoredElement, new LinkedHashMap<Metric, MetricValue>());
    }

    public void keepOnlyDataForElement(MonitoredElement monitoredElement) {
        if (!monitoredData.containsKey(monitoredElement.getLevel())) {
            return;
        }

        MonitoredElementMonitoringSnapshot elementMonitoringSnapshot = null;

        for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : monitoredData.get(monitoredElement.getLevel()).entrySet()) {
            if (monitoredElement.equals(entry.getKey())) {
                elementMonitoringSnapshot = entry.getValue();
                break;
            }
        }

        monitoredData.clear();
        if (elementMonitoringSnapshot != null) {
            this.addMonitoredData(elementMonitoringSnapshot);
        }
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

            for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : monitoredData.get(level).entrySet()) {
                if (MonitoredElementIDs.contains(entry.getKey().getId())) {
                    filtered.add(entry.getKey());
                }
            }
            return filtered;
        }

    }

    public void applyMetricFilters(Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> metricFilters) {

        for (MonitoredElement.MonitoredElementLevel level : monitoredData.keySet()) {
            if (metricFilters.containsKey(level)) {

                Map<MonitoredElement, List<Metric>> metricsToKeep = new LinkedHashMap<MonitoredElement, List<Metric>>();

                List<MetricFilter> filters = metricFilters.get(level);

                //make one pass over the filters to extract a common metric list from multiple filters for one serviceStructure (ex filter for all Units and then keep VM)
                //for each filter
                for (MetricFilter filter : filters) {

                    //get elements targeted by filter
                    Map<MonitoredElement, MonitoredElementMonitoringSnapshot> targetElements = getMonitoredData(level, filter.getTargetMonitoredElementIDs());

                    //foreach element create a common list of metrics to keep
                    for (MonitoredElement element : targetElements.keySet()) {
                        if (metricsToKeep.containsKey(element)) {
                            metricsToKeep.get(element).addAll(filter.getMetrics());
                        } else {
                            List<Metric> metrics = new ArrayList<Metric>();
                            metrics.addAll(filter.getMetrics());
                            metricsToKeep.put(element, metrics);
                        }
                    }
                }

                for (MonitoredElement element : monitoredData.get(level).keySet()) {
                    List<Metric> toKeep = metricsToKeep.get(element);
                    MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = monitoredData.get(level).get(element);
                    MonitoredElementMonitoringSnapshot.keepMetrics(toKeep);
                }
            }
        }

        //make second pass in which we apply the filters on the targeted elements
    }

    @Override
    public String toString() {
        String description = "\tServiceMonitoringSnapshot{";
        //traverse in DFS the tree

        for (Map.Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : monitoredData.get(MonitoredElement.MonitoredElementLevel.SERVICE).entrySet()) {

            List<MonitoredElement> stack = new ArrayList<MonitoredElement>();
            stack.add(entry.getKey());
            while (!stack.isEmpty()) {
                MonitoredElement currentElement = stack.remove(stack.size() - 1);
                stack.addAll(currentElement.getContainedElements());
                String space = "";
                switch (currentElement.getLevel()) {
                    case SERVICE:
                        space = "";
                        break;
                    case SERVICE_TOPOLOGY:
                        space = "\t";
                        break;
                    case SERVICE_UNIT:
                        space = "\t\t";
                        break;
                    case VM:
                        space = "\t\t\t";
                        break;

                }
                description += "\n" + space + currentElement.getLevel() + ": " + currentElement.getId() + " Metrics:" + monitoredData.get(currentElement.getLevel()).get(currentElement).getMonitoredData().size();
            }

        }

        return description;
    }

    public void setExecutingActions(List<Action> actions) {
        for (Action action : actions) {
            for (MonitoredElement.MonitoredElementLevel level : monitoredData.keySet()) {
                for (Entry<MonitoredElement, MonitoredElementMonitoringSnapshot> entry : monitoredData.get(level).entrySet()) {
                    if (entry.getKey().getId().contains(action.getTargetEntityID())) {
                        entry.getValue().addExecutingAction(action);
                        break;
                    }
                }
            }
        }
        //add executing actions also on the overall service, to have an overall view 
        monitoredData.get(MonitoredElement.MonitoredElementLevel.SERVICE).values().iterator().next().setExecutingActions(actions);

    }

    public ServiceMonitoringSnapshot withTimestamp(final String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ServiceMonitoringSnapshot withTimestampID(final int timestampID) {
        this.timestampID = timestampID;
        return this;
    }

    public ServiceMonitoringSnapshot withMonitoredData(final Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> monitoredData) {
        this.monitoredData = monitoredData;
        return this;
    }

    public ServiceMonitoringSnapshot clone() {
        ServiceMonitoringSnapshot clone = new ServiceMonitoringSnapshot();
        clone.timestamp = "" + timestamp;
        clone.timestampID = timestampID;

        MonitoredElement service = this.getMonitoredService().clone();
        MonitoredElementMonitoringSnapshot serviceSnapshot = monitoredData.get(service.getLevel()).get(service).clone();

        Map<MonitoredElement.MonitoredElementLevel, Map<MonitoredElement, MonitoredElementMonitoringSnapshot>> clonedMonitoredData = new HashMap<>();

        //udate the cloned snapshot with cloned service
        for (MonitoredElementMonitoringSnapshot elementMonitoringSnapshot : serviceSnapshot) {

            MonitoredElement clonedElement = null;

            {
                MonitoredElement element = elementMonitoringSnapshot.getMonitoredElement();
                //search the coresponding element in the cloned service
                for (MonitoredElement me : service) {
                    if (element.equals(me)) {
                        clonedElement = me;
                        //update cloned element on snapshot
                        elementMonitoringSnapshot.withMonitoredElement(clonedElement);
                        break;
                    }
                }
            }

            //if clonedMonitoredData is null, then we had a big problem in cloning the MonitoredElement, so 
            //no check here, as the error, if appears, shows code bug
            Map<MonitoredElement, MonitoredElementMonitoringSnapshot> levelData;
            if (clonedMonitoredData.containsKey(elementMonitoringSnapshot.getMonitoredElement().getLevel())) {
                levelData = clonedMonitoredData.get(elementMonitoringSnapshot.getMonitoredElement().getLevel());
            } else {
                levelData = new HashMap<>();
                clonedMonitoredData.put(elementMonitoringSnapshot.getMonitoredElement().getLevel(), levelData);
            }

            levelData.put(elementMonitoringSnapshot.getMonitoredElement(), elementMonitoringSnapshot);
        }

        clone.monitoredData = clonedMonitoredData;
        return clone;
    }

}
