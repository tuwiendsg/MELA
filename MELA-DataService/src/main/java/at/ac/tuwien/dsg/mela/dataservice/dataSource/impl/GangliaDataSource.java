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
package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractPollingDataSource;
import org.apache.log4j.Level;

import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MetricInfo;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoredElementData;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import org.apache.log4j.Logger;

/**
 * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class GangliaDataSource extends AbstractPollingDataSource {

    public static final String DEFAULT_HOST = "localhost";

    public static final int DEFAULT_PORT = 8649;

    private String hostname = DEFAULT_HOST;

    private int port = DEFAULT_PORT;
	
	public MonitoringData getMonitoringData() throws DataAccessException {

        // todo: configure dinamically port and IP
        // todo DO NOT RELY ON TELNET being present on the machine, instead use a socket connection
        String cmd = "telnet " + hostname + " " + port;
        String content = "";

        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;

            while ((line = reader.readLine()) != null) {

                //if ganglia does not respond
                if (line.contains("Unable to connect")) {
                    Logger.getLogger(this.getClass()).log(Level.WARN, "Unable to execute " + cmd);
                    return null;
                }
                if (line.contains("<") || line.endsWith("]>")) {
                    content += line + "\n";
                }
            }

            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            p.destroy();

            //if ganglia does not respond
            if (content.length() == 0) {
                Logger.getLogger(this.getClass()).log(Level.WARN, "" + "Unable to execute " + cmd);
                return new MonitoringData();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, ex);
            return new MonitoringData();
        }

        StringReader stringReader = new StringReader(content);
        try {
            JAXBContext jc = JAXBContext.newInstance(GangliaSystemInfo.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            GangliaSystemInfo info = (GangliaSystemInfo) unmarshaller.unmarshal(stringReader);

            //create monitoring data representation to be returned
            MonitoringData monitoringData = new MonitoringData();
            monitoringData.setTimestamp("" + new Date().getTime());
            monitoringData.setSource(info.getSource());

            for (GangliaClusterInfo gangliaCluster : info.getClusters()) {
                //currently ClusterInfo is ignored, and we go and extract the HostInfo

                for (GangliaHostInfo gangliaHostInfo : gangliaCluster.hostsInfo) {

                    MonitoredElementData elementData = new MonitoredElementData();

                    //create representation of monitored element to associate this data in the overall monitored service
                    MonitoredElement monitoredElement = new MonitoredElement();

                    //for VM level, we use IP as monitored element ID
                    monitoredElement.setId(gangliaHostInfo.ip);
                    monitoredElement.setName(gangliaHostInfo.name);
                    
                    //for the moment we assume all what Ganglia returns is associated to VM level
                    //TODO: consider inserting better level management mechanism in which one data source can return data for multiple levels
                    monitoredElement.setLevel(MonitoredElement.MonitoredElementLevel.VM);

                    elementData.setMonitoredElement(monitoredElement);


                    //for each metric retrieved from ganglia, create its representation and store it in elementData
                    for (GangliaMetricInfo gangliaMetricInfo : gangliaHostInfo.metrics) {
                        MetricInfo metricInfo = new MetricInfo();
                        metricInfo.setName(gangliaMetricInfo.name);
                        metricInfo.setType(gangliaMetricInfo.type);
                        metricInfo.setUnits(gangliaMetricInfo.units);
                        metricInfo.setValue(gangliaMetricInfo.value);
                        elementData.addMetric(metricInfo);
                    }

                    monitoringData.addMonitoredElementData(elementData);
                }


            }

            stringReader.close();

            //dataSQLWriteAccess.writeMonitoringData(gangliaClusterInfo);

            return monitoringData;
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).log(Level.ERROR, null,e);
            return new MonitoringData();
        }
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GangliaDataSource)) return false;

        GangliaDataSource that = (GangliaDataSource) o;

        if (port != that.port) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = hostname != null ? hostname.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "GangliaDataSource{" +
                "hostname='" + hostname + '\'' +
                ", port=" + port +
                ", pollingInterval=" + getPollingIntervalMs() +
                "}";
    }

    /**
     * From here onwards we have the classes which are needed to deserialize
     * Ganglia input.
     */
    /**
     *
     * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at class
     * representing the Ganglia info root element
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "GANGLIA_XML")
    public static class GangliaSystemInfo {

        @XmlElement(name = "CLUSTER")
        private Collection<GangliaClusterInfo> clusters;

        @XmlAttribute(name = "VERSION")
        private String version;

        @XmlAttribute(name = "SOURCE")
        private String source;

        public Collection<GangliaClusterInfo> getClusters() {
            return clusters;
        }

        public void setClusters(Collection<GangliaClusterInfo> clusters) {
            this.clusters = clusters;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    /**
     *
     * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at class
     * representing a Ganglia cluster
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "CLUSTER")
    public static class GangliaClusterInfo {

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
        private Collection<GangliaHostInfo> hostsInfo;

        {
            hostsInfo = new ArrayList<GangliaHostInfo>();
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

        public Collection<GangliaHostInfo> getHostsInfo() {
            return hostsInfo;
        }

        public void setHostsInfo(Collection<GangliaHostInfo> hostsInfo) {
            this.hostsInfo = hostsInfo;
        }

        public Collection<GangliaHostInfo> searchHostsByName(String name) {
            Collection<GangliaHostInfo> hosts = new ArrayList<GangliaHostInfo>();
            for (GangliaHostInfo hostInfo : this.hostsInfo) {
                if (hostInfo.getName().contains(name)) {
                    hosts.add(hostInfo);
                }
            }
            return hosts;
        }

        public Collection<GangliaHostInfo> searchHostsByIP(String ip) {
            Collection<GangliaHostInfo> hosts = new ArrayList<GangliaHostInfo>();
            for (GangliaHostInfo hostInfo : this.hostsInfo) {
                if (hostInfo.getIp().contains(ip)) {
                    hosts.add(hostInfo);
                }
            }
            return hosts;
        }

        //if gmodstart has same value means same machine
        public Collection<GangliaHostInfo> searchHostsByGmodStart(String gmodstarted) {
            Collection<GangliaHostInfo> hosts = new ArrayList<GangliaHostInfo>();
            for (GangliaHostInfo hostInfo : this.hostsInfo) {
                if (hostInfo.getGmondStarted().contains(gmodstarted)) {
                    hosts.add(hostInfo);
                }
            }
            return hosts;
        }

        @Override
        public String toString() {
            String info = "ClusterInfo{"
                    + "name='" + name + '\''
                    + ", owner='" + owner + '\''
                    + ", latlong='" + latlong + '\''
                    + ", url='" + url + '\''
                    + ", localtime='" + localtime + '\'' + ", hostsInfo=";


            for (GangliaHostInfo hostInfo : hostsInfo) {
                info += "\n " + hostInfo.toString() + "\n";
            }


            info += '}';
            return info;
        }
    }

    /**
     *
     * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at class
     * representing a Ganglia host
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "HOST")
    public static class GangliaHostInfo {

        @XmlAttribute(name = "NAME", required = true)
        private String name;

        @XmlAttribute(name = "IP", required = true)
        private String ip;

        @XmlAttribute(name = "LOCATION", required = true)
        private String location;

        @XmlAttribute(name = "TAGS")
        private String tags;

        @XmlAttribute(name = "REPORTED")
        private String reported;

        @XmlAttribute(name = "TN")
        private String tn;

        @XmlAttribute(name = "TMAX")
        private String tmax;

        @XmlAttribute(name = "DMAX")
        private String dmax;

        @XmlAttribute(name = "GMOND_STARTED")
        private String gmondStarted;

        @XmlAttribute(name = "SOURCE")
        private String source;

        @XmlElement(name = "METRIC")
        Collection<GangliaMetricInfo> metrics;

        {
            metrics = new ArrayList<GangliaMetricInfo>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getReported() {
            return reported;
        }

        public void setReported(String reported) {
            this.reported = reported;
        }

        public String getTn() {
            return tn;
        }

        public void setTn(String tn) {
            this.tn = tn;
        }

        public String getTmax() {
            return tmax;
        }

        public void setTmax(String tmax) {
            this.tmax = tmax;
        }

        public String getDmax() {
            return dmax;
        }

        public void setDmax(String dmax) {
            this.dmax = dmax;
        }

        public String getGmondStarted() {
            return gmondStarted;
        }

        public void setGmondStarted(String gmondStarted) {
            this.gmondStarted = gmondStarted;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Collection<GangliaMetricInfo> getMetrics() {
            return metrics;
        }

        public void setMetrics(Collection<GangliaMetricInfo> metrics) {
            this.metrics = metrics;
        }

        /**
         * @param name name to search for. All Metrics that CONTAIN the supplied
         * name will be returned
         * @return
         */
        public Collection<GangliaMetricInfo> searchMetricsByName(String name) {
            List<GangliaMetricInfo> metrics = new ArrayList<GangliaMetricInfo>();
            for (GangliaMetricInfo GangliaMetricInfo : this.metrics) {
                if (GangliaMetricInfo.getName().contains(name)) {
                    metrics.add(GangliaMetricInfo);
                }
            }
            return metrics;
        }

        @Override
        public String toString() {
            String info = "HostInfo{"
                    + "name='" + name + '\''
                    + ", ip='" + ip + '\''
                    + ", location='" + location + '\''
                    + ", tags='" + tags + '\''
                    + ", reported='" + reported + '\''
                    + ", tn='" + tn + '\''
                    + ", tmax='" + tmax + '\''
                    + ", dmax='" + dmax + '\''
                    + ", gmondStarted='" + gmondStarted + '\''
                    + ", source='" + source + '\'' + ", metrics=";

            for (GangliaMetricInfo GangliaMetricInfo : metrics) {
                info += "\n\t " + GangliaMetricInfo.toString();
            }
            info += '}';
            return info;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GangliaHostInfo that = (GangliaHostInfo) o;

            if (ip != null ? !ip.equals(that.ip) : that.ip != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (ip != null ? ip.hashCode() : 0);
            return result;
        }
    }

    /**
     *
     * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at class
     * representing a Ganglia metric
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "METRIC")
    public static class GangliaMetricInfo {

        @XmlAttribute(name = "NAME", required = true)
        private String name;
        @XmlAttribute(name = "VAL", required = true)
        private String value;
        @XmlAttribute(name = "TYPE", required = true)
        private String type;
        @XmlAttribute(name = "UNITS")
        private String units;
        @XmlAttribute(name = "TN")
        private String tn;
        @XmlAttribute(name = "TMAX")
        private String tmax;
        @XmlAttribute(name = "DMAX")
        private String dmax;
        @XmlAttribute(name = "SLOPE")
        private String slope;
        @XmlAttribute(name = "SOURCE")
        private String source;
        @XmlElement(name = "EXTRA_DATA")
        private Collection<GangliaExtraDataInfo> gangliaExtraDataInfoCollection;

        {
            gangliaExtraDataInfoCollection = new ArrayList<GangliaExtraDataInfo>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        public String getTn() {
            return tn;
        }

        public void setTn(String tn) {
            this.tn = tn;
        }

        public String getTmax() {
            return tmax;
        }

        public void setTmax(String tmax) {
            this.tmax = tmax;
        }

        public String getDmax() {
            return dmax;
        }

        public void setDmax(String dmax) {
            this.dmax = dmax;
        }

        public String getSlope() {
            return slope;
        }

        public void setSlope(String slope) {
            this.slope = slope;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Collection<GangliaExtraDataInfo> getgangliaExtraDataInfoCollection() {
            return gangliaExtraDataInfoCollection;
        }

        public void setgangliaExtraDataInfoCollection(Collection<GangliaExtraDataInfo> gangliaExtraDataInfoCollection) {
            this.gangliaExtraDataInfoCollection = gangliaExtraDataInfoCollection;
        }

        @Override
        public String toString() {
            String info = "GangliaMetricInfo{"
                    + "name='" + name + '\''
                    + ", value='" + value + '\''
                    + ", type='" + type + '\''
                    + ", units='" + units + '\''
                    + ", tn='" + tn + '\''
                    + ", tmax='" + tmax + '\''
                    + ", dmax='" + dmax + '\''
                    + ", slope='" + slope + '\''
                    + ", source='" + source + '\''
                    + ", gangliaExtraDataInfoCollection=";

            for (GangliaExtraDataInfo dataInfo : gangliaExtraDataInfoCollection) {
                info += "\t " + dataInfo.toString() + "\n";
            }
            info += '}';
            return info;
        }
    }

    /**
     *
     * Author: Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at class
     * representing Ganglia extra information
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "EXTRA_DATA")
    public static class GangliaExtraDataInfo {

        @XmlElement(name = "EXTRA_ELEMENT")
        private Collection<GangliaExtraElementInfo> gangliaExtraElementInfo;

        {
            gangliaExtraElementInfo = new ArrayList<GangliaExtraElementInfo>();
        }

        public Collection<GangliaExtraElementInfo> getGangliaExtraElementInfo() {
            return gangliaExtraElementInfo;
        }

        public void setGangliaExtraElementInfo(Collection<GangliaExtraElementInfo> gangliaExtraElementInfo) {
            this.gangliaExtraElementInfo = gangliaExtraElementInfo;
        }

        @Override
        public String toString() {
            String info = "ExtraDataInfo{"
                    + "ExtraElementInfo=";
            for (GangliaExtraElementInfo elementInfo : gangliaExtraElementInfo) {
                info += "\t " + elementInfo + "\n";
            }
            info += '}';
            return info;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "EXTRA_ELEMENT")
    public static class GangliaExtraElementInfo {

        @XmlAttribute(name = "NAME")
        private String name;
        @XmlAttribute(name = "VAL")
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "ExtraElementInfo{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
        }
    }
}
