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
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityPathway.som;

import java.io.Serializable;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public abstract class SOMStrategy implements Serializable {
    protected Double distanceRestraintFactor;
    protected Double learningFactor;
    private Double toleranceRange;
    protected int neighbourhoodSize;

    {
        distanceRestraintFactor = 1d;
        learningFactor = 1d;
    }

    protected SOMStrategy(Double distanceRestraintFactor, Double learningFactor, int neighbourhoodSize, Double toleranceRange) {
        this.distanceRestraintFactor = distanceRestraintFactor;
        this.learningFactor = learningFactor;
        this.toleranceRange = toleranceRange;
        this.neighbourhoodSize = neighbourhoodSize;
    }

    public abstract Double getDistanceRestraintFactor(int distanceLevel, int neighboursCount);

    public abstract Double geLearningRestraintFactor(int distanceLevel);

    public Double getToleranceRange() {
        return toleranceRange;
    }

    public int getNeighbourhoodSize() {
        return neighbourhoodSize;
    }
}
