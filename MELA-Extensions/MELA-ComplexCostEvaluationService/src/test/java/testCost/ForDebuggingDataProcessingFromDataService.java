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
package testCost;

import at.ac.tuwien.dsg.mela.common.configuration.metricComposition.CompositionRulesBlock;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.configuration.ConfigurationXMLRepresentation;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.ServiceMonitoringSnapshot;
import at.ac.tuwien.dsg.mela.common.persistence.PersistenceSQLAccess;
import at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngine;
import at.ac.tuwien.dsg.mela.costeval.engines.CostEvalEngineTest;
import at.ac.tuwien.dsg.mela.costeval.model.CostEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.model.LifetimeEnrichedSnapshot;
import at.ac.tuwien.dsg.mela.costeval.persistence.PersistenceDelegate;
import at.ac.tuwien.dsg.mela.costeval.utils.conversion.CostJSONConverter;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudOfferedService;
import at.ac.tuwien.dsg.mela.dataservice.aggregation.DataAggregationEngine;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class ForDebuggingDataProcessingFromDataService {

    static final Logger log = LoggerFactory.getLogger(ForDebuggingDataProcessingFromDataService.class);

//    @Value("#{persistenceDelegate}")
    private PersistenceDelegate persistenceDelegate;
    private at.ac.tuwien.dsg.mela.dataservice.persistence.PersistenceDelegate dataAccessPersistenceDelegate;

    private PersistenceSQLAccess generalAccess;

    public ForDebuggingDataProcessingFromDataService() {
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
            dataSource.setUrl("jdbc:hsqldb:hsql://localhost:9001/mela;hsqldb.cache_rows=100;hsqldb.log_data=false");
            dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
            dataSource.setUsername("sa");
            dataSource.setPassword("");

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            //read content of sql schema
            BufferedReader reader = null;
            try {

                reader = new BufferedReader(new FileReader("/home/daniel-tuwien/Documents/TUW_GIT/MELA/MELA-Extensions/MELA-ComplexCostEvaluationService/src/main/resources/sql/schema-continous.sql"));
            } catch (FileNotFoundException ex) {
                java.util.logging.Logger.getLogger(CostEvalEngineTest.class.getName()).log(Level.SEVERE, null, ex);
                fail(ex.getMessage());
            }
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    jdbcTemplate.execute(line);
                }
            }

            persistenceDelegate = new PersistenceDelegate();
            persistenceDelegate.setDataSource(dataSource);
            persistenceDelegate.setJdbcTemplate(jdbcTemplate);

            generalAccess = new PersistenceSQLAccess().withDataSource(dataSource).withJdbcTemplate(jdbcTemplate);

            persistenceDelegate.setPersistenceSQLAccess(generalAccess);

        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(CostEvalEngineTest.class.getName()).log(Level.SEVERE, null, ex);
            fail(ex.getMessage());
        }

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of applyCompositionRules method, of class CostEvalEngine.
     */
    @Test
    public void testProcessing() throws Exception {

        CostEvalEngine costEvalEngine = new CostEvalEngine();
        DataAggregationEngine aggregationEngine = new DataAggregationEngine();
        costEvalEngine.setInstantMonitoringDataEnrichmentEngine(aggregationEngine);

        String serviceID = "Service";

        Date before = new Date();

        //if service DI not found
        ConfigurationXMLRepresentation cfg = persistenceDelegate.getLatestConfiguration(serviceID);

        if (cfg == null) {
            log.debug("Service ID {} not found", serviceID);
            return;
        }

        LifetimeEnrichedSnapshot previouselyDeterminedUsage = persistenceDelegate.extractTotalUsageWithCompleteHistoricalStructureSnapshot(serviceID);

        int lastRetrievedTimestampID = (previouselyDeterminedUsage != null) ? previouselyDeterminedUsage.getLastUpdatedTimestampID() : 0;

        List<ServiceMonitoringSnapshot> allMonData = persistenceDelegate.extractMonitoringData(lastRetrievedTimestampID, serviceID);

        if (!allMonData.isEmpty()) {

            //as I extract 1000 entries at a time to avoid memory overflow, I need to read the rest
            do {
                lastRetrievedTimestampID = allMonData.get(allMonData.size() - 1).getTimestampID();
                List<ServiceMonitoringSnapshot> restOfData = persistenceDelegate.extractMonitoringData(lastRetrievedTimestampID, serviceID);
                if (restOfData.isEmpty()) {
                    break;
                } else {
                    allMonData.addAll(restOfData);
                }
            } while (true);
        }

        final List<CloudProvider> cloudProviders = new ArrayList<>();

        //test we can read what was generated
        {
            JAXBContext jAXBContext = JAXBContext.newInstance(CloudProvider.class);
            InputStream fileStream = new FileInputStream(new File("config/default/Flexiant.xml"));
            CloudProvider cloudProvider = (CloudProvider) jAXBContext.createUnmarshaller().unmarshal(fileStream);
            cloudProviders.add(cloudProvider);
        }

        if (cloudProviders == null) {
            log.debug("No cloud providers found in repository. Cannot compute cost");
            return;
        }

        Map<UUID, Map<UUID, CloudOfferedService>> cloudProvidersMap = costEvalEngine.cloudProvidersToMap(cloudProviders);

        log.debug("Updating usage and instant cost for {} snapshots", allMonData.size());

        for (ServiceMonitoringSnapshot monitoringSnapshot : allMonData) {
            //update total usage so far and persist

            //compute total usage so far
            previouselyDeterminedUsage = costEvalEngine.updateTotalUsageSoFarWithCompleteStructureIncludingServicesASVMTypes(cloudProvidersMap, previouselyDeterminedUsage, monitoringSnapshot);

            //persist the total usage
//            persistenceDelegate.persistTotalUsageWithCompleteHistoricalStructureSnapshot(serviceID, previouselyDeterminedUsage);
            //as the previous method has also the currently unused services, we must remove them for computing instant cost
            LifetimeEnrichedSnapshot cleanedCostSnapshot = costEvalEngine.cleanUnusedServices(previouselyDeterminedUsage);

            //compute composition rules to create instant cost based on total usage so far
            CompositionRulesBlock block = costEvalEngine.createCompositionRulesForInstantUsageCostIncludingServicesASVMTypes(cloudProvidersMap, cleanedCostSnapshot.getSnapshot().getMonitoredService(), cleanedCostSnapshot, monitoringSnapshot.getTimestamp());
            ServiceMonitoringSnapshot enrichedSnapshot = costEvalEngine.applyCompositionRules(block, costEvalEngine.convertToStructureIncludingServicesASVMTypes(cloudProvidersMap, monitoringSnapshot));

            CostJSONConverter converter = new CostJSONConverter();

//        totalCostEnrichedSnapshot.getMonitoredData().remove(MonitoredElement.MonitoredElementLevel.VM);
            log.info("");
            log.info("Radial : instantCost");
            log.info("");
            log.info(converter.toJSONForRadialPieChart(enrichedSnapshot));
            log.info("Tree view : instant");
            log.info("");
            log.info(new CostJSONConverter().convertMonitoringSnapshotAndCompositionRules(enrichedSnapshot, block));
            log.info("");

            //persist instant cost
//            persistenceDelegate.persistInstantCostSnapshot(serviceID, new CostEnrichedSnapshot().withCostCompositionRules(block)
//                    .withLastUpdatedTimestampID(enrichedSnapshot.getTimestampID()).withSnapshot(enrichedSnapshot));
            //retrieve the previousely computed total usage, as the computation of the instant cost destr
//            previouselyDeterminedUsage = persistenceDelegate.extractCachedServiceUsage(serviceID);
            //create rules for metrics for total cost based on usage so far
            CompositionRulesBlock totalCostBlock = costEvalEngine.createCompositionRulesForTotalCostIncludingServicesASVMTypes(cloudProvidersMap, previouselyDeterminedUsage, monitoringSnapshot.getTimestamp());
            ServiceMonitoringSnapshot snapshotWithTotalCost = costEvalEngine.applyCompositionRules(totalCostBlock, previouselyDeterminedUsage.getSnapshot());

            log.info("");
            log.info("Radial : totalCost");
            log.info("");
            log.info(converter.toJSONForRadialPieChart(snapshotWithTotalCost));
            log.info("Tree view : totalCost");
            log.info("");
            log.info(converter.convertMonitoringSnapshotAndCompositionRules(snapshotWithTotalCost, totalCostBlock));
            log.info("");

//            persist mon snapshot enriched with total cost
//            persistenceDelegate.persistTotalCostSnapshot(serviceID, new CostEnrichedSnapshot().withCostCompositionRules(totalCostBlock)
//                    .withLastUpdatedTimestampID(snapshotWithTotalCost.getTimestampID()).withSnapshot(snapshotWithTotalCost));
        }

        Date after = new Date();
        log.debug("UpdateAndCacheEvaluatedServiceUsageWithCurrentStructure time in ms:  " + new Date(after.getTime() - before.getTime()).getTime());

    }

}
