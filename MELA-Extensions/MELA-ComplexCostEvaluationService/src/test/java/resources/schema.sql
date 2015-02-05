create CACHED table IF NOT EXISTS CaschedHistoricalUsage (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER );
create CACHED table IF NOT EXISTS InstantCostHistory (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER );
create CACHED table IF NOT EXISTS TotalCostHistory (ID int IDENTITY PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  OTHER  );
