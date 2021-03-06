package at.ac.tuwien.dsg.mela.costeval.cxf;

import at.ac.tuwien.dsg.mela.costeval.control.ServiceElementNotFoundException;
import at.ac.tuwien.dsg.mela.costeval.documentation.DocumentationProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by omoser on 2/14/14.
 */
@Provider
@Component
public class MelaCostEvalServiceExceptionMapper implements ExceptionMapper<Exception> {

    static final Logger log = LoggerFactory.getLogger(MelaCostEvalServiceExceptionMapper.class);

    @Autowired
    DocumentationProvider documentationProvider;

    @PostConstruct
    public void init() {
        System.out.println("initialized");
    }

    public Response toResponse(Exception exception) {
        if (exception instanceof ServiceElementNotFoundException) {
            return Response
                    .status(NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new ErrorResponse()
                            .withCode(NOT_FOUND.getStatusCode())
                            .withMessage(exception.getMessage())
                            .withDocumentationUri(documentationProvider.buildDocumentationUri(NOT_FOUND.getStatusCode())))
                    .build();
        }

//        return Response.serverError().entity("Internal Error: " + exception.getMessage()).build();
        log.error("Internal error", exception.getMessage(), exception);
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
