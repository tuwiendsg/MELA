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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
public class InMemoryEncounterRateElasticityPathway {

    int cellsSize = 10;
    int upperNormalizationValue = 100;

    public List<InMemoryEncounterRateElasticityPathway.SignatureEntry> getElasticitySignature(Map<Metric, List<MetricValue>> dataToClassify) {
        SOM2 som = new SOM2(cellsSize, dataToClassify.keySet().size(), 0, upperNormalizationValue, new SimpleSOMStrategy());

        List<SignatureEntry> mapped = new ArrayList<SignatureEntry>();
        //classify all monitoring data
        //need to go trough all monitoring data, and push the classified items, such that I respect the monitored sequence.
        if (dataToClassify.values().isEmpty()) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Empty data to classify as elasticity pathway");
            return null;
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
                    values.add(Double.parseDouble(value.getValueRepresentation()));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can't be applied on non-numeric metric value " + value);
                }
            }
            Neuron neuron = som.updateMap(new Neuron(values));

        }

        //computes neuron usage statistics per entire map
        som.updateMapUsage();


        //classify all entries
        for (SignatureEntry signatureEntry : mapped) {

            //build the mappedNeuron used to classify the situation
            ArrayList<Double> values = new ArrayList<Double>();
            //for each metric column, get value from measurement row
            for (MetricValue value : signatureEntry.classifiedSituation.values()) {

                //avoid using non-numeric values. Although a non-numeric value should not happen
                if (value.getValueType() == MetricValue.ValueType.NUMERIC) {
                    values.add(Double.parseDouble(value.getValueRepresentation()));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can;t be applied on non-numeric metric value " + value);
                }
            }
            Neuron neuron = som.classifySituation(new Neuron(values));
            signatureEntry.mappedNeuron = neuron;
        }


        //classify entries by encounterRate? Nooo? Yes? Who Knows? I mean, we need to be carefull not to lose monitoring information
        //we might need two things to do: 1 to say in 1 chart this metric point is usual, this not
        //the second to say ok in usual case the metric value combinations are ..
        //currently we go with the second, for which we did not need storing the order of the monitoring data in the entries, but it might be useful later
//        Map<NeuronUsageLevel, List<SignatureEntry>> pathway = new LinkedHashMap<NeuronUsageLevel, List<SignatureEntry>>();
//        List<SignatureEntry> rare = new ArrayList<SignatureEntry>();
//        List<SignatureEntry> neutral = new ArrayList<SignatureEntry>();
//        List<SignatureEntry> dominant = new ArrayList<SignatureEntry>();
//
//        pathway.put(NeuronUsageLevel.DOMINANT, rare);
//        pathway.put(NeuronUsageLevel.NEUTRAL, neutral);
//        pathway.put(NeuronUsageLevel.RARE, dominant);
//
//        for (SignatureEntry signatureEntry : mapped) {
//            switch (signatureEntry.getMappedNeuron().getUsageLevel()) {
//                case RARE:
//                    rare.add(signatureEntry);
//                    break;
//                case NEUTRAL:
//                    neutral.add(signatureEntry);
//                    break;
//                case DOMINANT:
//                    dominant.add(signatureEntry);
//                    break;
//            }
//        }

        //returning all, such that I can sort them after occurrence and say this pair of values has been encountered 70%
        return mapped;

    }

    public List<Neuron> getSituationGroups(Map<Metric, List<MetricValue>> dataToClassify) {

        SOM2 som = new SOM2(cellsSize, dataToClassify.keySet().size(), 0, upperNormalizationValue, new SimpleSOMStrategy());

        List<SignatureEntry> mapped = new ArrayList<SignatureEntry>();
        //classify all monitoring data
        //need to go trough all monitoring data, and push the classified items, such that I respect the monitored sequence.
        if (dataToClassify.values().size() == 0) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, "Empty data to classify as elasticity pathway");
            return null;
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
                    values.add(Double.parseDouble(value.getValueRepresentation()));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can't be applied on non-numeric metric value " + value);
                }
            }
            Neuron neuron = som.updateMap(new Neuron(values));

        }

        //computes neuron usage statistics per entire map
        som.updateMapUsage();


        //classify all entries
        for (SignatureEntry signatureEntry : mapped) {

            //build the mappedNeuron used to classify the situation
            ArrayList<Double> values = new ArrayList<Double>();
            //for each metric column, get value from measurement row
            for (MetricValue value : signatureEntry.classifiedSituation.values()) {

                //avoid using non-numeric values. Although a non-numeric value should not happen
                if (value.getValueType() == MetricValue.ValueType.NUMERIC) {
                    values.add(Double.parseDouble(value.getValueRepresentation()));
                } else {
                    Logger.getLogger(this.getClass()).log(Level.ERROR, "Elasticity Pathway can;t be applied on non-numeric metric value " + value);
                }
            }
            Neuron neuron = som.classifySituation(new Neuron(values));
            signatureEntry.mappedNeuron = neuron;
        }

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

    public class SignatureEntry {

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
