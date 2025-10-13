package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.domain.service.NotificationProcessingService;

import com.twilio.security.RequestValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/webhooks/twilio")
public class TwilioWebhookController {

    private final NotificationProcessingService notificationProcessingService;
    private final RequestValidator twilioRequestValidator;

    public TwilioWebhookController(NotificationProcessingService notificationProcessingService,
                                 @Value("${twilio.auth-token}") String twilioAuthToken) {
        this.notificationProcessingService = notificationProcessingService;
        this.twilioRequestValidator = new RequestValidator(twilioAuthToken);
    }


    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleStatusUpdate(@RequestParam Map<String, String> allRequestParams,
                                                 @RequestHeader("X-Twilio-Signature") String twilioSignature,
                                                 HttpServletRequest request) {
        
        // Constrói a URL do Twilio
        String url = request.getRequestURL().toString();
        
        // Pega os parâmetros da requisição (Twilio envia como form-urlencoded)
        Map<String, String> formParams = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));

        // Garante que a requisição veio mesmo do Twilio
        if (!twilioRequestValidator.validate(url, formParams, twilioSignature)) {
            return ResponseEntity.status(403).build();
        }

        String messageSid = allRequestParams.get("MessageSid");
        String messageStatus = allRequestParams.get("MessageStatus");

        if (messageSid != null && messageStatus != null) {
            notificationProcessingService.processarStatusDaMensagem(messageSid, messageStatus);
        }
        
        return ResponseEntity.ok().build();
    }
    
}