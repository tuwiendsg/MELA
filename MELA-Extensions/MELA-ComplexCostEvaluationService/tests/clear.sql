delete from INSTANTCOSTELASTICITYSPACE;
delete from CASCHEDCOMPLETEHISTORICALUSAGE where TIMESTAMPID > 17280;
delete from INSTANTCOSTHISTORY where TIMESTAMPID > 17280;
delete from STRUCTUREDCOLLECTEDDATA where TIMESTAMPID > 17280;
delete from AGGREGATEDDATA where TIMESTAMPID > 17280;
delete from TOTALCOSTHISTORY where TIMESTAMPID > 17280;
delete from TIMESTAMP where ID > 17280;




select MAX(ID) from (select ID from TIMESTAMP where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY' LIMIT 480);
select MAX(ID) from (select ID from TIMESTAMP where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED' LIMIT 480);


delete from INSTANTCOSTELASTICITYSPACE where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY';
delete from CASCHEDCOMPLETEHISTORICALUSAGE where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY' and TIMESTAMPID > 960 ;
delete from INSTANTCOSTHISTORY where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY' and TIMESTAMPID > 960;
delete from STRUCTUREDCOLLECTEDDATA where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY' and TIMESTAMPID > 960;
delete from AGGREGATEDDATA where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY' and TIMESTAMPID > 960;
delete from TOTALCOSTHISTORY where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY' and TIMESTAMPID > 960;
delete from TIMESTAMP where monseqid='EventProcessingTopology_STRATEGY_MELA_COST_RECOMMENDATION_EFFICIENCY' and ID > 960;
 
delete from INSTANTCOSTELASTICITYSPACE where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED';
delete from CASCHEDCOMPLETEHISTORICALUSAGE where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED' and TIMESTAMPID > 957 ;
delete from INSTANTCOSTHISTORY where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED' and TIMESTAMPID > 957;
delete from STRUCTUREDCOLLECTEDDATA where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED' and TIMESTAMPID > 957;
delete from AGGREGATEDDATA where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED' and TIMESTAMPID > 957;
delete from TOTALCOSTHISTORY where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED' and TIMESTAMPID > 957;
delete from TIMESTAMP where monseqid='EventProcessingTopology_STRATEGY_LAST_ADDED' and ID > 957;
 
