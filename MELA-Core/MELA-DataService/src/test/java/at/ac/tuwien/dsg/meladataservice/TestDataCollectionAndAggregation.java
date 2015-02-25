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
package at.ac.tuwien.dsg.meladataservice;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesConfiguration;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.mela.common.utils.outputConverters.JsonConverter;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.mela.dataservice.dataSource.impl.DataAccessWithManualStructureManagement;
import at.ac.tuwien.dsg.mela.dataservice.qualityanalysis.impl.DefaultFreshnessAnalysisEngine;
import at.ac.tuwien.dsg.meladataservice.resources.FileBasedGangliaParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import org.hsqldb.server.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class TestDataCollectionAndAggregation {

    static final Logger log = LoggerFactory.getLogger(TestDataCollectionAndAggregation.class);

//    @Value("#{persistenceDelegate}")
    private PersistenceSQLAccess persistenceDelegate;

    public TestDataCollectionAndAggregation() {
    }

    @BeforeClass
    public static void setUpClass() {

    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        try {
             //run hsql in memory only for testing purposes
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl("jdbc:hsqldb:mem:mela-test-db");
            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");
 
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            //read content of sql schema
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader("src/main/resources/sql/schema.sql"));
            } catch (FileNotFoundException ex) {
                log.error(ex.getMessage(), ex);
                fail(ex.getMessage());
            }
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    jdbcTemplate.execute(line);
                }
            }

            persistenceDelegate = new PersistenceSQLAccess();
            persistenceDelegate.setDataSource(dataSource);
            persistenceDelegate.init();
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            fail(ex.getMessage());
        }

    }

    @After
    public void tearDown() {
    }

    /**
     * Tests the core functionality, i.e., converting monitoring snapshots and
     * applying composition rules, with analyzing freshness. Does NOT test data
     * collection adapters
     */
    @Test
    public void testDataCollectionAndAggregation() throws Exception {

        JAXBContext elementContext = JAXBContext.newInstance(MonitoredElement.class);

        MonitoredElement element = (MonitoredElement) elementContext.createUnmarshaller()
                .unmarshal(new FileReader(new File("src/test/java/resources/serviceDescription.xml")));

        {
            persistenceDelegate.writeMonitoringSequenceId(element.getId());

            JAXBContext rulesContext = JAXBContext.newInstance(CompositionRulesConfiguration.class);

            CompositionRulesConfiguration compositionRulesConfiguration = (CompositionRulesConfiguration) rulesContext.createUnmarshaller()
                    .unmarshal(new FileReader(new File("src/test/java/resources/compositionRules.xml")));

            compositionRulesConfiguration.setTargetServiceID(element.getId());

            persistenceDelegate.writeConfiguration(element.getId(), new ConfigurationXMLRepresentation(element, compositionRulesConfiguration, new Requirements()));
        }

        ConfigurationXMLRepresentation config = persistenceDelegate.getLatestConfiguration(element.getId());

        element = config.getServiceConfiguration();

        CompositionRulesConfiguration compositionRulesConfiguration = config.getCompositionRulesConfiguration();

        FileBasedGangliaParser basedGangliaParser = new FileBasedGangliaParser(new File("src/test/java/resources/monitoring_snapshot_1.xml"));

        MonitoringData snapshot1 = basedGangliaParser.getMonitoringData();

        DataAccessWithManualStructureManagement access = DataAccessWithManualStructureManagement.createInstance();
        access.addDataSource(basedGangliaParser);
        access.setDataFreshnessAnalysisEngine(new DefaultFreshnessAnalysisEngine());

        ServiceMonitoringSnapshot monitoringSnapshot;
        while (access.getFreshestMonitoredData().isEmpty()) {
            Thread.sleep(1000);
        }

        monitoringSnapshot = access.getStructuredMonitoredData(element);

        DataAggregationEngine aggregationEngine = new DataAggregationEngine();

        ServiceMonitoringSnapshot enrichedSnapshot = aggregationEngine.enrichMonitoringData(compositionRulesConfiguration, monitoringSnapshot);
         

        persistenceDelegate.writeInTimestamp("1", element, element.getId());
        persistenceDelegate.writeMonitoringData("1", monitoringSnapshot, element.getId());

        enrichedSnapshot = persistenceDelegate.extractLatestMonitoringData(element.getId());

        {

            Metric serviceCostMetric = new Metric("cost", "$/1000ops", Metric.MetricType.COST);
            MetricValue value = enrichedSnapshot.getMonitoredData(element).getMetricValue(serviceCostMetric);
            log.info(value.toString());
        }
        {
            MonitoredElement vm = new MonitoredElement("10.99.0.65").withLevel(MonitoredElement.MonitoredElementLevel.VM);

            Map<Metric, MetricValue> data = enrichedSnapshot.getMonitoredData(vm).getMonitoredData();

            for (Metric metric : data.keySet()) {
                MetricValue value = data.get(metric);
                log.info(metric.toString() + " Value: " + value.getValueRepresentation() + " Freshness: " + value.getFreshness());
            }
        }

        persistenceDelegate.removeMonitoringSequenceId(element.getId());

    }
}
