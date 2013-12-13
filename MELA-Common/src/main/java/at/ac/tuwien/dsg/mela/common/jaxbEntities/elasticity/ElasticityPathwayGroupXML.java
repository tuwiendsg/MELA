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
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
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
@XmlRootElement(name = "ElasticityPathwayGroup")
public class ElasticityPathwayGroupXML {

	@XmlElement(name = "EncounterRate")
	private Double encounterRate;

	@XmlElement(name = "Entry")
	private Collection<ElasticityPathwayGroupEntryXML> entries;

	{
		entries = new ArrayList<ElasticityPathwayGroupEntryXML>();
	}

	public ElasticityPathwayGroupXML() {
	}

	public Double getEncounterRate() {
		return encounterRate;
	}

	public ElasticityPathwayGroupXML(Double encounterRate, Collection<ElasticityPathwayGroupEntryXML> entries) {
		super();
		this.encounterRate = encounterRate;
		this.entries = entries;
	}

	public void setEncounterRate(Double encounterRate) {
		this.encounterRate = encounterRate;
	}

	public Collection<ElasticityPathwayGroupEntryXML> getEntries() {
		return entries;
	}

	public void setEntries(Collection<ElasticityPathwayGroupEntryXML> entries) {
		this.entries = entries;
	}
	
	public void addEntries(Collection<ElasticityPathwayGroupEntryXML> entries) {
		this.entries.addAll(entries);
	}
	
	public void addEntry(ElasticityPathwayGroupEntryXML entrie) {
		this.entries.add(entrie);
	}
	
	
	public void removeEntries(Collection<ElasticityPathwayGroupEntryXML> entries) {
		this.entries.removeAll(entries);
	}
	
	public void removeEntry(ElasticityPathwayGroupEntryXML entry) {
		this.entries.remove(entry);
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlRootElement(name = "ElasticityPathwayGroupEntry")
	public static class ElasticityPathwayGroupEntryXML {
		@XmlElement(name = "Metric")
		public Metric metric;

		@XmlElement(name = "MetricValue")
		public MetricValue metricValue;

		
		
		public ElasticityPathwayGroupEntryXML() {
			super();
		}

		public ElasticityPathwayGroupEntryXML(Metric metric, MetricValue metricValue) {
			super();
			this.metric = metric;
			this.metricValue = metricValue;
		}

		public Metric getMetric() {
			return metric;
		}

		public void setMetric(Metric metric) {
			this.metric = metric;
		}

		public MetricValue getMetricValue() {
			return metricValue;
		}

		public void setMetricValue(MetricValue metricValue) {
			this.metricValue = metricValue;
		}

	}

}
