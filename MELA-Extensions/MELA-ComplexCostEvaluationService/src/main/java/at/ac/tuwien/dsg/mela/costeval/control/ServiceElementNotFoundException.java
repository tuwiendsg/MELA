package at.ac.tuwien.dsg.mela.costeval.control;

import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;

/**
 * Created by omoser on 2/14/14.
 */
public class ServiceElementNotFoundException extends RuntimeException {
    public ServiceElementNotFoundException() {
    }

    public ServiceElementNotFoundException(String message) {
        super(message);
    }

    public ServiceElementNotFoundException(MonitoredElement element) {
        super("ServiceElement not found: " + element.toString());
    }
}
