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
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
public abstract class ElasticitySpaceFunction {
    //this contains the service structure with a single VM per Service Unit
    //to be able to say for VMs from Service_Unit X these are the elasticity limits

    protected MonitoredElement serviceStructure;
    protected ElasticitySpace elasticitySpace;
    protected Requirements requirements;

    {
        requirements = new Requirements();
    }

    //whenever new requirements are submitted, retrain the elasticity space
    public final void setRequirements(Requirements requirements) {
        this.requirements = requirements;
        ElasticitySpace oldElasticitySpace = elasticitySpace;

        //create new Elasticity Space
        {
            elasticitySpace = new ElasticitySpace(serviceStructure);
            //add generic VM per each service unit

            ServiceMonitoringSnapshot upperBoundary = elasticitySpace.getElasticitySpaceBoundary().getUpperBoundary();
            ServiceMonitoringSnapshot lowerBoundary = elasticitySpace.getElasticitySpaceBoundary().getLowerBoundary();

            List<MonitoredElement> processingList = new ArrayList<MonitoredElement>();
            processingList.add(serviceStructure);

            //DFS traversal until I get ServiceUnit level. Need to change this when we also insert VirtualClusters
            while (!processingList.isEmpty()) {
                MonitoredElement element = processingList.remove(processingList.size() - 1);

                //create empty boundaries for each element
                upperBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(element));
                lowerBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(element));

                //if we reach service unit, add a VM at it to contain the boundaries for ANY vm at this unit
                if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                    MonitoredElement genericVMPerUnit = new MonitoredElement();
                    genericVMPerUnit.setId(element.getId() + "_VMs");
                    genericVMPerUnit.setName(element.getId() + "_VMs");
                    genericVMPerUnit.setLevel(MonitoredElement.MonitoredElementLevel.VM);
                    element.addElement(genericVMPerUnit);

                    upperBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(genericVMPerUnit));
                    lowerBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(genericVMPerUnit));

                } else {
                    //if not service unit level, also process children
                    processingList.addAll(element.getContainedElements());
                }
            }
        }

        //go trough all old monitoring snapshots and retrain
        for (ElasticitySpace.ElasticitySpaceEntry entry : oldElasticitySpace.getSpaceEntries()) {
            this.trainElasticitySpace(entry.getServiceMonitoringSnapshot());
        }

    }

    public final void addRequirement(Requirement requirement) {
        this.requirements.getRequirements().add(requirement);
    }

    public final void removeRequirement(Requirement requirement) {
        this.requirements.getRequirements().add(requirement);
    }
    
    public ElasticitySpaceFunction(){
    	
    }

    public ElasticitySpaceFunction(MonitoredElement service) {
        //gets clone without VM level elements such that I can combine all VMs into 1 and retrieve a set of common boundaries
        this.serviceStructure = service.clone();
        elasticitySpace = new ElasticitySpace(serviceStructure);
        //add generic VM per each service unit

        ServiceMonitoringSnapshot upperBoundary = elasticitySpace.getElasticitySpaceBoundary().getUpperBoundary();
        ServiceMonitoringSnapshot lowerBoundary = elasticitySpace.getElasticitySpaceBoundary().getLowerBoundary();

        List<MonitoredElement> processingList = new ArrayList<MonitoredElement>();
        processingList.add(serviceStructure);

        //DFS traversal until I get ServiceUnit level. Need to change this when we also insert VirtualClusters
        while (!processingList.isEmpty()) {
            MonitoredElement element = processingList.remove(processingList.size() - 1);

            //create empty boundaries for each element
            upperBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(element));
            lowerBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(element));

            //if we reach service unit, add a VM at it to contain the boundaries for ANY vm at this unit
            if (element.getLevel().equals(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT)) {
                MonitoredElement genericVMPerUnit = new MonitoredElement();
                genericVMPerUnit.setId(element.getId() + "_VMs");
                genericVMPerUnit.setName(element.getId() + "_VMs");
                genericVMPerUnit.setLevel(MonitoredElement.MonitoredElementLevel.VM);
                element.addElement(genericVMPerUnit);

                upperBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(genericVMPerUnit));
                lowerBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(genericVMPerUnit));

            } else {
                //if not service unit level, also process children
                processingList.addAll(element.getContainedElements());
            }
        }
//        System.out.println(serviceStructure);
    }

    /**
     *
     * @return the elasticity space obtained from training with
     * "trainElasticitySpace" functions
     */
    public ElasticitySpace getElasticitySpace() {
        return elasticitySpace;
    }

    /**
     * resets the ElasticitySpace to initial state after construction the object
     */
    public void resetElasticitySpace() {
        elasticitySpace.reset();
        //gets clone without VM level elements such that I can combine all VMs into 1 and retrieve a set of common boundaries
//        elasticitySpace = new ElasticitySpace(serviceStructure);
        //add generic VM per each service unit

        ServiceMonitoringSnapshot upperBoundary = elasticitySpace.getElasticitySpaceBoundary().getUpperBoundary();
        ServiceMonitoringSnapshot lowerBoundary = elasticitySpace.getElasticitySpaceBoundary().getLowerBoundary();

        List<MonitoredElement> processingList = new ArrayList<MonitoredElement>();
        processingList.add(serviceStructure);

        //DFS traversal until I get ServiceUnit level. Need to change this when we also insert VirtualClusters
        while (!processingList.isEmpty()) {
            MonitoredElement element = processingList.remove(processingList.size() - 1);

            //create empty boundaries for each element
            upperBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(element));
            lowerBoundary.addMonitoredData(new MonitoredElementMonitoringSnapshot(element));

            processingList.addAll(element.getContainedElements());
        }
//        System.out.println(serviceStructure);
    }

    /**
     * The two trainElasticitySpace functions have the role of updating the
     * elasticity space boundaries at run-time
     *
     * @param monitoringData
     */
    public abstract void trainElasticitySpace(Collection<ServiceMonitoringSnapshot> monitoringData);

    /**
     * The two trainElasticitySpace functions have the role of updating the
     * elasticity space boundaries at run-time
     *
     * @param monitoringData
     */
    public abstract void trainElasticitySpace(ServiceMonitoringSnapshot monitoringData);
    
    
    /**
     * Used to train an already existent space
     * @param elasticitySpace
     * @param monitoringData
     * @param requirements
     */
    public abstract void trainElasticitySpace(ElasticitySpace elasticitySpace, ServiceMonitoringSnapshot monitoringData, Requirements requirements);
    
    
    
    public abstract void trainElasticitySpace(ElasticitySpace elasticitySpace, Collection<ServiceMonitoringSnapshot> monitoringData, Requirements requirements);
}
