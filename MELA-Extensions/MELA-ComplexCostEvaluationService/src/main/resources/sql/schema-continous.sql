create CACHED table IF NOT EXISTS CaschedHistoricalUsage (monSeqID VARCHAR(200) PRIMARY KEY, timestampID int, data  OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create CACHED table IF NOT EXISTS InstantCostHistory (ID int PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );


