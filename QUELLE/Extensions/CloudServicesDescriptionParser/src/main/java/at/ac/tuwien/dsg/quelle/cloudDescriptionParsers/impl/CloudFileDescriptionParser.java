/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.cloudDescriptionParsers.impl;

import at.ac.tuwien.dsg.quelle.descriptionParsers.CloudDescriptionParser;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
public class CloudFileDescriptionParser implements CloudDescriptionParser {

    static final org.slf4j.Logger log = LoggerFactory.getLogger(CloudFileDescriptionParser.class);

    private String descriptionFile;

    @Autowired
    private ApplicationContext context;

    public String getDescriptionFile() {
        return descriptionFile;
    }

    public void setDescriptionFile(String descriptionFile) {
        this.descriptionFile = descriptionFile;
    }

    public CloudProvider getCloudProviderDescription() {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(CloudProvider.class);
            InputStream fileStream = context.getResource(descriptionFile).getInputStream();
            return (CloudProvider) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        } catch (Exception ex) {
            log.error("Cannot unmarshall : {}", ex.getMessage());
            ex.printStackTrace();
            return new CloudProvider("empty");
        }
    }

    public CloudProvider getCloudProviderDescription(String descriptionFile) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(CloudProvider.class);
            InputStream fileStream = context.getResource(descriptionFile).getInputStream();
            return (CloudProvider) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        } catch (Exception ex) {
            log.error("Cannot unmarshall : {}", ex.getMessage());
            ex.printStackTrace();
            return new CloudProvider("empty");
        }
    }
}
