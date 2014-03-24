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
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.SOM2;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.SimpleSOMStrategy;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 *
 */
/**
 *
 * Accesses elasticity space from local data service, and is trained
 * sequentially instead of keeping everything in memory
 */
public class LightweightEncounterRateElasticityPathway implements Serializable {

    private int cellsSize = 10;
    private int upperNormalizationValue = 100;
    private int sizeOfDataToClassify = 10;
    private SOM2 som;

    public LightweightEncounterRateElasticityPathway(int sizeOfDataToClassify) {
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
    public void trainElasticityPathway(Map<Metric, List<MetricValue>> dataToClassify) {

        List<SignatureEntry> mapped = new ArrayList<SignatureEntry>();
        //classify all monitoring data
        //need to go trough all monitoring data, and push the classified items, such that I respect the monitored sequence.
        if (dataToClassify == null || dataToClassify.values().isEmpty()) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Empty data to classify as elasticity pathway");
            return;
        }
        int maxIndex = dataToClassify.values().iterator().next().size();

        for (int i = 0; i < maxIndex; i++) {
            SignatureEntry signatureEntry = new SignatureEntry();
            for (Metric metric : dataToClassify.keySet()) {
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

    public class SignatureEntry  implements Serializable{

        private Neuron mappedNeuron;
        private Map<Metric, MetricValue> classifiedSituation;

        {
            classifiedSituation = new LinkedHashMap<Metric, MetricValue>();
        }

        public Neuron getMappedNeuron() {
            return mappedNeuron;
        }

        public Map<Metric, MetricValue> getClassifiedSituation() {
            return classifiedSituation;
        }

        private void addEntry(Metric metric, MetricValue value) {
            classifiedSituation.put(metric, value.clone());
        }
    }
}
