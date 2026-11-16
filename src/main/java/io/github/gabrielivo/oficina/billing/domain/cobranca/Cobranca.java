package io.github.gabrielivo.oficina.billing.domain.cobranca;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("cobrancas")
public class Cobranca {

    @Id
    private String id;

    private String osId;
    private String orcamentoId;
    private BigDecimal valor;
    private String idExterno;
    private String linkPagamento;
    private Instant expiraEm;
    private StatusCobranca status;
    private String pagamentoIdExterno;
    private String formaPagamento;
    private String motivo;
    private String motivoCancelamento;
    private String estornoIdExterno;
    private Instant criadaEm;
    private Instant atualizadaEm;

    protected Cobranca() {}

    public Cobranca(
            String osId,
            String orcamentoId,
            BigDecimal valor,
            ResultadoPreferenciaGateway preferencia,
            Instant expiraEm) {
        if (isBlank(osId)
                || isBlank(orcamentoId)
                || valor == null
                || valor.signum() <= 0
                || preferencia == null
                || isBlank(preferencia.idExterno())
                || isBlank(preferencia.linkPagamento())
                || expiraEm == null) {
            throw new IllegalArgumentException("Dados válidos de cobrança e preferência são obrigatórios.");
        }
        this.id = UUID.randomUUID().toString();
        this.osId = osId;
        this.orcamentoId = orcamentoId;
        this.valor = valor;
        this.idExterno = preferencia.idExterno();
        this.linkPagamento = preferencia.linkPagamento();
        this.expiraEm = expiraEm;
        this.status = StatusCobranca.PENDENTE;
        this.criadaEm = Instant.now();
        this.atualizadaEm = this.criadaEm;
    }

    public boolean atualizarPagamento(ResultadoPagamentoGateway pagamento) {
        if (pagamento == null || pagamento.status() == null) {
            throw new IllegalArgumentException("Resultado de pagamento é obrigatório.");
        }
        if (pagamento.valor() != null && valor.compareTo(pagamento.valor()) != 0) {
            throw new IllegalStateException("O valor confirmado pelo gateway diverge do valor da cobrança.");
        }
        if (pagamento.status() == StatusPagamentoGateway.APROVADO
                && (pagamento.valor() == null || isBlank(pagamento.pagamentoId()))) {
            throw new IllegalStateException("Pagamento aprovado deve informar valor e identificador do gateway.");
        }
        if (status == StatusCobranca.CANCELADA || status == StatusCobranca.ESTORNADA) {
            return false;
        }
        StatusCobranca novoStatus = switch (pagamento.status()) {
            case PENDENTE -> StatusCobranca.PENDENTE;
            case APROVADO -> StatusCobranca.APROVADA;
            case REJEITADO -> StatusCobranca.REJEITADA;
        };
        if (status == novoStatus) {
            return false;
        }
        if (status != StatusCobranca.PENDENTE) {
            throw new IllegalStateException("A cobrança já atingiu um estado terminal.");
        }
        this.status = novoStatus;
        this.pagamentoIdExterno = pagamento.pagamentoId();
        this.formaPagamento = pagamento.formaPagamento();
        this.motivo = pagamento.motivo();
        this.atualizadaEm = Instant.now();
        return novoStatus != StatusCobranca.PENDENTE;
    }

    public void cancelar(String motivo) {
        if (status == StatusCobranca.CANCELADA || status == StatusCobranca.REJEITADA) {
            return;
        }
        if (status != StatusCobranca.PENDENTE) {
            throw new IllegalStateException("Somente cobrança pendente pode ser cancelada.");
        }
        status = StatusCobranca.CANCELADA;
        motivoCancelamento = motivo;
        atualizadaEm = Instant.now();
    }

    public void estornar(String estornoIdExterno) {
        if (status == StatusCobranca.ESTORNADA) {
            return;
        }
        if (status != StatusCobranca.APROVADA) {
            throw new IllegalStateException("Somente cobrança aprovada pode ser estornada.");
        }
        if (isBlank(estornoIdExterno)) {
            throw new IllegalArgumentException("Identificador de estorno é obrigatório.");
        }
        this.estornoIdExterno = estornoIdExterno;
        status = StatusCobranca.ESTORNADA;
        atualizadaEm = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getOsId() {
        return osId;
    }

    public String getOrcamentoId() {
        return orcamentoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public String getLinkPagamento() {
        return linkPagamento;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public StatusCobranca getStatus() {
        return status;
    }

    public String getPagamentoIdExterno() {
        return pagamentoIdExterno;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public String getEstornoIdExterno() {
        return estornoIdExterno;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
