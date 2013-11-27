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

    @XmlElement(name = "MonitoredElement", required = false)
    private MonitoredElement MonitoredElement;
    @XmlElement(name = "Metrics", required = false)
    @XmlJavaTypeAdapter(MonitoringEntriesAdapter.class)
    private HashMap<Metric, MetricValue> monitoredData;
    @XmlElement(name = "MonitoredElementSnapshot")
    private ArrayList<MonitoredElementMonitoringSnapshot> children;

    {
        monitoredData = new LinkedHashMap<Metric, MetricValue>();
        children = new ArrayList<MonitoredElementMonitoringSnapshot>();
    }

    public MonitoredElementMonitoringSnapshot(MonitoredElement MonitoredElement, HashMap<Metric, MetricValue> monitoredData) {
        this.MonitoredElement = MonitoredElement;
        this.monitoredData = monitoredData;
    }

    public MonitoredElementMonitoringSnapshot(MonitoredElement MonitoredElement) {
        this.MonitoredElement = MonitoredElement;
    }

    public MonitoredElementMonitoringSnapshot() {
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

    public synchronized Map<Metric, MetricValue> getMonitoredData() {
        return monitoredData;
    }

    public synchronized MonitoredElement getMonitoredElement() {
        return MonitoredElement;
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

    private class MyIterator implements Iterator<MonitoredElementMonitoringSnapshot> {

        List<MonitoredElementMonitoringSnapshot> toProcess;

        {
            toProcess = new ArrayList<MonitoredElementMonitoringSnapshot>();
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
