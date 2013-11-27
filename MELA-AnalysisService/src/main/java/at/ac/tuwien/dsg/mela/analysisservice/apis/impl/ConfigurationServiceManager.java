/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group E184
 *
 * This work was partially supported by the European Commission in terms of the CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.mela.analysisservice.apis.impl;

import java.util.ArrayList;
import java.util.Collection;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionOperationType;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Condition;
import at.ac.tuwien.dsg.mela.common.requirements.MetricFilter;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.analysisservice.apis.ConfigurationServiceAPI;
import at.ac.tuwien.dsg.mela.analysisservice.control.SystemControl;
import at.ac.tuwien.dsg.mela.analysisservice.control.SystemControlFactory;
import at.ac.tuwien.dsg.mela.analysisservice.utils.exceptions.ConfigurationException;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 *
 * Provides a centralized API (Facade actually) for the SystemControl
 */
public class ConfigurationServiceManager implements ConfigurationServiceAPI {

    private SystemControl systemControl;

    public ConfigurationServiceManager(SystemControlFactory systemControlFactory) {
        this.systemControl = systemControlFactory.getSystemControlInstance();
    }

    public void submitServiceConfiguration(MonitoredElement MonitoredElement) throws ConfigurationException {
       systemControl.setServiceConfiguration(MonitoredElement);
    }

    
    public void submitMetricCompositionRulesConfiguration(CompositionRulesConfiguration compositionRulesConfiguration) throws ConfigurationException {
        systemControl.setCompositionRulesConfiguration(compositionRulesConfiguration);
    }

    
    public void submitRequirementsConfiguration(Requirements requirements) throws ConfigurationException {
        systemControl.setRequirements(requirements);
    }

    
    public Collection<CompositionOperationType> getMetricCompositionOperations() {
        Collection<CompositionOperationType> operationTypes = new ArrayList<CompositionOperationType>();
        operationTypes.add(CompositionOperationType.SUM);
        operationTypes.add(CompositionOperationType.MAX);
        operationTypes.add(CompositionOperationType.MIN);
        operationTypes.add(CompositionOperationType.AVG);
        operationTypes.add(CompositionOperationType.DIV);
        operationTypes.add(CompositionOperationType.ADD);
        operationTypes.add(CompositionOperationType.SUB);
        operationTypes.add(CompositionOperationType.MUL);
        operationTypes.add(CompositionOperationType.CONCAT);
        operationTypes.add(CompositionOperationType.UNION);
        operationTypes.add(CompositionOperationType.KEEP);
        operationTypes.add(CompositionOperationType.KEEP_LAST);
        operationTypes.add(CompositionOperationType.KEEP_FIRST);
        return operationTypes;
    }

    
    public Collection<Condition.Type> getRequirementConditionTypes() {
        Collection<Condition.Type> operationTypes = new ArrayList<Condition.Type>();
        operationTypes.add(Condition.Type.LESS_THAN);
        operationTypes.add(Condition.Type.LESS_EQUAL);
        operationTypes.add(Condition.Type.GREATER_THAN);
        operationTypes.add(Condition.Type.GREATER_EQUAL);
        operationTypes.add(Condition.Type.EQUAL);
        operationTypes.add(Condition.Type.RANGE);
        operationTypes.add(Condition.Type.ENUMERATION);
        return operationTypes;
    }

    
    public Collection<Metric> getAvailableMetricsForMonitoredElement(MonitoredElement MonitoredElement) throws ConfigurationException {
        return systemControl.getDataAccess().getAvailableMetricsForMonitoredElement(MonitoredElement);
    }

    
    public void addMetricFilters(Collection<MetricFilter> metricFilters) throws ConfigurationException {
        systemControl.getDataAccess().addMetricFilters(metricFilters);
    }

    
    public void removeMetricFilters(Collection<MetricFilter> metricFilters) throws ConfigurationException {
        systemControl.getDataAccess().removeMetricFilters(metricFilters);
    }
    
    public void addMetricFilter(MetricFilter metricFilter) throws ConfigurationException {
        systemControl.getDataAccess().addMetricFilter(metricFilter);
    }

    
    public void removeMetricFilter(MetricFilter metricFilter) throws ConfigurationException {
        systemControl.getDataAccess().removeMetricFilter(metricFilter);
    }
}
