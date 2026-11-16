package io.github.gabrielivo.oficina.billing.domain.mensageria;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MensagemOutboxRepository extends MongoRepository<MensagemOutbox, String> {

    List<MensagemOutbox> findBySentAtIsNullOrderByCreatedAtAsc(Pageable pageable);
}
