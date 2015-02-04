create CACHED table IF NOT EXISTS CaschedHistoricalUsage (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create CACHED table IF NOT EXISTS InstantCostHistory (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );


