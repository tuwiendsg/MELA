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
 *
 */
package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.persistence;

import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticityDependencies.ServiceElasticityDependencies;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElSpaceDefaultFunction;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpace;
import at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace.ElasticitySpaceFunction;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service("ElasticyDependencyAnalysisPersistenceDelegate")
public class PersistenceDelegate {

    static final Logger log = LoggerFactory.getLogger(PersistenceDelegate.class);

    @Autowired
    private PersistenceSQLAccess persistenceSQLAccess;

    @Value("#{lobHandler}")
    private LobHandler lobHandler;

    public void writeElasticitySpace(ElasticitySpace elasticitySpace, String monitoringSequenceID) {
        try {
            persistenceSQLAccess.writeElasticitySpace(elasticitySpace, monitoringSequenceID);
        } catch (Exception e) {
            log.error("Space for " + monitoringSequenceID + " allready exists");
        }
    }

    public ElasticitySpace extractLatestElasticitySpace(String monitoringSequenceID) {

        ElasticitySpace space = persistenceSQLAccess.extractLatestElasticitySpace(monitoringSequenceID);

        //update space with new data
        ConfigurationXMLRepresentation cfg = this.getLatestConfiguration(monitoringSequenceID);

        if (cfg == null) {
            log.error("Retrieved empty configuration.");
            return null;
        } else if (cfg.getRequirements() == null) {
            log.error("Retrieved configuration does not contain Requirements.");
            return null;
        } else if (cfg.getServiceConfiguration() == null) {
            log.error("Retrieved configuration does not contain Service Configuration.");
            return null;
        }
        Requirements requirements = cfg.getRequirements();
        MonitoredElement serviceConfiguration = cfg.getServiceConfiguration();

        //if space == null, compute it 
        if (space == null) {

            //if space is null, compute it from all aggregated monitored data recorded so far
            List<ServiceMonitoringSnapshot> dataFromTimestamp = this.extractMonitoringData(monitoringSequenceID);

            ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
            fct.setRequirements(requirements);
            fct.trainElasticitySpace(dataFromTimestamp);
            space = fct.getElasticitySpace();

            //set to the new space the timespaceID of the last snapshot monitored data used to compute it
            space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

            //persist cached space
            this.writeElasticitySpace(space, monitoringSequenceID);
        } else {
            //else read max 1000 monitoring data records at a time, train space, and repeat as needed

            //if space is not null, update it with new data
            List<ServiceMonitoringSnapshot> dataFromTimestamp = null;

            //used to detect last snapshot timestamp, and then extratc new data until that timestamp
            ServiceMonitoringSnapshot monitoringSnapshot = this.extractLatestMonitoringData(monitoringSequenceID);

            Integer lastTimestampID = (monitoringSnapshot == null) ? Integer.MAX_VALUE : monitoringSnapshot.getTimestampID();

            //as this method retrieves in steps of 1000 the data to avoids killing the HSQL
            do {
                dataFromTimestamp = this.extractMonitoringData(space.getTimestampID(), monitoringSequenceID);

                if (dataFromTimestamp != null) {

                    //remove all data after monitoringSnapshot timestamp
                    Iterator<ServiceMonitoringSnapshot> it = dataFromTimestamp.iterator();
                    while (it.hasNext()) {

                        Integer timestampID = it.next().getTimestampID();
                        if (timestampID > lastTimestampID) {
                            it.remove();
                        }

                    }
                }
                //check if new data has been collected between elasticity space querries
                if (!dataFromTimestamp.isEmpty()) {
                    ElasticitySpaceFunction fct = new ElSpaceDefaultFunction(serviceConfiguration);
                    fct.setRequirements(requirements);
                    fct.trainElasticitySpace(space, dataFromTimestamp, requirements);
                    //set to the new space the timespaceID of the last snapshot monitored data used to compute it
                    space.setTimestampID(dataFromTimestamp.get(dataFromTimestamp.size() - 1).getTimestampID());

                }

            } while (!dataFromTimestamp.isEmpty());

            //persist cached space
            this.writeElasticitySpace(space, monitoringSequenceID);
        }

        return space;
    }

    public List<ServiceMonitoringSnapshot> extractLastXMonitoringDataSnapshots(int x, String monitoringSequenceID) {

        return persistenceSQLAccess.extractLastXMonitoringDataSnapshots(x, monitoringSequenceID);

    }

    public List<ServiceMonitoringSnapshot> extractMonitoringDataByTimeInterval(String startTime, String endTime, String monitoringSequenceID) {

        return persistenceSQLAccess.extractMonitoringDataByTimeInterval(startTime, endTime, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(int timestamp, String monitoringSequenceID) {
        return persistenceSQLAccess.extractMonitoringData(timestamp, monitoringSequenceID);
    }

    public List<ServiceMonitoringSnapshot> extractMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractMonitoringData(monitoringSequenceID);

    }

    public ServiceMonitoringSnapshot extractLatestMonitoringData(String monitoringSequenceID) {
        return persistenceSQLAccess.extractLatestMonitoringData(monitoringSequenceID);

    }

    public List<Metric> getAvailableMetrics(MonitoredElement monitoredElement, String monitoringSequenceID) {
        return persistenceSQLAccess.getAvailableMetrics(monitoredElement, monitoringSequenceID);
    }

    public ConfigurationXMLRepresentation getLatestConfiguration(String monitoringSequenceID) {
        return persistenceSQLAccess.getLatestConfiguration(monitoringSequenceID);

    }

    public ServiceElasticityDependencies extractLatestElasticityDependencies(String monitoringSequenceID) {
        JdbcTemplate jdbcTemplate = persistenceSQLAccess.getJdbcTemplate();
        String sql = "SELECT elasticityDependency from ElasticityDependency where monSeqID=? and ID=(SELECT MAX(ID) from ElasticityDependency where monSeqID=?);";

        RowMapper<ServiceElasticityDependencies> rowMapper = new RowMapper<ServiceElasticityDependencies>() {
            public ServiceElasticityDependencies mapRow(ResultSet rs, int rowNum) throws SQLException {
                ServiceElasticityDependencies dependencies = null;
                try {
                    Reader repr = rs.getClob(1).getCharacterStream();
                    JAXBContext context = JAXBContext.newInstance(ServiceElasticityDependencies.class);
                    dependencies = (ServiceElasticityDependencies) context.createUnmarshaller().unmarshal(repr);

                } catch (JAXBException ex) {
                    java.util.logging.Logger.getLogger(PersistenceSQLAccess.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                }
                return dependencies;
            }
        };

        List<ServiceElasticityDependencies> dependencies = jdbcTemplate.query(sql, rowMapper, monitoringSequenceID, monitoringSequenceID);
        if (dependencies.isEmpty()) {
            return null;
        } else {
            return dependencies.get(0);
        }
    }

    public void writeElasticityDependencies(String monitoringSequenceID, final ServiceElasticityDependencies dependencies) {

        JdbcTemplate jdbcTemplate = persistenceSQLAccess.getJdbcTemplate();

        String sql = "insert into ElasticityDependency (monSeqID, timestampID, elasticityDependency) " + "VALUES "
                + "( (select ID from MonitoringSeq where id='" + monitoringSequenceID + "')"
                + ", ? , ?)";

        try {
            JAXBContext context = JAXBContext.newInstance(ServiceElasticityDependencies.class);

            final StringWriter stringWriter = new StringWriter();
            context.createMarshaller().marshal(dependencies, stringWriter);

            PreparedStatementSetter preparedStatementSetter = new PreparedStatementSetter() {
                public void setValues(PreparedStatement ps) throws SQLException {
                    ps.setString(1, "" + dependencies.getTimestampID());
                    lobHandler.getLobCreator().setClobAsString(ps, 2, stringWriter.getBuffer().toString());
                }
            };

            jdbcTemplate.update(sql, preparedStatementSetter);

        } catch (JAXBException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
