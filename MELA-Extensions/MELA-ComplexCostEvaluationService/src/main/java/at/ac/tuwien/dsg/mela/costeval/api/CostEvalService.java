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
package at.ac.tuwien.dsg.mela.costeval.api;

import at.ac.tuwien.dsg.mela.costeval.control.CostEvalManager;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticityPathwayXML;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity.ElasticitySpaceXML;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Action;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import java.io.InputStream;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshots;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 */
@Service
@Provider
@Path("/")
@Api(value = "/", description = "The ElasticityAnalysisService is the entry point for all elasticity related monitoring data")
public class CostEvalService {

    @Autowired
    private CostEvalManager costEvalManager;

    public CostEvalService() {
    }

//    @POST
//    @Path("/{serviceID}/elasticitypathway")
//    @Consumes("application/xml")
//    @Produces("application/json")
//    @ApiOperation(value = "Retrieve elasticity pathway",
//            notes = "Retrieves the elasticity pathway for the given MonitoredElement",
//            response = String.class)
//    @ApiResponses(value = {
//        @ApiResponse(code = 400, message = "Invalid ID supplied"),
//        @ApiResponse(code = 404, message = "Service element not found in service structure")
//    })
//    public String getElasticityPathwayInJSON(@PathParam("serviceID") String serviceID, MonitoredElement element) {
//        return systemControl.getElasticityPathway(serviceID, element);
//
//    }
////    @POST
////    @Path("/{serviceID}/elasticitypathwayxml")
////    @Consumes("application/xml")
////    @Produces("application/xml")
////    public ElasticityPathwayXML getElasticityPathwayInXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {
////        return systemControl.getElasticityPathwayInXML(serviceID, element);
////
////    }
////
////    /**
////     * @param element the MonitoredElement for which the elasticity space must
////     * be returned. Needs BOTH the Element ID and the Element LEVEL (SERVICE,
////     * SERVICE_TOPOLOGY, etc)
////     * @return the elasticity space in JSON
////     */
////    @POST
////    @Path("/{serviceID}/elasticityspace")
////    @Consumes("application/xml")
////    @Produces("application/json")
////    public String getLatestElasticitySpaceInJSON(@PathParam("serviceID") String serviceID, MonitoredElement element) {
////        return systemControl.getElasticitySpaceJSON(serviceID, element);
////    }
//
//    /**
//     * @param element the MonitoredElement for which the elasticity space must
//     * be returned. Needs BOTH the Element ID and the Element LEVEL (SERVICE,
//     * SERVICE_TOPOLOGY, etc)
//     * @return the elasticity space in XML WITH historical monitoring data
//     */
//    @POST
//    @Path("/{serviceID}/elasticityspacecompletexml")
//    @Consumes("application/xml")
//    @Produces("application/xml")
//    public ElasticitySpaceXML getLatestElasticitySpaceInXMLComplete(@PathParam("serviceID") String serviceID, MonitoredElement element) {
//        return systemControl.getCompleteElasticitySpaceXML(serviceID, element);
//    }
//
//    /**
//     * @param element the MonitoredElement for which the elasticity space must
//     * be returned. Needs BOTH the Element ID and the Element LEVEL (SERVICE,
//     * SERVICE_TOPOLOGY, etc)
//     * @return the elasticity space in XML WITH historical monitoring data
//     */
//    @POST
//    @Path("/{serviceID}/elasticityspacexml")
//    @Consumes("application/xml")
//    @Produces("application/xml")
//    public ElasticitySpaceXML getLatestElasticitySpaceInXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {
//        return systemControl.getElasticitySpaceXML(serviceID, element);
//    }
    /**
     * Method for retrieving an easy to display JSON string of the latest
     * monitored Data complete with composed metrics
     *
     * @return JSON representation of the monitored data. JSON Format:
     * {"name":"ServiceEntityName"
     * ,"type":"MonitoredElementType","children":[{"name":"metric value
     * [metricName]","type":"metric"}]} JSON Example:
     * {"name":"LoadBalancer","children":[{"name":"1
     * [vmCount]","type":"metric"},{"name":"51 [clients]","type":"metric"},{"
     * name":"10.99.0.62","children":[{"name":"51
     * [activeConnections]","type":"metric"},{"name":"1
     * [vmCount]","type":"metric"}],"type":"VM"}],"type":"SERVICE_UNIT"}
     */
    @GET
    @Path("/{serviceID}/monitoringdataJSON")
    @Produces("application/json")
    public String getLatestMonitoringDataInJSON(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getLatestMonitoringDataEnrichedWithCostINJSON(serviceID);
    }

    /**
     * @return the service structure containing all virtual machines currently
     * running service units
     */
    @GET
    @Path("/{serviceID}/servicestructure")
    @Produces("application/xml")
    public MonitoredElement getLatestServiceStructure(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getLatestServiceStructure(serviceID);
    }

//    @GET
//    @Path("/{serviceID}/monitoringdataXML")
//    @Produces("application/xml")
//    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML(@PathParam("serviceID") String serviceID) {
//        return systemControl.getLatestMonitoringData(serviceID);
//    }
//
//    @POST
//    @Path("/{serviceID}/monitoringdataXML")
//    @Consumes("application/xml")
//    @Produces("application/xml")
//    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {
//        return systemControl.getLatestMonitoringData(serviceID, element);
//    }
//
//    @GET
//    @Path("/{serviceID}/historicalmonitoringdataXML/all")
//    @Produces("application/xml")
//    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringData(@PathParam("serviceID") String serviceID) {
//        return systemControl.getAllAggregatedMonitoringData(serviceID);
//    }
//
//    @GET
//    @Path("/{serviceID}/historicalmonitoringdataXML/ininterval")
//    @Produces("application/xml")
//    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringDataInTimeInterval(@PathParam("serviceID") String serviceID, @QueryParam("startTimestamp") int startTimestamp,
//            @QueryParam("endTimestamp") int endTimestamp) {
//        return systemControl.getAggregatedMonitoringDataInTimeInterval(serviceID, startTimestamp, endTimestamp);
//    }
//
//    @GET
//    @Path("/{serviceID}/historicalmonitoringdataXML/lastX")
//    @Produces("application/xml")
//    public MonitoredElementMonitoringSnapshots getLastXAggregatedMonitoringData(@PathParam("serviceID") String serviceID, @QueryParam("count") int count) {
//        return systemControl.getLastXAggregatedMonitoringData(serviceID, count);
//    }
    @GET
    @Path("/{serviceID}/servicerequirements")
    @Produces("application/xml")
    public Requirements getRequirements(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getRequirements(serviceID);
    }

    @GET
    @Path("/{serviceID}/metriccompositionrules")
    @Produces("application/json")
    public String getMetricCompositionRules(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getMetricCompositionRules(serviceID);
    }

    @GET
    @Path("/{serviceID}/metriccompositionrulesxml")
    @Produces("application/xml")
    public CompositionRulesConfiguration getMetricCompositionRulesXML(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getCompositionRulesConfiguration(serviceID);
    }

    @GET
    @Path("/elasticservices")
    @Produces("application/json")
    public String getServices() {
        return costEvalManager.getAllManagedServicesIDs();
    }

    @PUT
    @Path("/cloudofferedservice/pricingscheme")
    @Consumes("application/xml")
    public void putPricingScheme(List<ServiceUnit> serviceUnits) {
        costEvalManager.setServiceUnits(serviceUnits);
    }

    @GET
    @Path("/cloudofferedservice/pricingscheme")
    @Produces("application/xml")
    public List<ServiceUnit> getPricingScheme() {
        return costEvalManager.getServiceUnits();
    }

    @DELETE
    @Path("/{cloudofferedserviceID}/pricingscheme")
    public void deletePricingScheme(@PathParam("cloudofferedserviceID") String cloudofferedserviceID) {

    }

    @GET
    @Path("/{cloudofferedserviceID}/pricingscheme")
    @Produces("application/xml")
    public void getPricingSchemeForService(@PathParam("cloudofferedserviceID") String cloudofferedserviceID) {

    }

    @GET
    @Path("/{serviceID}/cost/total/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshot getTotalCostForServiceXML(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getTotalServiceCostXML(serviceID);
    }

    @GET
    @Path("/{serviceID}/cost/total/json")
    @Produces("application/json")
    public String getTotalCostForServiceJSON(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getTotalServiceCostJSON(serviceID);
    }

    @GET
    @Path("/{serviceID}/cost/total/detailed")
    @Produces("application/csv")
    public void getTotalCostForServiceCSV(@PathParam("serviceID") String serviceID) {

    }

    @GET
    @Path("/{serviceID}/cost/ininterval/{start}/{end}/xml")
    @Produces("application/xml")
    public void getTotalCostForServiceInIntervalXML(@PathParam("serviceID") String serviceID,
            @PathParam("start") String start, @PathParam("end") String end) {

    }

    @GET
    @Path("/{serviceID}/cost/ininterval/{start}/{end}//csv")
    @Produces("application/csv")
    public void getTotalCostForServiceInIntervalCSV(@PathParam("serviceID") String serviceID,
            @PathParam("start") String start, @PathParam("end") String end) {

    }

    @POST
    @Path("/{serviceID}/cost/total/xml")
    @Consumes("application/xml")
    @Produces("application/xml")
    @ApiOperation(value = "Retrieve Cost for monitored element",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 404, message = "Service element not found in service structure")
    })
    public void getTotalCostForElementXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {

    }

    @POST
    @Path("/{serviceID}/cost/total/json")
    @Consumes("application/xml")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve Cost for monitored element",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 404, message = "Service element not found in service structure")
    })
    public void getTotalCostForElementJson(@PathParam("serviceID") String serviceID, MonitoredElement element) {

    }

    @POST
    @Path("/{serviceID}/cost/ininterval/{start}/{end}/xml")
    @Consumes("application/xml")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve Cost for monitored element",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 404, message = "Service element not found in service structure")
    })
    public void getTotalCostForElementInIntervalXML(@PathParam("serviceID") String serviceID,
            @PathParam("start") String start, @PathParam("end") String end, MonitoredElement element) {

    }

    @POST
    @Path("/{serviceID}/cost/ininterval/{start}/{end}/detailed")
    @Consumes("application/xml")
    @Produces("application/csv")
    @ApiOperation(value = "Retrieve Cost for monitored element",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 404, message = "Service element not found in service structure")
    })
    public void getTotalCostForElementInIntervalCSV(@PathParam("serviceID") String serviceID,
            @PathParam("start") String start, @PathParam("end") String end, MonitoredElement element) {

    }

    @Path("/{serviceID}/cost/ininterval/{start}/{end}/json")
    @Consumes("application/xml")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve Cost for monitored element",
            notes = "",
            response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 404, message = "Service element not found in service structure")
    })
    public void getTotalCostForElementInIntervalJSON(@PathParam("serviceID") String serviceID,
            @PathParam("start") String start, @PathParam("end") String end, MonitoredElement element) {

    }

}
