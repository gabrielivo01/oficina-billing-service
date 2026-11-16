package io.github.gabrielivo.oficina.billing.infrastructure.messaging;

import io.github.gabrielivo.oficina.billing.application.mensageria.ProcessadorMensagensService;
import io.github.gabrielivo.oficina.billing.domain.mensageria.EnvelopeMensagem;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ConsumidorComandos {

    private final JsonMapper jsonMapper;
    private final ProcessadorMensagensService processador;

    public ConsumidorComandos(JsonMapper jsonMapper, ProcessadorMensagensService processador) {
        this.jsonMapper = jsonMapper;
        this.processador = processador;
    }

    @RabbitListener(queues = "${app.mensageria.fila-comandos}")
    public void consumir(String payload) {
        try {
            processador.processar(jsonMapper.readValue(payload, EnvelopeMensagem.class));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Comando recebido não corresponde ao envelope esperado.", e);
        }
    }
}
