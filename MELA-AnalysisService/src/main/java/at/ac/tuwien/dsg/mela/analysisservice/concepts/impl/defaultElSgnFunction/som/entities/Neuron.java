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
package at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElSgnFunction.som.entities;

import at.ac.tuwien.dsg.mela.analysisservice.utils.Configuration;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Level;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class Neuron { //implements Iterable<Neuron> {
    private List<Double> weights;
    //    private List<Neuron> neighbours;
//    private List<List<Double>> mappedWeights;
    private AtomicInteger mappedWeights;
    
    private NeuronUsageLevel usageLevel;
    private DecimalFormat df = new DecimalFormat("#.###");

    //percentage of mapped values from the total mapped in the map used by the neuron
    private Double usagePercentage = 0d;


    {
        weights = new ArrayList<Double>();
//        neighbours = new ArrayList<Neuron>();
//        mappedWeights = new ArrayList<List<Double>>();
        mappedWeights = new AtomicInteger(0);
        usageLevel = NeuronUsageLevel.RARE;
    }


    public Neuron() {

    }

    public NeuronUsageLevel getUsageLevel() {
        return usageLevel;
    }


    public void setUsageLevel(NeuronUsageLevel usageLevel) {
        this.usageLevel = usageLevel;
    }

    public Neuron(List<Double> weights) {
        this.weights = weights;
    }

//    public void addNeighbour(Neuron neuron) {
//        if (!neighbours.contains(neuron)) {
//            neighbours.add(neuron);
//            neuron.neighbours.add(this);
//        }
//    }
//
//    public void removeNeighbour(Neuron neuron) {
//        if (neighbours.contains(neuron)) {
//            neighbours.remove(neuron);
//            neuron.neighbours.add(this);
//        }
//    }

    public synchronized List<Double> getWeights() {
        return weights;
    }

//    public ArrayList<Double> getEuclideanDistanceFromNeighbours() {
//
//        return weights;
//    }


//    public synchronized List<List<Double>> getMappedWeights() {
//        return mappedWeights;
//    }
    public synchronized int getMappedWeights() {
        return mappedWeights.get();
    }

//    public void setMappedWeights(List<List<Double>> mappedWeights) {
//        this.mappedWeights = mappedWeights;
//    }
//
//    public void addMappedWeights(ArrayList<Double> mappedWeights) {
//        this.mappedWeights.add(mappedWeights);
//    }
    public void setMappedWeights(int mappedWeights) {
        this.mappedWeights.set(mappedWeights);
    }

    public void addMappedWeights(int mappedWeights) {
        this.mappedWeights.addAndGet(mappedWeights);
    }

    /**
     * @param neuron the neuron between from which the distance is computed
     * @return
     */
    public List<Double> computeEuclideanDistanceFromNeuron(Neuron neuron) {
        List<Double> distance = new ArrayList<Double>(weights.size());
        List<Double> neuronWeights = neuron.weights;

        for (int i = 0; i < weights.size(); i++) {
            distance.add(Double.parseDouble(df.format(Math.abs(weights.get(i) - neuronWeights.get(i)))));
        }
        return distance;
    }

    public Double getUsagePercentage() {
        return usagePercentage;
    }

    public void setUsagePercentage(Double usagePercentage) {
        this.usagePercentage = usagePercentage;
    }


    /**
     * @param neuron the neuron between from which the distance is computed
     * @return
     */
    public Double computeEuclideanDistanceFromNeuronAsSingleValue(Neuron neuron) {
        Double distance = 0d;
        List<Double> neuronWeights = neuron.weights;

        if (weights.size() != neuronWeights.size()) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, "Neurons to compute distance do not have the same weights cardinality");
//            System.exit(1);
            return Double.MAX_VALUE;
        }

        for (int i = 0; i < weights.size(); i++) {
            distance += Math.abs(weights.get(i) - neuronWeights.get(i));
        }


        return Double.parseDouble(df.format(distance));
    }

    public void setWeights(ArrayList<Double> weights) {
        this.weights.clear();
        this.weights.addAll(weights);
    }

//    public List<Neuron> getNeighbours() {
//        return neighbours;
//    }

    //    /**
//     * moves the current weights and the neighbours weights closer to the supplied neuron values
//     *
//     * @param neuron
//     */
    public void updateNeuron(Neuron neuron) {
//        mappedWeights.addAndGet(neuron.getWeights().size());
        mappedWeights.addAndGet(1);
    }
//
//
//    private void updateNeuron(Neuron neuron, SOMStrategy strategy, int level) {
//        if (level < 1) {
//            Configuration.getLogger(this.getClass()).log(Level.ERROR, "Level < 1");
//            System.exit(1);
//        }
//        ArrayList<Double> neuronWeights = neuron.weights;
//        Double distanceRestraintFactor = strategy.getDistanceRestraintFactor(level, this.neighbours.size());
//        Double learningFactor = strategy.geLearningRestraintFactor(level);
//        for (int i = 0; i < weights.size(); i++) {
//            Double oldVal = weights.get(i);
//            Double newVal = oldVal + distanceRestraintFactor * learningFactor * (neuronWeights.get(i) - oldVal);
//            weights.set(i, newVal);
//        }
//
//        if (level < strategy.getNeighbourhoodSize()) {
//            int newLevel = level++;
//            for (Neuron neighbour : neighbours) {
//                neighbour.updateNeuron(neuron, strategy, newLevel);
//            }
//        }
//
//    }

    /**
     * @return an iterator which iterates the neurons in a Breadth First approach
     */
//    @Override
//    public Iterator<Neuron> iterator() {
//        return new BreadthFirstNeuronIterator(this);
//    }
//
//    private class BreadthFirstNeuronIterator implements Iterator<Neuron> {
//
//        private List<Neuron> neuronsToProcess = new ArrayList<Neuron>();
//
//
//        private BreadthFirstNeuronIterator(Neuron neuron) {
//            neuronsToProcess.add(neuron);
//        }
//
//        @Override
//        public boolean hasNext() {
//            return !neuronsToProcess.isEmpty();
//        }
//
//        @Override
//        public Neuron next() {
//            if (neuronsToProcess.isEmpty()) {
//                return null;
//            }
//            Neuron next = neuronsToProcess.remove(0);
//            neuronsToProcess.addAll(next.getNeighbours());
//            return next;
//        }
//
//        /**
//         * NOT SUPPORTED
//         */
//        @Override
//        public void remove() {
//            throw new UnsupportedOperationException("Not supported");
//        }
//    }
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Neuron)) {
            return false;
        }
        Neuron obj = (Neuron) o;
        List<Double> thisWeights = this.weights;
        List<Double> theirsWeights = obj.weights;
        if (thisWeights.size() != theirsWeights.size()) {
            return false;
        } else {
            for (int i = 0; i < thisWeights.size(); i++) {
                if (!thisWeights.get(i).equals(theirsWeights.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }
}
