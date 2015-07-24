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
import at.ac.tuwien.dsg.mela.costeval.model.UnusedCostUnitsReport;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import org.apache.cxf.common.i18n.UncheckedException;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at *
 */
@Service
@Provider
@Path("/")
@Api(value = "/", description = "The ElasticityAnalysisService is the entry point for all elasticity related monitoring data")
public class CloudProvidersManagementService {

    @Autowired
    private CostEvalManager costEvalManager;

    public CloudProvidersManagementService() {
    }
    
    @PUT
    @Path("/cloudProvider")
    @Consumes("application/xml")
    public void putPricingScheme(CloudProvider cloudProvider) {
        costEvalManager.addCloudProvider(cloudProvider);
    }
    
    @PUT
    @Path("/cloudProviders")
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
        throw new UnsupportedOperationException("Not implemented");
    }

    @GET
    @Path("/cloudofferedservice/cost/minimum/byID/{cloudofferedserviceUUID}")
    @Produces("text/plain")
    public Double getPricingSchemeForServiceByID(@PathParam("cloudofferedserviceUUID") String cloudofferedserviceUUID) {
        CloudOfferedService cloudOfferedService = new CloudOfferedService().withUuid(UUID.fromString(cloudofferedserviceUUID));
        return costEvalManager.getMinimumCostEstimationForServiceByUUID(cloudOfferedService);
    }

    @GET
    @Path("/cloudofferedservice/cost/minimum/byName/{cloudofferedserviceName}")
    @Produces("text/plain")
    public Double getPricingSchemeForServiceByName(@PathParam("cloudofferedserviceName") String cloudofferedserviceName) {
        CloudOfferedService cloudOfferedService = new CloudOfferedService().withName(cloudofferedserviceName);
        return costEvalManager.getMinimumCostEstimationForServiceByName(cloudOfferedService);
    }

}
