drop database mela;
create database mela;
use mela;

drop table IF EXISTS EVENTS;
drop table IF EXISTS ELASTICITYDEPENDENCY;
drop table IF EXISTS ElasticityPathway;
drop table IF EXISTS ElasticitySpace;
drop table IF EXISTS AggregatedData;
drop table IF EXISTS Configuration;
drop table IF EXISTS RawCollectedData;
drop table IF EXISTS Timestamp;
drop table IF EXISTS MonitoringSeq;

create table MonitoringSeq (ID VARCHAR(200) PRIMARY KEY);
create table Timestamp (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestamp BIGINT, serviceStructure LONGTEXT, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID) );
create table RawCollectedData (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, metricName VARCHAR(100), metricUnit VARCHAR(100), metrictype VARCHAR(20), value VARCHAR(50),  monitoredElementID VARCHAR (50), monitoredElementLevel VARCHAR (50), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID));
create table Configuration (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200),configuration LONGTEXT, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID));
create table AggregatedData (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create table ElasticitySpace (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200),  startTimestampID int, endTimestampID int, elasticitySpace  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (startTimestampID) REFERENCES Timestamp(ID), FOREIGN KEY (endTimestampID) REFERENCES Timestamp(ID) );
create table ElasticityPathway (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticityPathway  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create table ELASTICITYDEPENDENCY (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), startTimestampID int, endTimestampID int, elasticityDependency LONGTEXT, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (startTimestampID) REFERENCES Timestamp(ID), FOREIGN KEY (endTimestampID) REFERENCES Timestamp(ID) );
create table Events (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), event VARCHAR(200), flag VARCHAR(10));


