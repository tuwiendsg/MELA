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
package at.ac.tuwien.dsg.mela.dataservice.validation.impl;

import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects.CollectedMetricValue;
import at.ac.tuwien.dsg.mela.dataservice.validation.MetricValidationTest;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class DefaultMetricValidator extends MetricValidationTest {

    {
        this.humanReadableDescription = "Checks if value >=0 , ~= NaN, null, -/+ Infinity";
    }

    @Override
    public boolean isValid(CollectedMetricValue metricValue) {
        Double value = metricValue.getValue();

        return value != null && !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0;
    }

}
