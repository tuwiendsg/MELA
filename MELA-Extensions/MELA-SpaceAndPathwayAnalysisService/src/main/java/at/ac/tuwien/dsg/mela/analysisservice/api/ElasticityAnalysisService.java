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
package at.ac.tuwien.dsg.mela.analysisservice.api;

import at.ac.tuwien.dsg.mela.analysisservice.control.ElasticityAnalysisManager;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshots;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 */
@Service
@Provider
@Path("/")
@Api(value = "/", description = "The ElasticityAnalysisService is the entry point for all elasticity related monitoring data")
public class ElasticityAnalysisService {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(ElasticityAnalysisService.class);

    @Autowired
    private ElasticityAnalysisManager systemControl;

    public ElasticityAnalysisService() {
    }

    @POST
    @Path("/{serviceID}/elasticitypathway/json")
    @Consumes("application/xml")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve elasticity pathway",
            notes = "Retrieves the elasticity pathway for the given MonitoredElement",
            response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 404, message = "Service element not found in service structure")
    })
    public String getElasticityPathwayInJSON(@PathParam("serviceID") String serviceID, MonitoredElement element) {
        return systemControl.getElasticityPathway(serviceID, element);

    }

    @POST
    @Path("/{serviceID}/elasticitypathway/xml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public ElasticityPathwayXML getElasticityPathwayInXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {
        return systemControl.getElasticityPathwayInXML(serviceID, element);

    }
    
     @GET
    @Path("/{serviceID}/events/json")
    @Produces("application/json")
    public String getEvents(@PathParam("serviceID") String serviceID) {
        return systemControl.getEvents(serviceID);
    }

    /**
     * @param element the MonitoredElement for which the elasticity space must
     * be returned. Needs BOTH the Element ID and the Element LEVEL (SERVICE,
     * SERVICE_TOPOLOGY, etc)
     * @return the elasticity space in JSON
     */
    @POST
    @Path("/{serviceID}/elasticityspace/json")
    @Consumes("application/xml")
    @Produces("application/json")
    public String getLatestElasticitySpaceInJSON(@PathParam("serviceID") String serviceID, MonitoredElement element) {
        return systemControl.getElasticitySpaceJSON(serviceID, element);
    }

    /**
     * @param element the MonitoredElement for which the elasticity space must
     * be returned. Needs BOTH the Element ID and the Element LEVEL (SERVICE,
     * SERVICE_TOPOLOGY, etc)
     * @return the elasticity space in XML WITH historical monitoring data
     */
    @POST
    @Path("/{serviceID}/elasticityspace/complete/xml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public ElasticitySpaceXML getLatestElasticitySpaceInXMLComplete(@PathParam("serviceID") String serviceID, MonitoredElement element) {
        return systemControl.getCompleteElasticitySpaceXML(serviceID, element);
    }

    /**
     * @param element the MonitoredElement for which the elasticity space must
     * be returned. Needs BOTH the Element ID and the Element LEVEL (SERVICE,
     * SERVICE_TOPOLOGY, etc)
     * @return the elasticity space in XML WITH historical monitoring data
     */
    @POST
    @Path("/{serviceID}/elasticityspace/xml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public ElasticitySpaceXML getLatestElasticitySpaceInXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {
        return systemControl.getElasticitySpaceXML(serviceID, element);
    }

    
   /**
    * All API below is here for legacy purposes and mimics the DataService API. 
    */
    
    
    /**
     * @param compositionRulesConfiguration the metric composition rules, both
     * the HISTORICAL and MULTI_LEVEL rules
     */
    @PUT
    @Path("/{serviceID}/metricscompositionrules")
    @Consumes("application/xml")
    public void putCompositionRules(CompositionRulesConfiguration compositionRulesConfiguration, @PathParam("serviceID") String serviceID) {
        if (compositionRulesConfiguration != null) {
            compositionRulesConfiguration.setTargetServiceID(serviceID);
            systemControl.setCompositionRulesConfiguration(compositionRulesConfiguration);
        } else {
            log.warn("supplied compositionRulesConfiguration is null");
        }
    }

    /**
     * @param element the service topology to be monitored
     */
    @PUT
    @Path("/service")
    @Consumes("application/xml")
    public void putServiceDescription(MonitoredElement element) {
        if (element != null) {
            systemControl.setServiceConfiguration(element);
        } else {
            log.warn("supplied service description is null");
        }
    }

    /**
     * @param serviceID the service to be removed
     */
    @DELETE
    @Path("/{serviceID}")
    @Consumes("application/xml")
    public void removeServiceDescription(@PathParam("serviceID") String serviceID) {
//        systemControl.removeService(serviceID);
    }

    /**
     * @param element refreshes the VM's attached to each Service Unit. For a
     * structural update, use "PUT servicedescription", as in such a case the
     * elasticity signature needs to be recomputed
     */
    @POST
    @Path("/{serviceID}/structure")
    @Consumes("application/xml")
    public void updateServiceDescription(MonitoredElement element) {
        if (element != null) {
            systemControl.updateServiceConfiguration(element);
        } else {
            log.warn("supplied service description is null");
        }
    }

    /**
     * @param requirements service behavior limits on metrics directly measured
     * or obtained from metric composition
     */
    @PUT
    @Path("/{serviceID}/requirements")
    @Consumes("application/xml")
    public void putServiceRequirements(Requirements requirements, @PathParam("serviceID") String serviceID) {
        if (requirements != null) {
            requirements.setTargetServiceID(serviceID);
            systemControl.setRequirements(requirements);
        } else {
            log.warn("supplied service requirements are null");
        }
    }

    
    
    /**
     * Method used to list for a particular service unit ID what are the
     * available metrics that can be monitored directly
     *
     * @param monitoredElementID ID of the service UNIT from which to return the
     * available monitored data (before composition) for the VMS belonging to
     * the SERVICE UNIT example: http://localhost:8080/MELA/REST_WS/metrics
     * ?serviceID=CassandraController
     * @return
     */
    @GET
    @Path("/{serviceID}/metrics")
    @Produces("application/xml")
    public Collection<Metric> getAvailableMetrics(@PathParam("serviceID") String serviceID, @QueryParam("monitoredElementID") String monitoredElementID,
            @QueryParam("monitoredElementLevel") String monitoredElementLevel) {
        try {
            JAXBContext context = JAXBContext.newInstance(MonitoredElement.class);
            String monElementRepr = "<MonitoredElement id=\"" + monitoredElementID + "\"  level=\"" + monitoredElementLevel + "\"/>";
            MonitoredElement monitoredElement = (MonitoredElement) context.createUnmarshaller().unmarshal(new StringReader(monElementRepr));

            return systemControl.getAvailableMetricsForMonitoredElement(serviceID, monitoredElement);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
            return new ArrayList<Metric>();
        }

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
        return systemControl.getLatestMonitoringDataINJSON(serviceID);
    }

    /**
     * @return the service structure containing all virtual machines currently
     * running service units
     */
    @GET
    @Path("/{serviceID}/structure")
    @Produces("application/xml")
    public MonitoredElement getLatestServiceStructure(@PathParam("serviceID") String serviceID) {
        return systemControl.getLatestServiceStructure(serviceID);
    }

    @GET
    @Path("/{serviceID}/monitoringdata/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML(@PathParam("serviceID") String serviceID) {
        return systemControl.getLatestMonitoringData(serviceID);
    }

    @POST
    @Path("/{serviceID}/monitoringdata/xml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {
        return systemControl.getLatestMonitoringData(serviceID, element);
    }

    @GET
    @Path("/{serviceID}/historicalmonitoringdata/all/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringData(@PathParam("serviceID") String serviceID) {
        return systemControl.getAllAggregatedMonitoringData(serviceID);
    }

    @GET
    @Path("/{serviceID}/historicalmonitoringdata/ininterval/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringDataInTimeInterval(@PathParam("serviceID") String serviceID, @QueryParam("startTimestamp") int startTimestamp,
            @QueryParam("endTimestamp") int endTimestamp) {
        return systemControl.getAggregatedMonitoringDataInTimeInterval(serviceID, startTimestamp, endTimestamp);
    }

    @GET
    @Path("/{serviceID}/historicalmonitoringdata/lastX/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshots getLastXAggregatedMonitoringData(@PathParam("serviceID") String serviceID, @QueryParam("count") int count) {
        return systemControl.getLastXAggregatedMonitoringData(serviceID, count);
    }

    @GET
    @Path("/{serviceID}/metriccompositionrules/json")
    @Produces("application/json")
    public String getMetricCompositionRules(@PathParam("serviceID") String serviceID) {
        return systemControl.getMetricCompositionRules(serviceID);
    }

    @GET
    @Path("/{serviceID}/metriccompositionrules/xml")
    @Produces("application/xml")
    public CompositionRulesConfiguration getMetricCompositionRulesXML(@PathParam("serviceID") String serviceID) {
        return systemControl.getCompositionRulesConfiguration(serviceID);
    }

    @PUT
    @Path("/{serviceID}/{targetEntityID}/executingaction/{action}")
    public void addExecutingAction(@PathParam("serviceID") String serviceID, @PathParam("targetEntityID") String targetEntityID, @PathParam("action") String action
    ) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new Action(targetEntityID, action));
        systemControl.addExecutingAction(serviceID, targetEntityID, action);
    }

    @DELETE
    @Path("/{serviceID}/{targetEntityID}/executingaction/{action}")
    public void removeExecutingAction(@PathParam("serviceID") String serviceID, @PathParam("targetEntityID") String targetEntityID, @PathParam("action") String action
    ) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new Action(targetEntityID, action));
        systemControl.removeExecutingAction(serviceID, targetEntityID, action);
    }

    @GET
    @Path("/{serviceID}/metricsGreaterEqualThanZero")
    public String testIfAllVMsReportMEtricsGreaterThanZero(@PathParam("serviceID") String serviceID) {
        return "" + systemControl.testIfAllVMsReportMEtricsGreaterThanZero(serviceID);
    }

    @GET
    @Path("/elasticservices")
    @Produces("application/json")
    public String getServices() {
        return systemControl.getAllManagedServicesIDs();
    }

}
