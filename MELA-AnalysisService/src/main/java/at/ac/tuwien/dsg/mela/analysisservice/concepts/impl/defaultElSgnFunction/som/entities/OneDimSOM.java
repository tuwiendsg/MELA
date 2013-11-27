package at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElSgnFunction.som.entities;

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
//package at.ac.tuwien.dsg.mela.analysis.concepts.impl.defaultElSgnFunction.som.entities;
//
//import at.ac.tuwien.dsg.mela.analysis.concepts.impl.defaultElSgnFunction.som.strategy.SOMStrategy;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Random;
//
///**
// * 
//Date:
// * Date: 4/28/13
// * Time: 9:58 AM
//
// */
//public class OneDimSOM implements Iterable<Neuron> {
//    //    private Neuron root;
////    private int initialSize;
//    protected int maxNeighboursNo = 4;
//    protected int elementsNo;
//    protected int weightsNo;
//    protected int minWeightValue;
//    protected int maxWeightValue;
//    protected Random random;
//    protected SOMStrategy strategy;
//    protected ArrayList<Neuron> neurons;
//
//    {
//        random = new Random();
//    }
//
//
//    protected OneDimSOM() {
//    }
//
//    public OneDimSOM(int elementsNo, int weightsNo, int minWeightValue, int maxWeightValue, SOMStrategy strategy) {
//        this.elementsNo = elementsNo;
////        this.maxNeighboursNo = maxNeighboursNo;
//        this.weightsNo = weightsNo;
//        this.minWeightValue = minWeightValue;
//        this.maxWeightValue = maxWeightValue;
//        this.strategy = strategy;
//
//        ArrayList<String> generatedWords = new ArrayList<String>();
//
//        neurons = new ArrayList<Neuron>();
//
//        int sumPerCell = maxWeightValue / elementsNo;
//
//
//        for (int i = 0; i < elementsNo; i++) {
//
//            String generatedWord = "";
//
//
//
//            Neuron newNeuron = new Neuron();
//            ArrayList<Double> weights = new ArrayList<Double>();
//            do {
//
//                int sumPerCurrentCell = i*sumPerCell;
//
//                //generate elementsNo elements that sum up to sumPerCurrentCell
//
//                ArrayList<Double> sumElements =  new ArrayList<Double>();
//                int remaining  = sumPerCurrentCell;
//
//                for(int k = 0; k < weightsNo-1; k++){
//                    if(remaining>0){
//                    int sumElement = random.nextInt(remaining);
//                    sumElements.add((double)sumElement);
//                    remaining -= sumElement;
//                    }else{
//                        sumElements.add(0d);
//                    }
//                }
//                sumElements.add((double)remaining);
//
//                weights.clear();
//                for (int k = 0; k < weightsNo; k++) {
//
//                    weights.add(sumElements.remove(random.nextInt(sumElements.size())));
////                    weights.add((double)i);
////                        weights.add((double) random.nextInt(maxWeightValue) + minWeightValue);
////                    weights.add(Double.parseDouble(i + "." + j));
//                }
//                generatedWord = weights.toString();
//            } while (generatedWords.contains(generatedWord));
//
//            newNeuron.setWeights(weights);
//            neurons.add(newNeuron);
//
//        }
//
//    }
//
//
//
//    public int getElementsNo() {
//        return elementsNo;
//    }
//
//    public ArrayList<Neuron> getNeurons() {
//        return neurons;
//    }
//
////    public SOM(int maxNeighboursNo, int weightsNo, int minWeightValue, int maxWeightValue, SOMStrategy strategy) {
////        this.maxNeighboursNo = maxNeighboursNo;
////        this.weightsNo = weightsNo;
////        this.minWeightValue = minWeightValue;
////        this.maxWeightValue = maxWeightValue;
////        this.strategy = strategy;
////        root = new Neuron();
////        ArrayList<Double> weights = new ArrayList<Double>();
////        for (int i = 0; i < weightsNo; i++) {
////            weights.add(0d);
////        }
////        root.setWeights(weights);
////    }
//
////    public Neuron getRoot() {
////        return root;
////    }
//
//    public ArrayList<Double> getWeightsMean() {
//        ArrayList<Double> weightsMean = new ArrayList<Double>();
//        int nodesCount = 0;
//        for (int i = 0; i < weightsNo; i++) {
//            weightsMean.add(i, 0d);
//        }
//
//        for (int row = 0; row < elementsNo; row++) {
//
//            List<Double> neuronWeights = neurons.get(row).getWeights();
//                for (int i = 0; i < weightsNo; i++) {
//                    weightsMean.set(i, weightsMean.get(i) + neuronWeights.get(i));
//                }
//                nodesCount++;
//
//        }
//
//
//        for (int i = 0; i < weightsNo; i++) {
//            weightsMean.set(i, weightsMean.get(i) / nodesCount);
//        }
//
//        return weightsMean;
//    }
//
//
//    public Double getWeightsMeanAsSingleValue() {
//        ArrayList<Double> weightsMean = new ArrayList<Double>();
//        int nodesCount = 0;
//        for (int i = 0; i < weightsNo; i++) {
//            weightsMean.add(i, 0d);
//        }
//
//        for (int row = 0; row < elementsNo; row++) {
//
//            List<Double> neuronWeights = neurons.get(row).getWeights();
//                for (int i = 0; i < weightsNo; i++) {
//                    weightsMean.set(i, weightsMean.get(i) + neuronWeights.get(i));
//                }
//                nodesCount++;
//
//        }
//
//        Double value = 0d;
//        for (int i = 0; i < weightsNo; i++) {
//            value += weightsMean.get(i) / nodesCount;
//        }
//
//
//        return value;
//    }
//
//
//    public ArrayList<Double> getStandardVariance(final ArrayList<Double> weightsMean) {
//        ArrayList<Double> weightsStandardVariance = new ArrayList<Double>();
//        int nodesCount = 0;
//
//        for (int i = 0; i < weightsNo; i++) {
//            weightsStandardVariance.add(i, 0d);
//        }
//
//        for (int row = 0; row < elementsNo; row++) {
//
//            List<Double> neuronWeights =  neurons.get(row).getWeights();
//                for (int i = 0; i < weightsNo; i++) {
//                    //squared distance from the mean
//                    weightsStandardVariance.set(i, Math.pow(weightsMean.get(i) - neuronWeights.get(i), 2));
//                }
//                nodesCount++;
//
//        }
//
//        for (int i = 0; i < weightsNo; i++) {
//            weightsStandardVariance.set(i, weightsStandardVariance.get(i) / nodesCount);
//        }
//
//        return weightsStandardVariance;
//    }
//
//
////    //actually computes Average Absolute Deviation
////    public Double getStandardVarianceAsSingleValue() {
////        ArrayList<Double> weightsMean = getWeightsMean();
////        Double weightsStandardVariance = 0d;
////        int nodesCount = 0;
////
////
////        for (int row = 0; row < elementsNo; row++) {
////            for (int column = 0; column < elementsNo; column++) {
////                ArrayList<Double> neuronWeights = neurons[row][column].getWeights();
////                for (int i = 0; i < weightsNo; i++) {
////                    //squared distance from the mean
////                    weightsStandardVariance += Math.abs(weightsMean.get(i) - neuronWeights.get(i));
////                }
////                nodesCount++;
////            }
////        }
////
////        weightsStandardVariance /= nodesCount;
////        Double weightsStandardDeviation =  weightsStandardVariance;
////
////        return weightsStandardDeviation;
////    }
//
//
//    //computes Standard standard deviation
//    public Double getStandardVarianceAsSingleValue() {
//        ArrayList<Double> weightsMean = getWeightsMean();
//        Double weightsStandardVariance = 0d;
//        int nodesCount = 0;
//
//
//        for (int row = 0; row < elementsNo; row++) {
//
//            List<Double> neuronWeights =  neurons.get(row).getWeights();
//                for (int i = 0; i < weightsNo; i++) {
//                    //squared distance from the mean
//                    weightsStandardVariance += Math.pow(weightsMean.get(i) - neuronWeights.get(i), 2);
//                }
//                nodesCount++;
//
//        }
//
//        weightsStandardVariance /= nodesCount;
//        Double weightsStandardDeviation = Math.sqrt(weightsStandardVariance);
//
//        return weightsStandardDeviation;
//    }
//
//////computes rare sit which are behind the standard deviation from the euclidean distance with their neighbours
////    public List<Neuron> getRareSituations() {
////
////        Double mean = getWeightsMeanAsSingleValue();
////        Double standardDeviation = getStandardVarianceAsSingleValue();
////
////        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();
////
////        for (int row = 0; row < elementsNo; row++) {
////            for (int column = 0; column < elementsNo; column++) {
////                ArrayList<Double> neuronWeights = neurons[row][column].getWeights();
////                Double weightsValue = 0d;
////                for (int i = 0; i < weightsNo; i++) {
////                    //squared distance from the mean
////                    weightsValue += neuronWeights.get(i);
////                }
////
////                if(Math.abs(weightsValue-mean)>=standardDeviation){
////                    rareSituations.add(neurons[row][column]);
////                }
////
////            }
////        }
////
////        return rareSituations;
////        }
//
//    /**
//     * @return nodes mapping situations which have mapped situations deviation > 80% std deviation
//     */
//    public List<Neuron> getAlmostRareSituations() {
//
//        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();
//
//        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
//        for (Neuron neuron : this) {
//            int mappedSituations = neuron.getMappedWeights().size();
//            if (mappedSituations > 0) {
//                encounteredSituationsPerNeuron.add(neuron.getMappedWeights().size());
//            }
//        }
//
//        //compute mean of the number of mapped situations per neuron
//        Integer mean = 0;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            mean += integer;
//        }
//
//        mean /= encounteredSituationsPerNeuron.size();
//
////compute variance
//        Double variance = 0d;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            variance += Math.pow(mean - integer, 2);
//        }
//
//        variance /= encounteredSituationsPerNeuron.size();
//
//        Integer stdDeviation = new Double(Math.sqrt(variance)).intValue();
//        Integer partialDeviation = new Double(stdDeviation * 0.8).intValue();
//
//        for (Neuron neuron : this) {
//            int mappedSituations = neuron.getMappedWeights().size();
//            if (mappedSituations > 0) {
//                Integer distanceFromMean = mappedSituations - mean;
//                if (distanceFromMean < 0 && Math.abs(distanceFromMean) >= partialDeviation && Math.abs(distanceFromMean) <= stdDeviation) {
//                    rareSituations.add(neuron);
//                }
//            }
//        }
//
//        return rareSituations;
//    }
//
//
//    public List<Neuron> getRareSituations() {
//
//        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();
//
//        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
//        for (Neuron neuron : this) {
//            int mappedSituations = neuron.getMappedWeights().size();
//            if (mappedSituations > 0) {
//                encounteredSituationsPerNeuron.add(neuron.getMappedWeights().size());
//            }
//        }
//
//        //compute mean of the number of mapped situations per neuron
//        Integer mean = 0;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            mean += integer;
//        }
//
//        mean /= encounteredSituationsPerNeuron.size();
//
////compute variance
//        Double variance = 0d;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            variance += Math.pow(mean - integer, 2);
//        }
//
//        variance /= encounteredSituationsPerNeuron.size();
//
//        Integer stdDeviation = new Double(Math.sqrt(variance)).intValue();
//
//
//        for (Neuron neuron : this) {
//            int mappedSituations = neuron.getMappedWeights().size();
//            if (mappedSituations > 0) {
//                Integer distanceFromMean = mappedSituations - mean;
//                if (distanceFromMean < 0 && Math.abs(distanceFromMean) >= stdDeviation) {
//                    rareSituations.add(neuron);
//                }
//            }
//        }
//
//        return rareSituations;
//    }
//
//    public Integer[] getUsageMeanAndStdDeviation() {
//
//        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();
//
//        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
//        for (Neuron neuron : this) {
//            int mappedSituations = neuron.getMappedWeights().size();
//            if (mappedSituations > 0) {
//                encounteredSituationsPerNeuron.add(neuron.getMappedWeights().size());
//            }
//        }
//
//        //compute mean of the number of mapped situations per neuron
//        Integer mean = 0;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            mean += integer;
//        }
//
//        mean /= encounteredSituationsPerNeuron.size();
//
////compute variance
//        Double variance = 0d;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            variance += Math.pow(mean - integer, 2);
//        }
//
//        variance /= encounteredSituationsPerNeuron.size();
//
//        Integer stdDeviation = new Double(Math.sqrt(variance)).intValue();
//
//
//        for (Neuron neuron : this) {
//            int mappedSituations = neuron.getMappedWeights().size();
//            if (mappedSituations > 0) {
//                Integer distanceFromMean = mappedSituations - mean;
//                if (distanceFromMean < 0 && Math.abs(distanceFromMean) >= stdDeviation) {
//                    rareSituations.add(neuron);
//                }
//            }
//        }
//
//
//        return new Integer[]{mean, stdDeviation};
//    }
//
//    //actually computes Average Absolute Variation  on nr of MAPPED SITUATIONS
//
////    public Integer[] getUsageMeanAndStdDeviation() {
////        ArrayList<Integer> mappedSituationsNr = new ArrayList<Integer>();
////        Integer average = 0;
////
////
////        for (int row = 0; row < elementsNo; row++) {
////
////                final Neuron neuron = neurons.get(row);
////                mappedSituationsNr.add(neuron.getMappedWeights().size());
////                average += neuron.getMappedWeights().size();
////
////        }
////
////        Integer totalMapped = average;
////
////        average /= mappedSituationsNr.size();
////        Double sdtDev = 0d;
////        for (Integer sitNr : mappedSituationsNr) {
////            sdtDev += Math.abs(sitNr - average);
////        }
////
////        sdtDev /= mappedSituationsNr.size();
//////        sdtDev += Math.sqrt(sdtDev);
////
////        System.out.println("Mapped " + totalMapped + " average " + average + " stdDev " + sdtDev);
////
////        return new Integer[]{average.intValue(), sdtDev.intValue()};
////    }
//
//    public void updateMapUsage() {
//        Integer[] usageMeanAndStdDev = getUsageMeanAndStdDeviation();
//        final Integer averageUsageLevel = usageMeanAndStdDev[0];
//        final Integer standardDeviation = usageMeanAndStdDev[1];
//
//
//        for (int row = 0; row < elementsNo; row++) {
//                final Neuron source = neurons.get(row);
//
//                Thread t = new Thread() {
//                    @Override
//                    public void run() {
//
//                        int mappedSituations = source.getMappedWeights().size();
//
//                        Integer partialDeviation = new Double(standardDeviation * 0.7).intValue();
//
//                        if (mappedSituations > 0) {
//                            Integer distanceFromMean = mappedSituations - averageUsageLevel;
//
//                            //if more usage than average
//                            if (distanceFromMean >= 0) {
//                                if (distanceFromMean > standardDeviation) {
//                                    source.setUsageLevel(NeuronUsageLevel.DOMINANT);
//                                } else if (distanceFromMean >= partialDeviation && distanceFromMean <= standardDeviation) {
//                                    source.setUsageLevel(NeuronUsageLevel.DOMINANT);
//                                } else {
//                                    source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
//                                }
//                            } else {
//                                //if less usage than average
//                                Integer distanceAbsValue = Math.abs(distanceFromMean);
//                                if (distanceAbsValue > standardDeviation) {
//                                    source.setUsageLevel(NeuronUsageLevel.RARE);
//                                } else if (distanceAbsValue >= partialDeviation && Math.abs(distanceFromMean) <= standardDeviation) {
//                                    source.setUsageLevel(NeuronUsageLevel.RARE);
//                                } else {
//                                    source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
//                                }
//                            }
//                        } else {
//                            source.setUsageLevel(NeuronUsageLevel.RARE);
//                        }
//
//                    }
//                };
//                t.run();
//
//            }
//
//    }
//
//
//    //in order to retrain the map, it clears the previous mapped values
////or just to remove values used in training
//    public void clearMappings() {
//        for (Neuron neuron : this) {
//            neuron.getMappedWeights().clear();
//        }
//    }
//
//
//    public List<Neuron> getDominantSituations() {
//
//        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();
//
//        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
//        for (Neuron neuron : this) {
//            encounteredSituationsPerNeuron.add(neuron.getMappedWeights().size());
//        }
//
//        //compute mean of the number of mapped situations per neuron
//        Integer mean = 0;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            mean += integer;
//        }
//
//        mean /= encounteredSituationsPerNeuron.size();
//
////compute variance
//        Double variance = 0d;
//        for (Integer integer : encounteredSituationsPerNeuron) {
//            variance += Math.pow(mean - integer, 2);
//        }
//
//        variance /= encounteredSituationsPerNeuron.size();
//
//        Integer stdDeviation = new Double(Math.sqrt(variance)).intValue();
//
//
//        for (Neuron neuron : this) {
//            Integer distanceFromMean = neuron.getMappedWeights().size() - mean;
//            if (distanceFromMean >= stdDeviation) {
//                rareSituations.add(neuron);
//            }
//        }
//
//        return rareSituations;
//    }
//
//
//    /**
//     * @param newData
//     * @return neuron on which the new data has been mapped to
//     */
//    public Neuron updateMap(Neuron newData) {
//        int closestI = 0;
//
//        double minDistance = Double.POSITIVE_INFINITY;
//
//        for (int row = 0; row < elementsNo; row++) {
//
//                Neuron neuron = neurons.get(row);
//                double newDistance = neuron.computeEuclideanDistanceFromNeuronAsSingleValue(newData);
//                if (minDistance > newDistance) {
//                    minDistance = newDistance;
//                    closestI = row;
//                }
//
//        }
//
////        //if need to grow map
////        if (minDistance > getStandardVarianceAsSingleValue()) {
////
////            //expand only corner nodes (that do not have the maximum nr of neighbours)
////            if (closest.getNeighbours().size() < maxNeighboursNo) {
////                closest.addNeighbour(newData);
////            } else {
////                //do a regular node update
////                updateNeuron(closest, newData, strategy);
////            }
////        } else {
////            updateNeuron(closest, newData, strategy);
////        }
//
//        updateNeuron(closestI, newData, strategy);
//        return neurons.get(closestI);
//    }
//
//
//    /**
//     * Turns out it is useless. Does not learn continuousely, it can't classify new situations
//     *
//     * @param newData
//     * @return neuron on which the new data has been mapped to
//     */
//    public Neuron classifySituation(Neuron newData) {
//        int closestI = 0;
//        int closestJ = 0;
//
//        double minDistance = Double.POSITIVE_INFINITY;
//
//        for (int row = 0; row < elementsNo; row++) {
//
//                Neuron neuron = neurons.get(row);
//                double newDistance = neuron.computeEuclideanDistanceFromNeuronAsSingleValue(newData);
//                if (minDistance > newDistance) {
//                    minDistance = newDistance;
//                    closestI = row;
//                }
//
//        }
//
//
//        return neurons.get(closestI);
//    }
//
//
//    public void updateNeuron(int row, Neuron neuron, SOMStrategy strategy) {
//        final Neuron source = neurons.get(row) ;
//        source.updateNeuron(neuron);
//        updateNeuron(row, neuron, strategy, 1);
////        Integer[] usageMeanAndStdDev = getUsageMeanAndStdDeviation();
//////        source.updateUsageLevel(usageMeanAndStdDev[0],usageMeanAndStdDev[1]);
////        final Integer averageUsageLevel = usageMeanAndStdDev[0];
////        final Integer standardDeviation = usageMeanAndStdDev[1];
////        Thread t = new Thread() {
////            @Override
////            public void run() {
////                int mappedSituations = source.getMappedWeights().size();
////
////                Integer partialDeviation = new Double(standardDeviation * 0.7).intValue();
////
////                if (mappedSituations > 0) {
////                    Integer distanceFromMean = mappedSituations - averageUsageLevel;
////
////                    //if more usage than average
////                    if (distanceFromMean >= 0) {
////                        if (distanceFromMean > standardDeviation) {
////                            source.setUsageLevel(NeuronUsageLevel.CONTINUOUSLY);
////                        } else if (distanceFromMean >= partialDeviation && distanceFromMean <= standardDeviation) {
////                            source.setUsageLevel(NeuronUsageLevel.OFTEN);
////                        } else {
////                            source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
////                        }
////                    } else {
////                        //if less usage than average
////                        Integer distanceAbsValue = Math.abs(distanceFromMean);
////                        if (distanceAbsValue > standardDeviation) {
////                            source.setUsageLevel(NeuronUsageLevel.VERY_RARE);
////                        } else if (distanceAbsValue >= partialDeviation && Math.abs(distanceFromMean) <= standardDeviation) {
////                            source.setUsageLevel(NeuronUsageLevel.RARE);
////                        } else {
////                            source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
////                        }
////                    }
////                } else {
////                    source.setUsageLevel(NeuronUsageLevel.NEVER);
////                }
////            }
////        };
////
////        t.run();
//    }
//
//
//    protected void updateNeuron(int row,Neuron neuron, SOMStrategy strategy, int level) {
//        Neuron source = neurons.get(row);
//
//        List<Double> weights = source.getWeights();
//        List<Double> neuronWeights = neuron.getWeights();
//        Double distanceRestraintFactor = strategy.getDistanceRestraintFactor(level, 4);
//        Double learningFactor = strategy.geLearningRestraintFactor(level);
//
//        for (int i = 0; i < weights.size(); i++) {
//            Double oldVal = weights.get(i);
//            Double newVal = oldVal + distanceRestraintFactor * learningFactor * (neuronWeights.get(i) - oldVal);
//            weights.set(i, newVal);
//        }
//
//
//        if (level < strategy.getNeighbourhoodSize()) {
//            level++;
//
//
////update neighbours if needed
//            {
//                int leftNeighbourI = row;
//
//
//                if (leftNeighbourI >= 0 && leftNeighbourI < elementsNo ) {
//                    updateNeuron(leftNeighbourI,  neuron, strategy, level);
//                }
//            }
//
//
//            {
//                int rightNeighbourI = row;
//
//
//                if (rightNeighbourI >= 0 && rightNeighbourI < elementsNo ) {
//                    updateNeuron(rightNeighbourI, neuron, strategy, level);
//                }
//            }
//
//
//
//        }
//
//    }
//
//
//    public Iterator<Neuron> iterator() {
//        return neurons.iterator();
//    }
//
//
//
//    public String toString() {
//        String description = "";
//
//        for (int row = 0; row < elementsNo; row++) {
//
//                description += neurons.get(row).getWeights() + ", ";
//
//            description += "\n";
//        }
//
//        return description;
//
//    }
//
//
//    public String toStringUsageLevels() {
//        String description = "";
//
//        for (int row = 0; row < elementsNo; row++) {
//                description +=  neurons.get(row).getUsageLevel() + ", ";
//            description += "\n";
//        }
//
//        return description;
//
//    }
//
//
//    public Neuron[] cloneNeurons() {
//        Neuron[]clones = new Neuron[elementsNo];
//
//        for (int i = 0; i < elementsNo; i++) {
//
//                Neuron clone = new Neuron();
//                Neuron source = neurons.get(i);
//                ArrayList<Double> clonedWeights = new ArrayList<Double>();
//                for (Double aDouble : source.getWeights()) {
//                    clonedWeights.add(new Double(aDouble.doubleValue()));
//                }
//                clone.setWeights(clonedWeights);
//                clones[i] = clone;
//        }
//        return clones;
//    }
//
//
//    /**
//     * @param clone the SOM to be filled with data from this source
//     */
//    public void clone(OneDimSOM clone) {
//        Neuron[] clones = new Neuron[elementsNo];
//
//        for (int i = 0; i < elementsNo; i++) {
//                Neuron neuronClone = new Neuron();
//                Neuron source = neurons.get(i);
//                ArrayList<Double> clonedWeights = new ArrayList<Double>();
//                for (Double aDouble : source.getWeights()) {
//                    clonedWeights.add(new Double(aDouble.doubleValue()));
//                }
//                neuronClone.setWeights(clonedWeights);
//                clones[i] = neuronClone;
//        }
//
//
//        clone.elementsNo = elementsNo;
//        clone.maxNeighboursNo = maxNeighboursNo;
//        clone.maxWeightValue = maxWeightValue;
//        clone.minWeightValue = minWeightValue;
//        clone.random = random;
//        clone.strategy = strategy;
//        clone.neurons = new ArrayList<Neuron>();
//        for (int i = 0; i < elementsNo; i++) {
//            neurons.add(clones[i]);
//        }
//
//        clone.weightsNo = weightsNo;
//    }
//
//
//}
