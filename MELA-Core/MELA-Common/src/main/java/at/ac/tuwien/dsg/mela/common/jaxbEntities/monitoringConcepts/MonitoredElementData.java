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

package at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts;

import javax.xml.bind.annotation.*;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

 /**
  * 
  * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
  * Class represents monitoring information collected for a MonitoredElement
  */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "MonitoredElementData")
public class MonitoredElementData {
	
    @XmlElement(name = "MonitoredElement", required = true)
    private MonitoredElement monitoredElement; 

    @XmlElement(name = "Metric")
    Collection<MetricInfo> metrics;

    {
        metrics = new ArrayList<MetricInfo>();
    }
 
    public Collection<MetricInfo> getMetrics() {
        return metrics;
    }

    public void setMetrics(Collection<MetricInfo> metrics) {
        this.metrics = metrics;
    }
    
    public void addMetrics(Collection<MetricInfo> metrics) {
        this.metrics.addAll(metrics);
    }
    
    public void addMetric(MetricInfo metric) {
        this.metrics.add(metric);
    }

    public MonitoredElement getMonitoredElement() {
		return monitoredElement;
	}

	public void setMonitoredElement(MonitoredElement monitoredElement) {
		this.monitoredElement = monitoredElement;
	}

	/**
     * @param name name to search for. All Metrics that CONTAIN the supplied name
     *             will be returned
     * @return
     */
    public Collection<MetricInfo> searchMetricsByName(String name) {
        List<MetricInfo> metrics = new ArrayList<MetricInfo>();
        for (MetricInfo metricInfo : this.metrics) {
            if (metricInfo.getName().contains(name)) {
                metrics.add(metricInfo);
            }
        }
        return metrics;
    }
    
    

    @Override
    public String toString() {
        String info = "MonitoredElement: " + monitoredElement.getId() +  ", metrics=";

        for (MetricInfo metricInfo : metrics) {
            info += "\n\t " + metricInfo.toString();
        }
        info += '}';
        return info;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((monitoredElement == null) ? 0 : monitoredElement.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MonitoredElementData other = (MonitoredElementData) obj;
		if (monitoredElement == null) {
			if (other.monitoredElement != null)
				return false;
		} else if (!monitoredElement.equals(other.monitoredElement))
			return false;
		return true;
	}
  
     
}
