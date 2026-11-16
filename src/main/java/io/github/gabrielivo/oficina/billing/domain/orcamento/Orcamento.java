package io.github.gabrielivo.oficina.billing.domain.orcamento;

import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada;
import io.github.gabrielivo.oficina.billing.domain.IdentidadeAutenticada.TipoIdentidade;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("orcamentos")
public class Orcamento {

    @Id
    private String id;

    private String osId;
    private String sagaId;
    private ClienteSnapshot cliente;
    private VeiculoSnapshot veiculo;
    private List<ItemOrcamento> itens;
    private BigDecimal valorTotal;
    private StatusOrcamento status;
    private Instant criadoEm;
    private Instant respondidoEm;
    private String respondidoPorTipo;
    private String respondidoPorIdentificador;
    private String observacao;

    protected Orcamento() {}

    public Orcamento(
            String osId,
            String sagaId,
            ClienteSnapshot cliente,
            VeiculoSnapshot veiculo,
            List<ItemOrcamento> itens,
            BigDecimal valorTotal) {
        if (osId == null || osId.isBlank() || sagaId == null || sagaId.isBlank()) {
            throw new IllegalArgumentException("osId e sagaId são obrigatórios.");
        }
        if (cliente == null
                || isBlank(cliente.id())
                || isBlank(cliente.nome())
                || isBlank(cliente.cpf())
                || isBlank(cliente.email())) {
            throw new IllegalArgumentException("Cliente deve conter id, nome, CPF e e-mail.");
        }
        if (veiculo == null || isBlank(veiculo.placa()) || isBlank(veiculo.marca()) || isBlank(veiculo.modelo())) {
            throw new IllegalArgumentException("Veículo deve conter placa, marca e modelo.");
        }
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("O orçamento deve conter pelo menos um item.");
        }
        BigDecimal soma = itens.stream().map(ItemOrcamento::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (valorTotal == null || valorTotal.signum() <= 0 || soma.compareTo(valorTotal) != 0) {
            throw new IllegalArgumentException("O valor total deve corresponder à soma dos itens.");
        }
        this.id = UUID.randomUUID().toString();
        this.osId = osId;
        this.sagaId = sagaId;
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.itens = List.copyOf(itens);
        this.valorTotal = valorTotal;
        this.status = StatusOrcamento.PENDENTE;
        this.criadoEm = Instant.now();
    }

    public boolean responder(boolean aprovado, String observacao, IdentidadeAutenticada identidade) {
        if (identidade == null || isBlank(identidade.identificador()) || identidade.tipo() == null) {
            throw new IllegalArgumentException("Identidade autenticada é obrigatória.");
        }
        if (identidade.tipo() == TipoIdentidade.CLIENTE && !cliente.cpf().equals(identidade.identificador())) {
            throw new SecurityException("O cliente autenticado não é titular deste orçamento.");
        }
        StatusOrcamento novoStatus = aprovado ? StatusOrcamento.APROVADO : StatusOrcamento.RECUSADO;
        if (status != StatusOrcamento.PENDENTE) {
            if (status == novoStatus) {
                return false;
            }
            throw new IllegalStateException("O orçamento já recebeu uma resposta diferente.");
        }
        status = novoStatus;
        respondidoEm = Instant.now();
        respondidoPorTipo = identidade.tipo().name();
        respondidoPorIdentificador = identidade.identificador();
        this.observacao = observacao == null || observacao.isBlank() ? null : observacao.strip();
        return true;
    }

    public String getId() {
        return id;
    }

    public String getOsId() {
        return osId;
    }

    public String getSagaId() {
        return sagaId;
    }

    public ClienteSnapshot getCliente() {
        return cliente;
    }

    public VeiculoSnapshot getVeiculo() {
        return veiculo;
    }

    public List<ItemOrcamento> getItens() {
        return itens;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusOrcamento getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getRespondidoEm() {
        return respondidoEm;
    }

    public String getRespondidoPorTipo() {
        return respondidoPorTipo;
    }

    public String getRespondidoPorIdentificador() {
        return respondidoPorIdentificador;
    }

    public String getObservacao() {
        return observacao;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
