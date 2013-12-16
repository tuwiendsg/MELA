/**
 * Copyright 2013 Technische Universitat Wien (TUW), Distributed Systems Group
 * E184
 *
 * This work was partially supported by the European Commission in terms of the
 * CELAR FP7 project (FP7-ICT-2011-8 \#317790)
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
package at.ac.tuwien.dsg.mela.dataservice.config.dataSourcesManagement;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DataSource")
public class DataSourceConfig {
	
	@XmlElement(name = "Name")
	private String name;

	@XmlElement(name = "DataCollectionMechanism")
	private String dataCollectionMechanism;

	@XmlElement(name = "Type")
	private String type;

	@XmlElement(name = "ClassName")
	private String className;

	@XmlElement(name = "Property", required = true)
	private List<String> properties;

	{
		properties = new ArrayList<String>();
	}

	public String getDataCollectionMechanism() {
		return dataCollectionMechanism;
	}

	public void setDataCollectionMechanism(String dataCollectionMechanism) {
		this.dataCollectionMechanism = dataCollectionMechanism;
	}
	
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}

}
