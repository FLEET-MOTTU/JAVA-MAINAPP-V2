package br.com.mottu.fleet.infrastructure.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.service.NotificationService;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação do NotificationService que utiliza a API do SendGrid para enviar e-mails.
 * Esta classe não é a primária (@Primary), sendo destinada para uso como fallback
 * quando a notificação via WhatsApp falha.
 */
@Service
public class SendGridEmailNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailNotificationServiceImpl.class);
    
    private final SendGrid sendGridClient;
    private final String fromEmail;

    public SendGridEmailNotificationServiceImpl(SendGrid sendGridClient,
                                                @Value("${mail.from.address}") String fromEmail) {
        this.sendGridClient = sendGridClient;
        this.fromEmail = fromEmail;
    }


    /**
     * Implementação do envio de Magic Link via E-mail usando o SendGrid.
     * Constrói e envia um e-mail transacional contendo a URL de acesso para o funcionário.
     *
     * @param funcionario O objeto do funcionário que receberá o e-mail.
     * @param magicLinkUrl A URL completa do Magic Link a ser enviada.
     */
    @Override
    public void enviarMagicLink(Funcionario funcionario, String magicLinkUrl) {
        log.info("Tentando enviar e-mail de fallback via SendGrid para: {}", funcionario.getEmail());

        Email from = new Email(this.fromEmail);
        String subject = "Seu Link de Acesso - F.L.E.E.T. Mottu";
        Email to = new Email(funcionario.getEmail());
        String emailBody = String.format(
            "Olá %s,\n\n" +
            "Tivemos um problema ao enviar seu link de acesso via WhatsApp. Por favor, use o link abaixo para seu primeiro acesso. Ele é válido por 24 horas.\n\n" +
            "Seu link: %s\n\n" +
            "Atenciosamente,\nEquipe Mottu.",
            funcionario.getNome().split(" ")[0],
            magicLinkUrl);
        Content content = new Content("text/plain", emailBody);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        try {
            // 1. Define a chamada para a API do SendGrid
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);
            
            // 2. Loga o resultado da tentativa de envio
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("E-mail de fallback enviado com sucesso para: {}. Status: {}", funcionario.getEmail(), response.getStatusCode());
            } else {
                // Se o SendGrid recusar, loga a falha. Isso aciona o EmailFailureListener.
                log.error("Falha ao enviar e-mail de fallback para {}. Status: {}. Body: {}",
                        funcionario.getEmail(), response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            log.error("FALHA CATASTRÓFICA ao tentar enviar e-mail. Causa Raiz:", e);
        }
    }
}