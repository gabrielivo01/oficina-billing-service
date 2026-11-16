package io.github.gabrielivo.oficina.billing.infrastructure.notification;

import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmail;
import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublicadorNotificacoesEmail {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorNotificacoesEmail.class);
    private final NotificacaoEmailRepository repository;
    private final JavaMailSender mailSender;
    private final BillingProperties properties;

    public PublicadorNotificacoesEmail(
            NotificacaoEmailRepository repository, JavaMailSender mailSender, BillingProperties properties) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.notificacao.intervalo:5s}")
    public void publicarPendentes() {
        for (NotificacaoEmail notificacao : repository.findTop50ByEnviadaEmIsNullOrderByCriadaEmAsc()) {
            publicar(notificacao);
        }
    }

    private void publicar(NotificacaoEmail notificacao) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(properties.notificacao().remetente());
        mensagem.setTo(notificacao.getDestinatario());
        mensagem.setSubject(notificacao.getAssunto());
        mensagem.setText(notificacao.getConteudo());
        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            notificacao.registrarFalha(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            repository.save(notificacao);
            LOGGER.error(
                    "Falha ao enviar notificação {} para {}; será tentada novamente",
                    notificacao.getId(),
                    notificacao.getDestinatario(),
                    e);
            return;
        }
        notificacao.marcarEnviada();
        repository.save(notificacao);
    }
}
