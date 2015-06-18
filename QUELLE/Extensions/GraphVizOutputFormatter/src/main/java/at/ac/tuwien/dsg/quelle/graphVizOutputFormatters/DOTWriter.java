/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
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
package at.ac.tuwien.dsg.quelle.graphVizOutputFormatters;

import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.ServiceUnitConfigurationSolution;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class DOTWriter {

    public static void writeServiceUnit(CloudOfferedService unit, Writer writer) throws IOException {

        String dot = "digraph Neo { \n"
                + "  node [ \n"
                + "fontname = \"Bitstream Vera Sans\" \n"
                + "shape = \"Mrecord\" \n"
                + "fontsize = \"15\" \n"
                + " ] \n"
                + " edge [\n"
                + "    fontname = \"Bitstream Vera Sans\"\n"
                + "    fontsize = \"13\"\n"
                + "  ]";

        DotNode.clear();
        DotNode serviceUnitNode = DotNode.toDotNode(unit);
        dot += serviceUnitNode.toDotString();
        dot += DotNode.getElasticityConfigurationAsDOT(unit, serviceUnitNode);

        dot += "}";

        //write dot
        writer.write(dot);
        writer.flush();
        writer.close();

    }

    public static void writeServiceUnitConfigurationSolution(ServiceUnitConfigurationSolution configurationSolution, Writer writer) throws IOException {

        String dot = "digraph Neo { rankdir=LR; \n"
                + "  node [ \n"
                + "fontname = \"Bitstream Vera Sans\" \n"
                + "shape = \"Mrecord\" \n"
                + "fontsize = \"15\" \n"
                + " ] \n"
                + " edge [\n"
                + "    fontname = \"Bitstream Vera Sans\"\n"
                + "    fontsize = \"13\"\n"
                + "  ]";

        SESConfigurationRecommandationToGraphViz.clear();
        SESConfigurationRecommandationToGraphViz serviceUnitNode = SESConfigurationRecommandationToGraphViz.toDotNode(configurationSolution);
        dot += serviceUnitNode.toDotString();
        dot += "}";

        //write dot
        writer.write(dot);
        writer.flush();
        writer.close();

    }

    public static void writeServiceUnitConfigurationSolutions(List<ServiceUnitConfigurationSolution> configurationSolutions, Writer writer) throws IOException {
        String dot = "digraph Neo { rankdir=LR; \n"
                + "  node [ \n"
                + "fontname = \"Bitstream Vera Sans\" \n"
                + "shape = \"Mrecord\" \n"
                + "fontsize = \"15\" \n"
                + " ] \n"
                + " edge [\n"
                + "    fontname = \"Bitstream Vera Sans\"\n"
                + "    fontsize = \"13\"\n"
                + "  ]";

        SESConfigurationRecommandationToGraphViz.clear();
        for (ServiceUnitConfigurationSolution configurationSolution : configurationSolutions) {
            SESConfigurationRecommandationToGraphViz serviceUnitNode = SESConfigurationRecommandationToGraphViz.toDotNode(configurationSolution);
            dot += serviceUnitNode.toDotString();
        }
        dot += "}";

        //write dot
        writer.write(dot);
        writer.flush();
        writer.close();
    }

    public static void writeTopologyConfigurationSolutions(List<List<ServiceUnitConfigurationSolution>> solutions, String path) throws IOException {

        SESConfigurationRecommandationToGraphViz.clear();

        int configSolsIndex = 0;

        Writer writer = new FileWriter(path + "_" + configSolsIndex + ".dot");
//            configSolsIndex ++;

        String dot = "digraph Neo { rankdir=LR; \n"
                + "  node [ \n"
                + "fontname = \"Bitstream Vera Sans\" \n"
                + "shape = \"Mrecord\" \n"
                + "fontsize = \"15\" \n"
                + " ] \n"
                + " edge [\n"
                + "    fontname = \"Bitstream Vera Sans\"\n"
                + "    fontsize = \"13\"\n"
                + "  ]";
        List<SESConfigurationRecommandationToGraphViz> nodes = new ArrayList<SESConfigurationRecommandationToGraphViz>();;

        for (List<ServiceUnitConfigurationSolution> configurationSolutions : solutions) {

            SESConfigurationRecommandationToGraphViz lastnode = null;
            for (ServiceUnitConfigurationSolution s : configurationSolutions) {
                SESConfigurationRecommandationToGraphViz serviceUnitNode = SESConfigurationRecommandationToGraphViz.toDotNode(s);
                if (!nodes.contains(serviceUnitNode)) {
                    nodes.add(serviceUnitNode);
                    dot += serviceUnitNode.toDotString();
                } else {
                    serviceUnitNode = nodes.get(nodes.indexOf(serviceUnitNode));
                }
                if (lastnode != null) {
                    String rel = SESConfigurationRecommandationToGraphViz.createRelationship(lastnode, serviceUnitNode, "associatedWith");
                    dot += rel;
                }
                lastnode = serviceUnitNode;
            }

//            dot += "}";
//            //write dot
//            writer.write(dot);
//            writer.flush();
//            writer.close();
        }
        dot += "}";
        //write dot
        writer.write(dot);
        writer.flush();
        writer.close();

    }

    public static void writeTopologyConfigurationSolutions(String topologyReqsName, Map<Requirements, List<ServiceUnitConfigurationSolution>> solutions, String path) throws IOException {

        SESConfigurationRecommandationToGraphViz.clear();

//        int configSolsIndex = 0;

        Writer writer = new FileWriter(path +  ".dot");
//            configSolsIndex ++;

        String dot = "digraph Neo { rankdir=TB; \n"
                + "  node [ \n"
                + "fontname = \"Bitstream Vera Sans\" \n"
                + "shape = \"Mrecord\" \n"
                + "fontsize = \"15\" \n"
                + " ] \n"
                + " edge [\n"
                + "    fontname = \"Bitstream Vera Sans\"\n"
                + "    fontsize = \"13\"\n"
                + "  ]";
        List<SESConfigurationRecommandationToGraphViz> nodes = new ArrayList<SESConfigurationRecommandationToGraphViz>();;

        dot += "subgraph cluster_" + topologyReqsName + "{\n" + "label =\"" + topologyReqsName + " Recommendation \"";

        Map<ServiceUnitConfigurationSolution, SESConfigurationRecommandationToGraphViz> allSolutions = new HashMap<>();

        for (Requirements r : solutions.keySet()) {

            List<ServiceUnitConfigurationSolution> configurationSolutions = solutions.get(r);

            for (ServiceUnitConfigurationSolution s : configurationSolutions) {

                SESConfigurationRecommandationToGraphViz serviceUnitNode = SESConfigurationRecommandationToGraphViz.toDotNode(s);
                serviceUnitNode.addMetaInfo(",style=filled,fillcolor=\"#C9E1F1\"");
                allSolutions.put(s, serviceUnitNode);

                if (!nodes.contains(serviceUnitNode)) {
                    nodes.add(serviceUnitNode);
                    dot += "subgraph cluster_" + r.getName() + "{\n" + "label =\"" + r.getName() + " Recommendation \"";

                    dot += serviceUnitNode.toDotString();

                    dot += "}";
                } else {
                    serviceUnitNode = nodes.get(nodes.indexOf(serviceUnitNode));
                }

            }

//            dot += "}";
//            //write dot
//            writer.write(dot);
//            writer.flush();
//            writer.close();
        }

        //create connections
        List<ServiceUnitConfigurationSolution> configurationSolutions = new ArrayList<>(allSolutions.keySet());
        //sort configs first by IaaS, then PaaS, then MaaS
        Collections.sort(configurationSolutions, new Comparator<ServiceUnitConfigurationSolution>() {

            @Override
            public int compare(ServiceUnitConfigurationSolution o1, ServiceUnitConfigurationSolution o2) {
                String o1_type = o1.getServiceUnit().getCategory();
                String o2_type = o2.getServiceUnit().getCategory();
                switch (o1_type) {
                    case "IaaS":
                        switch (o2_type) {
                            case "IaaS":
                                return 0;
                            case "PaaS":
                                return -1;
                            case "MaaS":
                                return -1;
                        }
                    case "PaaS":
                        switch (o2_type) {
                            case "IaaS":
                                return 1;
                            case "PaaS":
                                return 0;
                            case "MaaS":
                                return -1;
                        }

                    case "MaaS":
                        switch (o2_type) {
                            case "IaaS":
                                return 1;
                            case "PaaS":
                                return 1;
                            case "MaaS":
                                return 0;
                        }
                    default:
                        System.out.println("Category " + o1_type + " not recognized");
                        return 0;
                }

            }

        }
        );

        Iterator<ServiceUnitConfigurationSolution> it = configurationSolutions.iterator();

        //first is iaaS, which should be asociated with all
        ServiceUnitConfigurationSolution current = it.next();
        while (it.hasNext()) {
            ServiceUnitConfigurationSolution next = it.next();
            String rel = SESConfigurationRecommandationToGraphViz.createRelationship(allSolutions.get(current), allSolutions.get(next), "associatedWith");
            dot += rel;
        }

        dot += "}";
        dot += "}";
        //write dot
        writer.write(dot);
        writer.flush();
        writer.close();

    }
}
