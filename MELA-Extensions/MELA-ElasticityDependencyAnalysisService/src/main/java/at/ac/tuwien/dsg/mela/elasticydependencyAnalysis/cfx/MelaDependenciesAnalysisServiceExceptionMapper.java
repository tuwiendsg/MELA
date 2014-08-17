package at.ac.tuwien.dsg.mela.elasticydependencyAnalysis.cfx;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;


/**
 * Created by omoser on 2/14/14.
 */
@Provider
@Component
public class MelaDependenciesAnalysisServiceExceptionMapper implements ExceptionMapper<Exception> {

    @PostConstruct
    public void init() {
        System.out.println("initialized");
    }

    public Response toResponse(Exception exception) {

//        return Response.serverError().entity("Internal Error: " + exception.getMessage()).build();
        exception.printStackTrace();
        return Response.serverError().entity("Internal Error: " + exception).build();
    }

    class ErrorResponse {

        private int code;

        private String message;

        private String documentationUri;

        @JsonProperty("error-code")
        public int getCode() {
            return code;
        }

        @JsonProperty("error-message")
        public String getMessage() {
            return message;
        }

        @JsonProperty("documentation-uri")
        public String getDocumentationUri() {
            return documentationUri;
        }

        public ErrorResponse withDocumentationUri(final String documentationUri) {
            this.documentationUri = documentationUri;
            return this;
        }

        public ErrorResponse withCode(final int code) {
            this.code = code;
            return this;
        }

        public ErrorResponse withMessage(final String message) {
            this.message = message;
            return this;
        }

    }
}
