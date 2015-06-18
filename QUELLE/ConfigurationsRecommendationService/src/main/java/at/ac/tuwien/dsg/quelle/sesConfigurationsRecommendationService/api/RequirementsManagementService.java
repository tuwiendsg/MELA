/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.api;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.RequirementsManagementController;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.SESConstructionController;
import com.wordnik.swagger.annotations.Api;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service("requirementsManagementService")
@Provider
@Path("/requirementsManagement")
@Api(value = "/requirementsManagement", description = "The RequirementsManagementService is the entry point for all methods for managing multi-level requirements")
public class RequirementsManagementService {

    @Autowired
    private SESConstructionController sesConstructionController;

    @Autowired
    private RequirementsManagementController requirementsManagementController;

    @GET
    @Path("/xml/requirements")
    @Produces("application/xml")
    public MultiLevelRequirements getLatestRequirements() {
        return requirementsManagementController.getRequirements();
    }

    @GET
    @Path("/json/requirements")
    @Produces("application/json")
    public String getLatestRequirementsInJSON() {
        return requirementsManagementController.getRequirementsJSON();
    }

    @PUT
    @Path("/xml/requirements")
    public void setRequirements(MultiLevelRequirements levelRequirements) {
        requirementsManagementController.setRequirements(levelRequirements);
    }

    @GET
    @Path("/json/costmetrics")
    @Produces("application/json")
    public String getCostMetricsAsJSON() {
        return requirementsManagementController.getCostMetricsAsJSON();
    }

    @GET
    @Path("/json/qualitymetrics")
    @Produces("application/json")
    public String getQualityMetricsAsJSON() {
        return requirementsManagementController.getQualityMetricsAsJSON();
    }

    @GET
    @Path("/json/resourcemetrics")
    @Produces("application/json")
    public String getResourceMetricsAsJSON() {
        return requirementsManagementController.getResourceMetricsAsJSON();
    }

    @PUT
    @Path("/management/json/requirements")
    public void addToStructureRequirements(String jsonRepr) {
        requirementsManagementController.addToRequirements(jsonRepr);
    }

    @DELETE
    @Path("/management/json/requirements")
    public void removeFromStructureRequirements(String jsonRepr) {
        requirementsManagementController.removeFromRequirements(jsonRepr);
    }

}
