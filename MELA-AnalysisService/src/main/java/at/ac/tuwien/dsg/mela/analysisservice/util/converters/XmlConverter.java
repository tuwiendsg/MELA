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
package at.ac.tuwien.dsg.mela.analysisservice.util.converters;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som.Neuron;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayGroupXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayGroupXML.ElasticityPathwayGroupEntryXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticitySpaceDimensionXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticitySpaceXML;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
@Component
public class XmlConverter {

    /**
     * Also contains the monitored historical data for the space
     * @param space
     * @param spaceElement
     * @return 
     */
    public ElasticitySpaceXML convertElasticitySpaceToXMLCompletely(ElasticitySpace space, MonitoredElement spaceElement) {

        ElasticitySpaceXML elasticitySpaceXML = new ElasticitySpaceXML();

        Map<Metric, List<MetricValue>> monitoredData = space.getMonitoredDataForService(spaceElement);

        //for each monitored metric from the space, create a space Dimension
        for (Metric metric : monitoredData.keySet()) {
            ElasticitySpaceDimensionXML dimensionXML = new ElasticitySpaceDimensionXML();
            dimensionXML.setMetric(metric);
            dimensionXML.setMetricValues(monitoredData.get(metric));
            MetricValue[] boundary = space.getSpaceBoundaryForMetric(spaceElement, metric);
            dimensionXML.setLowerBoundary(boundary[0]);
            dimensionXML.setUpperBoundary(boundary[1]);
            
            elasticitySpaceXML.addDimension(dimensionXML);
        }

        //set the monitored element targeted byt he space, without all its children
        MonitoredElement element = new MonitoredElement();
        element.setId(spaceElement.getId());
        element.setLevel(spaceElement.getLevel());
        element.setName(spaceElement.getName());

        elasticitySpaceXML.setElement(element);

        return elasticitySpaceXML;
    }
    
    
    /**
     * Does NOT the monitored historical data for the space
     * @param space
     * @param spaceElement
     * @return 
     */
    public ElasticitySpaceXML convertElasticitySpaceToXML(ElasticitySpace space, MonitoredElement spaceElement) {

        ElasticitySpaceXML elasticitySpaceXML = new ElasticitySpaceXML();

        Map<Metric, List<MetricValue>> monitoredData = space.getMonitoredDataForService(spaceElement);

        //for each monitored metric from the space, create a space Dimension
        for (Metric metric : monitoredData.keySet()) {
            ElasticitySpaceDimensionXML dimensionXML = new ElasticitySpaceDimensionXML();
            dimensionXML.setMetric(metric);
            MetricValue[] boundary = space.getSpaceBoundaryForMetric(spaceElement, metric);
            dimensionXML.setLowerBoundary(boundary[0]);
            dimensionXML.setUpperBoundary(boundary[1]);
            
            elasticitySpaceXML.addDimension(dimensionXML);
        }

        //set the monitored element targeted byt he space, without all its children
        MonitoredElement element = new MonitoredElement();
        element.setId(spaceElement.getId());
        element.setLevel(spaceElement.getLevel());
        element.setName(spaceElement.getName());

        elasticitySpaceXML.setElement(element);

        return elasticitySpaceXML;
    }
    
    
    public ElasticityPathwayXML convertElasticityPathwayToXML(List<Metric> metrics, List<Neuron> elPathwayGroups, MonitoredElement monitoredElement) {
        
    	ElasticityPathwayXML elasticityPathwayXML = new ElasticityPathwayXML();
    	
    	if (elPathwayGroups == null || metrics == null) {
            Logger.getLogger(JsonConverter.class).log(Level.WARN, "Elasticity Pathway is null");
            return elasticityPathwayXML;
        }
    	elasticityPathwayXML.setElement(monitoredElement);
    	elasticityPathwayXML.setMetrics(metrics);
         

        for (Neuron neuron : elPathwayGroups) {
        	ElasticityPathwayGroupXML groupXML = new ElasticityPathwayGroupXML();
        	groupXML.setEncounterRate(neuron.getUsagePercentage());
           
            List<Double> values = neuron.getWeights();
            for (int i = 0; i < values.size(); i++) {
            	ElasticityPathwayGroupEntryXML entryXML = new ElasticityPathwayGroupEntryXML(metrics.get(i), new MetricValue(values.get(i)));
            	groupXML.addEntry(entryXML);
            }
            elasticityPathwayXML.addGroup(groupXML);
        }

        return elasticityPathwayXML;
 
    }
}
