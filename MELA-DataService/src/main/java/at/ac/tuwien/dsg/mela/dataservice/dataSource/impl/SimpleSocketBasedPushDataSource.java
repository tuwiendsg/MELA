/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.dsg.mela.dataservice.dataSource.impl;

import at.ac.tuwien.dsg.mela.common.exceptions.DataAccessException;
import at.ac.tuwien.dsg.mela.common.jaxbEntities.monitoringConcepts.MonitoringData;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.dataCollection.AbstractPushDataSource;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class SimpleSocketBasedPushDataSource extends AbstractPushDataSource{

    public SimpleSocketBasedPushDataSource() {
        
        
    }

    public MonitoringData getMonitoringData() throws DataAccessException {
        return freshestData;
    }

}
