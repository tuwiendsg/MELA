/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.documentation;

import org.springframework.stereotype.Component;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */

@Component
public class DocumentationProvider {

    public String buildDocumentationUri(int statusCode) {
        return "/api-docs/"; // todo append documentation path depending on the status code
    }

}