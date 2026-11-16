package io.github.gabrielivo.oficina.billing.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class MongoIndexInitializer implements ApplicationRunner {

    private static final String VERSION = "2026111601";
    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Query versionQuery = Query.query(Criteria.where("_id").is(VERSION));
        if (mongoTemplate.exists(versionQuery, "schema_migrations")) {
            return;
        }
        mongoTemplate
                .indexOps("orcamentos")
                .ensureIndex(new Index().on("osId", Sort.Direction.ASC).unique());
        mongoTemplate
                .indexOps("cobrancas")
                .ensureIndex(new Index().on("osId", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps("cobrancas").ensureIndex(new Index().on("status", Sort.Direction.ASC));
        mongoTemplate
                .indexOps("outbox")
                .ensureIndex(new Index().on("sentAt", Sort.Direction.ASC).on("createdAt", Sort.Direction.ASC));
        mongoTemplate
                .indexOps("notificacoes_email")
                .ensureIndex(new Index().on("enviadaEm", Sort.Direction.ASC).on("criadaEm", Sort.Direction.ASC));
        mongoTemplate.upsert(
                versionQuery, new Update().setOnInsert("appliedAt", java.time.Instant.now()), "schema_migrations");
    }
}
