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

import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElSgnFunction.som.strategy.SOMStrategy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class SOM implements Iterable<Neuron> {
    //    private Neuron root;
    //    private int initialSize;
    protected int maxNeighboursNo = 4;
    protected int elementsNo;
    protected int weightsNo;
    protected int minWeightValue;
    protected int maxWeightValue;
    protected Random random;
    protected SOMStrategy strategy;
    protected Neuron[][] neurons;
    protected Double toleranceRange;
    protected final AtomicInteger numberOfMappedSituations = new AtomicInteger(0);
    protected DecimalFormat decimalFormat = new DecimalFormat("#.###");

    {
        random = new Random();
    }


    protected SOM() {
    }

    public SOM(int elementsNo, int weightsNo, int minWeightValue, int maxWeightValue, SOMStrategy strategy) {
        this.elementsNo = elementsNo;
//        this.maxNeighboursNo = maxNeighboursNo;
        this.weightsNo = weightsNo;
        this.minWeightValue = minWeightValue;
        this.maxWeightValue = maxWeightValue;
        this.strategy = strategy;
        toleranceRange = strategy.getToleranceRange();

        ArrayList<String> generatedWords = new ArrayList<String>();
        int sumPerCell = maxWeightValue / elementsNo;


        neurons = new Neuron[elementsNo][elementsNo];

        for (int i = 0; i < elementsNo; i++) {
            neurons[i] = new Neuron[elementsNo];
            int sumPerCurrentCell = i*sumPerCell;

            for (int j = 0; j < elementsNo; j++) {
                String generatedWord = "";

                Neuron newNeuron = new Neuron();
                ArrayList<Double> weights = new ArrayList<Double>();
                do {
                   //generate elementsNo elements that sum up to sumPerCurrentCell

                    ArrayList<Double> sumElements =  new ArrayList<Double>();
                    int remaining  = sumPerCurrentCell;

                    for(int k = 0; k < weightsNo-1; k++){
                        if(remaining>0){
                            int sumElement = random.nextInt(remaining);
                            sumElements.add((double)sumElement);
                            remaining -= sumElement;
                        }else{
                            sumElements.add(0d);
                        }
                    }
                    sumElements.add((double)remaining);

                    weights.clear();
                    for (int k = 0; k < weightsNo; k++) {

                    weights.add(0d);
//                    weights.add((double)i);
//                        weights.add(sumElements.remove(random.nextInt(sumElements.size())));
//                        weights.add((double) random.nextInt(maxWeightValue) + minWeightValue);
//                    weights.add(Double.parseDouble(i + "." + j)/2);
                    }
                    generatedWord = weights.toString();
                } while (generatedWords.contains(generatedWord));

                newNeuron.setWeights(weights);

                neurons[i][j] = newNeuron;
            }
        }
//
//        Neuron middle =  neurons[elementsNo/2][elementsNo/2];
//        middle.getWeights().clear();
//        for(int k = 0; k < weightsNo; k++){
//            middle.getWeights().add((double)maxWeightValue);
//        }


    }


    public SOM(ArrayList<ArrayList<Double>> initialValues, SOMStrategy strategy) {
        this.elementsNo = new Double(Math.sqrt(initialValues.size())).intValue();

        this.strategy = strategy;

        neurons = new Neuron[elementsNo][elementsNo];

        for (int i = 0; i < elementsNo; i++) {
            neurons[i] = new Neuron[elementsNo];
            for (int j = 0; j < elementsNo; j++) {

                Neuron newNeuron = new Neuron();
                ArrayList<Double> weights = new ArrayList<Double>();
                for (int k = 0; k < weightsNo; k++) {
                    weights.addAll(initialValues.remove(0));
//                    weights.add(Double.parseDouble(i + "." + j));
                }
                newNeuron.setWeights(weights);

                neurons[i][j] = newNeuron;
            }
        }

    }

    public int getElementsNo() {
        return elementsNo;
    }

    public Neuron[][] getNeurons() {
        return neurons;
    }

    //    public SOM(int maxNeighboursNo, int weightsNo, int minWeightValue, int maxWeightValue, SOMSOMStrategy strategy) {
//        this.maxNeighboursNo = maxNeighboursNo;
//        this.weightsNo = weightsNo;
//        this.minWeightValue = minWeightValue;
//        this.maxWeightValue = maxWeightValue;
//        this.strategy = strategy;
//        root = new Neuron();
//        ArrayList<Double> weights = new ArrayList<Double>();
//        for (int i = 0; i < weightsNo; i++) {
//            weights.add(0d);
//        }
//        root.setWeights(weights);
//    }

//    public Neuron getRoot() {
//        return root;
//    }

    public ArrayList<Double> getWeightsMean() {
        ArrayList<Double> weightsMean = new ArrayList<Double>();
        int nodesCount = 0;
        for (int i = 0; i < weightsNo; i++) {
            weightsMean.add(i, 0d);
        }

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                List<Double> neuronWeights = neurons[row][column].getWeights();
                for (int i = 0; i < weightsNo; i++) {
                    weightsMean.set(i, weightsMean.get(i) + Math.abs(neuronWeights.get(i)));
                }
                nodesCount++;
            }
        }


        for (int i = 0; i < weightsNo; i++) {
            weightsMean.set(i, weightsMean.get(i) / nodesCount);
        }

        return weightsMean;
    }


    public Double getWeightsMeanAsSingleValue() {
        ArrayList<Double> weightsMean = new ArrayList<Double>();
        int nodesCount = 0;
        for (int i = 0; i < weightsNo; i++) {
            weightsMean.add(i, 0d);
        }

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                List<Double> neuronWeights = neurons[row][column].getWeights();
                for (int i = 0; i < weightsNo; i++) {
                    weightsMean.set(i, weightsMean.get(i) + neuronWeights.get(i));
                }
                nodesCount++;
            }
        }

        Double value = 0d;
        for (int i = 0; i < weightsNo; i++) {
            value += weightsMean.get(i) / nodesCount;
        }


        return value;
    }


    public ArrayList<Double> getStandardVariance(final ArrayList<Double> weightsMean) {
        ArrayList<Double> weightsStandardVariance = new ArrayList<Double>();
        int nodesCount = 0;

        for (int i = 0; i < weightsNo; i++) {
            weightsStandardVariance.add(i, 0d);
        }

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                List<Double> neuronWeights = neurons[row][column].getWeights();
                for (int i = 0; i < weightsNo; i++) {
                    //squared distance from the mean
                    weightsStandardVariance.set(i, Math.pow(weightsMean.get(i) - neuronWeights.get(i), 2));
                }
                nodesCount++;
            }
        }

        for (int i = 0; i < weightsNo; i++) {
            weightsStandardVariance.set(i, weightsStandardVariance.get(i) / nodesCount);
        }

        return weightsStandardVariance;
    }


//    //actually computes Average Absolute Deviation
//    public Double getStandardVarianceAsSingleValue() {
//        ArrayList<Double> weightsMean = getWeightsMean();
//        Double weightsStandardVariance = 0d;
//        int nodesCount = 0;
//
//
//        for (int row = 0; row < elementsNo; row++) {
//            for (int column = 0; column < elementsNo; column++) {
//                ArrayList<Double> neuronWeights = neurons[row][column].getWeights();
//                for (int i = 0; i < weightsNo; i++) {
//                    //squared distance from the mean
//                    weightsStandardVariance += Math.abs(weightsMean.get(i) - neuronWeights.get(i));
//                }
//                nodesCount++;
//            }
//        }
//
//        weightsStandardVariance /= nodesCount;
//        Double weightsStandardDeviation =  weightsStandardVariance;
//
//        return weightsStandardDeviation;
//    }


    //computes Standard standard deviation
    public Double getStandardVarianceAsSingleValue() {
        ArrayList<Double> weightsMean = getWeightsMean();
        Double weightsStandardVariance = 0d;
        int nodesCount = 0;


        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                List<Double> neuronWeights = neurons[row][column].getWeights();
                for (int i = 0; i < weightsNo; i++) {
                    //squared distance from the mean
                    weightsStandardVariance += Math.pow(weightsMean.get(i) - neuronWeights.get(i), 2);
                }
                nodesCount++;
            }
        }

        weightsStandardVariance /= nodesCount;
        Double weightsStandardDeviation = Math.sqrt(weightsStandardVariance);

        return weightsStandardDeviation;
    }

    ////computes rare sit which are behind the standard deviation from the euclidean distance with their neighbours
//    public List<Neuron> getRareSituations() {
//
//        Double mean = getWeightsMeanAsSingleValue();
//        Double standardDeviation = getStandardVarianceAsSingleValue();
//
//        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();
//
//        for (int row = 0; row < elementsNo; row++) {
//            for (int column = 0; column < elementsNo; column++) {
//                ArrayList<Double> neuronWeights = neurons[row][column].getWeights();
//                Double weightsValue = 0d;
//                for (int i = 0; i < weightsNo; i++) {
//                    //squared distance from the mean
//                    weightsValue += neuronWeights.get(i);
//                }
//
//                if(Math.abs(weightsValue-mean)>=standardDeviation){
//                    rareSituations.add(neurons[row][column]);
//                }
//
//            }
//        }
//
//        return rareSituations;
//        }

    /**
     * @return nodes mapping situations which have mapped situations deviation > 80% std deviation
     */
    public List<Neuron> getAlmostRareSituations() {

        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();

        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
        for (Neuron neuron : this) {
            int mappedSituations = neuron.getMappedWeights();
            if (mappedSituations > 0) {
                encounteredSituationsPerNeuron.add(neuron.getMappedWeights());
            }
        }

        //compute mean of the number of mapped situations per neuron
        Integer mean = 0;
        for (Integer integer : encounteredSituationsPerNeuron) {
            mean += integer;
        }

        mean /= encounteredSituationsPerNeuron.size();

        //compute variance
        Double variance = 0d;
        for (Integer integer : encounteredSituationsPerNeuron) {
            variance += Math.pow(mean - integer, 2);
        }

        variance /= encounteredSituationsPerNeuron.size();

        Integer stdDeviation = new Double(Math.sqrt(variance)).intValue();
        Integer partialDeviation = new Double(stdDeviation * toleranceRange).intValue();

        for (Neuron neuron : this) {
            int mappedSituations = neuron.getMappedWeights();
            if (mappedSituations > 0) {
                Integer distanceFromMean = mappedSituations - mean;
                if (distanceFromMean < 0 && Math.abs(distanceFromMean) >= partialDeviation && Math.abs(distanceFromMean) <= stdDeviation) {
                    rareSituations.add(neuron);
                }
            }
        }

        return rareSituations;
    }



    public List<Neuron> getRareSituations() {

        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();

        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
        for (Neuron neuron : this) {
            int mappedSituations = neuron.getMappedWeights();
            if (mappedSituations > 0) {
                encounteredSituationsPerNeuron.add(neuron.getMappedWeights());
            }
        }

        //compute mean of the number of mapped situations per neuron
        Integer mean = 0;
        for (Integer integer : encounteredSituationsPerNeuron) {
            mean += integer;
        }

        mean /= encounteredSituationsPerNeuron.size();

        //compute variance
        Double variance = 0d;
        for (Integer integer : encounteredSituationsPerNeuron) {
            variance += Math.pow(mean - integer, 2);
        }

        variance /= encounteredSituationsPerNeuron.size();

        Integer stdDeviation = new Double(Math.sqrt(variance)).intValue();


        for (Neuron neuron : this) {
            int mappedSituations = neuron.getMappedWeights();
            if (mappedSituations > 0) {
                Integer distanceFromMean = mappedSituations - mean;
                if (distanceFromMean < 0 && Math.abs(distanceFromMean) >= stdDeviation) {
                    rareSituations.add(neuron);
                }
            }
        }

        return rareSituations;
    }

    public Integer[] getUsageMeanAndStdDeviation() {

        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
        for (Neuron neuron : this) {
            int mappedSituations = neuron.getMappedWeights();
            if (mappedSituations > 0) {
                encounteredSituationsPerNeuron.add(neuron.getMappedWeights());
            }
        }

        //compute mean of the number of mapped situations per neuron
        Integer mean = 0;
        for (Integer integer : encounteredSituationsPerNeuron) {
            mean += integer;
        }

        mean /= encounteredSituationsPerNeuron.size();

        //compute ABSOLUTE AVERAGE deviation
        Double variance = 0d;
        for (Integer integer : encounteredSituationsPerNeuron) {
            variance += Math.abs(mean - integer);
        }

        variance /= encounteredSituationsPerNeuron.size();

        Integer stdDeviation = variance.intValue();//new Double(Math.sqrt(variance)).intValue();

        return new Integer[]{mean,stdDeviation};
    }



    //in order to retrain the map, it clears the previous mapped values
    //or just to remove values used in training
    public void clearMappings() {
        for (Neuron neuron : this) {
            neuron.setMappedWeights(0);
        }
    }




    public List<Neuron> getDominantSituations() {

        ArrayList<Neuron> rareSituations = new ArrayList<Neuron>();

        ArrayList<Integer> encounteredSituationsPerNeuron = new ArrayList<Integer>();
        for (Neuron neuron : this) {
            encounteredSituationsPerNeuron.add(neuron.getMappedWeights());
        }

        //compute mean of the number of mapped situations per neuron
        Integer mean = 0;
        for (Integer integer : encounteredSituationsPerNeuron) {
            mean += integer;
        }

        mean /= encounteredSituationsPerNeuron.size();

        //compute variance
        Double variance = 0d;
        for (Integer integer : encounteredSituationsPerNeuron) {
            variance += Math.pow(mean - integer, 2);
        }

        variance /= encounteredSituationsPerNeuron.size();

        Integer stdDeviation = new Double(Math.sqrt(variance)).intValue();


        for (Neuron neuron : this) {
            Integer distanceFromMean = neuron.getMappedWeights() - mean;
            if (distanceFromMean >= stdDeviation) {
                rareSituations.add(neuron);
            }
        }

        return rareSituations;
    }


    /**
     *
     * @param newData
     * @return neuron on which the new data has been mapped to
     */
    public synchronized Neuron updateMap(Neuron newData) {
        //add 1 to the number of mapped situations
        numberOfMappedSituations.incrementAndGet();

        int closestI = 0;
        int closestJ = 0;

        double minDistance = Double.POSITIVE_INFINITY;

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                Neuron neuron = neurons[row][column];
                double newDistance = neuron.computeEuclideanDistanceFromNeuronAsSingleValue(newData);
                if (minDistance > newDistance) {
                    minDistance = newDistance;
                    closestI = row;
                    closestJ = column;
                }
            }
        }

 
        updateNeuron(closestI, closestJ, newData, strategy);
        return neurons[closestI][closestJ];
    }


    /**
     * Does not classify new situations
     * @param newData
     * @return neuron on which the new data has been mapped to
     */
    public Neuron classifySituation(Neuron newData) {
        int closestI = 0;
        int closestJ = 0;

        double minDistance = Double.POSITIVE_INFINITY;

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                Neuron neuron = neurons[row][column];
                double newDistance = neuron.computeEuclideanDistanceFromNeuronAsSingleValue(newData);
                if (minDistance > newDistance) {
                    minDistance = newDistance;
                    closestI = row;
                    closestJ = column;
                }
            }
        }

        //only add to encounters if the map is learning. otherwise it still changes in time.
//        neurons[closestI][closestJ].addMappedWeights(newData.getWeights());
        return neurons[closestI][closestJ];
    }


    public void updateNeuron(int row, int column, Neuron neuron, SOMStrategy strategy) {
        final Neuron source = neurons[row][column];
        source.updateNeuron(neuron);
        updateNeuron(row, column, neuron, strategy, 1);
        Integer[] usageMeanAndStdDev = getUsageMeanAndStdDeviation();
//        source.updateUsageLevel(usageMeanAndStdDev[0],usageMeanAndStdDev[1]);
        final Integer averageUsageLevel = usageMeanAndStdDev [ 0 ];
        final Integer standardDeviation = usageMeanAndStdDev [ 1 ];
        Thread t = new Thread(){
            @Override
            public void run() {
                int mappedSituations = source.getMappedWeights();

                Integer partialDeviation = new Double(standardDeviation * toleranceRange).intValue();

                if (mappedSituations > 0) {
                    Integer distanceFromMean = mappedSituations - averageUsageLevel;

                    //if more usage than average
                    if (distanceFromMean >= 0) {
                        if (distanceFromMean > standardDeviation) {
                            source.setUsageLevel(NeuronUsageLevel.RARE);
                        } else if (distanceFromMean >= partialDeviation && distanceFromMean <= standardDeviation) {
                            source.setUsageLevel(NeuronUsageLevel.RARE);
                        } else{
                            source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
                        }
                    } else {
                        //if less usage than average
                        Integer distanceAbsValue = Math.abs(distanceFromMean);
                        if (distanceAbsValue > standardDeviation) {
                            source.setUsageLevel(NeuronUsageLevel.RARE);
                        } else if (distanceAbsValue >= partialDeviation && Math.abs(distanceFromMean) <= standardDeviation) {
                            source.setUsageLevel(NeuronUsageLevel.RARE);
                        } else{
                            source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
                        }
                    }
                } else {
                    source.setUsageLevel(NeuronUsageLevel.RARE);
                }
            }
        };

        t.run();
    }


    protected void updateNeuron(int row, int column, Neuron neuron, SOMStrategy strategy, int level) {
        Neuron source = neurons[row][column];

        List<Double> weights = source.getWeights();
        List<Double> neuronWeights = neuron.getWeights();
        Double distanceRestraintFactor = strategy.getDistanceRestraintFactor(level, 4);
        Double learningFactor = strategy.geLearningRestraintFactor(level);

        for (int i = 0; i < weights.size(); i++) {
            Double oldVal = weights.get(i);
            //to analzye best values for distanceRestraintFactor
            //check if the two neurons have same cardinality
            if(neuronWeights.size()>i){
                Double newVal = oldVal + distanceRestraintFactor * learningFactor * (neuronWeights.get(i) - oldVal);
                newVal = Double.parseDouble(decimalFormat.format(newVal));
                weights.set(i, newVal);
            }
        }


        if (level < strategy.getNeighbourhoodSize()) {
            level++;


            //update neighbours if needed
            {
                int leftNeighbourI = row;
                int leftNeighbourJ = column - 1;

                if (leftNeighbourI >= 0 && leftNeighbourJ >= 0 && leftNeighbourI < elementsNo && leftNeighbourJ < elementsNo) {
                    updateNeuron(leftNeighbourI, leftNeighbourJ, neuron, strategy, level);
                }
            }


            {
                int rightNeighbourI = row;
                int rightNeighbourJ = column + 1;

                if (rightNeighbourI >= 0 && rightNeighbourJ >= 0 && rightNeighbourI < elementsNo && rightNeighbourJ < elementsNo) {
                    updateNeuron(rightNeighbourI, rightNeighbourJ, neuron, strategy, level);
                }
            }

            {
                int topNeighbourI = row - 1;
                int topNeighbourJ = column;


                if (topNeighbourI >= 0 && topNeighbourJ >= 0 && topNeighbourI < elementsNo && topNeighbourJ < elementsNo) {
                    updateNeuron(topNeighbourI, topNeighbourJ, neuron, strategy, level);
                }
            }
            {
                int bottomNeighbourI = row + 1;
                int bottomNeighbourJ = column;
                if (bottomNeighbourI >= 0 && bottomNeighbourJ >= 0 && bottomNeighbourI < elementsNo && bottomNeighbourJ < elementsNo) {
                    updateNeuron(bottomNeighbourI, bottomNeighbourJ, neuron, strategy, level);
                }
            }

        }

    }


    public Iterator<Neuron> iterator() {
        return new SOMIterator(this);
    }

    private class SOMIterator implements Iterator<Neuron> {

        private int currentRow = 0;
        private int currentColumn = 0;


        private SOMIterator(SOM som) {

        }

        
        public boolean hasNext() {
            return currentRow != elementsNo && currentColumn != elementsNo;
        }

        
        public Neuron next() {
            if (!hasNext()) {
                return null;
            }
            Neuron next = neurons[currentRow][currentColumn];
            currentColumn++;
            if (currentColumn == elementsNo) {
                if (currentRow != elementsNo) {
                    currentColumn = 0;
                    currentRow++;
                }
            }

            return next;

        }

        /**
         * NOT SUPPORTED
         */
        
        public void remove() {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    public String toString() {
        String description = "";

        DecimalFormat myFormatter = new DecimalFormat("#.##");
        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {

                description += "[ ";

                for(Double weight : neurons[row][column].getWeights()){
                    description += myFormatter.format(weight) + ", " ;
                }
                description += " ]";
            }
            description += "\n";
        }

        return description;

    }


    public String toStringUsageLevels() {
        String description = "";

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                description += neurons[row][column].getUsageLevel() + ", ";
            }
            description += "\n";
        }

        return description;

    }


    public Neuron[][] cloneNeurons(){
        Neuron[][]  clones = new Neuron[elementsNo][elementsNo];

        for (int i = 0; i < elementsNo; i++) {
            clones[i] = new Neuron[elementsNo];
            for (int j = 0; j < elementsNo; j++) {
                Neuron clone = new Neuron();
                Neuron source = neurons[i][j];
                ArrayList<Double> clonedWeights = new ArrayList<Double>();
                for(Double aDouble : source.getWeights()){
                    clonedWeights.add(new Double(aDouble.doubleValue()));
                }
                clone.setWeights(clonedWeights);
                clones[i][j] = clone;
            }
        }
        return clones;
    }


    /**
     *
     * @param clone the SOM to be filled with data from this source
     */
    public void clone(SOM clone){
        Neuron[][]  clones = new Neuron[elementsNo][elementsNo];

        for (int i = 0; i < elementsNo; i++) {
            clones[i] = new Neuron[elementsNo];
            for (int j = 0; j < elementsNo; j++) {
                Neuron neuronClone = new Neuron();
                Neuron source = neurons[i][j];
                ArrayList<Double> clonedWeights = new ArrayList<Double>();
                for(Double aDouble : source.getWeights()){
                    clonedWeights.add(new Double(aDouble.doubleValue()));
                }
                neuronClone.setWeights(clonedWeights);
                clones[i][j] = neuronClone;
            }
        }


        clone.elementsNo = elementsNo;
        clone.maxNeighboursNo  = maxNeighboursNo;
        clone.maxWeightValue = maxWeightValue;
        clone.minWeightValue = minWeightValue;
        clone.random = random;
        clone.strategy = strategy;
        clone.neurons =clones;
        clone.weightsNo=weightsNo;
    }


}
