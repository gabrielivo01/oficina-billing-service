package io.github.gabrielivo.oficina.billing.domain.orcamento;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrcamentoRepository extends MongoRepository<Orcamento, String> {

    Optional<Orcamento> findByOsId(String osId);
}
