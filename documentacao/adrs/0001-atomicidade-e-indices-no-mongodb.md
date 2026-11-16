# ADR 0001 (Billing): Atomicidade estado + outbox e índices versionados no MongoDB

- Status: Aceita
- Data: 2026-11-16
- Relacionado: `oficina/documentacao/adrs/0007-mongodb-no-billing.md`, `0009-outbox-e-idempotencia.md` e
  `0012-transacoes-mongodb-billing.md` (decisão registrada também no repositório principal)

## Contexto

O Billing grava orçamento e cobrança no MongoDB e publica os eventos da Saga (Transactional Outbox, regra 3 do plano
da Fase 4). Precisamos que o estado, o registro do comando no inbox e os eventos do outbox sejam confirmados juntos.
O plano pede escolher entre transação Mongo (replica set) e evento gravado no próprio documento, e versionar os
índices (o MongoDB não tem Flyway).

## Decisão

1. **Transações multi-documento** (`MongoTransactionManager`, `@Transactional` nos serviços de application). Cada
   comando grava o resultado, o documento do inbox (`_id` = `messageId`) e as mensagens do outbox numa só transação.
   Respostas HTTP (resposta do orçamento, webhook) também gravam estado e outbox juntos. O MongoDB roda como replica
   set `rs0`, inclusive com um só membro (Compose local e StatefulSet do kind).
2. **E-mail também por outbox**: o pedido de envio é gravado na transação do orçamento e um publicador separado
   chama o SMTP depois do commit.
3. **Índices versionados no código**: `MongoIndexInitializer` (um `ApplicationRunner`) cria os índices de forma
   idempotente e registra a versão aplicada na coleção `schema_migrations`. Índice novo ou alterado = nova versão
   (constante `VERSION`) com as operações correspondentes; nunca alterar uma versão já aplicada.

## Alternativas consideradas

- **Evento embutido no documento de negócio** (sem transação): evita replica set, mas mistura o formato do evento
  com o documento, complica vários eventos por operação, o inbox e o reenvio.
- **MongoDB standalone**: não tem transação multi-documento.
- **Mongock/Liquibase-MongoDB para os índices**: dependência extra para meia dúzia de índices; o runner próprio
  cobre o caso com versão registrada e é testável com `MongoTemplate` mockado.
- **`spring.data.mongodb.auto-index-creation`**: cria índices a partir de anotações, sem versão nem controle de
  quando rodam; desaconselhado pelo próprio Spring Data em produção.

## Consequências

- Todo ambiente precisa de replica set (dev, testes de integração do Plano 2, kind).
- Entrega "pelo menos uma vez" para RabbitMQ e SMTP: consumidores idempotentes; um e-mail pode repetir se o processo
  cair entre o envio SMTP e a marcação do documento.
- Os testes do `verify` não sobem MongoDB: a lógica é testada com repositórios e `MongoTemplate` mockados e o
  contexto sobe com o inicializador de índices substituído (ADR 0013 do `oficina`). A transação real é validada no
  perfil `integracao`/Plano 2.
