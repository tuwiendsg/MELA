/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control;

import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.ServiceUnitConfigurationSolution;
import at.ac.tuwien.dsg.quelle.wineryOutputFormatters.CloudServicesToWinery;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service("sesRecommendationOutputController")
public class SESRecommendationOutputController implements InitializingBean {

    static final Logger log = LoggerFactory.getLogger(SESRecommendationOutputController.class);

    @Value("#{dataAccess}")
    private DataAccess dataAccess;
    @Autowired
    private ApplicationContext context;

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public void outputCloudServicesAsWineryNodes(String outputPath) {
        List<CloudProvider> providers = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        for (CloudProvider cloudProvider : providers) {

            CloudServicesToWinery cloudServicesToWinery = new CloudServicesToWinery();
            //I need to write for all services new types (except VM) , for which I can just instantiate the existing type
            cloudServicesToWinery.createWineryNodesFromCloudServices(cloudProvider, outputPath);
        }
    }

    public void outputConfigurationSolutionToWinery(List<List<ServiceUnitConfigurationSolution>> configurationsList, String serviceTemplateName, String serviceTemplatesOutputPath) {
        CloudServicesToWinery cloudServicesToWinery = new CloudServicesToWinery();

        //I need to write for all services new types (except VM) , for which I can just instantiate the existing type
        cloudServicesToWinery.createToscaServiceTemplate(configurationsList, serviceTemplateName, serviceTemplatesOutputPath + "/http%3A%2F%2Fwww.dsg.tuwien.ac.at%2Ftosca%2FServiceTemplates%2" + serviceTemplateName);
    }

}
