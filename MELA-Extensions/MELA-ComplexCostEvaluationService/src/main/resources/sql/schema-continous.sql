create CACHED table IF NOT EXISTS CaschedHistoricalUsage (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create CACHED table IF NOT EXISTS InstantCostHistory (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create CACHED table IF NOT EXISTS TotalCostHistory (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create CACHED table IF NOT EXISTS InstantCostElasticitySpace (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200),  startTimestampID int, endTimestampID int, elasticitySpace OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (startTimestampID) REFERENCES Timestamp(ID), FOREIGN KEY (endTimestampID) REFERENCES Timestamp(ID) )


