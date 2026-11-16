package io.github.gabrielivo.oficina.billing.infrastructure.messaging;

import io.github.gabrielivo.oficina.billing.application.mensageria.ContextoRastreamento;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class ContextoRastreamentoMicrometer implements ContextoRastreamento {

    private final Tracer tracer;
    private final Propagator propagator;

    public ContextoRastreamentoMicrometer(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Override
    public String traceParentAtual() {
        var span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        var headers = new HashMap<String, String>();
        propagator.inject(span.context(), headers, (carrier, key, value) -> carrier.put(key, value));
        return headers.get("traceparent");
    }
}
