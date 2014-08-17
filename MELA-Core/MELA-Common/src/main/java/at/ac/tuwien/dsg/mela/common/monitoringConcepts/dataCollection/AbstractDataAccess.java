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
package at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection;

import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
 
@Service
public abstract class AbstractDataAccess {

    static final Logger log = LoggerFactory.getLogger(AbstractDataAccess.class);

    protected Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> metricFilters;
    protected List<AbstractDataSource> dataSources;
    protected Map<AbstractDataSource, Timer> dataSourcesPoolingTimers;
    protected Map<AbstractDataSource, MonitoringData> freshestMonitoredData = new HashMap<AbstractDataSource, MonitoringData>();

    // such as timestamp || service ID
    // private String monSeqID;
    {
        freshestMonitoredData = Collections.synchronizedMap(new HashMap<AbstractDataSource, MonitoringData>());
        dataSourcesPoolingTimers = new HashMap<AbstractDataSource, Timer>();
        dataSources = new ArrayList<AbstractDataSource>();
    }

    {
        metricFilters = new LinkedHashMap<MonitoredElement.MonitoredElementLevel, List<MetricFilter>>();
        // add default filter to keep the MonitoredElementIDMetric for all
		// monitored VMs
		// MetricFilter metricFilter = new MetricFilter();
		// metricFilter.getMetrics().add(new
		// Metric(Configuration.getMonitoredElementIDMetricName()));
		// metricFilter.setLevel(MonitoredElement.MonitoredElementLevel.VM);
		// addMetricFilter(metricFilter);
	
    }

    public Collection<MonitoringData> getFreshestMonitoredData() {
        return freshestMonitoredData.values();
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

    public synchronized void addDataSource(AbstractDataSource dataSource) {
        this.dataSources.add(dataSource);
        this.dataSourcesPoolingTimers.put(dataSource, createMonitoringTimer(dataSource));
    }

    public synchronized void addDataSources(Collection<AbstractDataSource> dataSources) {
        this.dataSources.addAll(dataSources);
        for (AbstractDataSource dataSource : dataSources) {
            this.dataSourcesPoolingTimers.put(dataSource, createMonitoringTimer(dataSource));
        }
    }

    public synchronized void removeDataSource(AbstractDataSource dataSource) {
        if (this.dataSourcesPoolingTimers.containsKey(dataSources)) {
            this.dataSourcesPoolingTimers.remove(dataSources).cancel();
        }
        this.dataSources.remove(dataSource);
    }

    private Timer createMonitoringTimer(final AbstractDataSource dataSource) {
        Timer timer = new Timer();
 
        if (dataSource instanceof AbstractPollingDataSource) {
            final AbstractPollingDataSource abstractPollingDataSource = (AbstractPollingDataSource) dataSource;
 

            TimerTask dataCollectionTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        // pool data source
                        MonitoringData data = dataSource.getMonitoringData();
						// replace freshest monitoring data

 
                        freshestMonitoredData.put(abstractPollingDataSource, data);
                    } catch (DataAccessException e) {
                        // TODO Auto-generated catch block
                        log.error("Caught DataAccessException", e);
 
                    }

                }
            };
 
            timer.scheduleAtFixedRate(dataCollectionTask, 0, abstractPollingDataSource.getPollingIntervalMs());
        } else {
            // TODO: needs to be implemented
            log.error("Not supporting yet data source of type " + dataSource.getClass().getName());
 
        }
        return timer;
    }

    /**
     * @return a map containing as key the ID of the Service Element, and as
     * value another map containing all the monitored metrics and their values
     * for that particular MonitoredElement traverses the supplied tree and
     * returns data about the monitored element and their children.
     */
    public abstract ServiceMonitoringSnapshot getStructuredMonitoredData(MonitoredElement monitoredElement);

    /**
     * @param monitoredElement the MonitoredElement for which to retrieve the
     * data
     * @return all the monitored metrics and their values for that particular
     * MonitoredElement Does not return data also about the element children
     */
    public abstract MonitoredElementMonitoringSnapshot getSingleElementMonitoredData(MonitoredElement monitoredElement);

    /**
     * @param monitoredElement the element for which the available monitored
     * metrics is retrieved
     * @return
     */
    public Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement monitoredElement) {
        Map<MetricInfo, Metric> metrics = new HashMap<MetricInfo, Metric>();

        //get  monitored data from all data sources
        for (MonitoringData data : freshestMonitoredData.values()) {

            for (MonitoredElementData elementData : data.getMonitoredElementDatas()) {

                //if monitored data entry targets desired monitored element
                if (elementData.getMonitoredElement().equals(monitoredElement)) {
                    for (MetricInfo metricInfo : elementData.getMetrics()) {
                        // in case several data sources collect same metric for same element
                        if (!metrics.containsKey(metricInfo)) {
                            Metric metric = new Metric();
                            metric.setName(metricInfo.getName());
                            metric.setMeasurementUnit(metricInfo.getUnits());
                            metrics.put(metricInfo, metric);
                        }
                    }
                }
            }
        }

        return metrics.values();
    }
}
