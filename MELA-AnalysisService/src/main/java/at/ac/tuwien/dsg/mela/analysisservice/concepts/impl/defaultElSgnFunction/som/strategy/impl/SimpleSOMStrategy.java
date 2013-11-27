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
package at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElSgnFunction.som.strategy.impl;

import at.ac.tuwien.dsg.mela.analysisservice.concepts.impl.defaultElSgnFunction.som.strategy.SOMStrategy;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
public class SimpleSOMStrategy extends SOMStrategy {


    public SimpleSOMStrategy() {
        super(1d,1d,2,0.7);
    }

    /**
     *
     * @param distanceLevel > 1
     * @param neighboursCount
     * @return
     */
    public Double getDistanceRestraintFactor(int distanceLevel, int neighboursCount){
//        if(distanceLevel < 1){
//            Configuration.getLogger(this.getClass()).log(Level.ERROR,"Distance level < 1");
//            System.exit(1);
//        }
        if(neighboursCount < 1){
            neighboursCount = 1;
        }
        //very simple implementation
//        return (distanceLevel==1)? 1 : distanceRestraintFactor/distanceLevel;
        return (distanceLevel==1)? 1 : distanceRestraintFactor/distanceLevel/neighboursCount;
    }

    /**
     *
     * @param distanceLevel  > 0
     * @return
     */
    public Double geLearningRestraintFactor(int distanceLevel){
//        //very simple implementation
//        if(distanceLevel < 1){
//            Configuration.getLogger(this.getClass()).log(Level.ERROR,"Distance level < 1");
//            System.exit(1);
//        }
//        return (distanceLevel <= neighbourhoodSize)? 1d : 0d;
        return  distanceRestraintFactor / distanceLevel;
    }


}
