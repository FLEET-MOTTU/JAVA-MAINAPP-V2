package br.com.mottu.fleet.config;

import com.sendgrid.SendGrid;
import com.twilio.Twilio;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Configuração centralizada para os clientes das APIs da Twilio (WhatsApp e SendGrid).
 */
@Configuration
public class TwilioConfig {

    private final String accountSid;
    private final String authToken;
    private final String sendGridApiKey;

    /**
     * Construtor que injeta todas as credenciais necessárias dos serviços Twilio do env.
     * @param accountSid O Account SID da sua conta Twilio.
     * @param authToken O Auth Token da sua conta Twilio.
     * @param sendGridApiKey A chave da API do serviço SendGrid.
     */
    public TwilioConfig(@Value("${twilio.account-sid}") String accountSid,
                          @Value("${twilio.auth-token}") String authToken,
                          @Value("${sendgrid.api.key}") String sendGridApiKey) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.sendGridApiKey = sendGridApiKey;
    }

    /**
     * Inicializa o cliente principal do Twilio (para WhatsApp/SMS) após a
     * construção do bean. Usar @PostConstruct é uma boa prática para lógicas de inicialização.
     */
    @PostConstruct
    public void initTwilio() {
        Twilio.init(this.accountSid, this.authToken);
    }


    /**
     * Cria e expõe o bean do cliente SendGrid.
     * @return Uma instância do cliente SendGrid configurada.
     */
    @Bean
    public SendGrid sendGridClient() {
        return new SendGrid(this.sendGridApiKey);
    }

}
