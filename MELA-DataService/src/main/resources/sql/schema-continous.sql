create CACHED table IF NOT EXISTS MonitoringSeq (ID VARCHAR(200) PRIMARY KEY)
create CACHED table IF NOT EXISTS Timestamp (ID int IDENTITY, monSeqID VARCHAR(200), timestamp BIGINT, serviceStructure LONGVARCHAR, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID) )
create CACHED table IF NOT EXISTS RawCollectedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, metricName VARCHAR(100), metricUnit VARCHAR(100), metrictype VARCHAR(20), value VARCHAR(50),  monitoredElementID VARCHAR (50), monitoredElementLevel VARCHAR (50), FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID))
create CACHED table IF NOT EXISTS Configuration (ID int IDENTITY, configuration LONGVARCHAR)
create CACHED table IF NOT EXISTS AggregatedData (ID int IDENTITY, monSeqID VARCHAR(200), timestampID int, data OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) )
create CACHED table IF NOT EXISTS ElasticitySpace (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticitySpace OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) )
create CACHED table IF NOT EXISTS ElasticityPathway (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticityPathway OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) )
create CACHED table IF NOT EXISTS ELASTICITYDEPENDENCY (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, elasticityDependency LONGVARCHAR, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) )

