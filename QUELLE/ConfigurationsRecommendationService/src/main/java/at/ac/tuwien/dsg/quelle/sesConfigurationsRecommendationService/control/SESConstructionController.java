/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control;

import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.dtos.CloudServiceConfigurationRecommendation;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.dtos.ServiceUnitServicesRecommendation;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.quelle.descriptionParsers.CloudDescriptionParser;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.csvOutputFormatters.AnalysisResultCSVWriter;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.CloudServiceElasticityAnalysisEngine;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.CloudServiceUnitAnalysisEngine;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.RequirementsMatchingEngine;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.ServiceUnitComparators;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.RequirementsResolutionResult;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.ServiceUnitConfigurationSolution;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.ws.rs.core.Response;
import org.neo4j.graphdb.Transaction;
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
@Service("sesConstructionController")
public class SESConstructionController implements InitializingBean {

    static final Logger log = LoggerFactory.getLogger(SESConstructionController.class);

    @Value("#{dataAccess}")
    private DataAccess dataAccess;
    @Autowired
    private ApplicationContext context;

    @Autowired
    private CloudServiceElasticityAnalysisEngine cloudServiceElasticityAnalysisEngine;

    @Autowired
    private RequirementsMatchingEngine requirementsMatchingEngine;

    @Autowired
    private ServiceUnitComparators serviceUnitComparators;

    @Autowired
//    private ConfigurationUtil configurationUtil;

    @Override
    public void afterPropertiesSet() throws Exception {
//        CloudProvider provider = configurationUtil.createAmazonDefaultCloudProvider();
//
//        Transaction transaction = dataAccess.startTransaction();
//
//        try {
//
//            CloudProviderDAO.persistCloudProvider(provider, dataAccess.getGraphDatabaseService());
//
//            transaction.success();
//        } catch (Exception e) {
//            e.printStackTrace();
//            transaction.failure();
//        }
//        transaction.finish();
        updateCloudProvidersDescription();

    }

    public void updateCloudProvidersDescription() {

        List<CloudProvider> providers = new ArrayList<>();

        // list all MELA datasources from application context
        Map<String, CloudDescriptionParser> cloudParsers = context.getBeansOfType(CloudDescriptionParser.class);
        for (String name : cloudParsers.keySet()) {
            CloudDescriptionParser cloudDescriptionParser = cloudParsers.get(name);
            log.debug("Using CloudDescriptionParser '{}': {}  to update cloud description", name, cloudDescriptionParser);
            CloudProvider provider = cloudDescriptionParser.getCloudProviderDescription();
            providers.add(provider);
        }

        CloudProviderDAO.persistCloudProviders(providers, dataAccess.getGraphDatabaseService());

    }

    /**
     *
     * @param multiLevelRequirements
     * @return
     */
    public List<ServiceUnitServicesRecommendation> analyzeRequirements(MultiLevelRequirements multiLevelRequirements) {

        List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        List<MultiLevelRequirements> individualServiceUnitRequirements = multiLevelRequirements.flatten();

        List<ServiceUnitServicesRecommendation> recommendations = new ArrayList<>();

        //each MultiLevelRequirements means actually a Unit requirements (with topology, etc maped on the unit reqs)
        for (MultiLevelRequirements reqs : individualServiceUnitRequirements) {

            RequirementsResolutionResult result = requirementsMatchingEngine.analyzeMultiLevelRequirements(cloudProviders, reqs);
            Map<MultiLevelRequirements, Map<Requirements, List<ServiceUnitConfigurationSolution>>> bestElasticity = result.getConcreteConfigurations(serviceUnitComparators);

            {
                for (MultiLevelRequirements levelRequirements : bestElasticity.keySet()) {

                    Map<Requirements, List<ServiceUnitConfigurationSolution>> solutions = bestElasticity.get(levelRequirements);

//                    String strategies = "";
//                    for (Strategy s : levelRequirements.getOptimizationStrategies()) {
//                        strategies += "_" + s.getStrategyCategory();
//                    }
                    for (Requirements requirements : solutions.keySet()) {

                        List<CloudServiceConfigurationRecommendation> recommendedConfigurations = new ArrayList<>();

//                        String solutionsNames = "";
//
//                        int solutionsCount = solutions.get(requirements).size();
//
//                        // compute average elasticities
//                        double averageCostElasticity = 0d;
//                        double averageSUElasticity = 0d;
//                        double averageResourceElasticity = 0d;
//                        double averageQualityElasticity = 0d;
//
//                        double minCostElasticity = Double.POSITIVE_INFINITY;
//                        double minSUElasticity = Double.POSITIVE_INFINITY;
//                        double minResourceElasticity = Double.POSITIVE_INFINITY;
//                        double minQualityElasticity = Double.POSITIVE_INFINITY;
//
//                        double maxCostElasticity = Double.NEGATIVE_INFINITY;
//                        double maxSUElasticity = Double.NEGATIVE_INFINITY;
//                        double maxResourceElasticity = Double.NEGATIVE_INFINITY;
//                        double maxQualityElasticity = Double.NEGATIVE_INFINITY;
                        for (ServiceUnitConfigurationSolution solutionConfiguration : solutions.get(requirements)) {

                            //
                            // // System.out.println("Matched " +
                            // solutionConfiguration.getOverallMatched());
                            // // System.out.println("Unmatched " +
                            // solutionConfiguration.getOverallUnMatched());
                            //
                            // String configurationJSONDescription =
                            // solutionConfiguration.toJSON().toJSONString();
                            // System.out.println(configurationJSONDescription);
                            CloudServiceUnitAnalysisEngine.AnalysisResult analysisResult = cloudServiceElasticityAnalysisEngine.analyzeElasticity(solutionConfiguration.getServiceUnit());
//                            solutionsNames += " " + solutionConfiguration.getServiceUnit().getName();

                            double costElasticity = (double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.COST_ELASTICITY);
                            double sUElasticity = (double) analysisResult
                                    .getValue(CloudServiceElasticityAnalysisEngine.SERVICE_UNIT_ASSOCIATION_ELASTICITY);
                            double resourceElasticity = (double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.RESOURCE_ELASTICITY);
                            double qualityElasticity = (double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.QUALITY_ELASTICITY);

                            recommendedConfigurations.add(new CloudServiceConfigurationRecommendation().withServiceUnitConfigurationSolution(requirements.getName(), solutionConfiguration, costElasticity, sUElasticity, resourceElasticity, qualityElasticity));

//                            averageCostElasticity += costElasticity;
//                            averageSUElasticity += sUElasticity;
//                            averageResourceElasticity += resourceElasticity;
//                            averageQualityElasticity += qualityElasticity;
//
//                            if (minCostElasticity > costElasticity) {
//                                minCostElasticity = costElasticity;
//                            }
//
//                            if (minSUElasticity > sUElasticity) {
//                                minSUElasticity = sUElasticity;
//                            }
//
//                            if (minResourceElasticity > resourceElasticity) {
//                                minResourceElasticity = resourceElasticity;
//                            }
//
//                            if (minQualityElasticity > qualityElasticity) {
//                                minQualityElasticity = qualityElasticity;
//                            }
//
//                            if (maxCostElasticity < costElasticity) {
//                                maxCostElasticity = costElasticity;
//                            }
//
//                            if (maxSUElasticity < sUElasticity) {
//                                maxSUElasticity = sUElasticity;
//                            }
//
//                            if (maxResourceElasticity < resourceElasticity) {
//                                maxResourceElasticity = resourceElasticity;
//                            }
//
//                            if (maxQualityElasticity < qualityElasticity) {
//                                maxQualityElasticity = qualityElasticity;
//                            }
                        }

                        recommendations.add(new ServiceUnitServicesRecommendation().withSolutionRecommendation(reqs, requirements, recommendedConfigurations));

                        // write cfg sol as dot
                        // DOTWriter.writeServiceUnitConfigurationSolutions(solutions.get(requirements),
                        // new
                        // FileWriter("./experiments/scenario2/solutions_" +
                        // requirements.getName() + strategies + ".dot"));
//                        averageCostElasticity /= solutionsCount;
//                        averageSUElasticity /= solutionsCount;
//                        averageResourceElasticity /= solutionsCount;
//                        averageQualityElasticity /= solutionsCount;
//                        writer.write(requirements.getName() + "," + strategies + "," + solutionsNames + "," + solutionsCount + "," + averageCostElasticity + ","
//                                + minCostElasticity + "," + maxCostElasticity + "," + averageSUElasticity + "," + minSUElasticity + "," + maxSUElasticity
//                                + "," + averageResourceElasticity + "," + minResourceElasticity + "," + maxResourceElasticity + ","
//                                + averageQualityElasticity + "," + minQualityElasticity + "," + maxQualityElasticity);
//                        writer.write("\n");
                    }

                }
            }

        }


        return recommendations;
    }

    public String analyzeElasticityOfProvider(String cloudProvider) {

        CloudProvider provider = CloudProviderDAO.searchForCloudProvidersUniqueResult(new CloudProvider(cloudProvider), dataAccess.getGraphDatabaseService());
        if (provider == null) {
            String response = "Provider " + cloudProvider + " not found within providers: ";
            for (CloudProvider p : CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService())) {
                response += p.getName() + ", ";
            }
            return response;
        } else {
            try {
                return AnalysisResultCSVWriter.getAnalysisResult(cloudServiceElasticityAnalysisEngine.analyzeElasticity(provider));
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
                return ex.getMessage();
            }
        }

    }

}
