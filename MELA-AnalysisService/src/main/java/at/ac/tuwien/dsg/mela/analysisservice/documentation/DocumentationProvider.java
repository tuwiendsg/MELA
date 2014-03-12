package at.ac.tuwien.dsg.mela.analysisservice.documentation;

import org.springframework.stereotype.Component;

/**
 * Created by omoser on 2/14/14.
 */
@Component
public class DocumentationProvider {

    public String buildDocumentationUri(int statusCode) {
        return "/api-docs/"; // todo append documentation path depending on the status code
    }

}
