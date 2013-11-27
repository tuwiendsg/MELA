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
package at.ac.tuwien.dsg.mela.common.configuration.metricComposition;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElementMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement.MonitoredElementLevel;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at  *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompositionRule")
@XmlType(propOrder = {"targetMonitoredElementLevel", "targetMonitoredElementIDs", "resultingMetric", "operation"})
public class CompositionRule {

    @XmlAttribute(name = "TargetMonitoredElementLevel", required = true)
    private MonitoredElement.MonitoredElementLevel targetMonitoredElementLevel;
    @XmlElement(name = "TargetMonitoredElementID", required = false)
    private ArrayList<String> targetMonitoredElementIDs;
    @XmlElement(name = "ResultingMetric", required = true)
    private Metric resultingMetric;
    @XmlElement(name = "Operation", required = true)
    private CompositionOperation operation;

    {
        targetMonitoredElementIDs = new ArrayList<String>();
    }

    public CompositionRule() {
    }

    public ArrayList<String> getTargetMonitoredElementIDs() {
        return targetMonitoredElementIDs;
    }

    public void setTargetMonitoredElementIDs(ArrayList<String> metricSourceMonitoredElementIDs) {
        this.targetMonitoredElementIDs = metricSourceMonitoredElementIDs;
    }

    public void addTargetMonitoredElementIDS(String id) {
        targetMonitoredElementIDs.add(id);
    }

    public void removeTargetMonitoredElementID(String id) {
        targetMonitoredElementIDs.remove(id);
    }

    public Metric getResultingMetric() {
        return resultingMetric;
    }

    public void setResultingMetric(Metric resultingMetric) {
        this.resultingMetric = resultingMetric;
    }

    public CompositionOperation getOperation() {
        return operation;
    }

    public void setOperation(CompositionOperation operation) {
        this.operation = operation;
    }

    public MonitoredElementLevel getTargetMonitoredElementLevel() {
        return targetMonitoredElementLevel;
    }

    public void setTargetMonitoredElementLevel(MonitoredElementLevel targetMonitoredElementLevel) {
        this.targetMonitoredElementLevel = targetMonitoredElementLevel;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompositionRule other = (CompositionRule) obj;
        if (this.resultingMetric != other.resultingMetric && (this.resultingMetric == null || !this.resultingMetric.equals(other.resultingMetric))) {
            return false;
        }
        if (this.operation != other.operation && (this.operation == null || !this.operation.equals(other.operation))) {
            return false;
        }
        if (this.targetMonitoredElementLevel != other.targetMonitoredElementLevel) {
            return false;
        }
        if (this.targetMonitoredElementIDs != other.targetMonitoredElementIDs && (this.targetMonitoredElementIDs == null || !this.targetMonitoredElementIDs.equals(other.targetMonitoredElementIDs))) {
            return false;
        }
        return true;
    }

    /**
     * The idea is that the whole monitoring data is fed tot he rule, which
     * extracts the data for its target Level and Service Element IDs, and
     * applies composition rules on them, enriching the supplied data
     *
     * @param serviceMonitoringSnapshot complete monitoring snapshot (might be
     * trimmed in the future to minimize communication)
     * @return
     */
    public void apply(ServiceMonitoringSnapshot serviceMonitoringSnapshot) {

        //1'step extract the monitoring data for the target service elements
        Map<MonitoredElement, MonitoredElementMonitoringSnapshot> levelMonitoringData = serviceMonitoringSnapshot.getMonitoredData(targetMonitoredElementLevel);

        if(levelMonitoringData == null){
            Logger.getRootLogger().log(Level.WARN, "Level " + targetMonitoredElementLevel + " not found in monitoring data");
            return;
        }
        
        Collection<MonitoredElementMonitoringSnapshot> toBeProcessed = new ArrayList<MonitoredElementMonitoringSnapshot>();

        //step 2
        //if target IDs have ben supplied, use them to extract only the monitoring data for the target elements, else process all elements on this level
        if (targetMonitoredElementIDs.isEmpty()) {
            //if all on level, get each elementy data, and add its children data. Might destroy the data in the long run
            Collection<MonitoredElementMonitoringSnapshot> levelData = levelMonitoringData.values();
            for (MonitoredElementMonitoringSnapshot levelElementData : levelData) {

//                MonitoredElement element = levelElementData.getMonitoredElement();
//                //for each service element children, add the child to the elementData MonitoredElementMonitoringSnapshot
//                //only add the data of the direct children
//                for (MonitoredElement child : element.getContainedElements()) {
//                    MonitoredElementMonitoringSnapshot childData = serviceMonitoringSnapshot.getMonitoredData(child);
//                    if (childData != null) {
//                        levelElementData.addChild(childData);
//                    }
//                }
                toBeProcessed.add(levelElementData);
            }
        } else {
            for (String id : targetMonitoredElementIDs) {

                //TODO: an issue is that in the MonitoredElementMonitoringSnapshot I do NOT hold the children snapshots.
                //So now I follow the MonitoredElement children, get their MonitoredElementMonitoringSnapshot, and enrich the MonitoredElementMonitoringSnapshot supplied to the operations

                MonitoredElement element = new MonitoredElement(id);
                if (levelMonitoringData.containsKey(element)) {
                    MonitoredElementMonitoringSnapshot elementData = levelMonitoringData.get(element);
//                    //for each service element children, add the child to the elementData MonitoredElementMonitoringSnapshot
//                    //only add the data of the direct children
//                    for (MonitoredElement child : elementData.getMonitoredElement().getContainedElements()) {
//                        MonitoredElementMonitoringSnapshot childData = serviceMonitoringSnapshot.getMonitoredData(child);
//                        if (childData != null) {
//                            elementData.addChild(childData);
//                        }
//                    }
                    toBeProcessed.add(elementData);
                } else {
                    Logger.getRootLogger().log(Level.WARN, "Element with ID " + id + " not found in monitoring data");
                }
            }
        }
        //step 3 
        //for each MonitoredElementMonitoringSnapshot apply composition rule and enrich monitoring data in place
        for (MonitoredElementMonitoringSnapshot snapshot : toBeProcessed) {

            MetricValue result = operation.apply(snapshot);
            if (result != null) {
                //put new composite metric
                snapshot.putMetric(resultingMetric, result);
            }
        }
    }
//    
//    //used in historical data aggregation
//     public MetricValue apply(List<MetricValue> values) {
//         return this.operation.apply(values);
//     }
}
