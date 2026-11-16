package io.github.gabrielivo.oficina.billing.domain.notificacao;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("notificacoes_email")
public class NotificacaoEmail {

    @Id
    private String id;

    private String destinatario;
    private String assunto;
    private String conteudo;
    private Instant criadaEm;
    private Instant enviadaEm;
    private int tentativas;
    private String ultimoErro;

    protected NotificacaoEmail() {}

    public NotificacaoEmail(String destinatario, String assunto, String conteudo) {
        if (isBlank(destinatario) || isBlank(assunto) || isBlank(conteudo)) {
            throw new IllegalArgumentException("Destinatário, assunto e conteúdo do e-mail são obrigatórios.");
        }
        this.id = UUID.randomUUID().toString();
        this.destinatario = destinatario;
        this.assunto = assunto;
        this.conteudo = conteudo;
        this.criadaEm = Instant.now();
    }

    public void marcarEnviada() {
        if (enviadaEm == null) {
            enviadaEm = Instant.now();
            ultimoErro = null;
        }
    }

    public void registrarFalha(String erro) {
        if (isBlank(erro)) {
            throw new IllegalArgumentException("A falha do envio deve ser identificada.");
        }
        tentativas++;
        ultimoErro = erro.length() > 1000 ? erro.substring(0, 1000) : erro;
    }

    public String getId() {
        return id;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getConteudo() {
        return conteudo;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }

    public int getTentativas() {
        return tentativas;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
