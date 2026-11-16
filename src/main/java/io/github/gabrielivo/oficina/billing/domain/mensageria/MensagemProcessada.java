package io.github.gabrielivo.oficina.billing.domain.mensageria;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inbox")
public class MensagemProcessada {

    @Id
    private String messageId;

    private Instant processedAt;

    protected MensagemProcessada() {}

    public MensagemProcessada(String messageId) {
        this.messageId = messageId;
        this.processedAt = Instant.now();
    }

    public String getMessageId() {
        return messageId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
