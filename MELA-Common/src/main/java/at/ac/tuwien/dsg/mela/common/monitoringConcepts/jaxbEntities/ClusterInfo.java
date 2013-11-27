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

package at.ac.tuwien.dsg.mela.common.monitoringConcepts.jaxbEntities;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Author: Daniel Moldovan 
 * E-Mail: d.moldovan@dsg.tuwien.ac.at 

 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CLUSTER")
public class ClusterInfo {
    @XmlAttribute(name = "NAME")
    private String name;

    @XmlAttribute(name = "OWNER")
    private String owner;

    @XmlAttribute(name = "LATLONG")
    private String latlong;

    @XmlAttribute(name = "URL")
    private String url;

    @XmlAttribute(name = "LOCALTIME")
    private String localtime;

    @XmlElement(name = "HOST")
    private Collection<HostInfo> hostsInfo;

    {
        hostsInfo = new ArrayList<HostInfo>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getLatlong() {
        return latlong;
    }

    public void setLatlong(String latlong) {
        this.latlong = latlong;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocaltime() {
        return localtime;
    }

    public void setLocaltime(String localtime) {
        this.localtime = localtime;
    }

    public Collection<HostInfo> getHostsInfo() {
        return hostsInfo;
    }

    public void setHostsInfo(Collection<HostInfo> hostsInfo) {
        this.hostsInfo = hostsInfo;
    }

    public Collection<HostInfo> searchHostsByName(String name) {
        Collection<HostInfo> hosts = new ArrayList<HostInfo>();
        for (HostInfo hostInfo : this.hostsInfo) {
            if (hostInfo.getName().contains(name)) {
                hosts.add(hostInfo);
            }
        }
        return hosts;
    }

    public Collection<HostInfo> searchHostsByIP(String ip) {
        Collection<HostInfo> hosts = new ArrayList<HostInfo>();
        for (HostInfo hostInfo : this.hostsInfo) {
            if (hostInfo.getIp().contains(ip)) {
                hosts.add(hostInfo);
            }
        }
        return hosts;
    }

    //if gmodstart has same value means same machine
    public Collection<HostInfo> searchHostsByGmodStart(String gmodstarted) {
        Collection<HostInfo> hosts = new ArrayList<HostInfo>();
        for (HostInfo hostInfo : this.hostsInfo) {
            if (hostInfo.getGmondStarted().contains(gmodstarted)) {
                hosts.add(hostInfo);
            }
        }
        return hosts;
    }

    @Override
    public String toString() {
        String info = "ClusterInfo{" +
                "name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", latlong='" + latlong + '\'' +
                ", url='" + url + '\'' +
                ", localtime='" + localtime + '\'' + ", hostsInfo=";


        for (HostInfo hostInfo : hostsInfo) {
            info += "\n " + hostInfo.toString() + "\n";
        }


        info += '}';
        return info;
    }
}
