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
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.CollectedMetricValue;
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
 * This uses the poll or pushbased data sources to compute structured complex
 * monitoring information Author: Daniel Moldovan E-Mail:
 * d.moldovan@dsg.tuwien.ac.at
 *
 */
@Service
public abstract class AbstractDataAccess {

    static final Logger log = LoggerFactory.getLogger(AbstractDataAccess.class);

    protected Map<MonitoredElement.MonitoredElementLevel, List<MetricFilter>> metricFilters;

    private Map<String, Map<String, List<Metric>>> metricsToCollect;

    protected List<AbstractDataSource> dataSources;
    protected Map<AbstractDataSource, Timer> dataSourcesPoolingTimers;
    protected Map<AbstractDataSource, MonitoringData> freshestMonitoredData = new HashMap<AbstractDataSource, MonitoringData>();

    // such as timestamp || service ID
    // private String monSeqID;
    {
        freshestMonitoredData = Collections.synchronizedMap(new HashMap<AbstractDataSource, MonitoringData>());
        dataSourcesPoolingTimers = new HashMap<AbstractDataSource, Timer>();
        dataSources = new ArrayList<AbstractDataSource>();
        metricsToCollect = Collections.synchronizedMap(new HashMap<String, Map<String, List<Metric>>>());
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

        addMetricToCollect(metricFilter);

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

        addMetricsToCollect(newFilters);

    }

    public void removeMetricFilter(MetricFilter metricFilter) {
        if (metricFilters.containsKey(metricFilter.getLevel())) {
            List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
            if (list.contains(metricFilter)) {
                list.remove(metricFilter);
            }
        }

        removeMetricToCollect(metricFilter);

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

        removeMetricsToCollect(filtersToRemove);

    }

    private void addMetricToCollect(MetricFilter metricFilter) {
        Map<String, List<Metric>> metricsToCollectPerLevel;

        if (metricsToCollect.containsKey(metricFilter.getLevel().toString())) {
            metricsToCollectPerLevel = metricsToCollect.get(metricFilter.getLevel().toString());
        } else {
            metricsToCollectPerLevel = Collections.synchronizedMap(new HashMap<String, List<Metric>>());
            metricsToCollect.put(metricFilter.getLevel().toString(), metricsToCollectPerLevel);
        }

        for (String id : metricFilter.getTargetMonitoredElementIDs()) {
            List<Metric> metricsToCollectPerID;
            if (metricsToCollectPerLevel.containsKey(id)) {
                metricsToCollectPerID = metricsToCollectPerLevel.get(id);
            } else {
                metricsToCollectPerID = Collections.synchronizedList(new ArrayList<Metric>());
                metricsToCollectPerLevel.put(id, metricsToCollectPerID);
            }

            if (!metricFilter.equals(MetricFilter.ANY)) {
                metricsToCollectPerID.addAll(metricFilter.getMetrics());
            }
        }
    }

    private void addMetricsToCollect(Collection<MetricFilter> newFilters) {
        for (MetricFilter metricFilter : newFilters) {
            Map<String, List<Metric>> metricsToCollectPerLevel;

            if (metricsToCollect.containsKey(metricFilter.getLevel().toString())) {
                metricsToCollectPerLevel = metricsToCollect.get(metricFilter.getLevel().toString());
            } else {
                metricsToCollectPerLevel = Collections.synchronizedMap(new HashMap<String, List<Metric>>());
                metricsToCollect.put(metricFilter.getLevel().toString(), metricsToCollectPerLevel);
            }

            for (String id : metricFilter.getTargetMonitoredElementIDs()) {
                List<Metric> metricsToCollectPerID;
                if (metricsToCollectPerLevel.containsKey(id)) {
                    metricsToCollectPerID = metricsToCollectPerLevel.get(id);
                } else {
                    metricsToCollectPerID = Collections.synchronizedList(new ArrayList<Metric>());
                    metricsToCollectPerLevel.put(id, metricsToCollectPerID);
                }

                if (!metricFilter.equals(MetricFilter.ANY)) {
                    metricsToCollectPerID.addAll(metricFilter.getMetrics());
                }
            }
        }
    }

    private void removeMetricToCollect(MetricFilter metricFilter) {
        if (metricFilters.containsKey(metricFilter.getLevel())) {
            List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
            if (list.contains(metricFilter)) {
                list.remove(metricFilter);
            }
        }
        Map<String, List<Metric>> metricsToCollectPerLevel;

        if (metricsToCollect.containsKey(metricFilter.getLevel().toString())) {
            metricsToCollectPerLevel = metricsToCollect.get(metricFilter.getLevel().toString());
        } else {
            return;
        }

        for (String id : metricFilter.getTargetMonitoredElementIDs()) {
            List<Metric> metricsToCollectPerID;
            if (metricsToCollectPerLevel.containsKey(id)) {
                metricsToCollectPerID = metricsToCollectPerLevel.get(id);
            } else {
                break;
            }

            metricsToCollectPerID.removeAll(metricFilter.getMetrics());
        }
    }

    public void removeMetricsToCollect(Collection<MetricFilter> filtersToRemove) {
        for (MetricFilter metricFilter : filtersToRemove) {
            if (metricFilters.containsKey(metricFilter.getLevel())) {
                List<MetricFilter> list = metricFilters.get(metricFilter.getLevel());
                if (list.contains(metricFilter)) {
                    list.remove(metricFilter);
                }
            }
            Map<String, List<Metric>> metricsToCollectPerLevel;

            if (metricsToCollect.containsKey(metricFilter.getLevel().toString())) {
                metricsToCollectPerLevel = metricsToCollect.get(metricFilter.getLevel().toString());
            } else {
                return;
            }

            for (String id : metricFilter.getTargetMonitoredElementIDs()) {
                List<Metric> metricsToCollectPerID;
                if (metricsToCollectPerLevel.containsKey(id)) {
                    metricsToCollectPerID = metricsToCollectPerLevel.get(id);
                } else {
                    break;
                }

                metricsToCollectPerID.removeAll(metricFilter.getMetrics());
            }
        }

    }

    public void clearMetricFilters() {
        metricFilters.clear();
        metricsToCollect.clear();
//        for (AbstractDataSource dataSource : dataSources) {
//            dataSource.updateMetricsToCollect(metricsToCollect);
//        }
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
        if (this.dataSourcesPoolingTimers.containsKey(dataSource)) {

            this.dataSourcesPoolingTimers.remove(dataSource).cancel();
        }
        this.dataSources.remove(dataSource);
    }

    private Timer createMonitoringTimer(final AbstractDataSource dataSource) {
        Timer timer = new Timer();

        if (dataSource instanceof AbstractDataSource) {
            final AbstractDataSource abstractDataSource = (AbstractDataSource) dataSource;

            TimerTask dataCollectionTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        // pool data source
                        MonitoringData data = abstractDataSource.getMonitoringData();
                        // replace freshest monitoring data

                        freshestMonitoredData.put(abstractDataSource, data);
                    } catch (DataAccessException e) {
                        // TODO Auto-generated catch block
                        log.error("Caught DataAccessException", e);

                    }

                }
            };

            timer.scheduleAtFixedRate(dataCollectionTask, 0, abstractDataSource.getRateAtWhichDataShouldBeRead());
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
        Map<CollectedMetricValue, Metric> metrics = new HashMap<CollectedMetricValue, Metric>();

        //get  monitored data from all data sources
        for (MonitoringData data : freshestMonitoredData.values()) {

            for (MonitoredElementData elementData : data.getMonitoredElementDatas()) {

                //if monitored data entry targets desired monitored element
                if (elementData.getMonitoredElement().equals(monitoredElement)) {
                    for (CollectedMetricValue metricInfo : elementData.getMetrics()) {
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

    public AbstractDataAccess withDataSources(final List<AbstractDataSource> dataSources) {
        this.dataSources = dataSources;
        return this;
    }

    public AbstractDataAccess withDataSourcesPoolingTimers(final Map<AbstractDataSource, Timer> dataSourcesPoolingTimers) {
        this.dataSourcesPoolingTimers = dataSourcesPoolingTimers;
        return this;
    }

    public AbstractDataAccess withFreshestMonitoredData(final Map<AbstractDataSource, MonitoringData> freshestMonitoredData) {
        this.freshestMonitoredData = freshestMonitoredData;
        return this;
    }

}
