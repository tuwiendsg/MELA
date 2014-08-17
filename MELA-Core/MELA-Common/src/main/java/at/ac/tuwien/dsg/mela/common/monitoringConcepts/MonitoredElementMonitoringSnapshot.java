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

import java.io.Serializable;
import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
/**
 * Contains a MonitoredElement ID and associated metrics. Used in the
 * ServiceMonitoringSnapshotṡ Represents also in XML a tree structure of
 * monitored data, containing for each node the MonitoredElement, a map of
 * monitored data, and a list of children monitoring datas.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementSnapshot")
public class MonitoredElementMonitoringSnapshot implements Serializable, Iterable<MonitoredElementMonitoringSnapshot> {
    
    
    public static final long serialVersionUID = 7251370466732547372l;

    @XmlElement(name = "MonitoredElement", required = false)
    private MonitoredElement monitoredElement;

    @XmlElement(name = "Timestamp", required = false)
    private String timestamp;

    @XmlElement(name = "Metrics", required = false)

    @XmlJavaTypeAdapter(MonitoringEntriesAdapter.class)
    private HashMap<Metric, MetricValue> monitoredData;

    @XmlElement(name = "Action", required = false)
    private List<Action> executingActions;

    @XmlElement(name = "MonitoredElementSnapshot")
    private ArrayList<MonitoredElementMonitoringSnapshot> children;
    
    private int hashCode;

    {
        monitoredData = new LinkedHashMap<Metric, MetricValue>();
        executingActions = new ArrayList<Action>();
        children = new ArrayList<MonitoredElementMonitoringSnapshot>();
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }

    public MonitoredElementMonitoringSnapshot(MonitoredElement MonitoredElement, HashMap<Metric, MetricValue> monitoredData) {
        this.monitoredElement = MonitoredElement;
        this.monitoredData = monitoredData;
        hashCode = super.hashCode() + monitoredElement.hashCode() + monitoredData.hashCode();
    }

    public MonitoredElementMonitoringSnapshot(MonitoredElement MonitoredElement) {
        this.monitoredElement = MonitoredElement;
        hashCode = super.hashCode() + monitoredElement.hashCode();
    }

    public MonitoredElementMonitoringSnapshot() {
        hashCode = super.hashCode();
    }

    /**
     * adds new or overrides existent value
     */
    public synchronized void putMetric(Metric metric, MetricValue metricValue) {
        monitoredData.put(metric, metricValue);
    }
    
    public synchronized Collection<Metric> getMetrics() {
        return monitoredData.keySet();
    }

    public synchronized Collection<MetricValue> getValues() {
        return monitoredData.values();
    }

    public synchronized MetricValue getMetricValue(Metric metric) {
        return monitoredData.get(metric);
    }

    public synchronized boolean containsMetric(Metric metric) {
        return monitoredData.containsKey(metric);
    }

    public synchronized String getTimestamp() {
        return timestamp;
    }

    public synchronized void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public synchronized Map<Metric, MetricValue> getMonitoredData() {
        return monitoredData;
    }

    public synchronized MonitoredElement getMonitoredElement() {
        return monitoredElement;
    }

    public synchronized MetricValue getValueForMetric(Metric metric) {
        return monitoredData.get(metric);
    }

    public synchronized Collection<MonitoredElementMonitoringSnapshot> getChildren() {
        return children;
    }

    public synchronized void setChildren(ArrayList<MonitoredElementMonitoringSnapshot> children) {
        this.children = children;
    }

    public synchronized void addChild(MonitoredElementMonitoringSnapshot child) {
        this.children.add(child);
    }

    public synchronized void removeChild(MonitoredElementMonitoringSnapshot child) {
        this.children.remove(child);
    }

    public List<Action> getExecutingActions() {
        return executingActions;
    }

    public void setExecutingActions(List<Action> executingActions) {
        this.executingActions = executingActions;
    }

    public void addExecutingActions(List<Action> executingActions) {
        this.executingActions.addAll(executingActions);
    }

    public void addExecutingAction(Action executingAction) {
        this.executingActions.add(executingAction);
    }

    public synchronized void keepMetrics(Collection<Metric> metrics) {
        Map<Metric, MetricValue> filteredMonitoredData = new LinkedHashMap<Metric, MetricValue>();
        boolean noFiltering = false;
        for (Metric metric : metrics) {
            //* means keep all metrics
            if (metric.getName().equals("*")) {
                noFiltering = true;
                break;
            }
        }
        if (noFiltering) {
            return;
        } else {
            monitoredData.keySet().retainAll(metrics);
        }

    }

    public Iterator<MonitoredElementMonitoringSnapshot> iterator() {
        return new MyIterator(this);
    }

    private static class MyIterator implements Iterator<MonitoredElementMonitoringSnapshot> {

        List<MonitoredElementMonitoringSnapshot> toProcess;

        {
            toProcess = new ArrayList<MonitoredElementMonitoringSnapshot>();
        }

        public MyIterator() {
        }

        
        
        public MyIterator(MonitoredElementMonitoringSnapshot root) {
            toProcess.add(root);
        }

        public boolean hasNext() {
            return !toProcess.isEmpty();
        }

        public MonitoredElementMonitoringSnapshot next() {
            if (hasNext()) {
                MonitoredElementMonitoringSnapshot next = toProcess.remove(0);
                toProcess.addAll(next.getChildren());
                return next;
            }
            return null;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
