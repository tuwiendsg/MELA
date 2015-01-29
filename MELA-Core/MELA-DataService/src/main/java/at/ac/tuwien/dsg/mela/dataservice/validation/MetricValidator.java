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
package at.ac.tuwien.dsg.mela.dataservice.validation;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.CollectedMetricValue;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.queuebased.helpers.dataobjects.NumericalCollectedMetricValue;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class MetricValidator {

    private List<MetricValidationTest> validationTests;

    {
        validationTests = new ArrayList<>();
    }

    public MetricValidator() {
    }

    public MetricValidator withValidationTests(final List<MetricValidationTest> validationTests) {
        this.validationTests = validationTests;
        return this;
    }

    public MetricValidator withValidationTest(MetricValidationTest test) {
        this.validationTests.add(test);
        return this;
    }

    public List<MetricValidationTest> getValidationTests() {
        return validationTests;
    }

    public void setValidationTests(List<MetricValidationTest> validationTests) {
        this.validationTests = validationTests;
    }

    public void addValidationTest(MetricValidationTest test) {
        this.validationTests.add(test);
    }

    public void removeValidationTests(MetricValidationTest test) {
        this.validationTests.remove(test);
    }

    public boolean isValid(NumericalCollectedMetricValue metricValue) {
        for (MetricValidationTest metricValidationTest : validationTests) {
            if (!metricValidationTest.isValid(metricValue)) {
                return false;
            }
        }

        return true;
    }

//    @Override
//    protected MetricValidator clone() throws CloneNotSupportedException {
//        MetricValidator metricValidator = new MetricValidator();
//        for (MetricValidationTest test : validationTests) {
//            metricValidator.addValidationTests(test.clone());
//        }
//        return metricValidator;
//    }
    /**
     *
     * @param metricValue
     * @return a list of failed tests description
     */
    public List<MetricValidationTest> isValidDetailedAnalysis(NumericalCollectedMetricValue metricValue) {

        List<MetricValidationTest> analysys = new ArrayList<>();

        for (MetricValidationTest metricValidationTest : validationTests) {
            analysys.add(metricValidationTest);
        }

        return analysys;
    }

}
