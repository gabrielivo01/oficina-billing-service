package io.github.gabrielivo.oficina.billing.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmail;
import io.github.gabrielivo.oficina.billing.domain.notificacao.NotificacaoEmailRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class PublicadorNotificacoesEmailTest {

    @Mock
    private NotificacaoEmailRepository repository;

    @Mock
    private JavaMailSender mailSender;

    @Test
    void falhaDeSmtpFicaPersistidaParaNovaTentativa() {
        var notificacao = new NotificacaoEmail("maria@example.com", "Orçamento", "Acesse sua oficina.");
        when(repository.findTop50ByEnviadaEmIsNullOrderByCriadaEmAsc()).thenReturn(List.of(notificacao));
        doThrow(new MailSendException("SMTP indisponível")).when(mailSender).send(any(SimpleMailMessage.class));
        var properties = new BillingProperties(null, null, new BillingProperties.Notificacao("oficina@example.com"));
        var publicador = new PublicadorNotificacoesEmail(repository, mailSender, properties);

        publicador.publicarPendentes();

        assertEquals(1, notificacao.getTentativas());
        assertEquals("SMTP indisponível", notificacao.getUltimoErro());
        verify(repository).save(notificacao);
    }
}
