///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package at.ac.tuwien.dsg.cloudserviceselection.new4jAccess.daos;
//
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.CostElement;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.CostFunction;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.ElasticityCapability;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.Quality;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.Resource;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.ServiceUnit;
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.Volatility;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.DataAccess;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.daos.CloudProviderDAO;
//import at.ac.tuwien.dsg.cloudserviceselection.engines.CloudServiceElasticityAnalysisEngine;
//import at.ac.tuwien.dsg.cloudserviceselection.util.writers.AnalysisResultCSVWriter;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
//import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.Marshaller;
//import junit.framework.TestCase;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.JSONValue;
//import org.json.simple.parser.ParseException;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.neo4j.graphdb.Transaction;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//
///**
// * Used to generate XML description of Amazon EC2 from Amazon URL, used for
// * other test cases
// *
// * @author daniel-tuwien
// */
//public class AmazonCloudDescriptionGeneration extends TestCase {
//
//    @Autowired
//    private CloudServiceElasticityAnalysisEngine cloudServiceElasticityAnalysisEngine;
//
//    @Value("${dataAccess}")
//    private DataAccess access;
//    private Transaction transaction;
//
//    public AmazonCloudDescriptionGeneration(String testName) {
//        super(testName);
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        access = new DataAccess("/tmp/neo4j");
//        access.clear();
//
//        transaction = access.startTransaction();
//        
//        super.setUp();
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        super.tearDown();
//        access.clear();
//        transaction.success();
//        transaction.finish();
//        access.getGraphDatabaseService().shutdown();
//    }
//
//    public void testEcosystemDescription() throws IOException {
//
//        //used to fast add cost properties
//        //key is ServiceUnit name = its ID, such as m1.large
//        Map<String, ServiceUnit> units = new HashMap<>();
//
//        //key is ServiceUnit name = its ID, such as m1.large
//        Map<String, List<ElasticityCapability.Dependency>> costDependencies = new HashMap<>();
//
//        CloudProvider cloudProvider = new CloudProvider("Amazon EC2", CloudProvider.Type.IAAS);
//
//        //other misc Amazon Services 
//        {
//            //create EBS instance
//            ServiceUnit ebsStorageUtility = new ServiceUnit("IaaS", "Storage", "EBS");
//            cloudProvider.addServiceUnit(ebsStorageUtility);
//            {
//                List<ElasticityCapability.Dependency> qualityCapabilityTargets = new ArrayList<>();
//
//                // utility quality
//                Quality stdQuality = new Quality("Standard I/O Performance");
//                stdQuality.addProperty(new Metric("Storage", "IOPS"), new MetricValue("100"));
//                qualityCapabilityTargets.add(new ElasticityCapability.Dependency(stdQuality, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                        .withVolatility(new Volatility(1, 1)));
//				// utility.addQualityProperty(stdQuality);
//
//                // utility quality
//                Quality highQuality = new Quality("High I/O Performance");
//                highQuality.addProperty(new Metric("Storage", "IOPS"), new MetricValue("4000"));
//                qualityCapabilityTargets.add(new ElasticityCapability.Dependency(highQuality, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                        .withVolatility(new Volatility(1, 1)));
//                // utility.addQualityProperty(highQuality);
//
//                {
//                    ElasticityCapability characteristic = new ElasticityCapability("Quality");
//
//                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//
//                    for (ElasticityCapability.Dependency d : qualityCapabilityTargets) {
//
//                        characteristic.addCapabilityDependency(d);
//                    }
//
//                    ebsStorageUtility.addElasticityCapability(characteristic);
//                }
//
//                List<ElasticityCapability.Dependency> costCapabilityTargets = new ArrayList<>();
//
//                CostFunction costFunctionForStdPerformance = new CostFunction("StandardIOPerformanceCost");
//                costCapabilityTargets.add(new ElasticityCapability.Dependency(costFunctionForStdPerformance, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                        .withVolatility(new Volatility(1, 1)));
//                {
//                    // currently Cost is cost unit agnostic?
//                    CostElement costPerGB = new CostElement("StorageCost", new Metric("storageSize", "GB", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
//                    costPerGB.addCostInterval(new MetricValue(1), 0.1);
//                    costFunctionForStdPerformance.addCostElement(costPerGB);
//                }
//
//                {
//                    CostElement costPerIO = new CostElement("I/OCost", new Metric("I/Os", "$/million", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
//                    costPerIO.addCostInterval(new MetricValue(1), 0.1);
//                    costFunctionForStdPerformance.addCostElement(costPerIO);
//                }
//                costFunctionForStdPerformance.addUtilityAppliedInConjunctionWith(stdQuality);
//                // utility.addCostFunction(costFunctionForStdPerformance);
//
//                CostFunction costFunctionForMaxPerformance = new CostFunction("HighIOPerformanceCost");
//                costCapabilityTargets.add(new ElasticityCapability.Dependency(costFunctionForMaxPerformance, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                        .withVolatility(new Volatility(1, 1)));
//                {
//                    // currently Cost is cost unit agnostic?
//                    CostElement costPerGB = new CostElement("StorageCost", new Metric("storageSize", "GB", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
//                    costPerGB.addCostInterval(new MetricValue(1), 0.125);
//                    costFunctionForMaxPerformance.addCostElement(costPerGB);
//                }
//
//                {
//                    CostElement costPerIO = new CostElement("I/OCost", new Metric("I/Os", "$/million", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
//                    costPerIO.addCostInterval(new MetricValue(1), 0.1);
//                    costFunctionForMaxPerformance.addCostElement(costPerIO);
//                }
//                costFunctionForMaxPerformance.addUtilityAppliedInConjunctionWith(highQuality);
//                // utility.addCostFunction(costFunctionForMaxPerformance);
//
//                {
//
//                    ElasticityCapability characteristic = new ElasticityCapability("PerformanceCost");
//                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//                    for (ElasticityCapability.Dependency d : costCapabilityTargets) {
//                        characteristic.addCapabilityDependency(d);
//                    }
//
//                    ebsStorageUtility.addElasticityCapability(characteristic);
//                }
//            }
//        }
//        {
//            //Monitoring
//            {
//                ServiceUnit utility = new ServiceUnit("MaaS", "Monitoring", "Monitoring");
//                cloudProvider.addServiceUnit(utility);
//
//                List<ElasticityCapability.Dependency> qualityCapabilityTargets = new ArrayList<>();
//                List<ElasticityCapability.Dependency> costCapabilityTargets = new ArrayList<>();
//
//                //utility quality
//                Quality stdQuality = new Quality("StdMonitoringFreq");
//                stdQuality.addProperty(new Metric("monitoredFreq", "min"), new MetricValue(5));
//
//                qualityCapabilityTargets.add(new ElasticityCapability.Dependency(stdQuality, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                        .withVolatility(new Volatility(0, 0)));
//
//                //utility quality
//                Quality higherQuality = new Quality("HighMonitoringFreq");
//                higherQuality.addProperty(new Metric("monitoredFreq", "min"), new MetricValue(1));
//
//                qualityCapabilityTargets.add(new ElasticityCapability.Dependency(higherQuality, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                        .withVolatility(new Volatility(0, 0)));
//
//                //  quality elasticity
//                {
//                    ElasticityCapability characteristic = new ElasticityCapability("MonitoringQuality");
//                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//
//                    for (ElasticityCapability.Dependency d : qualityCapabilityTargets) {
//                        characteristic.addCapabilityDependency(d);
//                    }
//                    utility.addElasticityCapability(characteristic);
//                }
//
//                CostFunction costFunctionForStdMonitoring = new CostFunction("StdMonitoringFreqCost");
//                {
//                    //currently Cost is cost unit agnostic?
//                    CostElement monCost = new CostElement("MonitoringCost", new Metric("monitoringCost", "$/hour", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
//                    monCost.addCostInterval(new MetricValue(1), 0.0);
//                    costFunctionForStdMonitoring.addCostElement(monCost);
//                    costFunctionForStdMonitoring.addUtilityAppliedInConjunctionWith(stdQuality);
//                    costCapabilityTargets.add(new ElasticityCapability.Dependency(costFunctionForStdMonitoring, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                            .withVolatility(new Volatility(0, 0)));
//                }
//
//                CostFunction costFunctionForCustomMonitoring = new CostFunction("HighMonitoringFreqCost");
//                {
//                    CostElement monCost = new CostElement("MonitoringCost", new Metric("monitoringCost", "$/month", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
//                    monCost.addCostInterval(new MetricValue(1), 3.5);
//                    costFunctionForCustomMonitoring.addCostElement(monCost);
//                    costFunctionForCustomMonitoring.addUtilityAppliedInConjunctionWith(higherQuality);
//                    costCapabilityTargets.add(new ElasticityCapability.Dependency(costFunctionForCustomMonitoring, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                            .withVolatility(new Volatility(0, 0)));
//                }
//
//                //cost   elasticity
//                {
//                    ElasticityCapability characteristic = new ElasticityCapability("MonitoringCost");
//                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//
//                    for (ElasticityCapability.Dependency d : costCapabilityTargets) {
//                        characteristic.addCapabilityDependency(d);
//                    }
//                    utility.addElasticityCapability(characteristic);
//                }
//
//            }
//        }
//        {
//            //Amazon SQS 
//            {
//                ServiceUnit sqs = new ServiceUnit("PaaS", "CommunicationServices", "SimpleQueue");
//                cloudProvider.addServiceUnit(sqs);
//
//                //utility quality
//                Resource resource = new Resource("MessagingService");
//                resource.addProperty(new Metric("message", "queue"), new MetricValue(""));
//
//                sqs.addResourceProperty(resource);
//
//                CostFunction messagingCost = new CostFunction("MessagingCostFct");
//                {
//                    //currently Cost is cost unit agnostic?
//                    {
//                        CostElement d = new CostElement("MessagingCost", new Metric("messages", "$/million", Metric.MetricType.RESOURCE), CostElement.Type.USAGE);
//                        d.addCostInterval(new MetricValue(1), 0.5);
//                        messagingCost.addCostElement(d);
//                    }
//                }
//
//                sqs.addCostFunction(messagingCost);
//            }
//        }
//
//        List<CloudProvider> cloudProviders = new ArrayList<CloudProvider>();
//
//        cloudProviders.add(cloudProvider);
////        Document doc = Jsoup.connect("http://aws.amazon.com/ec2/instance-types/").get();
////        Element awsVMTypes = doc.select("aws-table section").first();
//        String html = "http://aws.amazon.com/ec2/instance-types/";
//
//        try {
//            Document doc = Jsoup.connect(html).get();
//            Elements tableElements = doc.select("div.aws-table*").first().getElementsByTag("table");
//
//            Elements tableHeaderEles = tableElements.select("thead tr th");
//            System.out.println("headers");
//            for (int i = 0; i < tableHeaderEles.size(); i++) {
//                System.out.println(tableHeaderEles.get(i).text());
//            }
//            System.out.println();
//
//            Elements tableRowElements = tableElements.select(":not(thead) tr");
//
//            //at i = 0 is the HEADER of the table
//            for (int i = 1; i < tableRowElements.size(); i++) {
//
//                //for each row we create another ServiceUnit
//                ServiceUnitBuilder builder = new ServiceUnitBuilder("IaaS", "VM");
//
//                Element row = tableRowElements.get(i);
//                System.out.println("row");
//
//                Elements rowItems = row.select("td");
//                for (int j = 0; j < rowItems.size(); j++) {
//                    //* marks notes, such as 1 *1 (note 1)
//                    String value = rowItems.get(j).text().split("\\*")[0];
//
//                    //do not know why, for large VMs amazon says 24 x 2,048 GB
//                    value = value.replaceAll(",", "");
//                    builder.addProperty(builder.getPropertyNames().get(j), value);
//                }
//                ServiceUnit unit = builder.getUnit();
//                cloudProvider.addServiceUnit(unit);
//                units.put(unit.getName(), unit);
//
//                System.out.println();
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        //spot price http://spot-price.s3.amazonaws.com/spot.js
//        //on demand http://a0.awsstatic.com/pricing/1/ec2/linux-od.min.js
//        //Reserved light a0.awsstatic.com/pricing/1/ec2/linux-ri-light.min.js 
//        //Reserved medium a0.awsstatic.com/pricing/1/ec2/linux-ri-medium.min.js
//        //Reserved heavy a0.awsstatic.com/pricing/1/ec2/linux-ri-heavy.min.js .
//        //get on demand price
//        //spot price http://a0.awsstatic.com/pricing/1/ec2/linux-od.min.js
//        try {
//            //get reserved light utilization
//            addReservedCostOptions("LightUtilization", "http://a0.awsstatic.com/pricing/1/ec2/linux-ri-light.min.js", cloudProvider, units, costDependencies);
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ParseException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        try {
//            addReservedCostOptions("MediumUtilization", "http://a0.awsstatic.com/pricing/1/ec2/linux-ri-medium.min.js", cloudProvider, units, costDependencies);
////            addReservedCostOptions("MediumUtilization", "http://s3.amazonaws.com/aws-assets-pricing-prod/pricing/ec2/SF-Summit-2014/medium_linux.js", cloudProvider, units, costDependencies);
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ParseException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        try {
//            addReservedCostOptions("HeavyUtilization", "http://a0.awsstatic.com/pricing/1/ec2/linux-ri-heavy.min.js", cloudProvider, units, costDependencies);
////            addReservedCostOptions("HeavyUtilization", "http://s3.amazonaws.com/aws-assets-pricing-prod/pricing/ec2/SF-Summit-2014/heavy_linux.js", cloudProvider, units, costDependencies);
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ParseException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        try {
//            addOndemandCostOptions("http://a0.awsstatic.com/pricing/1/ec2/linux-od.min.js", cloudProvider, units, costDependencies);
////            addReservedCostOptions("HeavyUtilization", "http://s3.amazonaws.com/aws-assets-pricing-prod/pricing/ec2/SF-Summit-2014/heavy_linux.js", cloudProvider, units, costDependencies);
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ParseException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        try {
//            addSpotCostOptions("http://spot-price.s3.amazonaws.com/spot.js", cloudProvider, units, costDependencies);
//        } catch (MalformedURLException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ParseException ex) {
//            Logger.getLogger(AmazonCloudDescriptionGeneration.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        //add for each unit its cost elasticity dependencies
//        {
//            for (String suName : costDependencies.keySet()) {
//
//                //currently due to Neo4J accesss tyle implemented by me, i need unique names for Cost elasticity dependencies.
//                ElasticityCapability characteristic = new ElasticityCapability("Cost_" + suName);
//                characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//
//                for (ElasticityCapability.Dependency d : costDependencies.get(suName)) {
//                    characteristic.addCapabilityDependency(d);
//                }
//                ServiceUnit unit = units.get(suName);
//                unit.addElasticityCapability(characteristic);
//            }
//        }
//
//        //remove units to see outcome
////        cloudProvider.setServiceUnits(cloudProvider.getServiceUnits().subList(4, 6));
//        CloudProviderDAO.persistCloudProviders(cloudProviders, access.getGraphDatabaseService());
//
//        AnalysisResultCSVWriter.writeAnalysisResult(cloudServiceElasticityAnalysisEngine.analyzeElasticity(cloudProviders.get(0)), "./experiments/AmazonAnalysis_esocc_partial.csv");
//
//        try {
//            access.writeGraphAsGraphVis("./AmazonExample_partial.dot");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        try {
//            JAXBContext context = JAXBContext.newInstance(CloudProvider.class);
//            Marshaller marshaller = context.createMarshaller();
//            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//            FileWriter w = new FileWriter(new File("./experiments/amazonDescription.xml"));
//            marshaller.marshal(cloudProviders.get(0), w);
//            w.flush();
//            w.close();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//    }
//
//    private void addReservedCostOptions(String schemeName, String pricingSchemeURL, CloudProvider cloudProvider, Map<String, ServiceUnit> units, Map<String, List<ElasticityCapability.Dependency>> costDependencies) throws MalformedURLException, IOException, ParseException {
//        {
//
//            URL url = new URL(pricingSchemeURL);
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
//            String json = "";
//            String line = "";
//            while ((line = reader.readLine()) != null) {
//                json += line;
//            }
//
//            //remove newline
//            json = json.replace("\\n", "");
//            json = json.replace(" ", "");
//
//            //for some URLs, the JSON is not json, it does not contain {"name, instead is {name
//            if (!json.contains("{\"")) {
//                json = json.replace("{", "{\"");
//                json = json.replace(":", "\":");
//                json = json.replace(",", ",\"");
//
//                //fix overreplace from above
//                json = json.replace("\"{", "{");
//                json = json.replace("\"\"", "\"");
//            }
//
//            //remove "callback"
//            json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
////            System.out.println(json);
////            System.exit(1);
//
//            JSONObject obj = (JSONObject) JSONValue.parseWithException(json);
//
//            //get regions
//            JSONArray regions = (JSONArray) ((JSONObject) obj.get("config")).get("regions");
//
//            for (int i = 0; i < regions.size(); i++) {
//                JSONObject region = (JSONObject) regions.get(i);
//                if (region.get("region").toString().equals("us-east")) {
//
//                    //types separate GeneralPurpose, and ComputeOptimized, etc
//                    JSONArray instanceTypes = (JSONArray) region.get("instanceTypes");
//                    for (int instanceIndex = 0; instanceIndex < instanceTypes.size(); instanceIndex++) {
//                        JSONObject instance = (JSONObject) instanceTypes.get(instanceIndex);
//
//                        //sizes separate m1.small, etc
//                        // sizes:[{valueColumns:[{name:"yrTerm1",prices:{USD:"110"}},{name:"yrTerm1Hourly",rate:"perhr",prices:{USD:"0.064"}},{name:"yrTerm3",prices:{USD:"172"}},{name:"yrTerm3Hourly",rate:"perhr",prices:{USD:"0.05"}}],size:"m3.medium"
//                        JSONArray sizes = (JSONArray) instance.get("sizes");
//                        for (int sizeIndex = 0; sizeIndex < sizes.size(); sizeIndex++) {
//
//                            JSONObject size = (JSONObject) sizes.get(sizeIndex);
//
//                            String sizeName = size.get("size").toString();
//
//                            if (units.containsKey(sizeName)) {
//
//                                CostFunction _1YearReservedCost = new CostFunction("1Year" + schemeName + "Cost_" + sizeName);
//                                CostFunction _3YearReservedCost = new CostFunction("3Year" + schemeName + "Cost_" + sizeName);
//
//                                List<ElasticityCapability.Dependency> costElasticityTargets = null;
//                                if (costDependencies.containsKey(sizeName)) {
//                                    costElasticityTargets = costDependencies.get(sizeName);
//                                } else {
//                                    costElasticityTargets = new ArrayList<>();
//                                    costDependencies.put(sizeName, costElasticityTargets);
//                                }
//
//                                costElasticityTargets.add(new ElasticityCapability.Dependency(_1YearReservedCost, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                                        .withVolatility(new Volatility(365 * 24, 1))); //365 days in 1 year, times 24 hours
//                                costElasticityTargets.add(new ElasticityCapability.Dependency(_3YearReservedCost, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                                        .withVolatility(new Volatility(3 * 365 * 24, 1))); //365 days in 1 year, times 24 hours
//
////                                ServiceUnit unit = units.get(sizeName);
//                                JSONArray prices = (JSONArray) size.get("valueColumns");
//                                for (int priceIndex = 0; priceIndex < prices.size(); priceIndex++) {
//                                    JSONObject price = (JSONObject) prices.get(priceIndex);
//                                    String priceName = price.get("name").toString();
//
////                                ServiceUnit _1YearReservationScheme = new ServiceUnit("Management", "ReservationScheme", "1YearLightUtilization");
////                                cloudProvider.addServiceUnit(_1YearReservationScheme);
////                                
////                                Resource r = new Resource("ReservationPeriod");
////                                r.addProperty(new Metric("Reservation", "duration"), new MetricValue("1 year"));
//                                    String priceValue = ((JSONObject) price.get("prices")).get("USD").toString();
//
//                                    switch (priceName) {
//                                        case "yrTerm1": {
//                                            CostElement upfrontCost = new CostElement("UpfrontCost", new Metric("OneTimePay", "value", Metric.MetricType.RESOURCE),
//                                                    CostElement.Type.PERIODIC);
//                                            upfrontCost.addCostInterval(new MetricValue(1), Double.parseDouble(priceValue));
//                                            _1YearReservedCost.addCostElement(upfrontCost);
//                                        }
//                                        break;
//                                        case "yrTerm1Hourly": {
//                                            CostElement hourlyCost = new CostElement("HourlyCost", new Metric("hourlyUsage", "$", Metric.MetricType.RESOURCE), CostElement.Type.PERIODIC);
//                                            hourlyCost.addCostInterval(new MetricValue(1), Double.parseDouble(priceValue));
//                                            _1YearReservedCost.addCostElement(hourlyCost);
//                                        }
//                                        break;
//
//                                        case "yrTerm3": {
//                                            CostElement upfrontCost = new CostElement("UpfrontCost", new Metric("OneTimePay", "value", Metric.MetricType.RESOURCE),
//                                                    CostElement.Type.PERIODIC);
//                                            upfrontCost.addCostInterval(new MetricValue(1), Double.parseDouble(priceValue));
//                                            _3YearReservedCost.addCostElement(upfrontCost);
//                                        }
//                                        break;
//                                        case "yrTerm3Hourly": {
//                                            CostElement hourlyCost = new CostElement("HourlyCost", new Metric("hourlyUsage", "$", Metric.MetricType.RESOURCE), CostElement.Type.PERIODIC);
//                                            hourlyCost.addCostInterval(new MetricValue(1), Double.parseDouble(priceValue));
//                                            _3YearReservedCost.addCostElement(hourlyCost);
//                                        }
//                                        break;
//
//                                    }
//                                }
//
//                            }
//
//                        }
//                    }
//
//                }
//
//            }
//
//        }
//    }
//
//    private void addOndemandCostOptions(String pricingSchemeURL, CloudProvider cloudProvider, Map<String, ServiceUnit> units, Map<String, List<ElasticityCapability.Dependency>> costDependencies) throws MalformedURLException, IOException, ParseException {
//        {
//
//            URL url = new URL(pricingSchemeURL);
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
//            String json = "";
//            String line = "";
//            while ((line = reader.readLine()) != null) {
//                json += line;
//            }
//
//            //remove newline
//            json = json.replace("\\n", "");
//            json = json.replace(" ", "");
//
//            //for some URLs, the JSON is not json, it does not contain {"name, instead is {name
//            if (!json.contains("{\"")) {
//                json = json.replace("{", "{\"");
//                json = json.replace(":", "\":");
//                json = json.replace(",", ",\"");
//
//                //fix overreplace from above
//                json = json.replace("\"{", "{");
//                json = json.replace("\"\"", "\"");
//            }
//
//            //remove "callback"
//            json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
////            System.out.println(json);
////            System.exit(1);
//
//            JSONObject obj = (JSONObject) JSONValue.parseWithException(json);
//
//            //get regions
//            JSONArray regions = (JSONArray) ((JSONObject) obj.get("config")).get("regions");
//
//            for (int i = 0; i < regions.size(); i++) {
//                JSONObject region = (JSONObject) regions.get(i);
//                if (region.get("region").toString().equals("us-east")) {
//
//                    //types separate GeneralPurpose, and ComputeOptimized, etc
//                    JSONArray instanceTypes = (JSONArray) region.get("instanceTypes");
//                    for (int instanceIndex = 0; instanceIndex < instanceTypes.size(); instanceIndex++) {
//                        JSONObject instance = (JSONObject) instanceTypes.get(instanceIndex);
//
//                        //sizes separate m1.small, etc
//                        // sizes:[{valueColumns:[{name:"yrTerm1",prices:{USD:"110"}},{name:"yrTerm1Hourly",rate:"perhr",prices:{USD:"0.064"}},{name:"yrTerm3",prices:{USD:"172"}},{name:"yrTerm3Hourly",rate:"perhr",prices:{USD:"0.05"}}],size:"m3.medium"
//                        JSONArray sizes = (JSONArray) instance.get("sizes");
//                        for (int sizeIndex = 0; sizeIndex < sizes.size(); sizeIndex++) {
//
//                            JSONObject size = (JSONObject) sizes.get(sizeIndex);
//
//                            String sizeName = size.get("size").toString();
//
//                            if (units.containsKey(sizeName)) {
//                                ServiceUnit unit = units.get(sizeName);
//
//                                JSONArray prices = (JSONArray) size.get("valueColumns");
//                                for (int priceIndex = 0; priceIndex < prices.size(); priceIndex++) {
//                                    JSONObject price = (JSONObject) prices.get(priceIndex);
//
////                                ServiceUnit _1YearReservationScheme = new ServiceUnit("Management", "ReservationScheme", "1YearLightUtilization");
////                                cloudProvider.addServiceUnit(_1YearReservationScheme);
//                                    //curently i need diff   CostFunctionNames
//                                    CostFunction onDemand = new CostFunction("OndemandCost_" + sizeName);
//
//                                    List<ElasticityCapability.Dependency> costElasticityTargets = null;
//                                    if (costDependencies.containsKey(sizeName)) {
//                                        costElasticityTargets = costDependencies.get(sizeName);
//                                    } else {
//                                        costElasticityTargets = new ArrayList<>();
//                                        costDependencies.put(sizeName, costElasticityTargets);
//                                    }
//
//                                    costElasticityTargets.add(new ElasticityCapability.Dependency(onDemand, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                                            .withVolatility(new Volatility(1, 1))); //365 days in 1 year, times 24 hours
//
//                                    String priceValue = ((JSONObject) price.get("prices")).get("USD").toString();
//
//                                    CostElement hourlyCost = new CostElement("HourlyCost", new Metric("hourlyUsage", "$", Metric.MetricType.RESOURCE),
//                                            CostElement.Type.PERIODIC);
//
//                                    hourlyCost.addCostInterval(new MetricValue(1), Double.parseDouble(priceValue));
//                                    onDemand.addCostElement(hourlyCost);
//
//                                }
//
//                            }
//
//                        }
//                    }
//
//                }
//
//            }
//
//        }
//    }
//
//    private void addSpotCostOptions(String pricingSchemeURL, CloudProvider cloudProvider, Map<String, ServiceUnit> units, Map<String, List<ElasticityCapability.Dependency>> costDependencies) throws MalformedURLException, IOException, ParseException {
//        {
//
//            URL url = new URL(pricingSchemeURL);
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
//            String json = "";
//            String line = "";
//            while ((line = reader.readLine()) != null) {
//                json += line;
//            }
//
//            //remove newline
//            json = json.replace("\\n", "");
//            json = json.replace(" ", "");
//
//            //remove "callback"
//            json = json.substring(json.indexOf("{"), json.lastIndexOf("}") + 1);
////            System.out.println(json);
////            System.exit(1);
//
//            JSONObject obj = (JSONObject) JSONValue.parse(json);
//
//            //get regions
//            JSONArray regions = (JSONArray) ((JSONObject) obj.get("config")).get("regions");
//
//            for (int i = 0; i < regions.size(); i++) {
//                JSONObject region = (JSONObject) regions.get(i);
//                if (region.get("region").toString().equals("us-east")) {
//
//                    //types separate GeneralPurpose, and ComputeOptimized, etc
//                    JSONArray instanceTypes = (JSONArray) region.get("instanceTypes");
//                    for (int instanceIndex = 0; instanceIndex < instanceTypes.size(); instanceIndex++) {
//                        JSONObject instance = (JSONObject) instanceTypes.get(instanceIndex);
//
//                        //sizes separate m1.small, etc
//                        // [{size:"m3.medium",vCPU:"1",ECU:"3",memoryGiB:"3.75",storageGB:"1 x 4 SSD",valueColumns:[{name:"linux",prices:{USD:"0.070"}}]}
//                        JSONArray sizes = (JSONArray) instance.get("sizes");
//                        for (int sizeIndex = 0; sizeIndex < sizes.size(); sizeIndex++) {
//
//                            JSONObject size = (JSONObject) sizes.get(sizeIndex);
//                            String sizeName = size.get("size").toString();
//                            if (units.containsKey(sizeName)) {
//                                ServiceUnit unit = units.get(sizeName);
//
//                                //go through different OS types
//                                JSONArray oss = (JSONArray) size.get("valueColumns");
//                                for (int osIndex = 0; osIndex < oss.size(); osIndex++) {
//                                    JSONObject os = (JSONObject) oss.get(osIndex);
//                                    if (os.get("name").toString().equals("linux")) {
//                                        JSONObject price = (JSONObject) os.get("prices");
//                                        String priceValue = price.get("USD").toString();
//
//                                        // add cost elasticity capability
//                                        {
////                                            ServiceUnit reservationScheme = new ServiceUnit("Management", "ReservationScheme", "Spot");
////                                            cloudProvider.addServiceUnit(reservationScheme);
////                                            Resource r = new Resource("ReservationPeriod");
////                                            r.addProperty(new Metric("Reservation", "duration"), new MetricValue("hour"));
////                                            reservationScheme.addResourceProperty(r);
//
//                                            // add cost per association with this reservationScheme
//                                            CostFunction spot = new CostFunction("SpotCost_" + sizeName);
//
//                                            List<ElasticityCapability.Dependency> costElasticityTargets = null;
//                                            if (costDependencies.containsKey(sizeName)) {
//                                                costElasticityTargets = costDependencies.get(sizeName);
//                                            } else {
//                                                costElasticityTargets = new ArrayList<>();
//                                                costDependencies.put(sizeName, costElasticityTargets);
//                                            }
//
//                                            costElasticityTargets.add(new ElasticityCapability.Dependency(spot, ElasticityCapability.Type.OPTIONAL_ASSOCIATION)
//                                                    .withVolatility(new Volatility(1, 1)));
//                                            {
//                                                // currently Cost is cost unit agnostic?
//                                                CostElement hourlyCost = new CostElement("HourlyCost", new Metric("hourlyUsage", "$", Metric.MetricType.RESOURCE), CostElement.Type.PERIODIC);
//                                                hourlyCost.addCostInterval(new MetricValue(1), Double.parseDouble(priceValue));
//                                                spot.addCostElement(hourlyCost);
//                                            }
////                                            spot.addUtilityAppliedInConjunctionWith(reservationScheme);
//
//                                        }
//                                    }
//                                }
//
//                            } else {
//                                System.err.println("Size " + sizeName + " not found");
//                            }
//                        }
//                    }
//
//                    break;
//
//                }
//
//            }
//
//        }
//
//    }
//
//    /**
//     * Test of matchServiceUnit method, of class RequirementsMatchingEngine.
//     */
//    private class ServiceUnitBuilder {
//
//        private ServiceUnit unit;
//        private List<Resource> cpuOptions = new ArrayList<>();
//        private boolean ebsOnly = false;
//
//        private List<String> propertyNames;
//
//        {
//            propertyNames = new ArrayList<>();
//            propertyNames.add("Instance Family");
//            propertyNames.add("Instance Type");
//            propertyNames.add("Processor Arch");
//            propertyNames.add("vCPU");
//            propertyNames.add("ECU");
//            propertyNames.add("Memory (GiB)");
//            propertyNames.add("Instance Storage (GB)");
//            propertyNames.add("EBS-optimized Available");
//            propertyNames.add("Network Performance");
//        }
//
//        //Instance Family   
//        //Instance Type
//        //Processor Arch
//        //vCPU
//        //ECU
//        //Memory (GiB)
//        //Instance Storage (GB)
//        //EBS-optimized Available
//        //Network Performance
//        public ServiceUnitBuilder(String category, String subcategory) {
//            unit = new ServiceUnit();
//            unit.setCategory(category);
//            unit.setSubcategory(subcategory);
//        }
//
//        public ServiceUnit getUnit() {
//            return unit;
//        }
//
//        public List<String> getPropertyNames() {
//            return propertyNames;
//        }
//
//        public void addProperty(String property, String value) {
//            switch (property) {
//                case "Instance Family":
//                    break;
//                case "Instance Type":
//                    unit.setName(value);
//                    break;
//                case "Processor Arch":
//                    String[] archs = value.split("or");
//
//                    //if more arch => el capabilit, else is just a resource
//                    if (archs.length > 1) {
//
//                        List<ElasticityCapability.Dependency> resourceCapabilityTargets = new ArrayList<>();
//
//                        for (String arch : archs) {
//                            Resource resource = new Resource("Computing");
//                            cpuOptions.add(resource);
//                            resource.addProperty(new Metric("Architecture", "type"), new MetricValue(arch.trim().split("-bit")[0].trim()));
//                            resourceCapabilityTargets.add(new ElasticityCapability.Dependency(resource, ElasticityCapability.Type.OPTIONAL_ASSOCIATION));
//                        }
//
//                        {
//                            ElasticityCapability characteristic = new ElasticityCapability("CPUArchitecture");
//                            characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//                            for (ElasticityCapability.Dependency d : resourceCapabilityTargets) {
//                                characteristic.addCapabilityDependency(d);
//                            }
//                            unit.addElasticityCapability(characteristic);
//                        }
//
//                    } else {
//                        Resource resource = new Resource("Computing");
//                        resource.addProperty(new Metric("Architecture", "type"), new MetricValue(archs[0].trim().split("-bit")[0].trim()));
//                        unit.addResourceProperty(resource);
//                        cpuOptions.add(resource);
//                    }
//                    break;
//                case "vCPU":
//                    Integer cpus = Integer.parseInt(value);
//                    for (Resource r : cpuOptions) {
//                        r.addProperty(new Metric("VCPU", "number"), new MetricValue(cpus));
//                    }
//                    break;
//                case "ECU":
//                    Double ecu = 0.0;
//                    try {
//                        ecu = Double.parseDouble(value);
//                    } catch (Exception e) {
//                        System.err.println("Value " + value + " can't be converted to double. Setting ECU = 0.0");
//                    }
//                    Quality computingQuality = new Quality("ComputingPerformance");
//                    computingQuality.addProperty(new Metric("ECU", "number"), new MetricValue(ecu));
//                    unit.addQualityProperty(computingQuality);
//                    break;
//                case "Memory (GiB)":
//                    Double memory = Double.parseDouble(value);
//                    Resource memoryResource = new Resource("Memory");
//                    memoryResource.addProperty(new Metric("Memory", "GB"), new MetricValue(memory));
//                    unit.addResourceProperty(memoryResource);
//                    break;
//                case "Instance Storage (GB)":
//                    //format is disc x size [SSD]
//                    String[] info = value.split("x");
//
//                    Resource diskResource = new Resource("InstanceStorage");
//
//                    switch (info[0].trim()) {
//
//                        case "EBS only":
//                            ebsOnly = true;
//                            diskResource.addProperty(new Metric("StorageDisks", "no."), new MetricValue(0));
//                            break;
//                        default: {
//                            diskResource.addProperty(new Metric("StorageDisks", "no."), new MetricValue(Integer.parseInt(info[0].trim())));
//
//                            if (info[1].contains("SSD")) {
//                                String diskSize = info[1].split("SSD")[0];
//                                diskResource.addProperty(new Metric("StorageSize", "GB"), new MetricValue(Integer.parseInt(diskSize.trim())));
//
//                                //also add quality storage SSD
//                                Quality storageTypeQuality = new Quality("StorageTypeQuality");
//                                storageTypeQuality.addProperty(new Metric("Type", "type"), new MetricValue("SSD"));
//                                unit.addQualityProperty(storageTypeQuality);
//
//                            } else {
//                                diskResource.addProperty(new Metric("StorageSize", "GB"), new MetricValue(Integer.parseInt(info[1].trim())));
//                            }
//                        }
//                        break;
//                    }
//
//                    unit.addResourceProperty(diskResource);
//
//                    break;
//                case "EBS-optimized Available":
//                    ServiceUnit ebsStorageUtility = new ServiceUnit("IaaS", "Storage", "EBS");
//                    switch (value) {
//                        case "Yes": {
//
//                            // utility optional association
//                            {
//                                // utility elasticity charact for resource" lets the utility
//                                // choose between diff resource values
//                                {
//                                    ElasticityCapability characteristic = new ElasticityCapability("Storage");
//                                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//                                    if (ebsOnly) {
//                                        characteristic.addCapabilityDependency(new ElasticityCapability.Dependency(ebsStorageUtility,
//                                                ElasticityCapability.Type.MANDATORY_ASSOCIATION));
//                                    } else {
//                                        characteristic.addCapabilityDependency(new ElasticityCapability.Dependency(ebsStorageUtility,
//                                                ElasticityCapability.Type.OPTIONAL_ASSOCIATION));
//                                    }
//                                    unit.addElasticityCapability(characteristic);
//                                }
//                            }
//
//                            // utility Storage quality: EBSOptimized
//                            {
//                                Quality q = new Quality("StoragePerformance");
//                                q.addProperty(new Metric("Network", "performance"), new MetricValue("EBSOptimized"));
//
//                                // utility elasticity charact for resource" lets the utility
//                                // choose between diff resource values
//                                {
//                                    ElasticityCapability characteristic = new ElasticityCapability("StorageQualityElasticity");
//                                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//                                    characteristic.addCapabilityDependency(new ElasticityCapability.Dependency(q, ElasticityCapability.Type.OPTIONAL_ASSOCIATION));
//                                    unit.addElasticityCapability(characteristic);
//                                }
//
//                                // add cost per association with this reservationScheme
//                                CostFunction onDemandCost = new CostFunction("EBSOptimizedCost");
//                                {
//                                    // currently Cost is cost unit agnostic?
//                                    CostElement hourlyCost = new CostElement("HourlyCost", new Metric("hourlyUsage", "$", Metric.MetricType.RESOURCE),
//                                            CostElement.Type.PERIODIC);
//                                    hourlyCost.addCostInterval(new MetricValue(1), 0.025);
//                                    onDemandCost.addCostElement(hourlyCost);
//                                    onDemandCost.addUtilityAppliedInConjunctionWith(q);
//                                }
//
//                                // utility elasticity charact for resource" lets the utility
//                                // choose between diff resource values
//                                {
//                                    ElasticityCapability characteristic = new ElasticityCapability("StorageQualityCostElasticity");
//                                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//                                    characteristic
//                                            .addCapabilityDependency(new ElasticityCapability.Dependency(onDemandCost, ElasticityCapability.Type.OPTIONAL_ASSOCIATION));
//                                    unit.addElasticityCapability(characteristic);
//                                }
//
//                            }
//                        }
//
//                        break;
//                        default:
//                            //solves issue of recognizing tiny which is EBSOnly but NOT EBSOptimized
//                            if (ebsOnly) {
//                                // utility elasticity charact for resource" lets the utility
//                                // choose between diff resource values
//                                {
//                                    ElasticityCapability characteristic = new ElasticityCapability("Storage");
//                                    characteristic.setPhase(ElasticityCapability.Phase.INSTANTIATION_TIME);
//                                    characteristic
//                                            .addCapabilityDependency(new ElasticityCapability.Dependency(ebsStorageUtility,
//                                                            ElasticityCapability.Type.MANDATORY_ASSOCIATION));
//
//                                    unit.addElasticityCapability(characteristic);
//                                }
//                            }
//                            break;
//                    }
//
//                    break;
//                case "Network Performance":
//                    Quality q = new Quality("NetworkPerformance");
//                    q.addProperty(new Metric("Network", "performance"), new MetricValue(value));
//                    unit.addQualityProperty(q);
//                    break;
//
//            }
//        }
//
//    }
//
//}
