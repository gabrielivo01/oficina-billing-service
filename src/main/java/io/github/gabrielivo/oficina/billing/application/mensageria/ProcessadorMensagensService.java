package io.github.gabrielivo.oficina.billing.application.mensageria;

import io.github.gabrielivo.oficina.billing.application.orcamento.GerarOrcamentoCommand;
import io.github.gabrielivo.oficina.billing.application.orcamento.OrcamentoService;
import io.github.gabrielivo.oficina.billing.application.pagamento.CobrancaService;
import io.github.gabrielivo.oficina.billing.application.pagamento.CriarCobrancaCommand;
import io.github.gabrielivo.oficina.billing.domain.mensageria.EnvelopeMensagem;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemProcessada;
import io.github.gabrielivo.oficina.billing.domain.mensageria.MensagemProcessadaRepository;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ClienteSnapshot;
import io.github.gabrielivo.oficina.billing.domain.orcamento.ItemOrcamento;
import io.github.gabrielivo.oficina.billing.domain.orcamento.VeiculoSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ProcessadorMensagensService {

    private final MensagemProcessadaRepository inbox;
    private final OrcamentoService orcamentoService;
    private final CobrancaService cobrancaService;
    private final JsonMapper jsonMapper;

    public ProcessadorMensagensService(
            MensagemProcessadaRepository inbox,
            OrcamentoService orcamentoService,
            CobrancaService cobrancaService,
            JsonMapper jsonMapper) {
        this.inbox = inbox;
        this.orcamentoService = orcamentoService;
        this.cobrancaService = cobrancaService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void processar(EnvelopeMensagem mensagem) {
        if (mensagem == null
                || mensagem.messageId() == null
                || mensagem.messageId().isBlank()) {
            throw new IllegalArgumentException("messageId é obrigatório.");
        }
        if (inbox.existsById(mensagem.messageId())) {
            return;
        }
        if (mensagem.type() == null
                || mensagem.type().isBlank()
                || mensagem.sagaId() == null
                || mensagem.sagaId().isBlank()
                || mensagem.osId() == null
                || mensagem.osId().isBlank()
                || mensagem.payload() == null) {
            throw new IllegalArgumentException("type, sagaId, osId e payload são obrigatórios.");
        }
        switch (mensagem.type()) {
            case "GerarOrcamento" -> orcamentoService.gerar(gerarOrcamento(mensagem));
            case "CriarCobranca" -> cobrancaService.criar(criarCobranca(mensagem));
            case "CancelarCobranca" ->
                cobrancaService.cancelar(
                        mensagem.osId(), texto(mensagem.payload(), "cobrancaId"), texto(mensagem.payload(), "motivo"));
            case "EstornarPagamento" ->
                cobrancaService.estornar(mensagem.osId(), texto(mensagem.payload(), "cobrancaId"));
            default -> throw new MensagemNaoSuportadaException(mensagem.type());
        }
        inbox.save(new MensagemProcessada(mensagem.messageId()));
    }

    private GerarOrcamentoCommand gerarOrcamento(EnvelopeMensagem mensagem) {
        Map<String, Object> payload = mensagem.payload();
        return new GerarOrcamentoCommand(
                mensagem.osId(),
                mensagem.sagaId(),
                converter(payload.get("cliente"), ClienteSnapshot.class),
                converter(payload.get("veiculo"), VeiculoSnapshot.class),
                converterLista(payload.get("itens"), ItemOrcamento.class),
                converter(payload.get("valorTotal"), java.math.BigDecimal.class));
    }

    private CriarCobrancaCommand criarCobranca(EnvelopeMensagem mensagem) {
        Map<String, Object> payload = mensagem.payload();
        return new CriarCobrancaCommand(
                mensagem.osId(),
                mensagem.sagaId(),
                texto(payload, "orcamentoId"),
                texto(payload, "descricao"),
                converter(payload.get("prazoPagamento"), Instant.class));
    }

    private <T> T converter(Object value, Class<T> targetType) {
        if (value == null) {
            throw new IllegalArgumentException("Campo obrigatório ausente no comando.");
        }
        return jsonMapper.convertValue(value, targetType);
    }

    private <T> List<T> converterLista(Object value, Class<T> targetType) {
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("O campo itens deve ser uma lista.");
        }
        return values.stream().map(item -> converter(item, targetType)).toList();
    }

    private String texto(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + field);
        }
        return text;
    }

    public static class MensagemNaoSuportadaException extends RuntimeException {
        public MensagemNaoSuportadaException(String type) {
            super("Comando não suportado pelo Billing Service: " + type);
        }
    }
}
