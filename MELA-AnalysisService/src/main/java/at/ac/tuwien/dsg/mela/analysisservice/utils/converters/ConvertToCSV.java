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
package at.ac.tuwien.dsg.mela.analysisservice.utils.converters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.ElasticitySpace;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElPthwFunction.InMemoryEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElSgnFunction.som.entities.Neuron;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
public class ConvertToCSV {

    /**
     * File arranged as LEVEL NAME, LEVEL INDEX, USAGE per group, and then
     * values, and then for each metric the DOMINANT,RARE, and NEUTRAL values
     * split in columns
     *
     * @param groups
     * @param targetMetrics
     * @param destinationFile
     * @throws IOException
     */
    public static void writeCSVFromElasticitySituationsGroups(List<Neuron> groups, List<Metric> targetMetrics, String destinationFile) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destinationFile));
        String columns = null;

        //create new list which I can sort
        List<Neuron> sortedAfterOccurrence = new ArrayList<Neuron>();
        sortedAfterOccurrence.addAll(groups);

        //sort list after usage level
        Collections.sort(sortedAfterOccurrence, new Comparator<Neuron>() {
            public int compare(Neuron neuron1, Neuron neuron2) {
                if (neuron1.getUsageLevel().equals(neuron2.getUsageLevel())) {
                    return neuron1.getUsagePercentage().compareTo(neuron2.getUsagePercentage());
                } else {
                    return neuron1.getUsageLevel().compareTo(neuron2.getUsageLevel());
                }
            }
        });

//
        for (Neuron neuron : sortedAfterOccurrence) {
            //if first time entering loop, write the columns
            if (columns == null) {
                columns = "USAGE_LEVEL\tUSAGE_LEVEL_INDEX\tUSAGE_PERCENTAGE";
                for (Metric metric : targetMetrics) {
                    columns += "\t" + metric.getName();
                }
                for (Metric metric : targetMetrics) {
                    columns += "\tDOMINANT" + metric.getName() + "\tNEUTRAL" + metric.getName() + "\tRARE" + metric.getName();
                }
                bufferedWriter.write(columns);
                bufferedWriter.newLine();
            }
            //construct information line

            String line = "" + neuron.getUsageLevel();
            switch (neuron.getUsageLevel()) {
                case DOMINANT:
                    line += "\t1";
                    break;
                case NEUTRAL:
                    line += "\t0";
                    break;
                case RARE:
                    line += "\t-1";
                    break;

            }
            line += "\t" + neuron.getUsagePercentage();
            for (Double value : neuron.getWeights()) {
                line += "\t" + value;
            }


            //for each metric write the encounter categories
            for (Double value : neuron.getWeights()) {
                switch (neuron.getUsageLevel()) {
                    case DOMINANT:
                        line += "\t" + value + "\t0\t0";
                        break;
                    case NEUTRAL:
                        line += "\t0\t" + value + "\t0";
                        break;
                    case RARE:
                        line += "\t0\t0\t" + value;
                        break;

                }
            }

            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();


    }

    /**
     * File arranged as LEVEL NAME, LEVEL INDEX, USAGE per group, and then
     * values, and then for each metric the DOMINANT,RARE, and NEUTRAL values
     * split in columns
     *
     * @param elasticitySignature
     * @param destinationFile
     * @throws IOException
     */
    public static void writeCSVFromElasticitySignature(List<InMemoryEncounterRateElasticityPathway.SignatureEntry> elasticitySignature, String destinationFile) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destinationFile));
        String columns = null;

        //create new list which I can sort
        List<InMemoryEncounterRateElasticityPathway.SignatureEntry> sortedAfterOccurrence = new ArrayList<InMemoryEncounterRateElasticityPathway.SignatureEntry>();
        sortedAfterOccurrence.addAll(elasticitySignature);

//        //sort list after usage level
//        Collections.sort(sortedAfterOccurrence, new Comparator<EncounterRateElasticitySignature.SignatureEntry>() {
//            @Override
//            public int compare(EncounterRateElasticitySignature.SignatureEntry signatureEntry, EncounterRateElasticitySignature.SignatureEntry signatureEntry1) {
//                return signatureEntry.getMappedNeuron().getUsagePercentage().compareTo(signatureEntry1.getMappedNeuron().getUsagePercentage());
//            }
//        });

        //sort after values
        Collections.sort(sortedAfterOccurrence, new Comparator<InMemoryEncounterRateElasticityPathway.SignatureEntry>() {
            public int compare(InMemoryEncounterRateElasticityPathway.SignatureEntry signatureEntry, InMemoryEncounterRateElasticityPathway.SignatureEntry signatureEntry1) {
//                Double sum1 = 0d;
//                Double sum2 = 0d;
//
//                for(MetricValue value : signatureEntry.getClassifiedSituation().values()){
//                    sum1 += Math.abs(Double.parseDouble(value.getValueRepresentation()));
//                }
//
//                for(MetricValue value : signatureEntry1.getClassifiedSituation().values()){
//                    sum2 += Math.abs(Double.parseDouble(value.getValueRepresentation()));
//                }
//
//                return sum1.compareTo(sum2);
                Neuron neuron1 = signatureEntry.getMappedNeuron();
                Neuron neuron2 = signatureEntry1.getMappedNeuron();
                if (neuron1.getUsageLevel().equals(neuron2.getUsageLevel())) {
                    return neuron1.getUsagePercentage().compareTo(neuron2.getUsagePercentage());
                } else {
                    return neuron1.getUsageLevel().compareTo(neuron2.getUsageLevel());
                }
            }
        });

        for (InMemoryEncounterRateElasticityPathway.SignatureEntry signatureEntry : sortedAfterOccurrence) {
            //if first time entering loop, write the columns
            if (columns == null) {
                columns = "USAGE_LEVEL\tUSAGE_LEVEL_INDEX\tUSAGE_PERCENTAGE";
                for (Metric metric : signatureEntry.getClassifiedSituation().keySet()) {
                    columns += "\t" + metric.getName();
                }
                for (Metric metric : signatureEntry.getClassifiedSituation().keySet()) {
                    columns += "\tDOMINANT" + metric.getName() + "\tNEUTRAL" + metric.getName() + "\tRARE" + metric.getName();
                }
                bufferedWriter.write(columns);
                bufferedWriter.newLine();
            }
            //construct information line
            Neuron neuron = signatureEntry.getMappedNeuron();
            String line = "" + neuron.getUsageLevel();
            switch (neuron.getUsageLevel()) {
                case DOMINANT:
                    line += "\t1";
                    break;
                case NEUTRAL:
                    line += "\t0";
                    break;
                case RARE:
                    line += "\t-1";
                    break;

            }
            line += "\t" + neuron.getUsagePercentage();
            for (Metric metric : signatureEntry.getClassifiedSituation().keySet()) {
                line += "\t" + signatureEntry.getClassifiedSituation().get(metric).getValueRepresentation();
            }


            //for each metric write the encounter categories
            for (Metric metric : signatureEntry.getClassifiedSituation().keySet()) {
                switch (neuron.getUsageLevel()) {
                    case DOMINANT:
                        line += "\t" + signatureEntry.getClassifiedSituation().get(metric).getValueRepresentation() + "\t0\t0";
                        break;
                    case NEUTRAL:
                        line += "\t0\t" + signatureEntry.getClassifiedSituation().get(metric).getValueRepresentation() + "\t0";
                        break;
                    case RARE:
                        line += "\t0\t0\t" + signatureEntry.getClassifiedSituation().get(metric).getValueRepresentation();
                        break;

                }
            }

            bufferedWriter.write(line);
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();


    }

    /**
     * Metric, Boundary UP, BOUNDARY LOW for all metrics, then if elastic or not
     * boolean, if elastic = 1, else -1 , elastic =
     *
     * @param space the elasticity space from which we extract the info for the
     * service element
     * @param destinationFile the file in which we write the space
     */
    public static void writeElasticitySpaceToCSV(MonitoredElement MonitoredElement, ElasticitySpace space, List<Metric> metricsToWrite, String destinationFile) throws IOException {

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destinationFile));

//        Map<Metric, List<MetricValue>> spaceForElement = space.getMonitoredDataForService(MonitoredElement);
//        if (spaceForElement == null) {
//            return;
//        }
        Map<Metric, MetricValue[]> boundaries = new HashMap<Metric, MetricValue[]>();
        //write file columns
        {
            String columns = "";
            for (Metric metric : metricsToWrite) {
                columns += "\t" + metric.getName() + "\tBOUNDARY_U_" + metric.getName() + "\tBOUNDARY_L_" + metric.getName();
                boundaries.put(metric, space.getSpaceBoundaryForMetric(MonitoredElement, metric));
            }

            columns += "\tELASTIC\tELASTIC_INT";

            bufferedWriter.write(columns);
            bufferedWriter.newLine();
        }

        List<ElasticitySpace.ElasticitySpaceEntry> spaceEntries = space.getSpaceEntries();

        for (ElasticitySpace.ElasticitySpaceEntry entry : spaceEntries) {
            ServiceMonitoringSnapshot snapshot = entry.getServiceMonitoringSnapshot();
            MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = snapshot.getMonitoredData(MonitoredElement);
            String line = "";

            //add metric values to line
            for (Metric metric : metricsToWrite) {
                if (MonitoredElementMonitoringSnapshot.getMetrics().contains(metric)) {
                    MetricValue values = MonitoredElementMonitoringSnapshot.getMetricValue(metric);

                    MetricValue[] boundaryForMetric = boundaries.get(metric);
                    line += "\t" + values + "\t" + boundaryForMetric[1] + "\t" + boundaryForMetric[0];
                }

            }

            boolean isClean = entry.getAnalysisReport().isClean();
            line += "\t" + isClean + "\t" + ((isClean) ? 1 : -1);

            bufferedWriter.write(line);
            bufferedWriter.newLine();


        }

        bufferedWriter.flush();
        bufferedWriter.close();

    }

    public static void writeWholeElasticitySpaceToCSV(MonitoredElement root, ElasticitySpace space, String destinationFile) throws IOException {

        for (MonitoredElement monitoredElement : root) {
            if(monitoredElement.getLevel().equals(MonitoredElement.MonitoredElementLevel.VM)){
                continue;
            }

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destinationFile + "_" + monitoredElement.getId() + ".csv"));

//        Map<Metric, List<MetricValue>> spaceForElement = space.getMonitoredDataForService(MonitoredElement);
//        if (spaceForElement == null) {
//            return;
//        }
            Map<Metric, MetricValue[]> boundaries = new HashMap<Metric, MetricValue[]>();
            Map<Metric, List<MetricValue>> dataForElement = space.getMonitoredDataForService(monitoredElement);
            //write file columns
            {
                String columns = "";
                for (Metric metric : dataForElement.keySet()) {
                    columns += "\t" + metric.getName() + "\tBOUNDARY_U_" + metric.getName() + "\tBOUNDARY_L_" + metric.getName();
                    boundaries.put(metric, space.getSpaceBoundaryForMetric(monitoredElement, metric));
                }

                columns += "\tELASTIC\tELASTIC_INT";

                bufferedWriter.write(columns);
                bufferedWriter.newLine();
            }

            List<ElasticitySpace.ElasticitySpaceEntry> spaceEntries = space.getSpaceEntries();

            for (ElasticitySpace.ElasticitySpaceEntry entry : spaceEntries) {
                ServiceMonitoringSnapshot snapshot = entry.getServiceMonitoringSnapshot();
                MonitoredElementMonitoringSnapshot MonitoredElementMonitoringSnapshot = snapshot.getMonitoredData(monitoredElement);
                String line = "";

                //add metric values to line
                for (Metric metric : dataForElement.keySet()) {
                    if (MonitoredElementMonitoringSnapshot.getMetrics().contains(metric)) {
                        MetricValue values = MonitoredElementMonitoringSnapshot.getMetricValue(metric);

                        MetricValue[] boundaryForMetric = boundaries.get(metric);
                        line += "\t" + values + "\t" + boundaryForMetric[1] + "\t" + boundaryForMetric[0];
                    }

                }

                boolean isClean = entry.getAnalysisReport().isClean();
                line += "\t" + isClean + "\t" + ((isClean) ? 1 : -1);

                bufferedWriter.write(line);
                bufferedWriter.newLine();


            }

            bufferedWriter.flush();
            bufferedWriter.close();
        }
    }
// /**
//     * Metric, Boundary UP, BOUNDARY LOW for all metrics, then if elastic or not boolean, if elastic = 1, else -1
//     * @param space the elasticity space from which we extract the info for the service element
//     * @param MonitoredElement for which the space will be written
//     * @param destinationFile the file in which we write the space
//     */
//    public static void writeElasticitySpaceToCSV(ElasticitySpace space, MonitoredElement MonitoredElement, String destinationFile) throws IOException {
//
//        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(destinationFile));
//
//
//        Map<Metric, List<MetricValue>> spaceForElement = space.getMonitoredDataForService(MonitoredElement);
//        if(spaceForElement == null){
//            return;
//        }
//        Map<Metric,MetricValue[]> boundaries = new HashMap<Metric, MetricValue[]>();
//        //write file columns
//        {
//            String columns = "";
//            for(Metric metric : spaceForElement.keySet()){
//                columns+="\t"+metric.getName() + "\tBOUNDARY_U_"+metric.getName() + "\tBOUNDARY_L_"+metric.getName();
//                boundaries.put(metric,space.getSpaceBoundaryForMetric(MonitoredElement,metric));
//            }
//
//            columns+="\tELASTIC\tELASTIC_INT";
//
//            bufferedWriter.write(columns);
//            bufferedWriter.newLine();
//        }
//
//        List<String> lines = new ArrayList<String>();
//        //classify all monitoring data
//        //need to go trough all monitoring data, and push the classified items, such that I respect the monitored sequence.
//        if (spaceForElement.values().size() == 0) {
//            Logger.getLogger(ConvertToCSV.class.getName()).log(Level.ERROR, "Empty data to classify as elasticity signature");
//            return;
//        }
//        int maxIndex = spaceForElement.values().iterator().next().size();
//
//        for (int i = 0; i < maxIndex; i++) {
//            String line = "";
//
//            //add metric values to line
//            for (Metric metric : spaceForElement.keySet()) {
//                List<MetricValue> values = spaceForElement.get(metric);
//
//                //maybe we have diff value count for different metrics. Not sure when his might happen though.
//                if (values.size() <= i - 1) {
//                    Logger.getLogger(ConvertToCSV.class.getName()).log(Level.ERROR, "Less values for metric " + metric);
//                    break;
//                }
//
//                MetricValue[] boundaryForMetric = boundaries.get(metric);
//                line +="\t" + values.get(i) + "\t"  + boundaryForMetric[1] + "\t" + boundaryForMetric[0];
//
//            }
//
//           line+=
//
//
//        }
//
//
//    }
}
