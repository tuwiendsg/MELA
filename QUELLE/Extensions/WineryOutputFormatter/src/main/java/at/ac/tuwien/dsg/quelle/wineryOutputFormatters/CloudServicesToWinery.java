/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.wineryOutputFormatters;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Unit;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.dtos.CloudServiceConfigurationRecommendation;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.RequirementsMatchingEngine;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.ServiceUnitConfigurationSolution;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.util.ServiceUnitComparator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class CloudServicesToWinery {

    String templatesPath = "./OpenToscaTemplates";
//    String outputPath = "./OpenToscaOutput/nodeTypes";

    private List<String> createdSoFar = new ArrayList<>();

    public void createWineryNodesFromCloudServices(CloudProvider provider, String outputPath) {

        //create path just in case
        File file = new File(outputPath);
        file.mkdir();

        for (CloudOfferedService service : provider.getCloudOfferedServices()) {
//            switch(service.getCategory()){
//                
//            }
            switch (service.getSubcategory()) {
                case "VM": {
                    //for VM we have node type
                }
                break;
                case "Storage": {//create file for the specialization to have in the repository
//                    BufferedWriter writer = new BufferedWriter
                    createNewNodeType(service, "", "ebsIcon.png", outputPath);
                }
                break;

                case "Monitoring":
                    createNewNodeType(service, "", "monitoringIcon.png", outputPath);
                    break;
                case "CommunicationServices":
                    createNewNodeType(service, "", "queueIcon.png", outputPath);
                    break;

            }

            //for all dependencies
            for (ElasticityCapability capability : service.getElasticityCapabilities()) {
                for (ElasticityCapability.Dependency dependency : capability.getCapabilityDependencies()) {
                    createNewNodeType(dependency.getTarget(), outputPath);
                }
            }

            //for all cost functions
            for (CostFunction costFunction : service.getCostFunctions()) {
                createNewNodeType(costFunction, outputPath);
            }
        }
    }

    private void createNewNodeType(CloudOfferedService service, String superClassName, String sourceIconName, String outputPath) {

        String storageName = service.getName().split("_")[0].replace(" ", "").replace("/", "").replace(" ", "");;

        if (createdSoFar.contains(storageName)) {
            return;
        } else {
            createdSoFar.add(storageName);
        }

        File file = new File(outputPath + "/" + storageName);
        file.mkdir();

        //create appearance dir
        {
            File images = new File(outputPath + "/" + storageName + "/appearance");

            images.mkdir();
            try {
                //copy storage images 
                Files.copy(Paths.get(templatesPath + "/icons/" + sourceIconName), Paths.get(images.getPath() + "/bigIcon.png"), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(Paths.get(templatesPath + "/icons/" + sourceIconName), Paths.get(images.getPath() + "/smallIcon.png"), StandardCopyOption.REPLACE_EXISTING);

                Files.copy(Paths.get(templatesPath + "/icons/bigIcon.png.mimetype"), Paths.get(images.getPath() + "/bigIcon.png.mimetype"), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(Paths.get(templatesPath + "/icons/smallIcon.png.mimetype"), Paths.get(images.getPath() + "/smallIcon.png.mimetype"), StandardCopyOption.REPLACE_EXISTING);

            } catch (IOException ex) {
                Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //create Tosca node template
        File toscaTemplate = new File(templatesPath + "/toscaTemplates/NodeType.tosca");
        File toscaTemplateMimetype = new File(templatesPath + "/toscaTemplates/NodeType.tosca.mimetype");
        try {
            Files.copy(Paths.get(toscaTemplateMimetype.getPath()), Paths.get(outputPath + "/" + storageName + "/NodeType.tosca.mimetype"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }

        File newToscaNodeFile = new File(outputPath + "/" + storageName + "/NodeType.tosca");
        try {
            //read source template line by line and create new tosca node as we go
            BufferedReader toscaTemplateReader = new BufferedReader(new FileReader(toscaTemplate));
            BufferedWriter toscaTemplateWriter = new BufferedWriter(new FileWriter(newToscaNodeFile));

            String line = "";
            while ((line = toscaTemplateReader.readLine()) != null) {

                //insert in template placeholders the correct values
                if (line.contains("TemplateNamePlaceholder")) {
                    line = line.replace("TemplateNamePlaceholder", storageName);
                } else if (line.contains("TemplatePropertiesPlaceholder")) {
                    String propertiesLine = "";
                    //put all static properties
                    for (Resource resource : service.getResourceProperties()) {
                        for (Map.Entry<Metric, MetricValue> entry : resource.getProperties().entrySet()) {
                            /**
                             * for each property I need
                             * <winery:properties>
                             * <winery:key>NetworkPerformance</winery:key>
                             * <winery:type>xsd:string</winery:type>
                             * </winery:properties>
                             */
                            String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");;
                            propertiesLine += "<winery:properties>";
                            propertiesLine += "<winery:key>" + entryName + "</winery:key>";

                            //currently all String, and then we can change this
                            propertiesLine += "<winery:type>xsd:string</winery:type>";
                            propertiesLine += "</winery:properties>";
                        }
                    }

                    for (Quality quality : service.getQualityProperties()) {
                        for (Map.Entry<Metric, MetricValue> entry : quality.getProperties().entrySet()) {
                            /**
                             * for each property I need
                             * <winery:properties>
                             * <winery:key>NetworkPerformance</winery:key>
                             * <winery:type>xsd:string</winery:type>
                             * </winery:properties>
                             */
                            String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");;
                            propertiesLine += "<winery:properties>";
                            propertiesLine += "<winery:key>" + entryName + "</winery:key>";

                            //currently all String, and then we can change this
                            propertiesLine += "<winery:type>xsd:string</winery:type>";
                            propertiesLine += "</winery:properties>";
                        }
                    }

                    for (CostFunction costFunction : service.getCostFunctions()) {

                        //if cost no mather what
                        if (costFunction.getAppliedIfServiceInstanceUses().isEmpty()) {

                            for (CostElement element : costFunction.getCostElements()) {
                                String entryName = element.getCostMetric().getName().replace("/", "").replace(" ", "");;
                                /**
                                 * for each property I need
                                 * <winery:properties>
                                 * <winery:key>NetworkPerformance</winery:key>
                                 * <winery:type>xsd:string</winery:type>
                                 * </winery:properties>
                                 */
                                propertiesLine += "<winery:properties>";
                                propertiesLine += "<winery:key>" + entryName + "</winery:key>";

                                //currently all String, and then we can change this
                                propertiesLine += "<winery:type>xsd:string</winery:type>";
                                propertiesLine += "</winery:properties>";

                            }
                        }
                    }

                    line = propertiesLine;
                } else if (line.contains("TemplateDerivedFromPlaceholder")) {
                    line = line.replace("TemplateDerivedFromPlaceholder", superClassName);
                }

                //write line
                toscaTemplateWriter.write(line);
                toscaTemplateWriter.newLine();
            }
            toscaTemplateReader.close();
            toscaTemplateWriter.flush();
            toscaTemplateWriter.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void createNewNodeType(Unit entity, String outputPath) {

        String storageName = entity.getName().split("_")[0].replace(" ", "").replace("/", "").replace(" ", "");;;
        String sourceIconName = "";
        String superClassName = "";

        if (entity instanceof Resource) {
            sourceIconName = "resourceIcon.png";
            superClassName = "Resource";
        } else if (entity instanceof Quality) {
            sourceIconName = "qualityIcon.png";
            superClassName = "Quality";
        } else if (entity instanceof CostFunction) {
            sourceIconName = "costIcon.png";
            superClassName = "Cost";
            //todo: 
        } else if (entity instanceof CloudOfferedService) {
            CloudOfferedService service = (CloudOfferedService) entity;
            switch (service.getSubcategory()) {
                case "VM": {

                }
                break;
                case "Storage": {//create file for the specialization to have in the repository
//                    BufferedWriter writer = new BufferedWriter
                    createNewNodeType(service, "", "ebsIcon.png", outputPath);
                }
                break;

                case "Monitoring":
                    createNewNodeType(service, "", "monitoringIcon.png", outputPath);
                    break;
                case "CommunicationServices":
                    createNewNodeType(service, "", "queueIcon.png", outputPath);
                    break;

            }
        } else {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.WARNING, "Entity type of " + entity + " not recognized");
        }

        if (createdSoFar.contains(storageName)) {
            return;
        } else {
            createdSoFar.add(storageName);
        }

        File file = new File(outputPath + "/" + storageName);
        file.mkdir();

        //create appearance dir
        {
            File images = new File(outputPath + "/" + storageName + "/appearance");

            images.mkdir();
            try {
                //copy storage images 
                Files.copy(Paths.get(templatesPath + "/icons/" + sourceIconName), Paths.get(images.getPath() + "/bigIcon.png"), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(Paths.get(templatesPath + "/icons/" + sourceIconName), Paths.get(images.getPath() + "/smallIcon.png"), StandardCopyOption.REPLACE_EXISTING);

                Files.copy(Paths.get(templatesPath + "/icons/bigIcon.png.mimetype"), Paths.get(images.getPath() + "/bigIcon.png.mimetype"), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(Paths.get(templatesPath + "/icons/smallIcon.png.mimetype"), Paths.get(images.getPath() + "/smallIcon.png.mimetype"), StandardCopyOption.REPLACE_EXISTING);

            } catch (IOException ex) {
                Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        //create Tosca node template
        File toscaTemplate = new File(templatesPath + "/toscaTemplates/NodeType.tosca");
        File toscaTemplateMimetype = new File(templatesPath + "/toscaTemplates/NodeType.tosca.mimetype");

        try {
            Files.copy(Paths.get(toscaTemplateMimetype.getPath()), Paths.get(outputPath + "/" + storageName + "/NodeType.tosca.mimetype"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }

        File newToscaNodeFile = new File(outputPath + "/" + storageName + "/NodeType.tosca");
        try {
            //read source template line by line and create new tosca node as we go
            BufferedReader toscaTemplateReader = new BufferedReader(new FileReader(toscaTemplate));
            BufferedWriter toscaTemplateWriter = new BufferedWriter(new FileWriter(newToscaNodeFile));

            String line = "";
            while ((line = toscaTemplateReader.readLine()) != null) {

                //insert in template placeholders the correct values
                if (line.contains("TemplateNamePlaceholder")) {
                    line = line.replace("TemplateNamePlaceholder", storageName);
                } else if (line.contains("TemplatePropertiesPlaceholder")) {
                    String propertiesLine = "";
                    //put all static properties

                    if (entity instanceof Resource) {

                        for (Map.Entry<Metric, MetricValue> entry : ((Resource) entity).getProperties().entrySet()) {
                            /**
                             * for each property I need
                             * <winery:properties>
                             * <winery:key>NetworkPerformance</winery:key>
                             * <winery:type>xsd:string</winery:type>
                             * </winery:properties>
                             */
                            String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");;
                            propertiesLine += "<winery:properties> \n";
                            propertiesLine += "<winery:key>" + entryName + "</winery:key> \n";

                            //currently all String, and then we can change this
                            propertiesLine += "<winery:type>xsd:string</winery:type> \n";
                            propertiesLine += "</winery:properties> \n";
                        }
                    } else if (entity instanceof Quality) {

                        for (Map.Entry<Metric, MetricValue> entry : ((Quality) entity).getProperties().entrySet()) {
                            /**
                             * for each property I need
                             * <winery:properties>
                             * <winery:key>NetworkPerformance</winery:key>
                             * <winery:type>xsd:string</winery:type>
                             * </winery:properties>
                             */
                            String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");;
                            propertiesLine += "<winery:properties> \n";
                            propertiesLine += "<winery:key>" + entryName + "</winery:key> \n";

                            //currently all String, and then we can change this
                            propertiesLine += "<winery:type>xsd:string</winery:type> \n";
                            propertiesLine += "</winery:properties> \n";
                        }
                    } else if (entity instanceof CostFunction) {

                        for (CostElement element : ((CostFunction) entity).getCostElements()) {

                            /**
                             * for each property I need
                             * <winery:properties>
                             * <winery:key>NetworkPerformance</winery:key>
                             * <winery:type>xsd:string</winery:type>
                             * </winery:properties>
                             */
                            String entryName = element.getCostMetric().getName().replace("/", "").replace(" ", "");;
                            propertiesLine += "<winery:properties> \n";
                            propertiesLine += "<winery:key>" + entryName + "</winery:key> \n";

                            //currently all String, and then we can change this
                            propertiesLine += "<winery:type>xsd:string</winery:type> \n";
                            propertiesLine += "</winery:properties> \n";

                        }
                    }

                    line = propertiesLine;
                } else if (line.contains("TemplateDerivedFromPlaceholder")) {
                    line = line.replace("TemplateDerivedFromPlaceholder", superClassName);
                }

                //write line
                toscaTemplateWriter.write(line);
                toscaTemplateWriter.newLine();
            }
            toscaTemplateReader.close();
            toscaTemplateWriter.flush();
            toscaTemplateWriter.close();

        } catch (FileNotFoundException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates Winery service template from SES Configuration Recommendation
     * @param configurationsList
     * @param serviceTemplateName
     * @param outputPath 
     */
    public void createToscaServiceTemplate(List<List<ServiceUnitConfigurationSolution>> configurationsList, String serviceTemplateName, String outputPath) {
        //create path just in case
        File file = new File(outputPath);
        file.mkdir();

        File concreteOutputDir = new File(outputPath + "/" + serviceTemplateName);
        concreteOutputDir.mkdir();

        File toscaTemplate = new File(templatesPath + "/serviceTemplates/ServiceTemplate.tosca");
        File toscaTemplateMimetype = new File(templatesPath + "/serviceTemplates/ServiceTemplate.tosca.mimetype");
        try {
            Files.copy(Paths.get(toscaTemplateMimetype.getPath()), Paths.get(outputPath + "/" + serviceTemplateName + "/ServiceTemplate.tosca.mimetype"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }

        File newToscaNodeFile = new File(outputPath + "/" + serviceTemplateName + "/ServiceTemplate.tosca");

        try {
            //read source template line by line and create new tosca node as we go
            BufferedReader toscaTemplateReader = new BufferedReader(new FileReader(toscaTemplate));
            BufferedWriter toscaTemplateWriter = new BufferedWriter(new FileWriter(newToscaNodeFile));

            String line = "";
            while ((line = toscaTemplateReader.readLine()) != null) {

                //insert in template placeholders the correct values
                if (line.contains("TemplateNodesPlaceholder")) {
                    line = toToscaTopologyTemplate(configurationsList);
                }

                //write line
                toscaTemplateWriter.write(line);
                toscaTemplateWriter.newLine();
            }
            toscaTemplateReader.close();
            toscaTemplateWriter.flush();
            toscaTemplateWriter.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param rootRequirements - contains the requirements hierarchy, and is
     * used to first generate Tosca for Units, then link the topologies, then
     * whole service
     * @param solutions
     * @param serviceTemplateName
     * @param outputPath
     */
    public void createToscaServiceTemplate(MultiLevelRequirements rootRequirements, Map<MultiLevelRequirements, Map<Requirements, List<ServiceUnitConfigurationSolution>>> solutions, String serviceTemplateName, String outputPath) {

        //create path just in case
        File file = new File(outputPath);
        file.mkdir();

        File concreteOutputDir = new File(outputPath + "/" + serviceTemplateName);
        concreteOutputDir.mkdir();

        File toscaTemplate = new File(templatesPath + "/serviceTemplates/ServiceTemplate.tosca");
        File toscaTemplateMimetype = new File(templatesPath + "/serviceTemplates/ServiceTemplate.tosca.mimetype");
        try {
            Files.copy(Paths.get(toscaTemplateMimetype.getPath()), Paths.get(outputPath + "/" + serviceTemplateName + "/ServiceTemplate.tosca.mimetype"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }

        File newToscaNodeFile = new File(outputPath + "/" + serviceTemplateName + "/ServiceTemplate.tosca");

        try {
            //read source template line by line and create new tosca node as we go
            BufferedReader toscaTemplateReader = new BufferedReader(new FileReader(toscaTemplate));
            BufferedWriter toscaTemplateWriter = new BufferedWriter(new FileWriter(newToscaNodeFile));

            String line = "";
            while ((line = toscaTemplateReader.readLine()) != null) {

                //insert in template placeholders the correct values
                if (line.contains("TemplateNodesPlaceholder")) {
                    line = toToscaTopologyTemplate(rootRequirements, solutions);
                }

                //write line
                toscaTemplateWriter.write(line);
                toscaTemplateWriter.newLine();
            }
            toscaTemplateReader.close();
            toscaTemplateWriter.flush();
            toscaTemplateWriter.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void createToscaServiceTemplate(CloudServiceConfigurationRecommendation recommendation, String serviceTemplateName, String outputPath) {

        //create path just in case
        File file = new File(outputPath);
        file.mkdir();

        File concreteOutputDir = new File(outputPath + "/" + serviceTemplateName);
        concreteOutputDir.mkdir();

        File toscaTemplate = new File(templatesPath + "/serviceTemplates/ServiceTemplate.tosca");
        File toscaTemplateMimetype = new File(templatesPath + "/serviceTemplates/ServiceTemplate.tosca.mimetype");
        try {
            Files.copy(Paths.get(toscaTemplateMimetype.getPath()), Paths.get(outputPath + "/" + serviceTemplateName + "/ServiceTemplate.tosca.mimetype"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }

        File newToscaNodeFile = new File(outputPath + "/" + serviceTemplateName + "/ServiceTemplate.tosca");

        try {
            //read source template line by line and create new tosca node as we go
            BufferedReader toscaTemplateReader = new BufferedReader(new FileReader(toscaTemplate));
            BufferedWriter toscaTemplateWriter = new BufferedWriter(new FileWriter(newToscaNodeFile));

            String line = "";
            while ((line = toscaTemplateReader.readLine()) != null) {

                //insert in template placeholders the correct values
                if (line.contains("TemplateNodesPlaceholder")) {
                    line = toToscaTopologyTemplate(recommendation);
                }

                //write line
                toscaTemplateWriter.write(line);
                toscaTemplateWriter.newLine();
            }
            toscaTemplateReader.close();
            toscaTemplateWriter.flush();
            toscaTemplateWriter.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    
    /**
     *
     * @param configurations contain all services for a particular System Unit
     * @return
     */
    private String toToscaTopologyTemplate(List<List<ServiceUnitConfigurationSolution>> configurationsList) {
        String tosca = "";
        List<CloudOfferedService> unitsIds = new ArrayList<>();

        for (List<ServiceUnitConfigurationSolution> configurations : configurationsList) {

            String prevElementID = null;
            for (ServiceUnitConfigurationSolution configurationSolution : configurations) {
                tosca += toToscaTopologyTemplate(configurationSolution);
                unitsIds.add(configurationSolution.getServiceUnit());
                if (prevElementID == null) {
                    prevElementID = "" + configurationSolution.getServiceUnit().getId();
                } else {
                    String target = "" + configurationSolution.getServiceUnit().getId();
                    tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\" " + prevElementID + "_" + target + "\" id=\"" + prevElementID + "_" + target + "\" type=\"exrt:connectsTo\">\n"
                            + "                <tosca:SourceElement ref=\"" + prevElementID + "\"/>\n"
                            + "                <tosca:TargetElement ref=\"" + configurationSolution.getServiceUnit().getId() + "\"/>\n"
                            + "            </tosca:RelationshipTemplate>";
                    prevElementID = "" + configurationSolution.getServiceUnit().getId();
                }
            }
        }

        Collections.sort(unitsIds, new ServiceUnitComparator());

        for (int i = 1; i < unitsIds.size(); i++) {
            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" "
                    + "name=\"" + unitsIds.get(i - 1).getId() + "_" + unitsIds.get(i).getId()
                    + "\" id=\"" + unitsIds.get(i - 1).getId() + "_" + unitsIds.get(i).getId() + "\" type=\"exrt:connectsTo\">\n"
                    + "                <tosca:SourceElement ref=\"" + unitsIds.get(i - 1).getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + unitsIds.get(i).getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        return tosca;
    }

    private String toToscaTopologyTemplate(MultiLevelRequirements rootRequirements, Map<MultiLevelRequirements, Map<Requirements, List<ServiceUnitConfigurationSolution>>> solutions) {
        String tosca = "";

        //means we have unit requirements
        if (rootRequirements.getContainedElements().isEmpty()) {
            List<CloudOfferedService> unitsIds = new ArrayList<>();

            for (Requirements requirements : solutions.get(rootRequirements).keySet()) {

                String prevElementID = null;
                for (ServiceUnitConfigurationSolution configurationSolution : solutions.get(rootRequirements).get(requirements)) {
                    tosca += toToscaTopologyTemplate(configurationSolution);
                    unitsIds.add(configurationSolution.getServiceUnit());
                    if (prevElementID == null) {
                        prevElementID = "" + configurationSolution.getServiceUnit().getId();
                    } else {
                        String target = "" + configurationSolution.getServiceUnit().getId();
                        tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\" "
                                + prevElementID + "_" + target
                                + "\" id=\"" + prevElementID + "_" + target + "\" type=\"exrt:connectsTo\">\n"
                                + "                <tosca:SourceElement ref=\"" + prevElementID + "\"/>\n"
                                + "                <tosca:TargetElement ref=\"" + configurationSolution.getServiceUnit().getId() + "\"/>\n"
                                + "            </tosca:RelationshipTemplate>";
                        prevElementID = "" + configurationSolution.getServiceUnit().getId();
                    }
                }
            }
            Collections.sort(unitsIds, new ServiceUnitComparator());

            for (int i = 1; i < unitsIds.size(); i++) {
                tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\"" + unitsIds.get(i - 1).getId() + "_" + unitsIds.get(i).getId()
                        + "\" id=\"" + unitsIds.get(i - 1).getId() + "_" + unitsIds.get(i).getId() + "\" type=\"exrt:connectsTo\">\n"
                        + "                <tosca:SourceElement ref=\"" + unitsIds.get(i - 1).getId() + "\"/>\n"
                        + "                <tosca:TargetElement ref=\"" + unitsIds.get(i).getId() + "\"/>\n"
                        + "            </tosca:RelationshipTemplate>";
            }
        } else {
            List<CloudOfferedService> unitsIds = new ArrayList<>();

            for (MultiLevelRequirements levelRequirements : rootRequirements.getContainedElements()) {
                tosca += toToscaTopologyTemplate(levelRequirements, solutions);
                for (List<ServiceUnitConfigurationSolution> sols : solutions.get(rootRequirements).values()) {
                    for (ServiceUnitConfigurationSolution solution : sols) {
                        if (solution.getServiceUnit().getCategory().equals(CloudOfferedService.Category.IaaS)) {
                            unitsIds.add(solution.getServiceUnit());
                        }
                    }
                }
            }

            //connect all IaaS connected elements
            Collections.sort(unitsIds, new ServiceUnitComparator());

            for (int i = 1; i < unitsIds.size(); i++) {
                tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\"" + unitsIds.get(i - 1).getId() + "_" + unitsIds.get(i).getId()
                        + "\" id=\"" + unitsIds.get(i - 1).getId() + "_" + unitsIds.get(i).getId() + "\" type=\"exrt:connectsTo\">\n"
                        + "                <tosca:SourceElement ref=\"" + unitsIds.get(i - 1).getId() + "\"/>\n"
                        + "                <tosca:TargetElement ref=\"" + unitsIds.get(i).getId() + "\"/>\n"
                        + "            </tosca:RelationshipTemplate>";
            }
        }

        return tosca;
    }

    private String toToscaTopologyTemplate(ServiceUnitConfigurationSolution configurationSolution) {
        String tosca = "";

        //add node template for service
        tosca += createNodeTemplate(configurationSolution.getServiceUnit());

        for (RequirementsMatchingEngine.RequirementsMatchingReport<Quality> report : configurationSolution.getChosenQualityOptions()) {
            tosca += createNodeTemplate(report.getConcreteConfiguration());
            //create RelationshipTemplate from Node to this quality

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\""
                    + configurationSolution.getServiceUnit().getId() + "_" + report.getConcreteConfiguration().getId()
                    + "\" id=\"" + configurationSolution.getServiceUnit().getId() + "_" + report.getConcreteConfiguration().getId() + "\" type=\"exrt:recommendedQuality\">\n"
                    + "                <tosca:SourceElement ref=\"" + configurationSolution.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + report.getConcreteConfiguration().getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (RequirementsMatchingEngine.RequirementsMatchingReport<Resource> report : configurationSolution.getChosenResourceOptions()) {
            tosca += createNodeTemplate(report.getConcreteConfiguration());
            //create RelationshipTemplate from Node to this quality

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\"" + configurationSolution.getServiceUnit().getId() + "_" + report.getConcreteConfiguration().getId()
                    + "\" id=\"" + configurationSolution.getServiceUnit().getId() + "_" + report.getConcreteConfiguration().getId() + "\" type=\"exrt:recommendedResource\">\n"
                    + "                <tosca:SourceElement ref=\"" + configurationSolution.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + report.getConcreteConfiguration().getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (CostFunction function : configurationSolution.getCostFunctions()) {
            tosca += createNodeTemplate(function);
            //create RelationshipTemplate from Node to this quality

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\"" + configurationSolution.getServiceUnit().getId() + "_" + function.getId() + "\" id=\""
                    + configurationSolution.getServiceUnit().getId() + "_" + function.getId() + "\" type=\"exrt:recommendedCost\">\n"
                    + "                <tosca:SourceElement ref=\"" + configurationSolution.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + function.getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (ServiceUnitConfigurationSolution mandatoryCfg : configurationSolution.getMandatoryAssociatedServiceUnits()) {
            tosca += toToscaTopologyTemplate(mandatoryCfg);

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\""
                    + configurationSolution.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId() + "\" id=\""
                    + configurationSolution.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId()
                    + "\" type=\"exrt:connectsTo\">\n"
                    + "                <tosca:SourceElement ref=\"" + configurationSolution.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + mandatoryCfg.getServiceUnit().getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (ServiceUnitConfigurationSolution mandatoryCfg : configurationSolution.getOptionallyAssociatedServiceUnits()) {
            tosca += toToscaTopologyTemplate(mandatoryCfg);

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\""
                    + configurationSolution.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId() + "\" id=\""
                    + configurationSolution.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId()
                    + "\" type=\"exrt:connectsTo\">\n"
                    + "                <tosca:SourceElement ref=\"" + configurationSolution.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + mandatoryCfg.getServiceUnit().getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        return tosca;
    }

private String toToscaTopologyTemplate(CloudServiceConfigurationRecommendation recommendation) {
        String tosca = "";

        //add node template for service
        tosca += createNodeTemplate(recommendation.getServiceUnit());

        for (Quality quality : recommendation.getChosenQualityOptions()) {
            tosca += createNodeTemplate(quality);
            //create RelationshipTemplate from Node to this quality

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\""
                    + recommendation.getServiceUnit().getId() + "_" + quality.getId()
                    + "\" id=\"" + recommendation.getServiceUnit().getId() + "_" + quality.getId() + "\" type=\"exrt:recommendedQuality\">\n"
                    + "                <tosca:SourceElement ref=\"" + recommendation.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + quality.getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (Resource resource : recommendation.getChosenResourceOptions()) {
            tosca += createNodeTemplate(resource);
            //create RelationshipTemplate from Node to this quality

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\"" + resource.getId() + "_" + resource.getId()
                    + "\" id=\"" + recommendation.getServiceUnit().getId() + "_" + resource.getId() + "\" type=\"exrt:recommendedResource\">\n"
                    + "                <tosca:SourceElement ref=\"" + recommendation.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + resource.getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (CostFunction function : recommendation.getCostFunctions()) {
            tosca += createNodeTemplate(function);
            //create RelationshipTemplate from Node to this quality

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\"" + recommendation.getServiceUnit().getId() + "_" + function.getId() + "\" id=\""
                    + recommendation.getServiceUnit().getId() + "_" + function.getId() + "\" type=\"exrt:recommendedCost\">\n"
                    + "                <tosca:SourceElement ref=\"" + recommendation.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + function.getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (ServiceUnitConfigurationSolution mandatoryCfg : recommendation.getMandatoryAssociatedServiceUnits()) {
            tosca += toToscaTopologyTemplate(mandatoryCfg);

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\""
                    + recommendation.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId() + "\" id=\""
                    + recommendation.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId()
                    + "\" type=\"exrt:connectsTo\">\n"
                    + "                <tosca:SourceElement ref=\"" + recommendation.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + mandatoryCfg.getServiceUnit().getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        for (ServiceUnitConfigurationSolution mandatoryCfg : recommendation.getOptionallyAssociatedServiceUnits()) {
            tosca += toToscaTopologyTemplate(mandatoryCfg);

            tosca += "<tosca:RelationshipTemplate xmlns:exrt=\"http://example.com/RelationshipTypes\" name=\""
                    + recommendation.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId() + "\" id=\""
                    + recommendation.getServiceUnit().getId() + "_" + mandatoryCfg.getServiceUnit().getId()
                    + "\" type=\"exrt:connectsTo\">\n"
                    + "                <tosca:SourceElement ref=\"" + recommendation.getServiceUnit().getId() + "\"/>\n"
                    + "                <tosca:TargetElement ref=\"" + mandatoryCfg.getServiceUnit().getId() + "\"/>\n"
                    + "            </tosca:RelationshipTemplate>";
        }

        return tosca;
    }

    
    private String createNodeTemplate(Unit entity) {

        String nodeTypeName = entity.getName().split("_")[0].replace("/", "").replace(" ", "");

        String nodeTemplateDescription = "";

        if (entity instanceof Resource) {

            nodeTemplateDescription = "<tosca:NodeTemplate xmlns:demo=\"http://www.dsg.tuwien.ac.at/SES\" "
                    + "name=\"" + entity.getName() + "\" id=\"" + entity.getId() + "\" type=\"demo:" + nodeTypeName + "\" winery:x=\"572\" winery:y=\"644\">\n"
                    + "                <tosca:Properties>\n"
                    + "                 <wpd:Properties xmlns:wpd=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns:ty=\"http://example.com/NodeTypes\">\n";

            for (Map.Entry<Metric, MetricValue> entry : ((Resource) entity).getProperties().entrySet()) {
                String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");
                nodeTemplateDescription += "<" + entryName + ">"
                        + entry.getValue() + "(" + entry.getKey().getMeasurementUnit() + ")" + "</" + entryName + "> \n";

            }
        } else if (entity instanceof Quality) {

            nodeTemplateDescription = "<tosca:NodeTemplate xmlns:demo=\"http://www.dsg.tuwien.ac.at/SES\" "
                    + "name=\"" + entity.getName() + "\" id=\"" + entity.getId() + "\" type=\"demo:" + nodeTypeName + "\" winery:x=\"572\" winery:y=\"644\">\n"
                    + "                <tosca:Properties>\n"
                    + "                 <wpd:Properties xmlns:wpd=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns:ty=\"http://example.com/NodeTypes\">\n";

            for (Map.Entry<Metric, MetricValue> entry : ((Quality) entity).getProperties().entrySet()) {
                String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");;
                nodeTemplateDescription += "<" + entryName + ">"
                        + entry.getValue() + "(" + entry.getKey().getMeasurementUnit() + ")" + "</" + entryName + "> \n";
            }
        } else if (entity instanceof CostFunction) {
            CostFunction costFunction = (CostFunction) entity;
            //if cost has target, it needs to be set as separate option, otherwise it is stored as property
//            if (!costFunction.getAppliedInConjunctionWith().isEmpty()) {

            nodeTemplateDescription = "<tosca:NodeTemplate xmlns:demo=\"http://www.dsg.tuwien.ac.at/SES\" "
                    + "name=\"" + entity.getName() + "\" id=\"" + entity.getId() + "\" type=\"demo:" + nodeTypeName + "\" winery:x=\"572\" winery:y=\"644\">\n"
                    + "                <tosca:Properties>\n"
                    + "                 <wpd:Properties xmlns:wpd=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns:ty=\"http://example.com/NodeTypes\">\n";

            for (CostElement element : ((CostFunction) entity).getCostElements()) {
                String entryName = element.getCostMetric().getName().replace("/", "").replace(" ", "");;
                String costInterval = "";
                for (Map.Entry<MetricValue, Double> entry : element.getCostIntervalFunction().entrySet()) {
                    costInterval += "[" + entry.getKey().getValueRepresentation() + "," + entry.getValue().toString() + "]";
                }

                nodeTemplateDescription += "<" + entryName + ">"
                        + costInterval + "(" + element.getCostMetric().getMeasurementUnit() + ")" + "</" + entryName + "> \n";

//                }
//            } else {
//                return "";
            }
        } else if (entity instanceof CloudOfferedService) {
            CloudOfferedService service = (CloudOfferedService) entity;
            //if service unit, I need to set all values. Then, I need also to create for its dependencies the coresponding node templates

            switch (service.getSubcategory()) {
                case "VM": {
                    nodeTemplateDescription = "<tosca:NodeTemplate xmlns:demo=\"http://www.dsg.tuwien.ac.at/SES\" "
                            + "name=\"" + entity.getName() + "\" id=\"" + entity.getId() + "\" type=\"demo:VirtualMachine\" winery:x=\"572\" winery:y=\"644\">\n"
                            + "                <tosca:Properties>\n"
                            + "                 <wpd:Properties xmlns:wpd=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns:ty=\"http://example.com/NodeTypes\">\n";

                }
                break;
                case "Storage": {//create file for the specialization to have in the repository
                    nodeTemplateDescription = "<tosca:NodeTemplate xmlns:demo=\"http://www.dsg.tuwien.ac.at/SES\" "
                            + "name=\"" + entity.getName() + "\" id=\"" + entity.getId() + "\" type=\"demo:Storage\" winery:x=\"572\" winery:y=\"644\">\n"
                            + "                <tosca:Properties>\n"
                            + "                 <wpd:Properties xmlns:wpd=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns:ty=\"http://example.com/NodeTypes\">\n";
                }
                break;

                case "Monitoring":
                    nodeTemplateDescription = "<tosca:NodeTemplate xmlns:demo=\"http://www.dsg.tuwien.ac.at/SES\" "
                            + "name=\"" + entity.getName() + "\" id=\"" + entity.getId() + "\" type=\"demo:Monitoring\" winery:x=\"572\" winery:y=\"644\">\n"
                            + "                <tosca:Properties>\n"
                            + "                 <wpd:Properties xmlns:wpd=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns:ty=\"http://example.com/NodeTypes\">\n";
                    break;
                case "CommunicationServices":
                    nodeTemplateDescription = "<tosca:NodeTemplate xmlns:demo=\"http://www.dsg.tuwien.ac.at/SES\" "
                            + "name=\"" + entity.getName() + "\" id=\"" + entity.getId() + "\" type=\"demo:MessageQueue\" winery:x=\"572\" winery:y=\"644\">\n"
                            + "                <tosca:Properties>\n"
                            + "                 <wpd:Properties xmlns:wpd=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns=\"http://example.com/NodeTypes/propertiesdefinition/winery\" xmlns:ty=\"http://example.com/NodeTypes\">\n";
                    break;
            }

            for (Resource resource : service.getResourceProperties()) {
                for (Map.Entry<Metric, MetricValue> entry : resource.getProperties().entrySet()) {
                    String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");;
                    nodeTemplateDescription += "<" + entryName + ">"
                            + entry.getValue() + "(" + entry.getKey().getMeasurementUnit() + ")" + "</" + entryName + "> \n";

                }
            }

            for (Quality quality : service.getQualityProperties()) {
                for (Map.Entry<Metric, MetricValue> entry : quality.getProperties().entrySet()) {

                    String entryName = entry.getKey().getName().replace("/", "").replace(" ", "");;
                    nodeTemplateDescription += "<" + entryName + ">"
                            + entry.getValue() + "(" + entry.getKey().getMeasurementUnit() + ")" + "</" + entryName + "> \n";
                }
            }

            for (CostFunction costFunction : service.getCostFunctions()) {

                //if cost no mather what
                if (costFunction.getAppliedIfServiceInstanceUses().isEmpty()) {

                    for (CostElement element : costFunction.getCostElements()) {
                        String entryName = element.getCostMetric().getName().replace("/", "").replace(" ", "");;
                        String costInterval = "";
                        for (Map.Entry<MetricValue, Double> entry : element.getCostIntervalFunction().entrySet()) {
                            costInterval += "[" + entry.getKey().getValueRepresentation() + "," + entry.getValue().toString() + "]";
                        }

                        nodeTemplateDescription += "<" + entryName + ">"
                                + costInterval + "(" + element.getCostMetric().getMeasurementUnit() + ")" + "</" + entryName + "> \n";
                    }
                }
            }

        } else {
            Logger.getLogger(CloudServicesToWinery.class.getName()).log(Level.WARNING, "Entity type of " + entity + " not recognized");
        }

        nodeTemplateDescription += "		 </wpd:Properties>\n"
                + "               </tosca:Properties>\n"
                + "            </tosca:NodeTemplate>";

        return nodeTemplateDescription;
    }
}
