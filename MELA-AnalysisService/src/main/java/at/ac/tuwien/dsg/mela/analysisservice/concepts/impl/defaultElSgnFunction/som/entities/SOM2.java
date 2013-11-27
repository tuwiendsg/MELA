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
import at.ac.tuwien.dsg.mela.analysisservice.utils.Configuration;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class SOM2 extends SOM {



    public SOM2(int elementsNo, int weightsNo, int minWeightValue, int maxWeightValue, SOMStrategy strategy) {
        super(elementsNo, weightsNo, minWeightValue, maxWeightValue, strategy);
    }

    public SOM2(ArrayList<ArrayList<Double>> initialValues, SOMStrategy strategy) {
        super(initialValues, strategy);
    }

    public SOM2() {

    }

    /**
     *
     * @return the difference in mapped values, such that we have also an overview in how close the mapped values are
     */
    public Integer[] getNeuronsMappedValuesMeanAndStdDeviation() {

        ArrayList<Double> mappedValues = new ArrayList<Double>();
        Double mappedValuesMean  = 0d;

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                Neuron neuron = neurons[row][column];
                Double mappedVal = 0d;
                for(Double weight: neuron.getWeights()){
                    mappedVal += Math.pow(weight,2);
                }
                if(mappedVal < 1d){
                    continue;
                }
                mappedValues.add(mappedVal);
                mappedValuesMean += mappedVal;

            }
        }
        mappedValuesMean /= mappedValues.size();


        Double variance = 0d;
        for (Double integer : mappedValues) {
            variance += Math.abs(integer - mappedValuesMean);
        }

        variance /= mappedValues.size();

        return new Integer[]{mappedValuesMean.intValue(), variance.intValue()};
    }

//
//    //actually computes Average Absolute Variation
//    @Override
//    public Integer[] getUsageMeanAndStdDeviation() {
//        ArrayList<Double> distancesFromNeighbours = new ArrayList<Double>();
//        Double average = 0d;
//
//
//        for (int row = 0; row < elementsNo; row++) {
//            for (int column = 0; column < elementsNo; column++) {
//                Neuron neuron = neurons[row][column];
//                Double distance = 0d;
//
//
//                {
//                    int leftNeighbourI = row;
//                    int leftNeighbourJ = column - 1;
//
//                    if (leftNeighbourI >= 0 && leftNeighbourJ >= 0 && leftNeighbourI < elementsNo && leftNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[leftNeighbourI][leftNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//                }
//
//
//                {
//                    int rightNeighbourI = row;
//                    int rightNeighbourJ = column + 1;
//
//                    if (rightNeighbourI >= 0 && rightNeighbourJ >= 0 && rightNeighbourI < elementsNo && rightNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[rightNeighbourI][rightNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//                }
//
//                {
//                    int topNeighbourI = row - 1;
//                    int topNeighbourJ = column;
//
//
//                    if (topNeighbourI >= 0 && topNeighbourJ >= 0 && topNeighbourI < elementsNo && topNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[topNeighbourI][topNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//                }
//                {
//                    int bottomNeighbourI = row + 1;
//                    int bottomNeighbourJ = column;
//                    if (bottomNeighbourI >= 0 && bottomNeighbourJ >= 0 && bottomNeighbourI < elementsNo && bottomNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[bottomNeighbourI][bottomNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//
//                }
//
//                distancesFromNeighbours.add(distance);
//                average += distance;
//
//            }
//        }
//
//        Double totalMapped = average;
//        average /= distancesFromNeighbours.size();
//        Double sdtDev = 0d;
//        for (Double distance : distancesFromNeighbours) {
//            sdtDev += Math.abs(distance - average);
//        }
//
//        sdtDev /= distancesFromNeighbours.size();
//        System.out.println("TotalDistance " + totalMapped + " average " + average + " stdDev " + sdtDev);
////        sdtDev += Math.sqrt(sdtDev);
//
//        return new Integer[]{average.intValue(), sdtDev.intValue()};
//    }

    //actually computes Average Absolute Variation  on nr of MAPPED SITUATIONS

    /**
     *
     * @return the mean and std deviation of the number of mapped situations pe neuron
     */
    @Override
    public Integer[] getUsageMeanAndStdDeviation() {
        ArrayList<Integer> mappedSituationsNr = new ArrayList<Integer>();
        Integer average = 0;


        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                Neuron neuron = neurons[row][column];
                if(neuron.getMappedWeights() == 0){
                    continue;
                }
                mappedSituationsNr.add(neuron.getMappedWeights());
                average += neuron.getMappedWeights();
            }
        }

        Integer totalMapped = average;

        average = (mappedSituationsNr.size() > 0)?average/mappedSituationsNr.size():average;
        Double sdtDev = 0d;
        for (Integer sitNr : mappedSituationsNr) {
            sdtDev += Math.abs(sitNr - average);
        }

        sdtDev /= mappedSituationsNr.size();
//        sdtDev += Math.sqrt(sdtDev);

//        System.out.println("Mapped " + totalMapped + " average " + average + " stdDev " + sdtDev);

        return new Integer[]{average.intValue(), sdtDev.intValue()};
    }

    public void updateMapUsage() {
        Integer[] usageMeanAndStdDev = getUsageMeanAndStdDeviation();
        Integer[] neuronsMappedValuesMeanAndStdDev = getNeuronsMappedValuesMeanAndStdDeviation();

        final Integer averageUsageLevel = usageMeanAndStdDev[0];
        final Integer standardDeviation = usageMeanAndStdDev[1];

        final Integer neuronsAverageMappedValue = neuronsMappedValuesMeanAndStdDev[0];
        final Integer neuronsMappedValueStandardDeviation = neuronsMappedValuesMeanAndStdDev[1];

        //traverse map and merge any elements which have the SAME value
        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                Neuron neuron = neurons[row][column];
                for (int i = 0; i < elementsNo; i++) {
                    for (int j = 0; j < elementsNo; j++) {
                        if (i != row || j != column) {
                            Neuron neuron2 = neurons[i][j];
                            if (neuron.equals(neuron2)) {
//                                List<List<Double>> l = new ArrayList<List<Double>>();
//                                l.addAll(neuron2.getMappedWeights());
//                                for(List<Double> doubleList : l){
//                                    neuron.updateNeuron(neuron2.getMappedWeights());
//                                }
                                //just increments with 1 the nr of mappedWeights
                                //if the neuron has not been used before, do not inchrease usage
                                if(neuron.getMappedWeights() != 0 ){
                                    neuron.updateNeuron(new Neuron());
                                }
                                neuron2.setMappedWeights(0);
//                                neuron2.getMappedWeights().clear();
                                List<Double> newWeights = neuron.getWeights();
                                for (int k = 0; k < newWeights.size(); k++) {
                                    newWeights.set(k, 0d);
                                }
                            }
                        }
                    }
                }
                
                
            }
        }

        for (int row = 0; row < elementsNo; row++) {
            for (int column = 0; column < elementsNo; column++) {
                final Neuron source = neurons[row][column];

                Thread t = new Thread() {
                    @Override
                    public void run() {

                        int mappedSituations = source.getMappedWeights();

                        Integer partialDeviation = new Double(standardDeviation * 0.7).intValue();

                        if (mappedSituations > 0) {
                            Integer distanceFromMean = mappedSituations - averageUsageLevel;
                            Double percentage = (mappedSituations * 100.d)/numberOfMappedSituations.get();
                            source.setUsagePercentage(percentage);

                            //if more usage than average
                            if (distanceFromMean >= 0) {
                                if (distanceFromMean > standardDeviation) {
                                    source.setUsageLevel(NeuronUsageLevel.DOMINANT);
                                } else if (distanceFromMean >= partialDeviation && distanceFromMean <= standardDeviation) {
                                    source.setUsageLevel(NeuronUsageLevel.DOMINANT);
                                } else {
                                    source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
                                }
                            } else {
                                //if less usage than average
                                Integer distanceAbsValue = Math.abs(distanceFromMean);
                                Double mappedValueDistanceFromAverage = 0d;
                                Double mappedValue = 0d;

                                //compute the sum of the weights square
                                for(Double weight: source.getWeights()){
                                    mappedValue += Math.pow(weight,2);
                                }
                                mappedValueDistanceFromAverage = Math.abs(mappedValue - neuronsAverageMappedValue);

                                if(mappedValueDistanceFromAverage <= neuronsMappedValueStandardDeviation) {
                                    source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
                                }else
                                if (distanceAbsValue > standardDeviation) {
                                    source.setUsageLevel(NeuronUsageLevel.RARE);
                                } else if (distanceAbsValue >= partialDeviation && Math.abs(distanceFromMean) <= standardDeviation) {
                                    source.setUsageLevel(NeuronUsageLevel.RARE);
                                }
                            }
                        } else {
                            source.setUsageLevel(NeuronUsageLevel.RARE);
                        }

                    }
                };
                t.run();

            }

        }
    }

    @Override
    public void updateNeuron(final int row, final int column, final Neuron neuron, SOMStrategy strategy) {
        final Neuron source = neurons[row][column];
        source.updateNeuron(neuron);
        updateNeuron(row, column, neuron, strategy, 1);
//        Integer[] usageMeanAndStdDev = getUsageMeanAndStdDeviation();
////        source.updateUsageLevel(usageMeanAndStdDev[0],usageMeanAndStdDev[1]);
//        final Integer averageUsageLevel = usageMeanAndStdDev[0];
//        final Integer standardDeviation = usageMeanAndStdDev[1];
//
//        Thread t = new Thread() {
//            @Override
//            public void run() {
//
//                //get distance from neighbours
//                Double distance = 0d;
//
//
//                //update neighbours if needed
//                {
//                    int leftNeighbourI = row;
//                    int leftNeighbourJ = column - 1;
//
//                    if (leftNeighbourI >= 0 && leftNeighbourJ >= 0 && leftNeighbourI < elementsNo && leftNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[leftNeighbourI][leftNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//                }
//
//
//                {
//                    int rightNeighbourI = row;
//                    int rightNeighbourJ = column + 1;
//
//                    if (rightNeighbourI >= 0 && rightNeighbourJ >= 0 && rightNeighbourI < elementsNo && rightNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[rightNeighbourI][rightNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//                }
//
//                {
//                    int topNeighbourI = row - 1;
//                    int topNeighbourJ = column;
//
//
//                    if (topNeighbourI >= 0 && topNeighbourJ >= 0 && topNeighbourI < elementsNo && topNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[topNeighbourI][topNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//                }
//                {
//                    int bottomNeighbourI = row + 1;
//                    int bottomNeighbourJ = column;
//                    if (bottomNeighbourI >= 0 && bottomNeighbourJ >= 0 && bottomNeighbourI < elementsNo && bottomNeighbourJ < elementsNo) {
//                        Neuron neighbour = neurons[bottomNeighbourI][bottomNeighbourJ];
//                        distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
//                    }
//
//                }
//
//
//                Integer partialDeviation = new Double(standardDeviation * 0.7).intValue();
//
//
//                Integer distanceFromMean = distance.intValue() - averageUsageLevel;
//                distanceFromMean = (int) Math.abs(distanceFromMean);
//
//                //if more usage than average
//                if (distanceFromMean > standardDeviation) {
//                    source.setUsageLevel(NeuronUsageLevel.VERY_RARE);
//                } else if (distanceFromMean >= partialDeviation && distanceFromMean <= standardDeviation) {
//                    source.setUsageLevel(NeuronUsageLevel.RARE);
//                } else if (distanceFromMean < standardDeviation) {
//                    source.setUsageLevel(NeuronUsageLevel.CONTINUOUSLY);
//                } else if (distanceFromMean <= partialDeviation && distanceFromMean >= standardDeviation) {
//                    source.setUsageLevel(NeuronUsageLevel.OFTEN);
//                } else {
//                    source.setUsageLevel(NeuronUsageLevel.NEUTRAL);
//                }
//
//
//            }
//        };
//
//        t.run();
    }

    public Double getDistanceToNeighbours(Neuron neuron) {

        int row = 0;
        int column = 0;
        boolean identified = false;

        for (int i = 0; i < elementsNo; i++) {
            for (int j = 0; j < elementsNo; j++) {
                Neuron found = neurons[i][j];
                if (found.equals(neuron)) {
                    row = i;
                    column = j;
                    identified = true;
                    break;
                }
            }
        }
        if (!identified) {
            Configuration.getLogger(this.getClass()).log(Level.ERROR, "Neuron not found");
            System.exit(1);
        }
        Double distance = 0d;


        //update neighbours if needed
        {
            int leftNeighbourI = row;
            int leftNeighbourJ = column - 1;

            if (leftNeighbourI >= 0 && leftNeighbourJ >= 0 && leftNeighbourI < elementsNo && leftNeighbourJ < elementsNo) {
                Neuron neighbour = neurons[leftNeighbourI][leftNeighbourJ];
                distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
            }
        }


        {
            int rightNeighbourI = row;
            int rightNeighbourJ = column + 1;

            if (rightNeighbourI >= 0 && rightNeighbourJ >= 0 && rightNeighbourI < elementsNo && rightNeighbourJ < elementsNo) {
                Neuron neighbour = neurons[rightNeighbourI][rightNeighbourJ];
                distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
            }
        }

        {
            int topNeighbourI = row - 1;
            int topNeighbourJ = column;


            if (topNeighbourI >= 0 && topNeighbourJ >= 0 && topNeighbourI < elementsNo && topNeighbourJ < elementsNo) {
                Neuron neighbour = neurons[topNeighbourI][topNeighbourJ];
                distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
            }
        }
        {
            int bottomNeighbourI = row + 1;
            int bottomNeighbourJ = column;
            if (bottomNeighbourI >= 0 && bottomNeighbourJ >= 0 && bottomNeighbourI < elementsNo && bottomNeighbourJ < elementsNo) {
                Neuron neighbour = neurons[bottomNeighbourI][bottomNeighbourJ];
                distance += neuron.computeEuclideanDistanceFromNeuronAsSingleValue(neighbour);
            }

        }

        return Double.parseDouble(decimalFormat.format(distance));
    }

}
