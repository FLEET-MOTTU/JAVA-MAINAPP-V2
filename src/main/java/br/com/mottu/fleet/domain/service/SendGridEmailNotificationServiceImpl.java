package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SendGridEmailNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailNotificationServiceImpl.class);

    private final String sendGridApiKey;
    private final String fromEmail;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SendGridEmailNotificationServiceImpl(@Value("${sendgrid.api.key}") String sendGridApiKey,
                                            @Value("${mail.from.address}") String fromEmail,
                                            ObjectMapper objectMapper) {
        this.sendGridApiKey = sendGridApiKey;
        this.fromEmail = fromEmail;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    @Override
    public void enviarMagicLink(Funcionario funcionario, String magicLinkUrl) {
        log.info("Tentando enviar e-mail de fallback via HttpClient nativo para: {}", funcionario.getEmail());
        
        try {
            String requestBody = objectMapper.writeValueAsString(new SendGridMail(
                new EmailAddress(this.fromEmail),
                "Seu Link de Acesso - F.L.E.E.T. Mottu",
                new Personalization[]{ new Personalization(new EmailAddress[]{ new EmailAddress(funcionario.getEmail()) })},
                new Content[]{ new Content("text/plain", formatarCorpoEmail(funcionario, magicLinkUrl)) }
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .header("Authorization", "Bearer " + this.sendGridApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("E-mail de fallback enviado com sucesso para: {}. Status: {}", funcionario.getEmail(), response.statusCode());
            } else {
                log.error("Falha ao enviar e-mail de fallback para {}. Status: {}. Body: {}",
                        funcionario.getEmail(), response.statusCode(), response.body());
            }

        } catch (Throwable t) {
            log.error("FALHA CATASTRÓFICA ao tentar enviar e-mail. Causa Raiz:", t);
        }
    }
    
    private String formatarCorpoEmail(Funcionario funcionario, String magicLinkUrl) {
        return String.format(
            "Olá %s,\n\n" +
            "Tivemos um problema ao enviar seu link de acesso via WhatsApp. Por favor, use o link abaixo. Ele é válido por 24 horas.\n\n" +
            "Seu link: %s\n\n" +
            "Atenciosamente,\nEquipe Mottu.",
            funcionario.getNome().split(" ")[0],
            magicLinkUrl);
    }
    
    // --- Records auxiliares para montar o JSON do SendGrid ---
    private record EmailAddress(String email) {}
    private record Content(String type, String value) {}
    private record Personalization(EmailAddress[] to) {}
    private record SendGridMail(EmailAddress from, String subject, Personalization[] personalizations, Content[] content) {}
}