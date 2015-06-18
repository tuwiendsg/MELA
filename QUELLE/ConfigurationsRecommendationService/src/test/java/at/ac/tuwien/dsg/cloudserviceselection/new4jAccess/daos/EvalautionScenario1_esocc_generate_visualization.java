///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package at.ac.tuwien.dsg.cloudserviceselection.new4jAccess.daos;
//
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.DataAccess;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.daos.CloudProviderDAO;
//import at.ac.tuwien.dsg.cloudofferedservices.requirements.MultiLevelRequirements;
//import at.ac.tuwien.dsg.cloudofferedservices.requirements.Strategy;
//import at.ac.tuwien.dsg.cloudofferedservices.requirements.StrategyCategory;
//import at.ac.tuwien.dsg.cloudserviceselection.engines.CloudServiceElasticityAnalysisEngine;
//import at.ac.tuwien.dsg.cloudserviceselection.engines.CloudServiceUnitAnalysisEngine.AnalysisResult;
//import at.ac.tuwien.dsg.cloudserviceselection.engines.RequirementsMatchingEngine;
//import at.ac.tuwien.dsg.cloudserviceselection.engines.ServiceUnitComparators;
//import at.ac.tuwien.dsg.cloudserviceselection.requirements.RequirementsResolutionResult;
//import at.ac.tuwien.dsg.cloudserviceselection.requirements.ServiceUnitConfigurationSolution;
//import at.ac.tuwien.dsg.cloudserviceselection.util.writers.AnalysisResultCSVWriter;
//import at.ac.tuwien.dsg.cloudserviceselection.util.writers.CloudServicesToOpenTosca;
//import at.ac.tuwien.dsg.cloudserviceselection.util.writers.DOTWriter;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
//import at.ac.tuwien.dsg.mela.common.requirements.Condition;
//import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
//import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.Marshaller;
//import javax.xml.bind.Unmarshaller;
//
//import junit.framework.TestCase;
//import org.neo4j.graphdb.Transaction;
//import org.springframework.beans.factory.annotation.Autowired;
//
///**
// *
// * @author daniel-tuwien
// */
//public class EvalautionScenario1_esocc_generate_visualization extends TestCase {
//
//    @Autowired
//    private CloudServiceElasticityAnalysisEngine cloudServiceElasticityAnalysisEngine;
//
//    @Autowired
//    private RequirementsMatchingEngine requirementsMatchingEngine;
//
//    @Autowired
//    private ServiceUnitComparators serviceUnitComparators;
//
//    private DataAccess access;
//    private Transaction transaction;
//
//    public EvalautionScenario1_esocc_generate_visualization(String testName) {
//        super(testName);
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        access = new DataAccess("/tmp/neo4j");
//        access.clear();
//
//        transaction = access.startTransaction();
//        super.setUp();
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        super.tearDown();
//
//        transaction.success();
//        transaction.failure();
//        access.getGraphDatabaseService().shutdown();
//    }
//
//    /**
//     * Test of matchServiceUnit method, of class RequirementsMatchingEngine.
//     *
//     * Also generates OpenTosca format
//     */
//    public void testEcosystemDescription() throws IOException {
//
//        List<CloudProvider> cloudProviders = new ArrayList<CloudProvider>();
////        
//
//        //
//        // ==========================================================================================
//        // amazon cloud description
//        try {
//            JAXBContext context = JAXBContext.newInstance(CloudProvider.class);
//            Unmarshaller unmarshaller = context.createUnmarshaller();
//            CloudProvider provider = (CloudProvider) unmarshaller.unmarshal(new File("./experiments/amazonDescription.xml"));
//            cloudProviders.add(provider);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        CloudProviderDAO.persistCloudProviders(cloudProviders, access.getGraphDatabaseService());
//
//        AnalysisResultCSVWriter.writeAnalysisResult(cloudServiceElasticityAnalysisEngine.analyzeElasticity(cloudProviders.get(0)), "./experiments/AmazonAnalysis_esocc.csv");
//
//        try {
//            access.writeGraphAsGraphVis("./AmazonExample.dot");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
////        ##########################################################################################################################
////         Selecting service units
//        List<MultiLevelRequirements> requirementsIteration = new ArrayList<MultiLevelRequirements>();
////        
//
//        //with cost and service unit elasticity
//        {
//            List<Strategy> strategies = new ArrayList<Strategy>();
//            {
//                Strategy serviceLevelStrategy = new Strategy();
//                serviceLevelStrategy.setStrategyCategory(StrategyCategory.OVERALL_REQUIREMENTS);
//                strategies.add(serviceLevelStrategy);
//            }
//            {
//                Strategy serviceLevelStrategy = new Strategy();
//                serviceLevelStrategy.setStrategyCategory(StrategyCategory.QUALITY_ELASTICITY);
//                strategies.add(serviceLevelStrategy);
//            }
//
//            {
//                Strategy serviceLevelStrategy = new Strategy();
//                serviceLevelStrategy.setStrategyCategory(StrategyCategory.COST_ELASTICITY);
//                strategies.add(serviceLevelStrategy);
//            }
//            {
//                Strategy serviceLevelStrategy = new Strategy();
//                serviceLevelStrategy.setStrategyCategory(StrategyCategory.MINIMUM_COST);
//                strategies.add(serviceLevelStrategy);
//            }
//
//            addRequirements(strategies, requirementsIteration);
//        }
//
//////
//        Writer writer = new FileWriter("./experiments/scenario1/scenario1_esocc.csv");
//
//        writer.write("ServiceUnit, Strategies,  Recommended Service Units , Recommended Service Units Count, Avg. Cost Elasticity, Min Cost Elasticity, Max Cost Elasticity, Avg. Service Unit Association Elasticity, Min Service Unit Association Elasticity, Max Service Unit Association Elasticity, Avg. Resource Elasticity, Min Resource Elasticity, Max Resource Elasticity, Avg. Quality Elasticity, Min Quality Elasticity, Max Quality Elasticity");
//        writer.write("\n");
//
//        new File("./OpenToscaOutput/servicesTemplates").mkdir();
//        new File("./OpenToscaOutput/servicesTemplates/http%3A%2F%2Fwww.dsg.tuwien.ac.at%2Ftosca%2FServiceTemplates%2FSES").mkdir();
//
//        for (MultiLevelRequirements serviceRequirements : requirementsIteration) {
//
//            List<MultiLevelRequirements> individualServiceUnitRequirements = serviceRequirements.flatten();
// 
//            for (MultiLevelRequirements reqs : individualServiceUnitRequirements) {
//                // System.out.println("Analyzing " + reqs);
//
//                RequirementsResolutionResult result = requirementsMatchingEngine.analyzeMultiLevelRequirements(cloudProviders, reqs,
//                        access.getGraphDatabaseService());
//                Map<MultiLevelRequirements, Map<Requirements, List<ServiceUnitConfigurationSolution>>> bestElasticity = result.getConcreteConfigurations(serviceUnitComparators);
//
//                {
//                    for (MultiLevelRequirements levelRequirements : bestElasticity.keySet()) {
//
//                        // System.out.println(" For " +
//                        // levelRequirements.getName() + " we have options");
//                        Map<Requirements, List<ServiceUnitConfigurationSolution>> solutions = bestElasticity.get(levelRequirements);
//
//                        DOTWriter.writeTopologyConfigurationSolutions(reqs.getName(), solutions,
//                                "./experiments/scenario1/" + levelRequirements.getName());
//
//                        CloudServicesToOpenTosca cloudServicesToOpenTosca = new CloudServicesToOpenTosca();
//
//                        //I need to write for all services new types (except VM) , for which I can just instantiate the existing type
//                        cloudServicesToOpenTosca.createToscaServiceTemplate(levelRequirements, bestElasticity, levelRequirements.getName(), "./OpenToscaOutput/servicesTemplates/http%3A%2F%2Fwww.dsg.tuwien.ac.at%2Ftosca%2FServiceTemplates%2FSES");
//
//                        String strategies = "";
//                        for (Strategy s : levelRequirements.getOptimizationStrategies()) {
//                            strategies += "_" + s.getStrategyCategory();
//                        }
//
//                        for (Requirements requirements : solutions.keySet()) {
//                            String solutionsNames = "";
//
//                            int solutionsCount = solutions.get(requirements).size();
//
//                            // compute average elasticities
//                            double averageCostElasticity = 0d;
//                            double averageSUElasticity = 0d;
//                            double averageResourceElasticity = 0d;
//                            double averageQualityElasticity = 0d;
//
//                            double minCostElasticity = Double.POSITIVE_INFINITY;
//                            double minSUElasticity = Double.POSITIVE_INFINITY;
//                            double minResourceElasticity = Double.POSITIVE_INFINITY;
//                            double minQualityElasticity = Double.POSITIVE_INFINITY;
//
//                            double maxCostElasticity = Double.NEGATIVE_INFINITY;
//                            double maxSUElasticity = Double.NEGATIVE_INFINITY;
//                            double maxResourceElasticity = Double.NEGATIVE_INFINITY;
//                            double maxQualityElasticity = Double.NEGATIVE_INFINITY;
//
//                            for (ServiceUnitConfigurationSolution solutionConfiguration : solutions.get(requirements)) {
//                                //
//                                // // System.out.println("Matched " +
//                                // solutionConfiguration.getOverallMatched());
//                                // // System.out.println("Unmatched " +
//                                // solutionConfiguration.getOverallUnMatched());
//                                //
//                                // String configurationJSONDescription =
//                                // solutionConfiguration.toJSON().toJSONString();
//                                // System.out.println(configurationJSONDescription);
//                                AnalysisResult analysisResult = cloudServiceElasticityAnalysisEngine.analyzeElasticity(solutionConfiguration.getServiceUnit());
//                                solutionsNames += " " + solutionConfiguration.getServiceUnit().getName();
//
//                                double costElasticity = (Double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.COST_ELASTICITY);
//                                double sUElasticity = (Double) analysisResult
//                                        .getValue(CloudServiceElasticityAnalysisEngine.SERVICE_UNIT_ASSOCIATION_ELASTICITY);
//                                double resourceElasticity = (Double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.RESOURCE_ELASTICITY);
//                                double qualityElasticity = (Double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.QUALITY_ELASTICITY);
//
//                                averageCostElasticity += costElasticity;
//                                averageSUElasticity += sUElasticity;
//                                averageResourceElasticity += resourceElasticity;
//                                averageQualityElasticity += qualityElasticity;
//
//                                if (minCostElasticity > costElasticity) {
//                                    minCostElasticity = costElasticity;
//                                }
//
//                                if (minSUElasticity > sUElasticity) {
//                                    minSUElasticity = sUElasticity;
//                                }
//
//                                if (minResourceElasticity > resourceElasticity) {
//                                    minResourceElasticity = resourceElasticity;
//                                }
//
//                                if (minQualityElasticity > qualityElasticity) {
//                                    minQualityElasticity = qualityElasticity;
//                                }
//
//                                if (maxCostElasticity < costElasticity) {
//                                    maxCostElasticity = costElasticity;
//                                }
//
//                                if (maxSUElasticity < sUElasticity) {
//                                    maxSUElasticity = sUElasticity;
//                                }
//
//                                if (maxResourceElasticity < resourceElasticity) {
//                                    maxResourceElasticity = resourceElasticity;
//                                }
//
//                                if (maxQualityElasticity < qualityElasticity) {
//                                    maxQualityElasticity = qualityElasticity;
//                                }
//
//                            }
//                            // write cfg sol as dot
//                            // DOTWriter.writeServiceUnitConfigurationSolutions(solutions.get(requirements),
//                            // new
//                            // FileWriter("./experiments/scenario2/solutions_" +
//                            // requirements.getName() + strategies + ".dot"));
//
//                            averageCostElasticity /= solutionsCount;
//                            averageSUElasticity /= solutionsCount;
//                            averageResourceElasticity /= solutionsCount;
//                            averageQualityElasticity /= solutionsCount;
//
//                            writer.write(requirements.getName() + "," + strategies + "," + solutionsNames + "," + solutionsCount + "," + averageCostElasticity + ","
//                                    + minCostElasticity + "," + maxCostElasticity + "," + averageSUElasticity + "," + minSUElasticity + "," + maxSUElasticity
//                                    + "," + averageResourceElasticity + "," + minResourceElasticity + "," + maxResourceElasticity + ","
//                                    + averageQualityElasticity + "," + minQualityElasticity + "," + maxQualityElasticity);
//                            writer.write("\n");
//                        }
//
//                    }
//                }
//
//            }
//            // break;
//        }
//
//        writer.flush();
//
//        writer.close();
//
//        Runtime.getRuntime().exec("/usr/bin/dot -Tpng ./experiments/scenario1/* -O");
//
//    }
//
//    public void _addRequirements(List<Strategy> strategies, List<MultiLevelRequirements> requirementsIteration) {
//        {
//            MultiLevelRequirements serviceRequirements = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE);
//            serviceRequirements.setName("ServiceReqs_overall_elasticity_multi");
//            for (Strategy strategy : strategies) {
//
//                serviceRequirements.addStrategy(strategy);
//            }
//
//            MultiLevelRequirements topologyRequirements = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//            topologyRequirements.setName("ServiceTopologyReqs");
//            serviceRequirements.addMultiLevelRequirements(topologyRequirements);
//
////
//            {
//                MultiLevelRequirements serviceUnitLevel = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                serviceUnitLevel.setName("EventProcessingTopology");
//
//                topologyRequirements.addMultiLevelRequirements(serviceUnitLevel);
//                {
//
//                    Requirements requirements = new Requirements();
//                    //construct requirements
//                    requirements.setName("EventProcessingUnit");
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("CPUArchitectureReq");
//                        Metric archMetric = new Metric("Architecture", "type", Metric.MetricType.RESOURCE);
//                        Metric coresMetric = new Metric("VCPU", "number", Metric.MetricType.RESOURCE);
//
//                        requirement.setMetric(archMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.EQUAL, archMetric, new MetricValue("64"));
//                            requirement.addCondition(condition);
//                        }
////                    {
////                        Condition condition = new Condition(Condition.Type.GREATER_EQUAL, coresMetric, new MetricValue(4));
////                        requirement.addCondition(condition);
////                    }
//                        requirements.addRequirement(requirement);
//
//                    }
//
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("CPUCoresReq");
//                        Metric coresMetric = new Metric("VCPU", "number", Metric.MetricType.RESOURCE);
//                        requirement.setMetric(coresMetric);
//                        {
////                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, coresMetric, new MetricValue(4));
//                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, coresMetric, new MetricValue(2)); //1 also includes more vms in the result
//                            requirement.addCondition(condition);
//                        }
//                        requirements.addRequirement(requirement);
//
//                    }
//
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("NetworkPerformanceReq");
//                        Metric targetMetric = new Metric("Network", "performance", Metric.MetricType.QUALITY);
//                        requirement.setMetric(targetMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.ENUMERATION, targetMetric, new MetricValue("Moderate"), new MetricValue("High"), new MetricValue("10 Gigabit"));
//                            requirement.addCondition(condition);
//                        }
//                        requirements.addRequirement(requirement);
////
//                    }
//
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("MemorySizeReq");
//                        Metric targetMetric = new Metric("Memory", "GB", Metric.MetricType.RESOURCE);
//                        requirement.setMetric(targetMetric);
//                        {
////                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, targetMetric, new MetricValue(10));
//                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, targetMetric, new MetricValue(5)); //3 also includes m1.medium and more others
//                            requirement.addCondition(condition);
//                        }
//                        requirements.addRequirement(requirement);
//                    }
//                    serviceUnitLevel.addRequirements(requirements);
//
//                }
//
////      
////
//            }
//            //TODO: de verificat de ce returneaza prostii pe resources.
//
//            {
//                MultiLevelRequirements serviceUnitLevel = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                serviceUnitLevel.setName("DataEnd");
//
////                {
////                    Strategy serviceLevelStrategy = new Strategy();
////                    serviceLevelStrategy.setStrategyCategory(StrategyCategory.COST_ELASTICITY);
////                    serviceUnitLevel.addStrategy(serviceLevelStrategy);
////                }
////
////                {
////                    Strategy serviceLevelStrategy = new Strategy();
////                    serviceLevelStrategy.setStrategyCategory(StrategyCategory.SERVICE_UNITS_ASSOCIATIONS_ELASTICITY);
////                    serviceUnitLevel.addStrategy(serviceLevelStrategy);
////                }
//                topologyRequirements.addMultiLevelRequirements(serviceUnitLevel);
//                {
//
//                    Requirements requirements = new Requirements();
//                    //construct requirements
//                    requirements.setName("DataNodeReqs");
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("CPUArchitectureReq");
//                        Metric archMetric = new Metric("Architecture", "type", Metric.MetricType.RESOURCE);
//                        Metric coresMetric = new Metric("VCPU", "number", Metric.MetricType.RESOURCE);
//
//                        requirement.setMetric(archMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.EQUAL, archMetric, new MetricValue("64"));
//                            requirement.addCondition(condition);
//                        }
////                    {
////                        Condition condition = new Condition(Condition.Type.GREATER_EQUAL, coresMetric, new MetricValue(4));
////                        requirement.addCondition(condition);
////                    }
//                        requirements.addRequirement(requirement);
//
//                    }
//
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("CPUCoresReq");
//                        Metric coresMetric = new Metric("VCPU", "number", Metric.MetricType.RESOURCE);
//                        requirement.setMetric(coresMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, coresMetric, new MetricValue(2));
//                            requirement.addCondition(condition);
//                        }
//                        requirements.addRequirement(requirement);
//
//                    }
//
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("NetworkPerformanceReq");
//                        Metric targetMetric = new Metric("Network", "performance", Metric.MetricType.QUALITY);
//                        requirement.setMetric(targetMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.ENUMERATION, targetMetric, new MetricValue("Moderate"), new MetricValue("High"), new MetricValue("10 Gigabit"));
//                            requirement.addCondition(condition);
//                        }
//                        requirements.addRequirement(requirement);
////
//                    }
//
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("MemorySizeReq");
//                        Metric targetMetric = new Metric("Memory", "GB", Metric.MetricType.RESOURCE);
//                        requirement.setMetric(targetMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, targetMetric, new MetricValue(10));
//                            requirement.addCondition(condition);
//                        }
//                        requirements.addRequirement(requirement);
//                    }
//
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("IOPerformanceReq");
//                        Metric targetMetric = new Metric("Storage", "IOPS", Metric.MetricType.QUALITY);
//                        requirement.setMetric(targetMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, targetMetric, new MetricValue("1000"));
//                            requirement.addCondition(condition);
//                        }
//                        requirements.addRequirement(requirement);
//
//                    }
//                    serviceUnitLevel.addRequirements(requirements);
//
//                }
//
//            }
//            requirementsIteration.add(serviceRequirements);
//        }
//    }
//
//    public void addRequirements(List<Strategy> strategies, List<MultiLevelRequirements> requirementsIteration) {
//        {
//            try {
//                MultiLevelRequirements serviceRequirements = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE);
//                serviceRequirements.setName("ServiceReqs_overall_elasticity_multi");
//                for (Strategy strategy : strategies) {
//
//                    serviceRequirements.addStrategy(strategy);
//                }
//
//                {
//                    Requirements requirements = new Requirements();
//                    // construct requirements
//                    requirements.setName("Monitoring");
//                    {
//                        Requirement requirement = new Requirement();
//                        requirement.setName("MonitoringFreqReq");
//                        Metric archMetric = new Metric("monitoredFreq", "min", Metric.MetricType.QUALITY);
//
//                        requirement.setMetric(archMetric);
//                        {
//                            Condition condition = new Condition(Condition.Type.GREATER_EQUAL, archMetric, new MetricValue(5));
//                            requirement.addCondition(condition);
//                        }
//                        //
//                        requirements.addRequirement(requirement);
//
//                    }
//                    serviceRequirements.addRequirements(requirements);
//                }
//
//                //
//                {
//                    MultiLevelRequirements topologyRequirements = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                    topologyRequirements.setName("EventProcessingTopology");
//
//                    serviceRequirements.addMultiLevelRequirements(topologyRequirements);
//
//                    {
//                        Requirements requirements = new Requirements();
//                        // construct requirements
//                        requirements.setName("Monitoring");
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("MonitoringFreqReq");
//                            Metric archMetric = new Metric("monitoredFreq", "min", Metric.MetricType.QUALITY);
//
//                            requirement.setMetric(archMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.LESS_EQUAL, archMetric, new MetricValue(1));
//                                requirement.addCondition(condition);
//                            }
//                            //
//                            requirements.addRequirement(requirement);
//
//                        }
//                        topologyRequirements.addRequirements(requirements);
//                    }
//
//                    MultiLevelRequirements serviceUnitLevel = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                    serviceUnitLevel.setName("EventProcessingTopology");
//
//                    topologyRequirements.addMultiLevelRequirements(serviceUnitLevel);
//
//                    {
//
//                        Requirements requirements = new Requirements();
//                        // construct requirements
//                        requirements.setName("EventProcessingUnit");
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("CPUArchitectureReq");
//                            Metric archMetric = new Metric("Architecture", "type", Metric.MetricType.RESOURCE);
//                            Metric coresMetric = new Metric("VCPU", "number", Metric.MetricType.RESOURCE);
//
//                            requirement.setMetric(archMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.EQUAL, archMetric, new MetricValue("64"));
//                                requirement.addCondition(condition);
//                            }
//                            // {
//                            // Condition condition = new
//                            // Condition(Condition.Type.GREATER_EQUAL, coresMetric,
//                            // new MetricValue(4));
//                            // requirement.addCondition(condition);
//                            // }
//                            requirements.addRequirement(requirement);
//
//                        }
//
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("CPUCoresReq");
//                            Metric coresMetric = new Metric("VCPU", "number", Metric.MetricType.RESOURCE);
//                            requirement.setMetric(coresMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.GREATER_EQUAL, coresMetric, new MetricValue(2));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//
//                        }
//
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("NetworkPerformanceReq");
//                            Metric targetMetric = new Metric("Network", "performance", Metric.MetricType.QUALITY);
//                            requirement.setMetric(targetMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.ENUMERATION, targetMetric, new MetricValue("Moderate"), new MetricValue("High"),
//                                        new MetricValue("10 Gigabit"));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//                            //
//                        }
//
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("MemorySizeReq");
//                            Metric targetMetric = new Metric("Memory", "GB", Metric.MetricType.RESOURCE);
//                            requirement.setMetric(targetMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.GREATER_EQUAL, targetMetric, new MetricValue(5));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//                        }
//
//                        serviceUnitLevel.addRequirements(requirements);
//
//                    }
//
//                    {
//
//                        Requirements requirements = new Requirements();
//                        // construct requirements
//                        requirements.setName("MessageOrientedMiddleware");
//                        {
//                            Requirement requirement = new Requirement();
//
//                            requirement.setName("MessagingReq");
//                            Metric archMetric = new Metric("message", "queue", Metric.MetricType.RESOURCE);
//
//                            requirement.setMetric(archMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.EQUAL, archMetric, new MetricValue(""));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//
//                        }
//
//                        serviceUnitLevel.addRequirements(requirements);
//
//                    }
//
//                }
//
//                // TODO: de verificat de ce returneaza prostii pe resources.
//                {
//                    MultiLevelRequirements topologyRequirements = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
//                    topologyRequirements.setName("DataEndTopology");
//                    serviceRequirements.addMultiLevelRequirements(topologyRequirements);
//
//                    MultiLevelRequirements serviceUnitLevel = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
//                    //same name as topiology due to bug in generating .dot
//                    serviceUnitLevel.setName("DataEndTopology");
//
//                    topologyRequirements.addMultiLevelRequirements(serviceUnitLevel);
//                    {
//
//                        Requirements requirements = new Requirements();
//                        // construct requirements
//                        requirements.setName("DataEndUnit");
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("CPUArchitectureReq");
//                            Metric archMetric = new Metric("Architecture", "type", Metric.MetricType.RESOURCE);
//                            requirement.setMetric(archMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.EQUAL, archMetric, new MetricValue("64"));
//                                requirement.addCondition(condition);
//                            }
//                            // {
//                            // Condition condition = new
//                            // Condition(Condition.Type.GREATER_EQUAL, coresMetric,
//                            // new MetricValue(4));
//                            // requirement.addCondition(condition);
//                            // }
//                            requirements.addRequirement(requirement);
//
//                        }
//
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("CPUCoresReq");
//                            Metric coresMetric = new Metric("VCPU", "number", Metric.MetricType.RESOURCE);
//                            requirement.setMetric(coresMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.GREATER_EQUAL, coresMetric, new MetricValue(2));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//
//                        }
//
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("NetworkPerformanceReq");
//                            Metric targetMetric = new Metric("Network", "performance", Metric.MetricType.QUALITY);
//                            requirement.setMetric(targetMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.ENUMERATION, targetMetric, new MetricValue("Moderate"), new MetricValue("High"),
//                                        new MetricValue("10 Gigabit"));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//                            //
//                        }
//
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("MemorySizeReq");
//                            Metric targetMetric = new Metric("Memory", "GB", Metric.MetricType.RESOURCE);
//                            requirement.setMetric(targetMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.GREATER_EQUAL, targetMetric, new MetricValue(10));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//                        }
//
//                        {
//                            Requirement requirement = new Requirement();
//                            requirement.setName("IOPerformanceReq");
//                            Metric targetMetric = new Metric("Storage", "IOPS", Metric.MetricType.QUALITY);
//                            requirement.setMetric(targetMetric);
//                            {
//                                Condition condition = new Condition(Condition.Type.GREATER_EQUAL, targetMetric, new MetricValue("1000"));
//                                requirement.addCondition(condition);
//                            }
//                            requirements.addRequirement(requirement);
//
//                        }
//                        serviceUnitLevel.addRequirements(requirements);
//
//                    }
//
//                }
//                requirementsIteration.add(serviceRequirements);
//
//                //store requirements as xml
//                JAXBContext jAXBContext = JAXBContext.newInstance(MultiLevelRequirements.class);
//                Marshaller marshaller = jAXBContext.createMarshaller();
//                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//
//                FileWriter w = new FileWriter(new File("./experiments/scenario1/requirements.xml"));
//                marshaller.marshal(serviceRequirements, w);
//                w.flush();
//                w.close();
//
//            } catch (Exception ex) {
//                Logger.getLogger(EvalautionScenario1_esocc_generate_visualization.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//
//    }
//
//}
