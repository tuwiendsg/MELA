/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.mela.common.elasticityAnalysis.concepts.elasticitySpace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class Statistics {

    /**
     *
     * @param behavior
     * @return the percentage from monitored values in which all elasticity
     * requirements are respected
     */
    public Double getOverallBoundaryFulillment(ElasticitySpace elasticitySpace) {

        int totalMonEntries = elasticitySpace.getSpaceEntries().size();
        int monEntriesFulfillingRequirements = 0;

        for (ElasticitySpace.ElasticitySpaceEntry entry : elasticitySpace.getSpaceEntries()) {
            if (entry.getAnalysisReport().isClean()) {
                monEntriesFulfillingRequirements++;
            }
        }

        return (100d * monEntriesFulfillingRequirements) / totalMonEntries;

    }
}
