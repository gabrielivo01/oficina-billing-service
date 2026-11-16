package io.github.gabrielivo.oficina.billing.domain.notificacao;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificacaoEmailRepository extends MongoRepository<NotificacaoEmail, String> {

    List<NotificacaoEmail> findTop50ByEnviadaEmIsNullOrderByCriadaEmAsc();
}
