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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import java.io.InputStream;
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
public class ElasticityAnalysisService {

    @Autowired
    private ElasticityAnalysisManager systemControl;

    public ElasticityAnalysisService() {
    }

    @POST
    @Path("/elasticitypathway")
    @Consumes("application/xml")
    @Produces("application/json")
    @ApiOperation(value = "Retrieve elasticity pathway",
            notes = "Retrieves the elasticity pathway for the given MonitoredElement",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 404, message = "Service element not found in service structure")
    })
    public String getElasticityPathwayInJSON(MonitoredElement element) {
        return systemControl.getElasticityPathway(element);
    }

    @POST
    @Path("/elasticitypathwayxml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public ElasticityPathwayXML getElasticityPathwayInXML(MonitoredElement element) {
        return systemControl.getElasticityPathwayInXML(element);
    }

    /**
     * @param element the MonitoredElement for which the elasticity space must be
     *                returned. Needs BOTH the Element ID and the Element LEVEL
     *                (SERVICE, SERVICE_TOPOLOGY, etc)
     * @return the elasticity space in JSON
     */
    @POST
    @Path("/elasticityspace")
    @Consumes("application/xml")
    @Produces("application/json")
    public String getLatestElasticitySpaceInJSON(MonitoredElement element) {
        return systemControl.getElasticitySpaceJSON(element);
    }

    /**
     * @param element the MonitoredElement for which the elasticity space must be
     *                returned. Needs BOTH the Element ID and the Element LEVEL
     *                (SERVICE, SERVICE_TOPOLOGY, etc)
     * @return the elasticity space in XML WITH historical monitoring data
     */
    @POST
    @Path("/elasticityspacecompletexml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public ElasticitySpaceXML getLatestElasticitySpaceInXMLComplete(MonitoredElement element) {
        return systemControl.getCompleteElasticitySpaceXML(element);
    }

    /**
     * @param element the MonitoredElement for which the elasticity space must be
     *                returned. Needs BOTH the Element ID and the Element LEVEL
     *                (SERVICE, SERVICE_TOPOLOGY, etc)
     * @return the elasticity space in XML WITH historical monitoring data
     */
    @POST
    @Path("/elasticityspacexml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public ElasticitySpaceXML getLatestElasticitySpaceInXML(MonitoredElement element) {
        return systemControl.getElasticitySpaceXML(element);
    }

    /**
     * @param compositionRulesConfiguration the metric composition rules, both the HISTORICAL and
     *                                      MULTI_LEVEL rules
     */
    @PUT
    @Path("/metricscompositionrules")
    @Consumes("application/xml")
    public void putCompositionRules(CompositionRulesConfiguration compositionRulesConfiguration) {
        if (compositionRulesConfiguration != null) {
            systemControl.setCompositionRulesConfiguration(compositionRulesConfiguration);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "supplied compositionRulesConfiguration is null");
        }
    }

    /**
     * @param element the service topology to be monitored
     */
    @PUT
    @Path("/servicedescription")
    @Consumes("application/xml")
    public void putServiceDescription(MonitoredElement element) {
        if (element != null) {
            systemControl.setServiceConfiguration(element);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "supplied service description is null");
        }
    }

    /**
     * @param element refreshes the VM's attached to each Service Unit. For a
     *                structural update, use "PUT servicedescription", as in such a
     *                case the elasticity signature needs to be recomputed
     */
    @POST
    @Path("/servicedescription")
    @Consumes("application/xml")
    public void updateServiceDescription(MonitoredElement element) {
        if (element != null) {
            systemControl.updateServiceConfiguration(element);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "supplied service description is null");
        }
    }

    /**
     * @param requirements service behavior limits on metrics directly measured or
     *                     obtained from metric composition
     */
    @PUT
    @Path("/servicerequirements")
    @Consumes("application/xml")
    public void putServiceRequirements(Requirements requirements) {
        if (requirements != null) {
            systemControl.setRequirements(requirements);
        } else {
            Logger.getLogger(this.getClass()).log(Level.WARN, "supplied service requirements are null");
        }
    }

    /**
     * Method used to list for a particular service unit ID what are the
     * available metrics that can be monitored directly
     *
     * @param monitoredElementID ID of the service UNIT from which to return the available
     *                           monitored data (before composition) for the VMS belonging to
     *                           the SERVICE UNIT example:
     *                           http://localhost:8080/MELA/REST_WS/metrics
     *                           ?serviceID=CassandraController
     * @return
     */
    @GET
    @Path("/metrics")
    @Produces("application/xml")
    public Collection<Metric> getAvailableMetrics(@QueryParam("monitoredElementID") String monitoredElementID,
                                                  @QueryParam("monitoredElementLevel") String monitoredElementLevel) {
        try {
            JAXBContext context = JAXBContext.newInstance(MonitoredElement.class);
            String monElementRepr = "<MonitoredElement id=\"" + monitoredElementID + "\"  level=\"" + monitoredElementLevel + "\"/>";
            MonitoredElement monitoredElement = (MonitoredElement) context.createUnmarshaller().unmarshal(new StringReader(monElementRepr));

            return systemControl.getAvailableMetricsForMonitoredElement(monitoredElement);
        } catch (Exception ex) {
            Logger.getLogger(ElasticityAnalysisService.class.getName()).log(Level.ERROR, null, ex);
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
     * [vmCount]","type":"metric"},{"name":"51
     * [clients]","type":"metric"},{"
     * name":"10.99.0.62","children":[{"name":"51
     * [activeConnections]","type":"metric"},{"name":"1
     * [vmCount]","type":"metric"}],"type":"VM"}],"type":"SERVICE_UNIT"}
     */
    @GET
    @Path("/monitoringdataJSON")
    @Produces("application/json")
    public String getLatestMonitoringDataInJSON() {
        return systemControl.getLatestMonitoringDataINJSON();
    }

    /**
     * @return the service structure containing all virtual machines currently
     * running service units
     */
    @GET
    @Path("/servicestructure")
    @Produces("application/xml")
    public MonitoredElement getLatestServiceStructure() {
        return systemControl.getLatestServiceStructure();
    }

    @GET
    @Path("/monitoringdataXML")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML() {
        return systemControl.getLatestMonitoringData();
    }

    @POST
    @Path("/monitoringdataXML")
    @Consumes("application/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML(MonitoredElement element) {
        return systemControl.getLatestMonitoringData(element);
    }

    @GET
    @Path("/historicalmonitoringdataXML")
    @Produces("application/xml")
    public List<MonitoredElementMonitoringSnapshot> getAllAggregatedMonitoringData() {
        return systemControl.getAllAggregatedMonitoringData();
    }

    @GET
    @Path("/metriccompositionrules")
    @Produces("application/json")
    public String getMetricCompositionRules() {
        return systemControl.getMetricCompositionRules();
    }

    @GET
    @Path("/metriccompositionrulesxml")
    @Produces("application/xml")
    public CompositionRulesConfiguration getMetricCompositionRulesXML() {
        return systemControl.getCompositionRulesConfiguration();
    }

    @POST
    @Path("/addexecutingactions")
    public void addExecutingAction(Action action) {
        systemControl.addExecutingAction(action.getTargetEntityID(), action.getAction());
    }

    @POST
    @Path("/removeexecutingactions")
    public void removeExecutingAction(Action action) {
        systemControl.removeExecutingAction(action.getTargetEntityID(), action.getAction());
    }
}
