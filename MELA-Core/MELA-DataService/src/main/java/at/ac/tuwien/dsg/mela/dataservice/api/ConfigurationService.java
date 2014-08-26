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
package at.ac.tuwien.dsg.mela.dataservice.api;

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
import at.ac.tuwien.dsg.mela.dataservice.DataCollectionService;

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
public class ConfigurationService {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(ConfigurationService.class);

    @Autowired
    private DataCollectionService collectionService;

    public ConfigurationService() {
    }

    /**
     * @param compositionRulesConfiguration the metric composition rules, both
     * the HISTORICAL and MULTI_LEVEL rules
     */
    @PUT
    @Path("/{serviceID}/metricscompositionrules")
    @Consumes("application/xml")
    public void putCompositionRules(CompositionRulesConfiguration compositionRulesConfiguration) {
        if (compositionRulesConfiguration != null) {
            collectionService.setCompositionRulesConfiguration(compositionRulesConfiguration.getTargetServiceID(), compositionRulesConfiguration);
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
            collectionService.addService(element);
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
        collectionService.removeService(serviceID);
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
            collectionService.updateServiceConfiguration(element);
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
    public void putServiceRequirements(Requirements requirements) {
        if (requirements != null) {
            collectionService.addRequirements(requirements.getTargetServiceID(), requirements);
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

            return collectionService.getAvailableMetricsForMonitoredElement(serviceID, monitoredElement);
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
        return collectionService.getLatestMonitoringDataINJSON(serviceID);
    }

    /**
     * @return the service structure containing all virtual machines currently
     * running service units
     */
    @GET
    @Path("/{serviceID}/structure")
    @Produces("application/xml")
    public MonitoredElement getLatestServiceStructure(@PathParam("serviceID") String serviceID) {
        return collectionService.getLatestServiceStructure(serviceID);
    }

    @GET
    @Path("/{serviceID}/monitoringdata/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML(@PathParam("serviceID") String serviceID) {
        return collectionService.getLatestMonitoringDataInXML(serviceID);
    }

    @POST
    @Path("/{serviceID}/monitoringdata/xml")
    @Consumes("application/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshot getLatestMonitoringDataInXML(@PathParam("serviceID") String serviceID, MonitoredElement element) {
        return collectionService.getLatestMonitoringData(serviceID, element);
    }

    @GET
    @Path("/{serviceID}/historicalmonitoringdata/all/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringData(@PathParam("serviceID") String serviceID) {
        return collectionService.getAllAggregatedMonitoringData(serviceID);
    }

    @GET
    @Path("/{serviceID}/historicalmonitoringdata/ininterval/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshots getAllAggregatedMonitoringDataInTimeInterval(@PathParam("serviceID") String serviceID, @QueryParam("startTimestamp") int startTimestamp,
            @QueryParam("endTimestamp") int endTimestamp) {
        return collectionService.getAggregatedMonitoringDataInTimeInterval(serviceID, startTimestamp, endTimestamp);
    }

    @GET
    @Path("/{serviceID}/historicalmonitoringdata/lastX/xml")
    @Produces("application/xml")
    public MonitoredElementMonitoringSnapshots getLastXAggregatedMonitoringData(@PathParam("serviceID") String serviceID, @QueryParam("count") int count) {
        return collectionService.getLastXAggregatedMonitoringData(serviceID, count);
    }

    @GET
    @Path("/{serviceID}/metriccompositionrules/json")
    @Produces("application/json")
    public String getMetricCompositionRules(@PathParam("serviceID") String serviceID) {
        return collectionService.getMetricCompositionRules(serviceID);
    }

    @GET
    @Path("/{serviceID}/metriccompositionrules/xml")
    @Produces("application/xml")
    public CompositionRulesConfiguration getMetricCompositionRulesXML(@PathParam("serviceID") String serviceID) {
        return collectionService.getMetricCompositionRulesXML(serviceID);
    }

    @PUT
    @Path("/{serviceID}/{targetEntityID}/executingaction/{action}")
    public void addExecutingAction(@PathParam("serviceID") String serviceID, @PathParam("targetEntityID") String targetEntityID, @PathParam("action") String action
    ) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new Action(targetEntityID, action));
        collectionService.addExecutingActions(serviceID, actions);
    }

    @DELETE
    @Path("/{serviceID}/{targetEntityID}/executingaction/{action}")
    public void removeExecutingAction(@PathParam("serviceID") String serviceID, @PathParam("targetEntityID") String targetEntityID, @PathParam("action") String action
    ) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new Action(targetEntityID, action));
        collectionService.removeExecutingActions(serviceID, actions);
    }

    @GET
    @Path("/{serviceID}/metricsGreaterEqualThanZero")
    public String testIfAllVMsReportMEtricsGreaterThanZero(@PathParam("serviceID") String serviceID) {
        return "" + collectionService.testIfAllVMsReportMEtricsGreaterThanZero(serviceID);
    }

    @GET
    @Path("/elasticservices")
    @Produces("application/json")
    public String getServices() {
        return collectionService.getAllManagedServicesIDs();
    }
}
