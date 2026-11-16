package io.github.gabrielivo.oficina.billing.domain.cobranca;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CobrancaRepository extends MongoRepository<Cobranca, String> {

    Optional<Cobranca> findByOsId(String osId);

    List<Cobranca> findTop100ByStatusOrderByCriadaEmAsc(StatusCobranca status);
}
