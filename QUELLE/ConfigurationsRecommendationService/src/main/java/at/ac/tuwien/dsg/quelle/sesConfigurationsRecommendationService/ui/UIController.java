/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.ui;

import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.SESConstructionController;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.RequirementsManagementController;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.dtos.ServiceUnitServicesRecommendation;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.bean.SessionScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class currently not in use. Kept in case of future use needed
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
//@Component("uiController")
//@ManagedBean(name = "uiController")
//@SessionScoped
public class UIController implements Serializable {

    @Value(value = "#{sesConstructionController}")
    private SESConstructionController controller;

    @Value(value = "#{requirementsManagementController}")
    private RequirementsManagementController requirementsManagementController;

    static final Logger log = LoggerFactory.getLogger(UIController.class);

    @Value(value = "#{dataAccess}")
    private DataAccess dataAccess;

    private MultiLevelRequirements requirements;

    private Map<Metric.MetricType, List<Metric>> cloudServicesMetrics;

    private Metric selectedMetric;

    private String testValue = "TEEEST";

    public SESConstructionController getController() {
        log.debug("Initializing UIController ");
        return controller;
    }

    public String getTestValue() {
        return testValue;
    }

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }

    public void setController(SESConstructionController controller) {
        this.controller = controller;
        cloudServicesMetrics = requirementsManagementController.getCloudServicesMetrics();
    }

    public DataAccess getDataAccess() {
        return dataAccess;
    }

    public void setDataAccess(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public MultiLevelRequirements getRequirements() {
        log.debug("RequirementsManagementController is " + requirementsManagementController);
        return requirementsManagementController.getRequirements();
    }

    public void setRequirements(MultiLevelRequirements requirements) {
        this.requirementsManagementController.setRequirements(requirements);
    }

    public Metric getSelectedMetric() {
        return selectedMetric;
    }

    public void setSelectedMetric(Metric selectedMetric) {
        this.selectedMetric = selectedMetric;
    }

    public List<Metric> getCostMetrics() {
        return cloudServicesMetrics.get(Metric.MetricType.COST);
    }

    public List<Metric> getQualityMetrics() {
        return cloudServicesMetrics.get(Metric.MetricType.QUALITY);
    }

    public List<Metric> getResourceMetrics() {
        return cloudServicesMetrics.get(Metric.MetricType.RESOURCE);
    }

    public List<ServiceUnitServicesRecommendation> getRecommendation() {
        return controller.analyzeRequirements(requirementsManagementController.getRequirements());

    }

}
