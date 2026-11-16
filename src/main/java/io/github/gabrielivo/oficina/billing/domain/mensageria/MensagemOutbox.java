package io.github.gabrielivo.oficina.billing.domain.mensageria;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("outbox")
public class MensagemOutbox {

    @Id
    private String id;

    private String type;
    private String exchange;
    private String routingKey;
    private String payload;
    private String traceParent;
    private Instant createdAt;
    private Instant sentAt;
    private int attempts;

    protected MensagemOutbox() {}

    public MensagemOutbox(String type, String exchange, String routingKey, String payload, String traceParent) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.traceParent = traceParent;
        this.createdAt = Instant.now();
    }

    public void markSent() {
        sentAt = Instant.now();
    }

    public void registerFailure() {
        attempts++;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getExchange() {
        return exchange;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public String getTraceParent() {
        return traceParent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public int getAttempts() {
        return attempts;
    }
}
