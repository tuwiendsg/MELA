/*
 *  Copyright 2015 Technische Universitat Wien (TUW), Distributed Systems Group E184
 * 
 *  This work was partially supported by the European Commission in terms of the 
 *  CELAR FP7 project (FP7-ICT-2011-8 \#317790)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package at.ac.tuwien.dsg.mela.costeval.model;

import at.ac.tuwien.dsg.mela.common.applicationdeploymentconfiguration.UsedCloudOfferedService;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class UnusedCostUnitsReport {

    static final Logger logger = LoggerFactory.getLogger(UnusedCostUnitsReport.class);

    private MonitoredElement unitInstance;

    private Double totalCostBilled = 0d;
    private Double totalCostUsed = 0d;

    //for each cloud offferd services used by the unit instance, we have multiple cost metrics
    private Map<UsedCloudOfferedService, Map<Metric, Double>> unusedCostMapForEachMetric;

    {
        unusedCostMapForEachMetric = new HashMap<>();
    }

    public MonitoredElement getUnitInstance() {
        return unitInstance;
    }

    public Map<UsedCloudOfferedService, Map<Metric, Double>> getUnusedCostMapForEachMetric() {
        return unusedCostMapForEachMetric;
    }

    public void setUnitInstance(MonitoredElement unitInstance) {
        this.unitInstance = unitInstance;
    }

    public UnusedCostUnitsReport withUnitInstance(final MonitoredElement unitInstance) {
        this.unitInstance = unitInstance;
        return this;
    }

    public Double getTotalCostUsedFromWhatWasBilled() {
        return totalCostUsed;
    }

    public Double getTotalCostBilled() {
        return totalCostBilled;
    }

    /**
     *
     * @return how much we used from total billed cost in percentage 1 means
     * 100% efficiency 0 means 0% 0.5 means 50% efficiency
     */
    public Double getCostEfficiency() {
//        //need to compute average unused cost
//        int size = 0;
//        Double efficiency = 0d;
//        for (Map<Metric, Double> map : unusedCostMapForEachMetric.values()) {
//            for (Double d : map.values()) {
//                size++;
//                efficiency += d;
//            }
//
//        }
//
//        //totalCostBilled is 100%, and we used totalCostUsedFromWhatWasBilled is y%, so simple equation 
//        return efficiency / size;

//        efficiency is how much we use from real cost
        return totalCostUsed / totalCostBilled;
    }

    /**
     *
     * @param cloudOfferedService
     * @param costMetric
     * @param usedCostPercentage - used cost means how usedCostPercentage I
     * actually used from what I payed
     * @param billedCost
     * @return
     */
    public UnusedCostUnitsReport withUsedCostForCloudOfferedService(UsedCloudOfferedService cloudOfferedService, Metric costMetric, Double usedCostPercentage, Double billedCost) {
        Map<Metric, Double> map;
        if (unusedCostMapForEachMetric.containsKey(cloudOfferedService)) {
            map = unusedCostMapForEachMetric.get(cloudOfferedService);
        } else {
            map = new HashMap<>();
            unusedCostMapForEachMetric.put(cloudOfferedService, map);
        }
        map.put(costMetric, usedCostPercentage);
        totalCostUsed += usedCostPercentage * billedCost;
        totalCostBilled += billedCost;
        return this;
    }

}
