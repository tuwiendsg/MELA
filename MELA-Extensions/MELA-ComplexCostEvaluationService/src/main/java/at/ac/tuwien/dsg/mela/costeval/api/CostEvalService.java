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
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.ext.Provider;
import at.ac.tuwien.dsg.mela.costeval.model.CloudServicesSpecification;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;

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
    @Path("/{serviceID}/monitoringdata/json")
    @Produces("application/json")
    public String getLatestMonitoringDataInJSON(@PathParam("serviceID") String serviceID) {
        return "{}";//costEvalManager.getLatestMonitoringDataEnrichedWithCostINJSON(serviceID);
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

    @GET
    @Path("/{serviceID}/servicerequirements")
    @Produces("application/xml")
    public Requirements getRequirements(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getRequirements(serviceID);
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
    public void putPricingScheme(CloudServicesSpecification cloudServicesSpecification) {
        costEvalManager.addCloudProviders(cloudServicesSpecification.getCloudProviders());
    }

    @GET
    @Path("/cloudofferedservice/pricingscheme")
    @Produces("application/xml")
    public CloudServicesSpecification getPricingScheme() {
        throw new UnsupportedOperationException("must implement");
//        return new CloudServicesSpecification().withServiceUnits(costEvalManager.getServiceUnits());
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
        return costEvalManager.getTotalCostJSON(serviceID);
    }

    @GET
    @Path("/{serviceID}/cost/instant/json")
    @Produces("application/json")
    public String instantCostPerUsage(@PathParam("serviceID") String serviceID) {
        return costEvalManager.getInstantCostJSON(serviceID);
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
