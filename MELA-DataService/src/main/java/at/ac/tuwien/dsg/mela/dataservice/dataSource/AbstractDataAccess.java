/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group E184
 *
 * This work was partially supported by the European Commission in terms of the CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package at.ac.tuwien.dsg.mela.dataservice.dataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public abstract class AbstractDataAccess{

    protected Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> metricFilters;

    {
        metricFilters = new LinkedHashMap<MonitoredElement.MonitoredElementLevel, List<MetricFilter>>();

        //add default filter to keep the MonitoredElementIDMetric for all monitored VMs
//        MetricFilter metricFilter = new MetricFilter();
//        metricFilter.getMetrics().add(new Metric(Configuration.getMonitoredElementIDMetricName()));
//        metricFilter.setLevel(MonitoredElement.MonitoredElementLevel.VM);
//        addMetricFilter(metricFilter);
    }

    public void addMetricFilter(MetricFilter metricFilter) {
        if (metricFilters.containsKey(metricFilter.getLevel())) {
            List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
            if (!list.contains(metricFilter)) {
                list.add(metricFilter);
            }
        } else {
            List<MetricFilter> list = new ArrayList<MetricFilter>();
            list.add(metricFilter);
            metricFilters.put(metricFilter.getLevel(), list);
        }
    }

    public void addMetricFilters(Collection<MetricFilter> newFilters) {
        for (MetricFilter metricFilter : newFilters) {
            if (metricFilters.containsKey(metricFilter.getLevel())) {
                List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
                if (!list.contains(metricFilter)) {
                    list.add(metricFilter);
                }
            } else {
                List<MetricFilter> list = new ArrayList<MetricFilter>();
                list.add(metricFilter);
                metricFilters.put(metricFilter.getLevel(), list);
            }
        }
    }

    public void removeMetricFilter(MetricFilter metricFilter) {
        if (metricFilters.containsKey(metricFilter.getLevel())) {
            List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
            if (list.contains(metricFilter)) {
                list.remove(metricFilter);
            }
        }
    }


    public void removeMetricFilters(Collection<MetricFilter> filtersToRemove) {
        for (MetricFilter metricFilter : filtersToRemove) {
            if (metricFilters.containsKey(metricFilter.getLevel())) {
                List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
                if (list.contains(metricFilter)) {
                    list.remove(metricFilter);
                }
            }
        }
    }

    public Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> getMetricFilters() {
        return metricFilters;
    }

    /**
     * @return a map containing as key the ID of the Service Element, and as value another map containing all the monitored metrics and their values for that particular  MonitoredElement
     *         traverses the supplied tree and returns data about the monitored element and their children.
     */
    public abstract ServiceMonitoringSnapshot getMonitoredData(MonitoredElement MonitoredElement);

    /**
     * @param MonitoredElement the MonitoredElement for which to retrieve the data
     * @return all the monitored metrics and their values for that particular  MonitoredElement
     *         Does not return data also about the element children
     */
    public abstract MonitoredElementMonitoringSnapshot getSingleElementMonitoredData(MonitoredElement MonitoredElement);


    /**
     * @param MonitoredElement the element for which the available monitored metrics is retrieved
     * @return
     */
    public abstract Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement MonitoredElement);


}
