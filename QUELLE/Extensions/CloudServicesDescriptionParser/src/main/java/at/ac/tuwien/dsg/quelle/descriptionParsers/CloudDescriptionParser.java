/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.descriptionParsers;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public interface CloudDescriptionParser {

    CloudProvider getCloudProviderDescription();
}
