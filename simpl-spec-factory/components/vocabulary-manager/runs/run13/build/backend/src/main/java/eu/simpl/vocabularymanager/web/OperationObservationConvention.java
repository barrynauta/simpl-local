package eu.simpl.vocabularymanager.web;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/** Adds the operationId of the decided contract to the request metrics (D86). */
public class OperationObservationConvention extends DefaultServerRequestObservationConvention {

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        Object operation = context.getCarrier().getAttribute(RequestGateFilter.OPERATION_ATTRIBUTE);
        String value = operation == null ? "none" : operation.toString();
        return super.getLowCardinalityKeyValues(context).and(KeyValue.of("operationId", value));
    }
}
