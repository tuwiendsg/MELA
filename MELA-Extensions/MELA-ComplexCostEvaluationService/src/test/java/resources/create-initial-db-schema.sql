drop table IF EXISTS InstantCostElasticitySpace
drop table IF EXISTS TotalCostHistory
drop table IF EXISTS InstantCostHistory
drop table IF EXISTS CaschedHistoricalUsage
drop table IF EXISTS CaschedCompleteHistoricalUsage
drop table IF EXISTS Configuration
drop table IF EXISTS RawCollectedData
drop table IF EXISTS Timestamp
drop table IF EXISTS MonitoringSeq

create CACHED table IF NOT EXISTS MonitoringSeq (ID VARCHAR(200) PRIMARY KEY)
create CACHED table IF NOT EXISTS Timestamp (ID int IDENTITY, monSeqID VARCHAR(200), timestamp BIGINT, serviceStructure LONGVARCHAR )
create CACHED table IF NOT EXISTS RawCollectedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, metricName VARCHAR(100), metricUnit VARCHAR(100), metrictype VARCHAR(20), value VARCHAR(50),  monitoredElementID VARCHAR (50), monitoredElementLevel VARCHAR (50))
create CACHED table IF NOT EXISTS Configuration (ID int IDENTITY, monSeqID VARCHAR(200),configuration LONGVARCHAR);
create CACHED table IF NOT EXISTS CaschedHistoricalUsage (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER);
create CACHED table IF NOT EXISTS CaschedCompleteHistoricalUsage (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER);
create CACHED table IF NOT EXISTS InstantCostHistory (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER);
create CACHED table IF NOT EXISTS TotalCostHistory (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER);
create CACHED table IF NOT EXISTS InstantCostElasticitySpace (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200),  startTimestampID int, endTimestampID int, elasticitySpace OTHER)
create CACHED table IF NOT EXISTS InstantCostElasticityPathway (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200),  timestampID int, elasticityPathway OTHER )




