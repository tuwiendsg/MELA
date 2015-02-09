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
package at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.impl;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.TimePeriod;
import at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.DataFreshnessAnalysisEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class DefaultFreshnessAnalysisEngine implements DataFreshnessAnalysisEngine {

    static final Logger logger = LoggerFactory.getLogger(DefaultFreshnessAnalysisEngine.class);

    @Override
    public Double evaluateFreshness(Metric m, Long ageInSeconds) {

        String timePeriod = "s";

        if (m.getMeasurementUnit().contains("/")) {
            timePeriod = m.getMeasurementUnit().split("/")[1].toLowerCase();
        }

        //convert to seconds
        Long secondsPerTimePeriod = TimePeriod.fromString(timePeriod).toSeconds();

        Long timeIntervalInSeconds = ageInSeconds;

        //confidence is 100% is one time period passed
        Double confidence = 100.d / (timeIntervalInSeconds / secondsPerTimePeriod);

        return confidence;

    }

}
