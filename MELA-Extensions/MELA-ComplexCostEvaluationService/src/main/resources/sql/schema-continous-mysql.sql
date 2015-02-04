create table IF NOT EXISTS CaschedHistoricalUsage (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );
create table IF NOT EXISTS InstantCostHistory (ID int AUTO_INCREMENT PRIMARY KEY, monSeqID VARCHAR(200), timestampID int, data  LONGBLOB, FOREIGN KEY (monSeqID) REFERENCES MonitoringSeq(ID), FOREIGN KEY (timestampID) REFERENCES Timestamp(ID) );



