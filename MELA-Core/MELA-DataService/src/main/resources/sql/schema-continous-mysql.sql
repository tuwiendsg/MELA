CREATE DATABASE IF NOT EXISTS mela;
use mela;


create table IF NOT EXISTS MonitoringSeq (ID VARCHAR(200) PRIMARY KEY);
create table IF NOT EXISTS Timestamp (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestamp BIGINT, serviceStructure LONGTEXT, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID) );
create table IF NOT EXISTS RawCollectedData (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, metricName VARCHAR(100), metricUnit VARCHAR(100), metrictype VARCHAR(20), value VARCHAR(50),  monitoredElementID VARCHAR (50), monitoredElementLevel VARCHAR (50), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID));
create table IF NOT EXISTS Configuration (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200),configuration LONGTEXT, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID));
create table IF NOT EXISTS AggregatedData (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create table IF NOT EXISTS StructuredCollectedData (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create table IF NOT EXISTS ElasticitySpace (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200),  startTimestampID int, endTimestampID int, elasticitySpace  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (startTimestampID) REFERENCES Timestamp(ID), FOREIGN KEY (endTimestampID) REFERENCES Timestamp(ID) );
create table IF NOT EXISTS ElasticityPathway (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticityPathway  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create table IF NOT EXISTS ElasticityDependency (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), startTimestampID int, endTimestampID int, elasticityDependency LONGTEXT, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (startTimestampID) REFERENCES Timestamp(ID), FOREIGN KEY (endTimestampID) REFERENCES Timestamp(ID) );
create table IF NOT EXISTS Events (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), event VARCHAR(200), flag VARCHAR(10));



