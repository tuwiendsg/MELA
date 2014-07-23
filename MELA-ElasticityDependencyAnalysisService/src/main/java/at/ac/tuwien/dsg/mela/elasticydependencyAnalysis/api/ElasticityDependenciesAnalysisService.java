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
    @Path("/{serviceID}/elasticitydependencies")
    @Produces("application/json")
    public String getElasticityDependenciesAsJSON(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesJSON(element);

        return jsonString;

    }
    
    
    @GET
    @Path("/{serviceID}/csv/elasticitydependencies")
    @Produces("text/csv")
    public String getElasticityDependenciesAsCSV(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested dependencies as CSV for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesAsCSV(element);

        return jsonString;

    }

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
    @Path("/{serviceID}/elasticitymetrics/elasticitydependencies")
    @Produces("application/json")
    public String getElasticityDependenciesBetweenElMetricsAsJSON(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested analysis for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesBetweenElMetricsJSON(element);

        return jsonString;

    }
    
    
    @GET
    @Path("/{serviceID}/csv/elasticitymetrics/elasticitydependencies")
    @Produces("text/csv")
    public String getElasticityDependenciesBetweenElMetricsAsCSV(@PathParam("serviceID") String serviceID) {
        Logger.getLogger(ElasticityDependenciesAnalysisService.class.getName()).log(Level.INFO, "Requested dependencies as CSV for " + serviceID);

        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);

        String jsonString = elasticityDependencyAnalysisManager.analyzeElasticityDependenciesBetweenElMetricsAsCSV(element);

        return jsonString;

    }

    @GET
    @Path("/{serviceID}/monitoringdataJSON")
    @Produces("application/json")
    public String getLatestMonitoringDataInJSON(@PathParam("serviceID") String serviceID) {
        MonitoredElement element = new MonitoredElement(serviceID);
        element.setLevel(MonitoredElement.MonitoredElementLevel.SERVICE);
        return elasticityDependencyAnalysisManager.getServiceStructureAndMetricsAsJSON(element);
    }

}
