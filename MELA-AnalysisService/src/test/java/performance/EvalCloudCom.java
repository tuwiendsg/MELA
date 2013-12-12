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
package performance;

import at.ac.tuwien.dsg.mela.analysisservice.concepts.ElasticitySpace;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElPthwFunction.LightweightEncounterRateElasticityPathway;
import at.ac.tuwien.dsg.mela.analysisservice.engines.InstantMonitoringDataAnalysisEngine;
import at.ac.tuwien.dsg.mela.analysisservice.utils.converters.ConvertToCSV;
import at.ac.tuwien.dsg.mela.analysisservice.utils.evalaution.PerformanceReport;
import at.ac.tuwien.dsg.mela.common.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.dataservice.AggregatedMonitoringDataSQLAccess;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.AbstractDataAccess;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccessWithAutoStructureDetection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class EvalCloudCom {

    public static void main(String[] args) throws JAXBException, FileNotFoundException, IOException {
        AbstractDataAccess dataAccess;
        InstantMonitoringDataAnalysisEngine instantMonitoringDataAnalysisEngine;
        ElasticitySpaceFunction elasticitySpaceFunction;
        AggregatedMonitoringDataSQLAccess aggregatedMonitoringDataSQLAccess;
        
//        ConfigurationXMLRepresentation configurationXMLRepresentation = new ConfigurationXMLRepresentation();
//        
//        dataAccess = DataAccessWithAutoStructureDetection.createInstance();
//
//        instantMonitoringDataAnalysisEngine = new InstantMonitoringDataAnalysisEngine();
//
//        aggregatedMonitoringDataSQLAccess = new AggregatedMonitoringDataSQLAccess("mela", "mela");
//
//
//        //read service configuration, metrics composition and requiremets from XML
//
//        MonitoredElement serviceStructure = null;
//        CompositionRulesConfiguration compositionRulesConfiguration = null;
//        Requirements requirements = null;
//
//        //read service structure
//        {
//            InputStream is = new FileInputStream("/home/daniel-tuwien/Documents/CELAR_GIT/multilevel-metrics-evaluation/MELA-Core/MELA-AnalysisService/src/test/java/testFiles/serviceDescription.xml");
//            JAXBContext jc = JAXBContext.newInstance(MonitoredElement.class);
//            Unmarshaller u = jc.createUnmarshaller();
//
//            serviceStructure = (MonitoredElement) u.unmarshal(is);
//        }
//
//
//
//        //read CompositionRulesConfiguration
//        {
//            InputStream is = new FileInputStream("/home/daniel-tuwien/Documents/CELAR_GIT/multilevel-metrics-evaluation/MELA-Core/MELA-AnalysisService/src/test/java/testFiles/compositionRules.xml");
//            JAXBContext jc = JAXBContext.newInstance(CompositionRulesConfiguration.class);
//            Unmarshaller u = jc.createUnmarshaller();
//
//            compositionRulesConfiguration = (CompositionRulesConfiguration) u.unmarshal(is);
//        }
//
//
//        //read Requirements
//        {
//            InputStream is = new FileInputStream("/home/daniel-tuwien/Documents/CELAR_GIT/multilevel-metrics-evaluation/MELA-Core/MELA-AnalysisService/src/test/java/testFiles/requirements.xml");
//            JAXBContext jc = JAXBContext.newInstance(Requirements.class);
//            Unmarshaller u = jc.createUnmarshaller();
//
//            requirements = (Requirements) u.unmarshal(is);
//        }
//
//
//
//        String monSnapshotsCountKey = "MonitoringSnapshotsUsed";
//        String monitoringSnapshotKey = "MonitoringSnapshotContruction";
//        String elasticitySpaceAnalysysKey = "ElasticitySpaceAnalysis";
//        String elasticityPathwayAnalysysKey = "ElasticityPathwayAnalysis";
//
//        int maxMonSnapshots = 1810;
//
//        elasticitySpaceFunction = null;
//
//        //report without sql access
//        {
//
//
//
//
//            //do the test
//
//            for (int i = 0; i < maxMonSnapshots; i++) {
//
////                Runtime.getRuntime().gc();
//
//
//                elasticitySpaceFunction = new ElSpaceDefaultFunction(serviceStructure);
//                elasticitySpaceFunction.setRequirements(requirements);
//
//
////                if (i % 100 == 0) {
//                System.out.println("Evaluating WITHOUT SQL on snapshot " + i);
////                }
//
//                ServiceMonitoringSnapshot monitoringData = dataAccess.getMonitoredData(serviceStructure);
//
//
//
//                if (monitoringData == null) {
//                    break;
//                }
//
//                ServiceMonitoringSnapshot aggregated = aggregatedMonitoringDataSQLAccess.extractLatestMonitoringData();
//
//                aggregatedMonitoringDataSQLAccess.writeMonitoringData(aggregated);
//
//
//            }
//
//        }
//
//        List<ServiceMonitoringSnapshot> extractedData = aggregatedMonitoringDataSQLAccess.extractMonitoringData();
//        if (extractedData != null) {
//            //for each extracted snapshot, trim it to contain data only for the targetedMonitoredElement (minimizes RAM usage)
//            for (ServiceMonitoringSnapshot monitoringSnapshot : extractedData) {
////                monitoringSnapshot.keepOnlyDataForElement(element);
//                elasticitySpaceFunction.trainElasticitySpace(monitoringSnapshot);
//            }
//        }
//
//        ElasticitySpace space = elasticitySpaceFunction.getElasticitySpace();
//
//        for (MonitoredElement element : serviceStructure) {
//
////            ConvertToCSV.writeWholeElasticitySpaceToCSV(element, space, "/home/daniel-tuwien/Documents/DSG_SVN/papers/IJBDI_cloud_com_extended/figures/experiments/MELA/new/" + element.getId() + "_space.csv");
//
//            Map<Metric, List<MetricValue>> map = space.getMonitoredDataForService(element);
//            if (map != null) {
//                ArrayList<Metric> metrics = new ArrayList<Metric>(map.keySet());
//                LightweightEncounterRateElasticityPathway elasticityPathway = new LightweightEncounterRateElasticityPathway(metrics.size());
//                elasticityPathway.trainElasticityPathway(map);
//                ConvertToCSV.writeCSVFromElasticitySituationsGroups(elasticityPathway.getSituationGroups(), metrics, "/home/daniel-tuwien/Documents/DSG_SVN/papers/IJBDI_cloud_com_extended/figures/experiments/MELA/new/" + element.getId() + "_pathway.csv");
//            }
//
//
//        }





    }
}
