/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
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
package at.ac.tuwien.dsg.mela.common.jaxbEntities.elasticity;

import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.ExtraElementInfo;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ElasticityPathway")
public class ElasticityPathwayXML {

	@XmlElement(name = "MonitoredElement")
	private MonitoredElement element;

	@XmlElement(name = "Metrics")
	private Collection<Metric> metrics;
	
	@XmlElement(name = "EncounterGroup")
	private Collection<ElasticityPathwayGroupXML> groups;
	 
	 
	
	{
		metrics = new ArrayList<Metric>();
		groups = new ArrayList<ElasticityPathwayGroupXML>();
	}

	public ElasticityPathwayXML() {
	}

	public ElasticityPathwayXML(Collection<Metric> metrics) {
		this.metrics = metrics;
	}

	public MonitoredElement getElement() {
		return element;
	}

	public void setElement(MonitoredElement element) {
		this.element = element;
	}

	public Collection<Metric> getMetrics() {
		return metrics;
	}

	public void setMetrics(Collection<Metric> metric) {
		this.metrics = metric;
	}

	public void addMetrics(Collection<Metric> metric) {
		this.metrics.addAll(metric);
	}

	public void addMetric(Metric metric) {
		this.metrics.add(metric);
	}

	public void removeMetrics(Collection<Metric> metric) {
		this.metrics.removeAll(metric);
	}

	public void removeMetric(Metric metric) {
		this.metrics.remove(metric);
	}

	public Collection<ElasticityPathwayGroupXML> getGroups() {
		return groups;
	}

	public void setGroups(Collection<ElasticityPathwayGroupXML> groups) {
		this.groups = groups;
	}
	
	public void addGroups(Collection<ElasticityPathwayGroupXML> groups) {
		this.groups.addAll(groups);
	}
	
	public void addGroup(ElasticityPathwayGroupXML group) {
		this.groups.add(group);
	}
	
	public void removeGroups(Collection<ElasticityPathwayGroupXML> groups) {
		this.groups.removeAll(groups);
	}
	
	public void removeGroup(ElasticityPathwayGroupXML group) {
		this.groups.remove(group);
	}
	
	

}
