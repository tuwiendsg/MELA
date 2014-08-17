drop table IF EXISTS ELASTICITYDEPENDENCY
drop table IF EXISTS ElasticityPathway
drop table IF EXISTS ElasticitySpace
drop table IF EXISTS AggregatedData
drop table IF EXISTS Configuration
drop table IF EXISTS RawCollectedData
drop table IF EXISTS Timestamp
drop table IF EXISTS MonitoringSeq

create CACHED table MonitoringSeq (ID VARCHAR(200) PRIMARY KEY)
create CACHED table Timestamp (ID int IDENTITY, monSeqID VARCHAR(200), timestamp BIGINT, serviceStructure LONGVARCHAR, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID) )
create CACHED table RawCollectedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, metricName VARCHAR(100), metricUnit VARCHAR(100), metrictype VARCHAR(20), value VARCHAR(50),  monitoredElementID VARCHAR (50), monitoredElementLevel VARCHAR (50), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID))
create CACHED table Configuration (ID int IDENTITY, monSeqID VARCHAR(200),configuration LONGVARCHAR, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID))
create CACHED table AggregatedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, data OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) )
create CACHED table ElasticitySpace (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200),  startTimestampID int, endTimestampID int, elasticitySpace OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (startTimestampID) REFERENCES Timestamp(ID), FOREIGN KEY (endTimestampID) REFERENCES Timestamp(ID) )
create CACHED table ElasticityPathway (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticityPathway OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) )
create CACHED table ELASTICITYDEPENDENCY (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), startTimestampID int, endTimestampID int, elasticityDependency LONGVARCHAR, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (startTimestampID) REFERENCES Timestamp(ID), FOREIGN KEY (endTimestampID) REFERENCES Timestamp(ID) )

