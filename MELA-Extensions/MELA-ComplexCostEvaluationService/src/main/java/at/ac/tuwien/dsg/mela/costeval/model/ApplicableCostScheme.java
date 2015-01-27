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
package at.ac.tuwien.dsg.mela.costeval.model;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.util.conversions.helper.CostIntervalMapAdapter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ApplicableCostScheme")
public class ApplicableCostScheme {

    @XmlJavaTypeAdapter(CostIntervalMapAdapter.class)
    private Map<MonitoredElement, CostFunction> applicableCostFunctions;

    {
        applicableCostFunctions = new HashMap<>();
    }

    public ApplicableCostScheme withServiceUnits(final Map<MonitoredElement, CostFunction> costFunctions) {
        this.applicableCostFunctions = costFunctions;
        return this;
    }

    public Map<MonitoredElement, CostFunction> getApplicableCostFunctions() {
        return applicableCostFunctions;
    }

    public CostFunction getApplicableCostFunctions(MonitoredElement element) {
        return applicableCostFunctions.get(element);
    }

    public void addCostFunction(MonitoredElement element, CostFunction costFunction) {
        this.applicableCostFunctions.put(element, costFunction);
    }

    public void removeCostFunction(MonitoredElement element) {
        this.applicableCostFunctions.remove(element);
    }

//    public CostFunction getApplicableCostFunctions(MonitoredElement element) {
//        return applicableCostFunctions.get(element.getId());
//    }
//
//    public CostFunction getApplicableCostFunctions(String monitoredElementID) {
//        return applicableCostFunctions.get(monitoredElementID);
//    }
//
//    public void addCostFunction(MonitoredElement element, CostFunction costFunction) {
//        this.applicableCostFunctions.put(element.getId(), costFunction);
//    }
//
//    public void addCostFunction(String monitoredElementID, CostFunction costFunction) {
//        this.applicableCostFunctions.put(monitoredElementID, costFunction);
//    }
//
//    public void removeCostFunction(MonitoredElement element) {
//        this.applicableCostFunctions.remove(element.getId());
//    }
//
//    public void removeCostFunction(String monitoredElementID) {
//        this.applicableCostFunctions.remove(monitoredElementID);
//    }
}
