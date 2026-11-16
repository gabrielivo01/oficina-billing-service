package io.github.gabrielivo.oficina.billing.presentation.orcamento;

import io.github.gabrielivo.oficina.billing.domain.orcamento.Orcamento;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoMapper {

    public OrcamentoResponse paraResponse(Orcamento orcamento) {
        var cliente = orcamento.getCliente();
        var veiculo = orcamento.getVeiculo();
        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getOsId(),
                new OrcamentoResponse.Cliente(
                        cliente.id(), cliente.nome(), cliente.cpf(), cliente.telefone(), cliente.email()),
                new OrcamentoResponse.Veiculo(veiculo.placa(), veiculo.marca(), veiculo.modelo(), veiculo.ano()),
                orcamento.getItens().stream()
                        .map(item -> new OrcamentoResponse.Item(
                                item.tipo(),
                                item.descricao(),
                                item.pecaId(),
                                item.quantidade(),
                                item.valorUnitario(),
                                item.valorTotal()))
                        .toList(),
                orcamento.getValorTotal(),
                orcamento.getStatus(),
                orcamento.getCriadoEm(),
                orcamento.getRespondidoEm(),
                orcamento.getRespondidoPorTipo(),
                orcamento.getObservacao());
    }
}
