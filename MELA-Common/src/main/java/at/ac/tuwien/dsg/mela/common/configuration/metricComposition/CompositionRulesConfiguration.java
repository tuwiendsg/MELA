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

import javax.xml.bind.annotation.*;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CompositionRulesConfiguration")
public class CompositionRulesConfiguration {

    @XmlAttribute(name = "TargetServiceID", required = true)
    private String targetServiceID;

    public String getTargetServiceID() {
        return targetServiceID;
    }

    public void setTargetServiceID(String targetServiceID) {
        this.targetServiceID = targetServiceID;
    }
    
    @XmlElement(name = "MetricsCompositionRules", required = false)
    private CompositionRulesBlock metricCompositionRules;
    
    @XmlElement(name = "HistoricalMetricsCompositionRules", required = false)
    private CompositionRulesBlock historicDataAggregationRules;

    public CompositionRulesConfiguration() {
        metricCompositionRules = new CompositionRulesBlock();
        historicDataAggregationRules = new CompositionRulesBlock();
    }

    public CompositionRulesBlock getMetricCompositionRules() {
        return metricCompositionRules;
    }

    public void setMetricCompositionRules(CompositionRulesBlock metricCompositionRules) {
        this.metricCompositionRules = metricCompositionRules;
    }

    public CompositionRulesBlock getHistoricMetricCompositionRules() {
        return historicDataAggregationRules;
    }

    public void setHistoricDataAggregationRules(CompositionRulesBlock historicDataAggregationRules) {
        this.historicDataAggregationRules = historicDataAggregationRules;
    }
}
