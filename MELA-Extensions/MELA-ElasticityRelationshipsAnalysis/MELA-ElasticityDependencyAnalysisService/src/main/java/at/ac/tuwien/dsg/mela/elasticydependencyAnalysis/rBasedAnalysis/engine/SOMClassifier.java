/*
 * Copyright 2014 daniel-tuwien.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.rBasedAnalysis.engine;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.SOM2;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.SimpleSOMStrategy;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class SOMClassifier implements Serializable {

    private int cellsSize = 10;
    private int upperNormalizationValue = 100;
    private int sizeOfDataToClassify = 10;
    private SOM2 som;

    public SOMClassifier(int sizeOfDataToClassify) {
        this.sizeOfDataToClassify = sizeOfDataToClassify;
        som = new SOM2(cellsSize, sizeOfDataToClassify, 0, upperNormalizationValue, new SimpleSOMStrategy());
    }

    public List<Neuron> getSituationGroups() {
        //computes neuron usage statistics per entire map
        som.updateMapUsage();

        //returning all, such that I can sort them after occurrence and say this pair of values has been encountered 70%
        List<Neuron> neurons = new ArrayList<Neuron>();
        for (Neuron neuron : som) {
            if (neuron.getMappedWeights() > 0) {
                neurons.add(neuron);
            }
        }

        //sort the list by occurence
        Collections.sort(neurons, new Comparator<Neuron>() {
            public int compare(Neuron o1, Neuron o2) {
                return o1.getUsagePercentage().compareTo(o2.getUsagePercentage());
            }
        });

        return neurons;
    }

    /**
     * The idea is to classify only a small set of monitoring snapshots at once,
     * to avoid using so much RAM
     *
     * @param dataToClassify a set of monitoring snapshots to classify
     */
    public void trainElasticityPathway(Map<MetricWrapper, List<MetricValue>> dataToClassify) {

        List<SignatureEntry> mapped = new ArrayList<SignatureEntry>();
        //classify all monitoring data
        //need to go trough all monitoring data, and push the classified items, such that I respect the monitored sequence.
        if (dataToClassify == null || dataToClassify.values().isEmpty()) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Empty data to classify as elasticity pathway");
            return;
        }

        int minIndex = Integer.MAX_VALUE;
//        List<MetricValue> minList = dataToClassify.values().iterator().next();
        for (List<MetricValue> metricValue : dataToClassify.values()) {
            if (minIndex > metricValue.size()) {
                minIndex = metricValue.size();
            }
        }
//
//        int minIndex = dataToClassify.values().stream().min(new Comparator<List<MetricValue>>() {
//
//            @Override
//            public int compare(List<MetricValue> o1, List<MetricValue> o2) {
//                return ((Integer) o1.size()).compareTo(o2.size());
//            }
//
//        }).get().size();

        //trim sequences
        for (List<MetricValue> list : dataToClassify.values()) {
            int difference = list.size() - minIndex;
            if (difference > 0) {
                list = list.subList(difference - 1, list.size() - 1);
//                list = list.subList(0, minIndex);
            }
        }

        for (int i = 0; i < minIndex; i++) {
            SignatureEntry signatureEntry = new SignatureEntry();
            for (MetricWrapper metric : dataToClassify.keySet()) {
                List<MetricValue> values = dataToClassify.get(metric);

                //maybe we have diff value count for different metrics. Not sure when his might happen though.
                if (values.size() <= i) {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Less values for metric " + mapped);
                    break;
                }

                signatureEntry.addEntry(metric, values.get(i));
            }
            mapped.add(signatureEntry);
        }

        //train map  completely pathway entries
        for (SignatureEntry signatureEntry : mapped) {

            //build the mappedNeuron used to classify the situation
            ArrayList<Double> values = new ArrayList<Double>();
            //for each metric column, get value from measurement row
            for (MetricValue value : signatureEntry.classifiedSituation.values()) {

                //avoid using non-numeric values. Although a non-numeric value should not happen
                if (value.getValueType() == MetricValue.ValueType.NUMERIC) {
                    try {
                        values.add(Double.parseDouble(value.getValueRepresentation()));
                    } catch (Exception e) {
                        Logger.getLogger(this.getClass()).log(Level.ERROR, e);
                    }
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can't be applied on non-numeric metric value " + value);
                }
            }
            som.updateMap(new Neuron(values));
        }

        //classify all entries
        //not really sure what this for does. I think nothing?
        for (SignatureEntry signatureEntry : mapped) {

            //build the mappedNeuron used to classify the situation
            ArrayList<Double> values = new ArrayList<Double>();
            //for each metric column, get value from measurement row
            for (MetricValue value : signatureEntry.classifiedSituation.values()) {

                //avoid using non-numeric values. Although a non-numeric value should not happen
                if (value.getValueType() == MetricValue.ValueType.NUMERIC) {
                    Number nr = (Number) value.getValue();
                    values.add(nr.doubleValue());
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can't be applied on non-numeric metric value " + value);
                }
            }
            signatureEntry.mappedNeuron = som.classifySituation(new Neuron(values));
        }

    }

    //workaround in case we want to clasify metrics having same name and measurement unit, but from diff monitored elements.
    public void trainElasticityPathway(List<MetricWrapper> metrics, List<List<MetricValue>> dataToClassify) {

        List<SignatureEntry> mapped = new ArrayList<SignatureEntry>();

        //classify all monitoring data
        //need to go trough all monitoring data, and push the classified items, such that I respect the monitored sequence.
        if (dataToClassify == null || dataToClassify.isEmpty()) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Empty data to classify as elasticity pathway");
            return;
        }

        //find minIndex, and then cut from the start of the longer monitored sequences
        //this is because when MELA starts, it might have diff composition rules, then we submit another, etc.
        int minIndex = Integer.MAX_VALUE;
//        List<MetricValue> minList = dataToClassify.values().iterator().next();
        for (List<MetricValue> metricValue : dataToClassify) {
            if (minIndex > metricValue.size()) {
                minIndex = metricValue.size();
            }
        }
        //trim sequences
        for (List<MetricValue> list : dataToClassify) {
            int difference = list.size() - minIndex;
            if (difference > 0) {
                list = list.subList(difference - 1, list.size() - 1);
//                list = list.subList(0, minIndex);
            }
        }

        for (int i = 0; i < minIndex; i++) {
            SignatureEntry signatureEntry = new SignatureEntry();
            for (int j = 0; j < metrics.size(); j++) {

                List<MetricValue> values = dataToClassify.get(j);

//                //maybe we have diff value count for different metrics. Not sure when his might happen though.
//                if (values.size() <= i) {
//                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Less values for metric " + metrics.get(j).getName());
//                    break;
//                }
                signatureEntry.addEntry(metrics.get(j), values.get(i));
            }
            mapped.add(signatureEntry);
        }

        //train map  completely pathway entries
        for (SignatureEntry signatureEntry : mapped) {

            //build the mappedNeuron used to classify the situation
            ArrayList<Double> values = new ArrayList<Double>();
            //for each metric column, get value from measurement row
            for (MetricValue value : signatureEntry.classifiedSituation.values()) {

                //avoid using non-numeric values. Although a non-numeric value should not happen
                if (value.getValueType() == MetricValue.ValueType.NUMERIC) {
                    try {
                        values.add(Double.parseDouble(value.getValueRepresentation()));
                    } catch (Exception e) {
                        Logger.getLogger(this.getClass()).log(Level.ERROR, e);
                    }
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can't be applied on non-numeric metric value " + value);
                }
            }
            som.updateMap(new Neuron(values));
        }

        //classify all entries
        //not really sure what this for does. I think nothing?
        for (SignatureEntry signatureEntry : mapped) {

            //build the mappedNeuron used to classify the situation
            ArrayList<Double> values = new ArrayList<Double>();
            //for each metric column, get value from measurement row
            for (MetricValue value : signatureEntry.classifiedSituation.values()) {

                //avoid using non-numeric values. Although a non-numeric value should not happen
                if (value.getValueType() == MetricValue.ValueType.NUMERIC) {
                    Number nr = (Number) value.getValue();
                    values.add(nr.doubleValue());
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can't be applied on non-numeric metric value " + value);
                }
            }
            signatureEntry.mappedNeuron = som.classifySituation(new Neuron(values));
        }

    }

    public class SignatureEntry implements Serializable {

        private Neuron mappedNeuron;
        private Map<MetricWrapper, MetricValue> classifiedSituation;

        {
            classifiedSituation = new LinkedHashMap<MetricWrapper, MetricValue>();
        }

        public Neuron getMappedNeuron() {
            return mappedNeuron;
        }

        public Map<MetricWrapper, MetricValue> getClassifiedSituation() {
            return classifiedSituation;
        }

        private void addEntry(MetricWrapper metric, MetricValue value) {
            classifiedSituation.put(metric, value.clone());
        }
    }

    //created to overcome backward compatibility problem
    //I need to compare qith equals metrics which are on diff monitored elements, but with same name and unit
    public static class MetricWrapper extends Metric {

        public MetricWrapper(MonitoredElement monitoredElement, Metric metric) {
            super(metric.getName(), metric.getMeasurementUnit(), metric.getType());
            this.monitoredElement = monitoredElement;
        }

        public MetricWrapper() {
        }

        @XmlElement(name = "MonitoredElement", required = false)
        private MonitoredElement monitoredElement;

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 17 * hash + (this.monitoredElement != null ? this.monitoredElement.getId().hashCode() : 0);
            hash = 17 * hash + (this.monitoredElement != null ? this.monitoredElement.getLevel().hashCode() : 0);
            hash = 17 * hash + (this.getName() != null ? this.getName().hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            final MetricWrapper other = (MetricWrapper) obj;
            if (this.monitoredElement != other.monitoredElement
                    && (this.monitoredElement == null || !this.monitoredElement.getId().equals(other.monitoredElement.getClass()))
                    || !this.monitoredElement.getLevel().equals(other.monitoredElement.getLevel())) {
                return false;
            }
            if ((this.getName() == null) ? (other.getName() != null) : !this.getName().equals(other.getName())) {
                return false;
            }
            return true;
        }

        public MonitoredElement getMonitoredElement() {
            return monitoredElement;
        }

    }
}
