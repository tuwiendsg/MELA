/*
 * Copyright 2014 daniel-tuwien.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.api;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ServiceElasticityDependencies;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service
@Provider
@Path("/")
public class ElasticityDependenciesAnalysisService {

    @Context
    private UriInfo context;

    @Autowired
    private ElasticityDependencyAnalysisManager elasticityDependencyAnalysisManager;

    public ElasticityDependenciesAnalysisService() {
    }

    /**
     * All service services directly with elasticitydependencies (no elasticitymetrics) 
     * in URL analyze all monitored metrics, while the others analyze only metrics which have requirements associated to them
     * @param serviceID
     * @return
     */
    @GET
    @Path("/{serviceID}/xml/elasticitydependencies")
    @Produces("application/xml")
    public ServiceElasticityDependencies getElasticityDependenciesAsXML(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);
        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
        return elasticityDependencyAnalysisManager.analyzeElasticityDependencies(element);
    }

    @GET
    @Path("/{serviceID}/json/elasticitydependencies")
    @Produces("application/json")
    public String getElasticityDependenciesAsJSON(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesJSON(element, null, null);

        return jsonString;

    }

    @GET
    @Path("/{serviceID}/json/time/elasticitydependencies/{startTime}/{endTime}")
    @Produces("application/json")
    public String getElasticityDependenciesAsJSONBetweenTime(@PathParam("serviceID") String serviceID, @PathParam("startTime") Integer startTime,
            @PathParam("endTime") Integer endTime) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesJSON(element, startTime, endTime);

        return jsonString;

    }

    @GET
    @Path("/{serviceID}/csv/elasticitydependencies")
    @Produces("text/csv")
    public Response getElasticityDependenciesAsCSV(@PathParam("serviceID") String serviceID) {

        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested dependencies as CSV for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesAsCSV(element);

        Response response = Response.status(200).header("Content-Disposition", "attachment; filename=" + serviceID + "_complete_deps.csv").entity(jsonString).build();
        return response;

    }

    @GET
    @Path("/{serviceID}/csv/time/elasticitydependencies/{startTime}/{endTime}")
    @Produces("text/csv")
    public Response getElasticityDependenciesAsCSVBetweenTime(@PathParam("serviceID") String serviceID, @PathParam("startTime") int startTime,
            @PathParam("endTime") int endTime) {

        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested dependencies as CSV for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesAsCSVBetweenTimeIntervals(element, startTime, endTime);

        Response response = Response.status(200).header("Content-Disposition", "attachment; filename=" + serviceID + "_complete_deps_" + startTime + "_" + endTime + "_.csv").entity(jsonString).build();
        return response;

    }

    /**
     * Any Service which has /elasticitymetrics in URL works only on the metrics
     * having requirements associated to them
     *
     * @param serviceID
     * @return
     */
    @GET
    @Path("/{serviceID}/xml/elasticitymetrics/elasticitydependencies")
    @Produces("application/xml")
    public ServiceElasticityDependencies getElasticityDependenciesBetweenElMetricsAsXML(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);
        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
        return elasticityDependencyAnalysisManager.analyzeElasticityDependencies(element);
    }

    @GET
    @Path("/{serviceID}/json/elasticitymetrics/elasticitydependencies")
    @Produces("application/json")
    public String getElasticityDependenciesBetweenElMetricsAsJSON(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesBetweenElMetricsJSON(element, null, null);

        return jsonString;

    }

    @GET
    @Path("/{serviceID}/json/time/elasticitymetrics/elasticitydependencies/{startTime}/{endTime}")
    @Produces("application/json")
    public String getElasticityDependenciesBetweenElMetricsAsJSON(@PathParam("serviceID") String serviceID, @PathParam("startTime") Integer startTime,
            @PathParam("endTime") Integer endTime) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesBetweenElMetricsJSON(element, startTime, endTime);

        return jsonString;

    }

    @GET
    @Path("/{serviceID}/csv/elasticitymetrics/elasticitydependencies")
    @Produces("text/csv")
    public Response getElasticityDependenciesBetweenElMetricsAsCSV(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested dependencies as CSV for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesBetweenElMetricsAsCSV(element);

        Response response = Response.status(200).header("Content-Disposition", "attachment; filename=" + serviceID + "_el_metrics_deps.csv").entity(jsonString).build();
        return response;

    }

    @GET
    @Path("/{serviceID}/csv/time/elasticitymetrics/elasticitydependencies/{startTime}/{endTime}")
    @Produces("text/csv")
    public Response getElasticityDependenciesBetweenElMetricsAsCSVBetweenTime(@PathParam("serviceID") String serviceID, @PathParam("startTime") int startTime,
            @PathParam("endTime") int endTime) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested dependencies as CSV for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesBetweenElMetricsAsCSVBetweenTimeIntervals(element, startTime, endTime);

        Response response = Response.status(200).header("Content-Disposition", "attachment; filename=" + serviceID + "_el_metrics_deps_" + startTime + "_" + endTime + "_.csv").entity(jsonString).build();
        return response;

    }

    @GET
    @Path("/{serviceID}/monitoringdataJSON")
    @Produces("application/json")
    public String getLatestMonitoringDataInJSON(@PathParam("serviceID") String serviceID) {
        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
        return elasticityDependencyAnalysisManager.getServiceStructureAndMetricsAsJSON(element);
    }

    @GET
    @Path("/elasticservices")
    @Produces("application/json")
    public String getServices() {
        return elasticityDependencyAnalysisManager.getAllManagedServicesIDs();
    }

}
