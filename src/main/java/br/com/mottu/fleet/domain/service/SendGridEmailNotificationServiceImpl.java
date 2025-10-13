package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Implementação do NotificationService que utiliza a API do SendGrid para enviar e-mails.
 * Esta classe não é a primária (@Primary), sendo destinada para uso como fallback.
 */
@Service
public class SendGridEmailNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailNotificationServiceImpl.class);
    private final SendGrid sendGridClient;
    private final String fromEmail;

    public SendGridEmailNotificationServiceImpl(@Value("${sendgrid.api.key}") String apiKey,
                                                @Value("${mail.from.address}") String fromEmail) {
        this.sendGridClient = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
    }

    /**
     * Implementação do envio de Magic Link via E-mail usando o SendGrid.
     * Constrói e envia um e-mail contendo a URL de acesso para o funcionário.
     * Falhas no envio são logadas, mas não lançam exceções para não interromper o fluxo principal.
     *
     * @param funcionario O objeto do funcionário que receberá o e-mail.
     * @param magicLinkUrl A URL completa do Magic Link a ser enviada.
     */
    @Override
    public void enviarMagicLink(Funcionario funcionario, String magicLinkUrl) {
        Email from = new Email(this.fromEmail);
        String subject = "Seu Link de Acesso - F.L.E.E.T. Mottu";
        Email to = new Email(funcionario.getEmail());
        String emailBody = String.format(
            "Olá %s,\n\n" +
            "Seu link de acesso ao F.L.E.E.T. está pronto. Use o link abaixo para seu primeiro acesso. Ele é válido por 24 horas.\n\n" +
            "Seu link: %s\n\n",
            funcionario.getNome().split(" ")[0],
            magicLinkUrl);
        Content content = new Content("text/plain", emailBody);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("E-mail com Magic Link enviado com sucesso para: {}", funcionario.getEmail());
            } else {
                log.error("Falha ao enviar e-mail para {}. Status: {}. Body: {}",
                        funcionario.getEmail(), response.getStatusCode(), response.getBody());
            }
        } catch (IOException ex) {
            log.error("Erro de IO ao enviar e-mail: {}", ex.getMessage());
        }
    }
}